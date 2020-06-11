package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;

public final class TargetInsn extends FixedSizeInsn {
    private CodeAddress target;

    public TargetInsn(Dop opcode, SourcePosition position, RegisterSpecList registers, CodeAddress target) {
        super(opcode, position, registers);
        if (target == null) {
            throw new NullPointerException("target == null");
        }
        this.target = target;
    }

    public DalvInsn withOpcode(Dop opcode) {
        return new TargetInsn(opcode, getPosition(), getRegisters(), this.target);
    }

    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new TargetInsn(getOpcode(), getPosition(), registers, this.target);
    }

    public TargetInsn withNewTargetAndReversed(CodeAddress target) {
        return new TargetInsn(getOpcode().getOppositeTest(), getPosition(), getRegisters(), target);
    }

    public CodeAddress getTarget() {
        return this.target;
    }

    public int getTargetAddress() {
        return this.target.getAddress();
    }

    public int getTargetOffset() {
        return this.target.getAddress() - getAddress();
    }

    public boolean hasTargetOffset() {
        return hasAddress() && this.target.hasAddress();
    }

    protected String argString() {
        if (this.target == null) {
            return "????";
        }
        return this.target.identifierString();
    }
}
