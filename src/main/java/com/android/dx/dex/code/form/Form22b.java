package com.android.dx.dex.code.form;

import com.android.dx.dex.code.CstInsn;
import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.io.Opcodes;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstLiteralBits;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form22b extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form22b();

    private Form22b() {
    }

    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() + ", " + InsnFormat.literalBitsString((CstLiteralBits) ((CstInsn) insn).getConstant());
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return InsnFormat.literalBitsComment((CstLiteralBits) ((CstInsn) insn).getConstant(), 8);
    }

    public int codeSize() {
        return 2;
    }

    public boolean isCompatible(DalvInsn insn) {
        boolean z = true;
        RegisterSpecList regs = insn.getRegisters();
        if (!(insn instanceof CstInsn) || regs.size() != 2 || !InsnFormat.unsignedFitsInByte(regs.get(0).getReg()) || !InsnFormat.unsignedFitsInByte(regs.get(1).getReg())) {
            return false;
        }
        Constant cst = ((CstInsn) insn).getConstant();
        if (!(cst instanceof CstLiteralBits)) {
            return false;
        }
        CstLiteralBits cb = (CstLiteralBits) cst;
        if (!(cb.fitsInInt() && InsnFormat.signedFitsInByte(cb.getIntBits()))) {
            z = false;
        }
        return z;
    }

    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(2);
        bits.set(0, InsnFormat.unsignedFitsInByte(regs.get(0).getReg()));
        bits.set(1, InsnFormat.unsignedFitsInByte(regs.get(1).getReg()));
        return bits;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.get(0).getReg()), InsnFormat.codeUnit(regs.get(1).getReg(), ((CstLiteralBits) ((CstInsn) insn).getConstant()).getIntBits() & Opcodes.CONST_METHOD_TYPE));
    }
}
