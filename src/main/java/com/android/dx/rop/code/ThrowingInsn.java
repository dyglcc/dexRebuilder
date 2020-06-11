package com.android.dx.rop.code;

import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;

public final class ThrowingInsn extends Insn {
    private final TypeList catches;

    public static String toCatchString(TypeList catches) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("catch");
        int sz = catches.size();
        for (int i = 0; i < sz; i++) {
            sb.append(" ");
            sb.append(catches.getType(i).toHuman());
        }
        return sb.toString();
    }

    public ThrowingInsn(Rop opcode, SourcePosition position, RegisterSpecList sources, TypeList catches) {
        super(opcode, position, null, sources);
        if (opcode.getBranchingness() != 6) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        } else if (catches == null) {
            throw new NullPointerException("catches == null");
        } else {
            this.catches = catches;
        }
    }

    public String getInlineString() {
        return toCatchString(this.catches);
    }

    public TypeList getCatches() {
        return this.catches;
    }

    public void accept(Visitor visitor) {
        visitor.visitThrowingInsn(this);
    }

    public Insn withAddedCatch(Type type) {
        return new ThrowingInsn(getOpcode(), getPosition(), getSources(), this.catches.withAddedType(type));
    }

    public Insn withRegisterOffset(int delta) {
        return new ThrowingInsn(getOpcode(), getPosition(), getSources().withOffset(delta), this.catches);
    }

    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new ThrowingInsn(getOpcode(), getPosition(), sources, this.catches);
    }
}
