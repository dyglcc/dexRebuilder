package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.ssa.RegisterMapper;

public final class LocalStart extends ZeroSizeInsn {
    private final RegisterSpec local;

    public static String localString(RegisterSpec spec) {
        return spec.regString() + ' ' + spec.getLocalItem().toString() + ": " + spec.getTypeBearer().toHuman();
    }

    public LocalStart(SourcePosition position, RegisterSpec local) {
        super(position);
        if (local == null) {
            throw new NullPointerException("local == null");
        }
        this.local = local;
    }

    public DalvInsn withRegisterOffset(int delta) {
        return new LocalStart(getPosition(), this.local.withOffset(delta));
    }

    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new LocalStart(getPosition(), this.local);
    }

    public RegisterSpec getLocal() {
        return this.local;
    }

    protected String argString() {
        return this.local.toString();
    }

    protected String listingString0(boolean noteIndices) {
        return "local-start " + localString(this.local);
    }

    public DalvInsn withMapper(RegisterMapper mapper) {
        return new LocalStart(getPosition(), mapper.map(this.local));
    }
}
