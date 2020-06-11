package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.SimpleInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form12x extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form12x();

    private Form12x() {
    }

    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int sz = regs.size();
        return regs.get(sz - 2).regString() + ", " + regs.get(sz - 1).regString();
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return "";
    }

    public int codeSize() {
        return 1;
    }

    public boolean isCompatible(DalvInsn insn) {
        boolean z = true;
        if (!(insn instanceof SimpleInsn)) {
            return false;
        }
        RegisterSpec rs1;
        RegisterSpecList regs = insn.getRegisters();
        RegisterSpec rs2;
        switch (regs.size()) {
            case 2:
                rs1 = regs.get(0);
                rs2 = regs.get(1);
                break;
            case 3:
                rs1 = regs.get(1);
                rs2 = regs.get(2);
                if (rs1.getReg() != regs.get(0).getReg()) {
                    return false;
                }
                break;
            default:
                return false;
        }
        if (!(InsnFormat.unsignedFitsInNibble(rs1.getReg()) && InsnFormat.unsignedFitsInNibble(rs2.getReg()))) {
            z = false;
        }
        return z;
    }

    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(2);
        int r0 = regs.get(0).getReg();
        int r1 = regs.get(1).getReg();
        switch (regs.size()) {
            case 2:
                bits.set(0, InsnFormat.unsignedFitsInNibble(r0));
                bits.set(1, InsnFormat.unsignedFitsInNibble(r1));
                break;
            case 3:
                if (r0 != r1) {
                    bits.set(0, false);
                    bits.set(1, false);
                } else {
                    boolean dstRegComp = InsnFormat.unsignedFitsInNibble(r1);
                    bits.set(0, dstRegComp);
                    bits.set(1, dstRegComp);
                }
                bits.set(2, InsnFormat.unsignedFitsInNibble(regs.get(2).getReg()));
                break;
            default:
                throw new AssertionError();
        }
        return bits;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int sz = regs.size();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(regs.get(sz - 2).getReg(), regs.get(sz - 1).getReg())));
    }
}
