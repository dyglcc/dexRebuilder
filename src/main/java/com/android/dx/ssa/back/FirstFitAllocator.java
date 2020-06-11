package com.android.dx.ssa.back;

import com.android.dx.rop.code.CstInsn;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.ssa.BasicRegisterMapper;
import com.android.dx.ssa.NormalSsaInsn;
import com.android.dx.ssa.RegisterMapper;
import com.android.dx.ssa.SsaMethod;
import com.android.dx.util.BitIntSet;
import com.android.dx.util.IntSet;
import java.util.BitSet;

public class FirstFitAllocator extends RegisterAllocator {
    private static final boolean PRESLOT_PARAMS = true;
    private final BitSet mapped;

    public FirstFitAllocator(SsaMethod ssaMeth, InterferenceGraph interference) {
        super(ssaMeth, interference);
        this.mapped = new BitSet(ssaMeth.getRegCount());
    }

    public boolean wantsParamsMovedHigh() {
        return true;
    }

    public RegisterMapper allocateRegisters() {
        int oldRegCount = this.ssaMeth.getRegCount();
        BasicRegisterMapper mapper = new BasicRegisterMapper(oldRegCount);
        int nextNewRegister = this.ssaMeth.getParamWidth();
        for (int i = 0; i < oldRegCount; i++) {
            if (!this.mapped.get(i)) {
                int newReg;
                int maxCategory = getCategoryForSsaReg(i);
                IntSet current = new BitIntSet(oldRegCount);
                this.interference.mergeInterferenceSet(i, current);
                boolean isPreslotted = false;
                if (isDefinitionMoveParam(i)) {
                    newReg = paramNumberFromMoveParam((NormalSsaInsn) this.ssaMeth.getDefinitionForRegister(i));
                    mapper.addMapping(i, newReg, maxCategory);
                    isPreslotted = true;
                } else {
                    mapper.addMapping(i, nextNewRegister, maxCategory);
                    newReg = nextNewRegister;
                }
                int j = i + 1;
                while (j < oldRegCount) {
                    if (!(this.mapped.get(j) || isDefinitionMoveParam(j) || current.has(j) || (isPreslotted && maxCategory < getCategoryForSsaReg(j)))) {
                        this.interference.mergeInterferenceSet(j, current);
                        maxCategory = Math.max(maxCategory, getCategoryForSsaReg(j));
                        mapper.addMapping(j, newReg, maxCategory);
                        this.mapped.set(j);
                    }
                    j++;
                }
                this.mapped.set(i);
                if (!isPreslotted) {
                    nextNewRegister += maxCategory;
                }
            }
        }
        return mapper;
    }

    private int paramNumberFromMoveParam(NormalSsaInsn ndefInsn) {
        return ((CstInteger) ((CstInsn) ndefInsn.getOriginalRopInsn()).getConstant()).getValue();
    }
}
