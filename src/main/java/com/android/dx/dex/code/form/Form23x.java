package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.SimpleInsn;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form23x extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form23x();

    private Form23x() {
    }

    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() + ", " + regs.get(2).regString();
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return "";
    }

    public int codeSize() {
        return 2;
    }

    public boolean isCompatible(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        if ((insn instanceof SimpleInsn) && regs.size() == 3 && InsnFormat.unsignedFitsInByte(regs.get(0).getReg()) && InsnFormat.unsignedFitsInByte(regs.get(1).getReg()) && InsnFormat.unsignedFitsInByte(regs.get(2).getReg())) {
            return true;
        }
        return false;
    }

    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(3);
        bits.set(0, InsnFormat.unsignedFitsInByte(regs.get(0).getReg()));
        bits.set(1, InsnFormat.unsignedFitsInByte(regs.get(1).getReg()));
        bits.set(2, InsnFormat.unsignedFitsInByte(regs.get(2).getReg()));
        return bits;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.get(0).getReg()), InsnFormat.codeUnit(regs.get(1).getReg(), regs.get(2).getReg()));
    }
}
