package com.android.dx.ssa;

import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.ssa.SsaInsn.Visitor;
import com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public final class SsaMethod {
    private boolean backMode = false;
    private ArrayList<SsaBasicBlock> blocks;
    private int borrowedSpareRegisters;
    private SsaInsn[] definitionList;
    private int entryBlockIndex;
    private int exitBlockIndex;
    private final boolean isStatic;
    private int maxLabel;
    private final int paramWidth;
    private int registerCount;
    private int spareRegisterBase;
    private List<SsaInsn>[] unmodifiableUseList;
    private ArrayList<SsaInsn>[] useList;

    public static SsaMethod newFromRopMethod(RopMethod ropMethod, int paramWidth, boolean isStatic) {
        SsaMethod result = new SsaMethod(ropMethod, paramWidth, isStatic);
        result.convertRopToSsaBlocks(ropMethod);
        return result;
    }

    private SsaMethod(RopMethod ropMethod, int paramWidth, boolean isStatic) {
        this.paramWidth = paramWidth;
        this.isStatic = isStatic;
        this.maxLabel = ropMethod.getBlocks().getMaxLabel();
        this.registerCount = ropMethod.getBlocks().getRegCount();
        this.spareRegisterBase = this.registerCount;
    }

    static BitSet bitSetFromLabelList(BasicBlockList blocks, IntList labelList) {
        BitSet result = new BitSet(blocks.size());
        int sz = labelList.size();
        for (int i = 0; i < sz; i++) {
            result.set(blocks.indexOfLabel(labelList.get(i)));
        }
        return result;
    }

    public static IntList indexListFromLabelList(BasicBlockList ropBlocks, IntList labelList) {
        IntList result = new IntList(labelList.size());
        int sz = labelList.size();
        for (int i = 0; i < sz; i++) {
            result.add(ropBlocks.indexOfLabel(labelList.get(i)));
        }
        return result;
    }

    private void convertRopToSsaBlocks(RopMethod rmeth) {
        int sz = rmeth.getBlocks().size();
        this.blocks = new ArrayList(sz + 2);
        for (int i = 0; i < sz; i++) {
            this.blocks.add(SsaBasicBlock.newFromRop(rmeth, i, this));
        }
        this.entryBlockIndex = ((SsaBasicBlock) this.blocks.get(rmeth.getBlocks().indexOfLabel(rmeth.getFirstLabel()))).insertNewPredecessor().getIndex();
        this.exitBlockIndex = -1;
    }

    void makeExitBlock() {
        if (this.exitBlockIndex >= 0) {
            throw new RuntimeException("must be called at most once");
        }
        this.exitBlockIndex = this.blocks.size();
        int i = this.exitBlockIndex;
        int i2 = this.maxLabel;
        this.maxLabel = i2 + 1;
        SsaBasicBlock exitBlock = new SsaBasicBlock(i, i2, this);
        this.blocks.add(exitBlock);
        Iterator it = this.blocks.iterator();
        while (it.hasNext()) {
            ((SsaBasicBlock) it.next()).exitBlockFixup(exitBlock);
        }
        if (exitBlock.getPredecessors().cardinality() == 0) {
            this.blocks.remove(this.exitBlockIndex);
            this.exitBlockIndex = -1;
            this.maxLabel--;
        }
    }

    private static SsaInsn getGoto(SsaBasicBlock block) {
        return new NormalSsaInsn(new PlainInsn(Rops.GOTO, SourcePosition.NO_INFO, null, RegisterSpecList.EMPTY), block);
    }

    public SsaBasicBlock makeNewGotoBlock() {
        int newIndex = this.blocks.size();
        int i = this.maxLabel;
        this.maxLabel = i + 1;
        SsaBasicBlock newBlock = new SsaBasicBlock(newIndex, i, this);
        newBlock.getInsns().add(getGoto(newBlock));
        this.blocks.add(newBlock);
        return newBlock;
    }

    public int getEntryBlockIndex() {
        return this.entryBlockIndex;
    }

    public SsaBasicBlock getEntryBlock() {
        return (SsaBasicBlock) this.blocks.get(this.entryBlockIndex);
    }

    public int getExitBlockIndex() {
        return this.exitBlockIndex;
    }

    public SsaBasicBlock getExitBlock() {
        return this.exitBlockIndex < 0 ? null : (SsaBasicBlock) this.blocks.get(this.exitBlockIndex);
    }

    public int blockIndexToRopLabel(int bi) {
        if (bi < 0) {
            return -1;
        }
        return ((SsaBasicBlock) this.blocks.get(bi)).getRopLabel();
    }

    public int getRegCount() {
        return this.registerCount;
    }

    public int getParamWidth() {
        return this.paramWidth;
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    public int borrowSpareRegister(int category) {
        int result = this.spareRegisterBase + this.borrowedSpareRegisters;
        this.borrowedSpareRegisters += category;
        this.registerCount = Math.max(this.registerCount, result + category);
        return result;
    }

    public void returnSpareRegisters() {
        this.borrowedSpareRegisters = 0;
    }

    public ArrayList<SsaBasicBlock> getBlocks() {
        return this.blocks;
    }

    public BitSet computeReachability() {
        int size = this.blocks.size();
        BitSet reachableUnvisited = new BitSet(size);
        BitSet reachableVisited = new BitSet(size);
        reachableUnvisited.set(getEntryBlock().getIndex());
        while (true) {
            int index = reachableUnvisited.nextSetBit(0);
            if (index == -1) {
                return reachableVisited;
            }
            reachableVisited.set(index);
            reachableUnvisited.or(((SsaBasicBlock) this.blocks.get(index)).getSuccessors());
            reachableUnvisited.andNot(reachableVisited);
        }
    }

    public void mapRegisters(RegisterMapper mapper) {
        Iterator it = getBlocks().iterator();
        while (it.hasNext()) {
            Iterator it2 = ((SsaBasicBlock) it.next()).getInsns().iterator();
            while (it2.hasNext()) {
                ((SsaInsn) it2.next()).mapRegisters(mapper);
            }
        }
        this.registerCount = mapper.getNewRegisterCount();
        this.spareRegisterBase = this.registerCount;
    }

    public SsaInsn getDefinitionForRegister(int reg) {
        if (this.backMode) {
            throw new RuntimeException("No def list in back mode");
        } else if (this.definitionList != null) {
            return this.definitionList[reg];
        } else {
            this.definitionList = new SsaInsn[getRegCount()];
            forEachInsn(new Visitor() {
                public void visitMoveInsn(NormalSsaInsn insn) {
                    SsaMethod.this.definitionList[insn.getResult().getReg()] = insn;
                }

                public void visitPhiInsn(PhiInsn phi) {
                    SsaMethod.this.definitionList[phi.getResult().getReg()] = phi;
                }

                public void visitNonMoveInsn(NormalSsaInsn insn) {
                    if (insn.getResult() != null) {
                        SsaMethod.this.definitionList[insn.getResult().getReg()] = insn;
                    }
                }
            });
            return this.definitionList[reg];
        }
    }

    private void buildUseList() {
        if (this.backMode) {
            throw new RuntimeException("No use list in back mode");
        }
        int i;
        this.useList = new ArrayList[this.registerCount];
        for (i = 0; i < this.registerCount; i++) {
            this.useList[i] = new ArrayList();
        }
        forEachInsn(new Visitor() {
            public void visitMoveInsn(NormalSsaInsn insn) {
                addToUses(insn);
            }

            public void visitPhiInsn(PhiInsn phi) {
                addToUses(phi);
            }

            public void visitNonMoveInsn(NormalSsaInsn insn) {
                addToUses(insn);
            }

            private void addToUses(SsaInsn insn) {
                RegisterSpecList rl = insn.getSources();
                int sz = rl.size();
                for (int i = 0; i < sz; i++) {
                    SsaMethod.this.useList[rl.get(i).getReg()].add(insn);
                }
            }
        });
        this.unmodifiableUseList = new List[this.registerCount];
        for (i = 0; i < this.registerCount; i++) {
            this.unmodifiableUseList[i] = Collections.unmodifiableList(this.useList[i]);
        }
    }

    void onSourceChanged(SsaInsn insn, RegisterSpec oldSource, RegisterSpec newSource) {
        if (this.useList != null) {
            if (oldSource != null) {
                this.useList[oldSource.getReg()].remove(insn);
            }
            int reg = newSource.getReg();
            if (this.useList.length <= reg) {
                this.useList = null;
            } else {
                this.useList[reg].add(insn);
            }
        }
    }

    void onSourcesChanged(SsaInsn insn, RegisterSpecList oldSources) {
        if (this.useList != null) {
            if (oldSources != null) {
                removeFromUseList(insn, oldSources);
            }
            RegisterSpecList sources = insn.getSources();
            int szNew = sources.size();
            for (int i = 0; i < szNew; i++) {
                this.useList[sources.get(i).getReg()].add(insn);
            }
        }
    }

    private void removeFromUseList(SsaInsn insn, RegisterSpecList oldSources) {
        if (oldSources != null) {
            int szNew = oldSources.size();
            int i = 0;
            while (i < szNew) {
                if (this.useList[oldSources.get(i).getReg()].remove(insn)) {
                    i++;
                } else {
                    throw new RuntimeException("use not found");
                }
            }
        }
    }

    void onInsnAdded(SsaInsn insn) {
        onSourcesChanged(insn, null);
        updateOneDefinition(insn, null);
    }

    void onInsnRemoved(SsaInsn insn) {
        if (this.useList != null) {
            removeFromUseList(insn, insn.getSources());
        }
        RegisterSpec resultReg = insn.getResult();
        if (this.definitionList != null && resultReg != null) {
            this.definitionList[resultReg.getReg()] = null;
        }
    }

    public void onInsnsChanged() {
        this.definitionList = null;
        this.useList = null;
        this.unmodifiableUseList = null;
    }

    void updateOneDefinition(SsaInsn insn, RegisterSpec oldResult) {
        if (this.definitionList != null) {
            if (oldResult != null) {
                this.definitionList[oldResult.getReg()] = null;
            }
            RegisterSpec resultReg = insn.getResult();
            if (resultReg != null) {
                if (this.definitionList[resultReg.getReg()] != null) {
                    throw new RuntimeException("Duplicate add of insn");
                }
                this.definitionList[resultReg.getReg()] = insn;
            }
        }
    }

    public List<SsaInsn> getUseListForRegister(int reg) {
        if (this.unmodifiableUseList == null) {
            buildUseList();
        }
        return this.unmodifiableUseList[reg];
    }

    public ArrayList<SsaInsn>[] getUseListCopy() {
        if (this.useList == null) {
            buildUseList();
        }
        ArrayList<SsaInsn>[] useListCopy = new ArrayList[this.registerCount];
        for (int i = 0; i < this.registerCount; i++) {
            useListCopy[i] = new ArrayList(this.useList[i]);
        }
        return useListCopy;
    }

    public boolean isRegALocal(RegisterSpec spec) {
        SsaInsn defn = getDefinitionForRegister(spec.getReg());
        if (defn == null) {
            return false;
        }
        if (defn.getLocalAssignment() != null) {
            return true;
        }
        for (SsaInsn use : getUseListForRegister(spec.getReg())) {
            Insn insn = use.getOriginalRopInsn();
            if (insn != null && insn.getOpcode().getOpcode() == 54) {
                return true;
            }
        }
        return false;
    }

    void setNewRegCount(int newRegCount) {
        this.registerCount = newRegCount;
        this.spareRegisterBase = this.registerCount;
        onInsnsChanged();
    }

    public int makeNewSsaReg() {
        int reg = this.registerCount;
        this.registerCount = reg + 1;
        this.spareRegisterBase = this.registerCount;
        onInsnsChanged();
        return reg;
    }

    public void forEachInsn(Visitor visitor) {
        Iterator it = this.blocks.iterator();
        while (it.hasNext()) {
            ((SsaBasicBlock) it.next()).forEachInsn(visitor);
        }
    }

    public void forEachPhiInsn(PhiInsn.Visitor v) {
        Iterator it = this.blocks.iterator();
        while (it.hasNext()) {
            ((SsaBasicBlock) it.next()).forEachPhiInsn(v);
        }
    }

    public void forEachBlockDepthFirst(boolean reverse, SsaBasicBlock.Visitor v) {
        BitSet visited = new BitSet(this.blocks.size());
        Stack<SsaBasicBlock> stack = new Stack();
        SsaBasicBlock rootBlock = reverse ? getExitBlock() : getEntryBlock();
        if (rootBlock != null) {
            stack.add(null);
            stack.add(rootBlock);
            while (stack.size() > 0) {
                SsaBasicBlock cur = (SsaBasicBlock) stack.pop();
                SsaBasicBlock parent = (SsaBasicBlock) stack.pop();
                if (!visited.get(cur.getIndex())) {
                    BitSet children = reverse ? cur.getPredecessors() : cur.getSuccessors();
                    for (int i = children.nextSetBit(0); i >= 0; i = children.nextSetBit(i + 1)) {
                        stack.add(cur);
                        stack.add(this.blocks.get(i));
                    }
                    visited.set(cur.getIndex());
                    v.visitBlock(cur, parent);
                }
            }
        }
    }

    public void forEachBlockDepthFirstDom(SsaBasicBlock.Visitor v) {
        BitSet visited = new BitSet(getBlocks().size());
        Stack<SsaBasicBlock> stack = new Stack();
        stack.add(getEntryBlock());
        while (stack.size() > 0) {
            SsaBasicBlock cur = (SsaBasicBlock) stack.pop();
            ArrayList<SsaBasicBlock> curDomChildren = cur.getDomChildren();
            if (!visited.get(cur.getIndex())) {
                for (int i = curDomChildren.size() - 1; i >= 0; i--) {
                    stack.add((SsaBasicBlock) curDomChildren.get(i));
                }
                visited.set(cur.getIndex());
                v.visitBlock(cur, null);
            }
        }
    }

    public void deleteInsns(Set<SsaInsn> deletedInsns) {
        for (SsaInsn deletedInsn : deletedInsns) {
            int i;
            SsaBasicBlock block = deletedInsn.getBlock();
            ArrayList<SsaInsn> insns = block.getInsns();
            for (i = insns.size() - 1; i >= 0; i--) {
                SsaInsn insn = (SsaInsn) insns.get(i);
                if (deletedInsn == insn) {
                    onInsnRemoved(insn);
                    insns.remove(i);
                    break;
                }
            }
            int insnsSz = insns.size();
            SsaInsn lastInsn;
            if (insnsSz == 0) {
                lastInsn = null;
            } else {
                lastInsn = (SsaInsn) insns.get(insnsSz - 1);
            }
            if (block != getExitBlock() && (insnsSz == 0 || lastInsn.getOriginalRopInsn() == null || lastInsn.getOriginalRopInsn().getOpcode().getBranchingness() == 1)) {
                insns.add(SsaInsn.makeFromRop(new PlainInsn(Rops.GOTO, SourcePosition.NO_INFO, null, RegisterSpecList.EMPTY), block));
                BitSet succs = block.getSuccessors();
                for (i = succs.nextSetBit(0); i >= 0; i = succs.nextSetBit(i + 1)) {
                    if (i != block.getPrimarySuccessorIndex()) {
                        block.removeSuccessor(i);
                    }
                }
            }
        }
    }

    public void setBackMode() {
        this.backMode = true;
        this.useList = null;
        this.definitionList = null;
    }
}
