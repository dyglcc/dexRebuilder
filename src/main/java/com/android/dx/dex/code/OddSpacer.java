package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.util.AnnotatedOutput;

public final class OddSpacer extends VariableSizeInsn {
    public OddSpacer(SourcePosition position) {
        super(position, RegisterSpecList.EMPTY);
    }

    public int codeSize() {
        return getAddress() & 1;
    }

    public void writeTo(AnnotatedOutput out) {
        if (codeSize() != 0) {
            out.writeShort(InsnFormat.codeUnit(0, 0));
        }
    }

    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new OddSpacer(getPosition());
    }

    protected String argString() {
        return null;
    }

    protected String listingString0(boolean noteIndices) {
        if (codeSize() == 0) {
            return null;
        }
        return "nop // spacer";
    }
}
