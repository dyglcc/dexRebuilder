package com.android.dx.dex.code.form;

import com.android.dx.dex.code.CstInsn;
import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form31c extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form31c();

    private Form31c() {
    }

    public String insnArgString(DalvInsn insn) {
        return insn.getRegisters().get(0).regString() + ", " + insn.cstString();
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        if (noteIndices) {
            return insn.cstComment();
        }
        return "";
    }

    public int codeSize() {
        return 3;
    }

    public boolean isCompatible(DalvInsn insn) {
        if (!(insn instanceof CstInsn)) {
            return false;
        }
        RegisterSpec reg;
        RegisterSpecList regs = insn.getRegisters();
        switch (regs.size()) {
            case 1:
                reg = regs.get(0);
                break;
            case 2:
                reg = regs.get(0);
                if (reg.getReg() != regs.get(1).getReg()) {
                    return false;
                }
                break;
            default:
                return false;
        }
        if (!InsnFormat.unsignedFitsInByte(reg.getReg())) {
            return false;
        }
        Constant cst = ((CstInsn) insn).getConstant();
        if ((cst instanceof CstType) || (cst instanceof CstFieldRef) || (cst instanceof CstString)) {
            return true;
        }
        return false;
    }

    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int sz = regs.size();
        BitSet bits = new BitSet(sz);
        boolean compat = InsnFormat.unsignedFitsInByte(regs.get(0).getReg());
        if (sz == 1) {
            bits.set(0, compat);
        } else if (regs.get(0).getReg() == regs.get(1).getReg()) {
            bits.set(0, compat);
            bits.set(1, compat);
        }
        return bits;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.get(0).getReg()), ((CstInsn) insn).getIndex());
    }
}
