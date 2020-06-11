package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;

public abstract class VariableSizeInsn extends DalvInsn {
    public VariableSizeInsn(SourcePosition position, RegisterSpecList registers) {
        super(Dops.SPECIAL_FORMAT, position, registers);
    }

    public final DalvInsn withOpcode(Dop opcode) {
        throw new RuntimeException("unsupported");
    }

    public final DalvInsn withRegisterOffset(int delta) {
        return withRegisters(getRegisters().withOffset(delta));
    }
}
