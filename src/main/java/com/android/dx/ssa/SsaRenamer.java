package com.android.dx.ssa;

import com.android.dx.rop.code.LocalItem;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.type.Type;
import com.android.dx.ssa.SsaInsn.Visitor;
import com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class SsaRenamer implements Runnable {
    private static final boolean DEBUG = false;
    private int nextSsaReg;
    private final int ropRegCount;
    private final SsaMethod ssaMeth;
    private final ArrayList<LocalItem> ssaRegToLocalItems;
    private IntList ssaRegToRopReg;
    private final RegisterSpec[][] startsForBlocks;
    private int threshold;

    private class BlockRenamer implements Visitor {
        private final SsaBasicBlock block;
        private final RegisterSpec[] currentMapping;
        private final HashMap<SsaInsn, SsaInsn> insnsToReplace = new HashMap();
        private final RenamingMapper mapper = new RenamingMapper();
        private final HashSet<SsaInsn> movesToKeep = new HashSet();

        private class RenamingMapper extends RegisterMapper {
            public int getNewRegisterCount() {
                return SsaRenamer.this.nextSsaReg;
            }

            public RegisterSpec map(RegisterSpec registerSpec) {
                if (registerSpec == null) {
                    return null;
                }
                return registerSpec.withReg(BlockRenamer.this.currentMapping[registerSpec.getReg()].getReg());
            }
        }

        BlockRenamer(SsaBasicBlock block) {
            this.block = block;
            this.currentMapping = SsaRenamer.this.startsForBlocks[block.getIndex()];
            SsaRenamer.this.startsForBlocks[block.getIndex()] = null;
        }

        public void process() {
            this.block.forEachInsn(this);
            updateSuccessorPhis();
            ArrayList<SsaInsn> insns = this.block.getInsns();
            for (int i = insns.size() - 1; i >= 0; i--) {
                SsaInsn insn = (SsaInsn) insns.get(i);
                SsaInsn replaceInsn = (SsaInsn) this.insnsToReplace.get(insn);
                if (replaceInsn != null) {
                    insns.set(i, replaceInsn);
                } else if (insn.isNormalMoveInsn() && !this.movesToKeep.contains(insn)) {
                    insns.remove(i);
                }
            }
            boolean first = true;
            Iterator it = this.block.getDomChildren().iterator();
            while (it.hasNext()) {
                SsaBasicBlock child = (SsaBasicBlock) it.next();
                if (child != this.block) {
                    RegisterSpec[] childStart;
                    if (first) {
                        childStart = this.currentMapping;
                    } else {
                        childStart = SsaRenamer.dupArray(this.currentMapping);
                    }
                    SsaRenamer.this.startsForBlocks[child.getIndex()] = childStart;
                    first = false;
                }
            }
        }

        private void addMapping(int ropReg, RegisterSpec ssaReg) {
            int i;
            int ssaRegNum = ssaReg.getReg();
            LocalItem ssaRegLocal = ssaReg.getLocalItem();
            this.currentMapping[ropReg] = ssaReg;
            for (i = this.currentMapping.length - 1; i >= 0; i--) {
                if (ssaRegNum == this.currentMapping[i].getReg()) {
                    this.currentMapping[i] = ssaReg;
                }
            }
            if (ssaRegLocal != null) {
                SsaRenamer.this.setNameForSsaReg(ssaReg);
                for (i = this.currentMapping.length - 1; i >= 0; i--) {
                    RegisterSpec cur = this.currentMapping[i];
                    if (ssaRegNum != cur.getReg() && ssaRegLocal.equals(cur.getLocalItem())) {
                        this.currentMapping[i] = cur.withLocalItem(null);
                    }
                }
            }
        }

        public void visitPhiInsn(PhiInsn phi) {
            processResultReg(phi);
        }

        public void visitMoveInsn(NormalSsaInsn insn) {
            LocalItem newLocal;
            boolean onlyOneAssociatedLocal;
            RegisterSpec ropResult = insn.getResult();
            int ropResultReg = ropResult.getReg();
            int ropSourceReg = insn.getSources().get(0).getReg();
            insn.mapSourceRegisters(this.mapper);
            int ssaSourceReg = insn.getSources().get(0).getReg();
            LocalItem sourceLocal = this.currentMapping[ropSourceReg].getLocalItem();
            LocalItem resultLocal = ropResult.getLocalItem();
            if (resultLocal == null) {
                newLocal = sourceLocal;
            } else {
                newLocal = resultLocal;
            }
            LocalItem associatedLocal = SsaRenamer.this.getLocalForNewReg(ssaSourceReg);
            if (associatedLocal == null || newLocal == null || newLocal.equals(associatedLocal)) {
                onlyOneAssociatedLocal = true;
            } else {
                onlyOneAssociatedLocal = false;
            }
            RegisterSpec ssaReg = RegisterSpec.makeLocalOptional(ssaSourceReg, ropResult.getType(), newLocal);
            if (!Optimizer.getPreserveLocals() || (onlyOneAssociatedLocal && SsaRenamer.equalsHandlesNulls(newLocal, sourceLocal) && SsaRenamer.this.threshold == 0)) {
                addMapping(ropResultReg, ssaReg);
            } else if (onlyOneAssociatedLocal && sourceLocal == null && SsaRenamer.this.threshold == 0) {
                this.insnsToReplace.put(insn, SsaInsn.makeFromRop(new PlainInsn(Rops.opMarkLocal(ssaReg), SourcePosition.NO_INFO, null, RegisterSpecList.make(RegisterSpec.make(ssaReg.getReg(), ssaReg.getType(), newLocal))), this.block));
                addMapping(ropResultReg, ssaReg);
            } else {
                processResultReg(insn);
                this.movesToKeep.add(insn);
            }
        }

        public void visitNonMoveInsn(NormalSsaInsn insn) {
            insn.mapSourceRegisters(this.mapper);
            processResultReg(insn);
        }

        void processResultReg(SsaInsn insn) {
            RegisterSpec ropResult = insn.getResult();
            if (ropResult != null) {
                int ropReg = ropResult.getReg();
                if (!SsaRenamer.this.isBelowThresholdRegister(ropReg)) {
                    insn.changeResultReg(SsaRenamer.this.nextSsaReg);
                    addMapping(ropReg, insn.getResult());
                    SsaRenamer.this.nextSsaReg = SsaRenamer.this.nextSsaReg + 1;
                }
            }
        }

        private void updateSuccessorPhis() {
            PhiInsn.Visitor visitor = new PhiInsn.Visitor() {
                public void visitPhiInsn(PhiInsn insn) {
                    int ropReg = insn.getRopResultReg();
                    if (!SsaRenamer.this.isBelowThresholdRegister(ropReg)) {
                        RegisterSpec stackTop = BlockRenamer.this.currentMapping[ropReg];
                        if (!SsaRenamer.this.isVersionZeroRegister(stackTop.getReg())) {
                            insn.addPhiOperand(stackTop, BlockRenamer.this.block);
                        }
                    }
                }
            };
            BitSet successors = this.block.getSuccessors();
            for (int i = successors.nextSetBit(0); i >= 0; i = successors.nextSetBit(i + 1)) {
                ((SsaBasicBlock) SsaRenamer.this.ssaMeth.getBlocks().get(i)).forEachPhiInsn(visitor);
            }
        }
    }

    public SsaRenamer(SsaMethod ssaMeth) {
        this.ropRegCount = ssaMeth.getRegCount();
        this.ssaMeth = ssaMeth;
        this.nextSsaReg = this.ropRegCount;
        this.threshold = 0;
        this.startsForBlocks = new RegisterSpec[ssaMeth.getBlocks().size()][];
        this.ssaRegToLocalItems = new ArrayList();
        RegisterSpec[] initialRegMapping = new RegisterSpec[this.ropRegCount];
        for (int i = 0; i < this.ropRegCount; i++) {
            initialRegMapping[i] = RegisterSpec.make(i, Type.VOID);
        }
        this.startsForBlocks[ssaMeth.getEntryBlockIndex()] = initialRegMapping;
    }

    public SsaRenamer(SsaMethod ssaMeth, int thresh) {
        this(ssaMeth);
        this.threshold = thresh;
    }

    public void run() {
        this.ssaMeth.forEachBlockDepthFirstDom(new SsaBasicBlock.Visitor() {
            public void visitBlock(SsaBasicBlock block, SsaBasicBlock unused) {
                new BlockRenamer(block).process();
            }
        });
        this.ssaMeth.setNewRegCount(this.nextSsaReg);
        this.ssaMeth.onInsnsChanged();
    }

    private static RegisterSpec[] dupArray(RegisterSpec[] orig) {
        RegisterSpec[] copy = new RegisterSpec[orig.length];
        System.arraycopy(orig, 0, copy, 0, orig.length);
        return copy;
    }

    private LocalItem getLocalForNewReg(int ssaReg) {
        if (ssaReg < this.ssaRegToLocalItems.size()) {
            return (LocalItem) this.ssaRegToLocalItems.get(ssaReg);
        }
        return null;
    }

    private void setNameForSsaReg(RegisterSpec ssaReg) {
        int reg = ssaReg.getReg();
        LocalItem local = ssaReg.getLocalItem();
        this.ssaRegToLocalItems.ensureCapacity(reg + 1);
        while (this.ssaRegToLocalItems.size() <= reg) {
            this.ssaRegToLocalItems.add(null);
        }
        this.ssaRegToLocalItems.set(reg, local);
    }

    private boolean isBelowThresholdRegister(int ssaReg) {
        return ssaReg < this.threshold;
    }

    private boolean isVersionZeroRegister(int ssaReg) {
        return ssaReg < this.ropRegCount;
    }

    private static boolean equalsHandlesNulls(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }
}
