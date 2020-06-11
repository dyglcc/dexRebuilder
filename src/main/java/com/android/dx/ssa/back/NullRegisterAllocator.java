package com.android.dx.ssa.back;

import com.android.dx.ssa.BasicRegisterMapper;
import com.android.dx.ssa.RegisterMapper;
import com.android.dx.ssa.SsaMethod;

public class NullRegisterAllocator extends RegisterAllocator {
    public NullRegisterAllocator(SsaMethod ssaMeth, InterferenceGraph interference) {
        super(ssaMeth, interference);
    }

    public boolean wantsParamsMovedHigh() {
        return false;
    }

    public RegisterMapper allocateRegisters() {
        int oldRegCount = this.ssaMeth.getRegCount();
        BasicRegisterMapper mapper = new BasicRegisterMapper(oldRegCount);
        for (int i = 0; i < oldRegCount; i++) {
            mapper.addMapping(i, i * 2, 2);
        }
        return mapper;
    }
}
