package com.android.dx.rop.code;

import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;

public final class PlainCstInsn extends CstInsn {
    public PlainCstInsn(Rop opcode, SourcePosition position, RegisterSpec result, RegisterSpecList sources, Constant cst) {
        super(opcode, position, result, sources, cst);
        if (opcode.getBranchingness() != 1) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }
    }

    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }

    public void accept(Visitor visitor) {
        visitor.visitPlainCstInsn(this);
    }

    public Insn withAddedCatch(Type type) {
        throw new UnsupportedOperationException("unsupported");
    }

    public Insn withRegisterOffset(int delta) {
        return new PlainCstInsn(getOpcode(), getPosition(), getResult().withOffset(delta), getSources().withOffset(delta), getConstant());
    }

    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new PlainCstInsn(getOpcode(), getPosition(), result, sources, getConstant());
    }
}
