package com.android.dx.ssa.back;

import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.ssa.NormalSsaInsn;
import com.android.dx.ssa.RegisterMapper;
import com.android.dx.ssa.SsaBasicBlock;
import com.android.dx.ssa.SsaInsn;
import com.android.dx.ssa.SsaMethod;
import com.android.dx.util.IntIterator;
import java.util.ArrayList;

public abstract class RegisterAllocator {
    protected final InterferenceGraph interference;
    protected final SsaMethod ssaMeth;

    public abstract RegisterMapper allocateRegisters();

    public abstract boolean wantsParamsMovedHigh();

    public RegisterAllocator(SsaMethod ssaMeth, InterferenceGraph interference) {
        this.ssaMeth = ssaMeth;
        this.interference = interference;
    }

    protected final int getCategoryForSsaReg(int reg) {
        SsaInsn definition = this.ssaMeth.getDefinitionForRegister(reg);
        if (definition == null) {
            return 1;
        }
        return definition.getResult().getCategory();
    }

    protected final RegisterSpec getDefinitionSpecForSsaReg(int reg) {
        SsaInsn definition = this.ssaMeth.getDefinitionForRegister(reg);
        return definition == null ? null : definition.getResult();
    }

    protected boolean isDefinitionMoveParam(int reg) {
        SsaInsn defInsn = this.ssaMeth.getDefinitionForRegister(reg);
        if ((defInsn instanceof NormalSsaInsn) && ((NormalSsaInsn) defInsn).getOpcode().getOpcode() == 3) {
            return true;
        }
        return false;
    }

    protected final RegisterSpec insertMoveBefore(SsaInsn insn, RegisterSpec reg) {
        SsaBasicBlock block = insn.getBlock();
        ArrayList<SsaInsn> insns = block.getInsns();
        int insnIndex = insns.indexOf(insn);
        if (insnIndex < 0) {
            throw new IllegalArgumentException("specified insn is not in this block");
        } else if (insnIndex != insns.size() - 1) {
            throw new IllegalArgumentException("Adding move here not supported:" + insn.toHuman());
        } else {
            RegisterSpec newRegSpec = RegisterSpec.make(this.ssaMeth.makeNewSsaReg(), reg.getTypeBearer());
            insns.add(insnIndex, SsaInsn.makeFromRop(new PlainInsn(Rops.opMove(newRegSpec.getType()), SourcePosition.NO_INFO, newRegSpec, RegisterSpecList.make(reg)), block));
            int newReg = newRegSpec.getReg();
            IntIterator liveOutIter = block.getLiveOutRegs().iterator();
            while (liveOutIter.hasNext()) {
                this.interference.add(newReg, liveOutIter.next());
            }
            RegisterSpecList sources = insn.getSources();
            int szSources = sources.size();
            for (int i = 0; i < szSources; i++) {
                this.interference.add(newReg, sources.get(i).getReg());
            }
            this.ssaMeth.onInsnsChanged();
            return newRegSpec;
        }
    }
}
