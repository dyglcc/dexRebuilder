package com.android.dx.ssa;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.ssa.SsaInsn.Visitor;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;

public class DeadCodeRemover {
    private final int regCount;
    private final SsaMethod ssaMeth;
    private final ArrayList<SsaInsn>[] useList = this.ssaMeth.getUseListCopy();
    private final BitSet worklist = new BitSet(this.regCount);

    private static class NoSideEffectVisitor implements Visitor {
        BitSet noSideEffectRegs;

        public NoSideEffectVisitor(BitSet noSideEffectRegs) {
            this.noSideEffectRegs = noSideEffectRegs;
        }

        public void visitMoveInsn(NormalSsaInsn insn) {
            if (!DeadCodeRemover.hasSideEffect(insn)) {
                this.noSideEffectRegs.set(insn.getResult().getReg());
            }
        }

        public void visitPhiInsn(PhiInsn phi) {
            if (!DeadCodeRemover.hasSideEffect(phi)) {
                this.noSideEffectRegs.set(phi.getResult().getReg());
            }
        }

        public void visitNonMoveInsn(NormalSsaInsn insn) {
            RegisterSpec result = insn.getResult();
            if (!DeadCodeRemover.hasSideEffect(insn) && result != null) {
                this.noSideEffectRegs.set(result.getReg());
            }
        }
    }

    public static void process(SsaMethod ssaMethod) {
        new DeadCodeRemover(ssaMethod).run();
    }

    private DeadCodeRemover(SsaMethod ssaMethod) {
        this.ssaMeth = ssaMethod;
        this.regCount = ssaMethod.getRegCount();
    }

    private void run() {
        pruneDeadInstructions();
        HashSet<SsaInsn> deletedInsns = new HashSet();
        this.ssaMeth.forEachInsn(new NoSideEffectVisitor(this.worklist));
        while (true) {
            int regV = this.worklist.nextSetBit(0);
            if (regV >= 0) {
                this.worklist.clear(regV);
                if (this.useList[regV].size() == 0 || isCircularNoSideEffect(regV, null)) {
                    SsaInsn insnS = this.ssaMeth.getDefinitionForRegister(regV);
                    if (!deletedInsns.contains(insnS)) {
                        RegisterSpecList sources = insnS.getSources();
                        int sz = sources.size();
                        for (int i = 0; i < sz; i++) {
                            RegisterSpec source = sources.get(i);
                            this.useList[source.getReg()].remove(insnS);
                            if (!hasSideEffect(this.ssaMeth.getDefinitionForRegister(source.getReg()))) {
                                this.worklist.set(source.getReg());
                            }
                        }
                        deletedInsns.add(insnS);
                    }
                }
            } else {
                this.ssaMeth.deleteInsns(deletedInsns);
                return;
            }
        }
    }

    private void pruneDeadInstructions() {
        HashSet<SsaInsn> deletedInsns = new HashSet();
        BitSet reachable = this.ssaMeth.computeReachability();
        ArrayList<SsaBasicBlock> blocks = this.ssaMeth.getBlocks();
        int blockIndex = 0;
        while (true) {
            blockIndex = reachable.nextClearBit(blockIndex);
            if (blockIndex < blocks.size()) {
                SsaBasicBlock block = (SsaBasicBlock) blocks.get(blockIndex);
                blockIndex++;
                for (int i = 0; i < block.getInsns().size(); i++) {
                    SsaInsn insn = (SsaInsn) block.getInsns().get(i);
                    RegisterSpecList sources = insn.getSources();
                    int sourcesSize = sources.size();
                    if (sourcesSize != 0) {
                        deletedInsns.add(insn);
                    }
                    for (int j = 0; j < sourcesSize; j++) {
                        this.useList[sources.get(j).getReg()].remove(insn);
                    }
                    RegisterSpec result = insn.getResult();
                    if (result != null) {
                        Iterator it = this.useList[result.getReg()].iterator();
                        while (it.hasNext()) {
                            SsaInsn use = (SsaInsn) it.next();
                            if (use instanceof PhiInsn) {
                                ((PhiInsn) use).removePhiRegister(result);
                            }
                        }
                    }
                }
            } else {
                this.ssaMeth.deleteInsns(deletedInsns);
                return;
            }
        }
    }

    private boolean isCircularNoSideEffect(int regV, BitSet set) {
        if (set != null && set.get(regV)) {
            return true;
        }
        Iterator it = this.useList[regV].iterator();
        while (it.hasNext()) {
            if (hasSideEffect((SsaInsn) it.next())) {
                return false;
            }
        }
        if (set == null) {
            set = new BitSet(this.regCount);
        }
        set.set(regV);
        it = this.useList[regV].iterator();
        while (it.hasNext()) {
            RegisterSpec result = ((SsaInsn) it.next()).getResult();
            if (result != null) {
                if (!isCircularNoSideEffect(result.getReg(), set)) {
                }
            }
            return false;
        }
        return true;
    }

    private static boolean hasSideEffect(SsaInsn insn) {
        if (insn == null) {
            return true;
        }
        return insn.hasSideEffect();
    }
}
