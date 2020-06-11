package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.RegisterSpecSet;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.ssa.RegisterMapper;

public final class LocalSnapshot extends ZeroSizeInsn {
    private final RegisterSpecSet locals;

    public LocalSnapshot(SourcePosition position, RegisterSpecSet locals) {
        super(position);
        if (locals == null) {
            throw new NullPointerException("locals == null");
        }
        this.locals = locals;
    }

    public DalvInsn withRegisterOffset(int delta) {
        return new LocalSnapshot(getPosition(), this.locals.withOffset(delta));
    }

    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new LocalSnapshot(getPosition(), this.locals);
    }

    public RegisterSpecSet getLocals() {
        return this.locals;
    }

    protected String argString() {
        return this.locals.toString();
    }

    protected String listingString0(boolean noteIndices) {
        int sz = this.locals.size();
        int max = this.locals.getMaxSize();
        StringBuilder sb = new StringBuilder((sz * 40) + 100);
        sb.append("local-snapshot");
        for (int i = 0; i < max; i++) {
            RegisterSpec spec = this.locals.get(i);
            if (spec != null) {
                sb.append("\n  ");
                sb.append(LocalStart.localString(spec));
            }
        }
        return sb.toString();
    }

    public DalvInsn withMapper(RegisterMapper mapper) {
        return new LocalSnapshot(getPosition(), mapper.map(this.locals));
    }
}
