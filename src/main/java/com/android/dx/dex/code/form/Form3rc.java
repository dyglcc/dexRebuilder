package com.android.dx.dex.code.form;

import com.android.dx.dex.code.CstInsn;
import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstCallSiteRef;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstType;
import com.android.dx.util.AnnotatedOutput;

public final class Form3rc extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form3rc();

    private Form3rc() {
    }

    public String insnArgString(DalvInsn insn) {
        return InsnFormat.regRangeString(insn.getRegisters()) + ", " + insn.cstString();
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
        CstInsn ci = (CstInsn) insn;
        int cpi = ci.getIndex();
        Constant cst = ci.getConstant();
        if (!InsnFormat.unsignedFitsInShort(cpi)) {
            return false;
        }
        if (!(cst instanceof CstMethodRef) && !(cst instanceof CstType) && !(cst instanceof CstCallSiteRef)) {
            return false;
        }
        RegisterSpecList regs = ci.getRegisters();
        int sz = regs.size();
        if (regs.size() == 0 || (InsnFormat.isRegListSequential(regs) && InsnFormat.unsignedFitsInShort(regs.get(0).getReg()) && InsnFormat.unsignedFitsInByte(regs.getWordCount()))) {
            return true;
        }
        return false;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        int firstReg = 0;
        RegisterSpecList regs = insn.getRegisters();
        int cpi = ((CstInsn) insn).getIndex();
        if (regs.size() != 0) {
            firstReg = regs.get(0).getReg();
        }
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.getWordCount()), (short) cpi, (short) firstReg);
    }
}
