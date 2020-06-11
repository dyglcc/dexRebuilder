package com.android.dx.dex.code.form;

import com.android.dx.dex.code.CstInsn;
import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.cst.CstLiteral64;
import com.android.dx.rop.cst.CstLiteralBits;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form51l extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form51l();

    private Form51l() {
    }

    public String insnArgString(DalvInsn insn) {
        return insn.getRegisters().get(0).regString() + ", " + InsnFormat.literalBitsString((CstLiteralBits) ((CstInsn) insn).getConstant());
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return InsnFormat.literalBitsComment((CstLiteralBits) ((CstInsn) insn).getConstant(), 64);
    }

    public int codeSize() {
        return 5;
    }

    public boolean isCompatible(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        if ((insn instanceof CstInsn) && regs.size() == 1 && InsnFormat.unsignedFitsInByte(regs.get(0).getReg())) {
            return ((CstInsn) insn).getConstant() instanceof CstLiteral64;
        }
        return false;
    }

    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(1);
        bits.set(0, InsnFormat.unsignedFitsInByte(regs.get(0).getReg()));
        return bits;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.get(0).getReg()), ((CstLiteral64) ((CstInsn) insn).getConstant()).getLongBits());
    }
}