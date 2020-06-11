package com.android.dx.dex.code.form;

import com.android.dx.dex.code.CstInsn;
import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstType;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form22c extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form22c();

    private Form22c() {
    }

    public String insnArgString(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        return regs.get(0).regString() + ", " + regs.get(1).regString() + ", " + insn.cstString();
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        if (noteIndices) {
            return insn.cstComment();
        }
        return "";
    }

    public int codeSize() {
        return 2;
    }

    public boolean isCompatible(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        if (!(insn instanceof CstInsn) || regs.size() != 2 || !InsnFormat.unsignedFitsInNibble(regs.get(0).getReg()) || !InsnFormat.unsignedFitsInNibble(regs.get(1).getReg())) {
            return false;
        }
        CstInsn ci = (CstInsn) insn;
        if (!InsnFormat.unsignedFitsInShort(ci.getIndex())) {
            return false;
        }
        Constant cst = ci.getConstant();
        if ((cst instanceof CstType) || (cst instanceof CstFieldRef)) {
            return true;
        }
        return false;
    }

    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(2);
        bits.set(0, InsnFormat.unsignedFitsInNibble(regs.get(0).getReg()));
        bits.set(1, InsnFormat.unsignedFitsInNibble(regs.get(1).getReg()));
        return bits;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(regs.get(0).getReg(), regs.get(1).getReg())), (short) ((CstInsn) insn).getIndex());
    }
}
