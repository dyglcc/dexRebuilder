package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.util.AnnotatedOutput;

public abstract class FixedSizeInsn extends DalvInsn {
    public FixedSizeInsn(Dop opcode, SourcePosition position, RegisterSpecList registers) {
        super(opcode, position, registers);
    }

    public final int codeSize() {
        return getOpcode().getFormat().codeSize();
    }

    public final void writeTo(AnnotatedOutput out) {
        getOpcode().getFormat().writeTo(out, this);
    }

    public final DalvInsn withRegisterOffset(int delta) {
        return withRegisters(getRegisters().withOffset(delta));
    }

    protected final String listingString0(boolean noteIndices) {
        return getOpcode().getFormat().listingString(this, noteIndices);
    }
}
