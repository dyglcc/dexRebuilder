package com.android.dx.rop.code;

import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;

public final class ThrowingCstInsn extends CstInsn {
    private final TypeList catches;

    public ThrowingCstInsn(Rop opcode, SourcePosition position, RegisterSpecList sources, TypeList catches, Constant cst) {
        super(opcode, position, null, sources, cst);
        if (opcode.getBranchingness() != 6) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        } else if (catches == null) {
            throw new NullPointerException("catches == null");
        } else {
            this.catches = catches;
        }
    }

    public String getInlineString() {
        Constant cst = getConstant();
        String constantString = cst.toHuman();
        if (cst instanceof CstString) {
            constantString = ((CstString) cst).toQuoted();
        }
        return constantString + " " + ThrowingInsn.toCatchString(this.catches);
    }

    public TypeList getCatches() {
        return this.catches;
    }

    public void accept(Visitor visitor) {
        visitor.visitThrowingCstInsn(this);
    }

    public Insn withAddedCatch(Type type) {
        return new ThrowingCstInsn(getOpcode(), getPosition(), getSources(), this.catches.withAddedType(type), getConstant());
    }

    public Insn withRegisterOffset(int delta) {
        return new ThrowingCstInsn(getOpcode(), getPosition(), getSources().withOffset(delta), this.catches, getConstant());
    }

    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new ThrowingCstInsn(getOpcode(), getPosition(), sources, this.catches, getConstant());
    }
}
