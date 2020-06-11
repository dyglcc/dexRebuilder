package com.android.dx.rop.code;

import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.IntList;

public final class SwitchInsn extends Insn {
    private final IntList cases;

    public SwitchInsn(Rop opcode, SourcePosition position, RegisterSpec result, RegisterSpecList sources, IntList cases) {
        super(opcode, position, result, sources);
        if (opcode.getBranchingness() != 5) {
            throw new IllegalArgumentException("bogus branchingness");
        } else if (cases == null) {
            throw new NullPointerException("cases == null");
        } else {
            this.cases = cases;
        }
    }

    public String getInlineString() {
        return this.cases.toString();
    }

    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }

    public void accept(Visitor visitor) {
        visitor.visitSwitchInsn(this);
    }

    public Insn withAddedCatch(Type type) {
        throw new UnsupportedOperationException("unsupported");
    }

    public Insn withRegisterOffset(int delta) {
        return new SwitchInsn(getOpcode(), getPosition(), getResult().withOffset(delta), getSources().withOffset(delta), this.cases);
    }

    public boolean contentEquals(Insn b) {
        return false;
    }

    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new SwitchInsn(getOpcode(), getPosition(), result, sources, this.cases);
    }

    public IntList getCases() {
        return this.cases;
    }
}
