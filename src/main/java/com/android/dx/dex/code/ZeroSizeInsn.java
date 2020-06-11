package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.util.AnnotatedOutput;

public abstract class ZeroSizeInsn extends DalvInsn {
    public ZeroSizeInsn(SourcePosition position) {
        super(Dops.SPECIAL_FORMAT, position, RegisterSpecList.EMPTY);
    }

    public final int codeSize() {
        return 0;
    }

    public final void writeTo(AnnotatedOutput out) {
    }

    public final DalvInsn withOpcode(Dop opcode) {
        throw new RuntimeException("unsupported");
    }

    public DalvInsn withRegisterOffset(int delta) {
        return withRegisters(getRegisters().withOffset(delta));
    }
}
