package com.android.dx.rop.code;

import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.util.Hex;
import com.android.dx.util.IntList;
import com.android.dx.util.LabeledList;

public final class BasicBlockList extends LabeledList {
    private int regCount;

    private static class RegCountVisitor implements Visitor {
        private int regCount = 0;

        public int getRegCount() {
            return this.regCount;
        }

        public void visitPlainInsn(PlainInsn insn) {
            visit(insn);
        }

        public void visitPlainCstInsn(PlainCstInsn insn) {
            visit(insn);
        }

        public void visitSwitchInsn(SwitchInsn insn) {
            visit(insn);
        }

        public void visitThrowingCstInsn(ThrowingCstInsn insn) {
            visit(insn);
        }

        public void visitThrowingInsn(ThrowingInsn insn) {
            visit(insn);
        }

        public void visitFillArrayDataInsn(FillArrayDataInsn insn) {
            visit(insn);
        }

        public void visitInvokePolymorphicInsn(InvokePolymorphicInsn insn) {
            visit(insn);
        }

        private void visit(Insn insn) {
            RegisterSpec result = insn.getResult();
            if (result != null) {
                processReg(result);
            }
            RegisterSpecList sources = insn.getSources();
            int sz = sources.size();
            for (int i = 0; i < sz; i++) {
                processReg(sources.get(i));
            }
        }

        private void processReg(RegisterSpec spec) {
            int reg = spec.getNextReg();
            if (reg > this.regCount) {
                this.regCount = reg;
            }
        }
    }

    public BasicBlockList(int size) {
        super(size);
        this.regCount = -1;
    }

    private BasicBlockList(BasicBlockList old) {
        super((LabeledList) old);
        this.regCount = old.regCount;
    }

    public BasicBlock get(int n) {
        return (BasicBlock) get0(n);
    }

    public void set(int n, BasicBlock bb) {
        super.set(n, bb);
        this.regCount = -1;
    }

    public int getRegCount() {
        if (this.regCount == -1) {
            RegCountVisitor visitor = new RegCountVisitor();
            forEachInsn(visitor);
            this.regCount = visitor.getRegCount();
        }
        return this.regCount;
    }

    public int getInstructionCount() {
        int sz = size();
        int result = 0;
        for (int i = 0; i < sz; i++) {
            BasicBlock one = (BasicBlock) getOrNull0(i);
            if (one != null) {
                result += one.getInsns().size();
            }
        }
        return result;
    }

    public int getEffectiveInstructionCount() {
        int sz = size();
        int result = 0;
        for (int i = 0; i < sz; i++) {
            BasicBlock one = (BasicBlock) getOrNull0(i);
            if (one != null) {
                InsnList insns = one.getInsns();
                int insnsSz = insns.size();
                for (int j = 0; j < insnsSz; j++) {
                    if (insns.get(j).getOpcode().getOpcode() != 54) {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    public BasicBlock labelToBlock(int label) {
        int idx = indexOfLabel(label);
        if (idx >= 0) {
            return get(idx);
        }
        throw new IllegalArgumentException("no such label: " + Hex.u2(label));
    }

    public void forEachInsn(Visitor visitor) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            get(i).getInsns().forEach(visitor);
        }
    }

    public BasicBlockList withRegisterOffset(int delta) {
        int sz = size();
        BasicBlockList result = new BasicBlockList(sz);
        for (int i = 0; i < sz; i++) {
            BasicBlock one = (BasicBlock) get0(i);
            if (one != null) {
                result.set(i, one.withRegisterOffset(delta));
            }
        }
        if (isImmutable()) {
            result.setImmutable();
        }
        return result;
    }

    public BasicBlockList getMutableCopy() {
        return new BasicBlockList(this);
    }

    public BasicBlock preferredSuccessorOf(BasicBlock block) {
        int primarySuccessor = block.getPrimarySuccessor();
        IntList successors = block.getSuccessors();
        switch (successors.size()) {
            case 0:
                return null;
            case 1:
                return labelToBlock(successors.get(0));
            default:
                if (primarySuccessor != -1) {
                    return labelToBlock(primarySuccessor);
                }
                return labelToBlock(successors.get(0));
        }
    }

    public boolean catchesEqual(BasicBlock block1, BasicBlock block2) {
        if (!StdTypeList.equalContents(block1.getExceptionHandlerTypes(), block2.getExceptionHandlerTypes())) {
            return false;
        }
        IntList succ1 = block1.getSuccessors();
        IntList succ2 = block2.getSuccessors();
        int size = succ1.size();
        int primary1 = block1.getPrimarySuccessor();
        int primary2 = block2.getPrimarySuccessor();
        if ((primary1 == -1 || primary2 == -1) && primary1 != primary2) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            int label1 = succ1.get(i);
            int label2 = succ2.get(i);
            if (label1 == primary1) {
                if (label2 != primary2) {
                    return false;
                }
            } else if (label1 != label2) {
                return false;
            }
        }
        return true;
    }
}
