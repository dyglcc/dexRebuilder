package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.MultiCstInsn;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.util.AnnotatedOutput;

public final class Form4rcc extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form4rcc();

    private Form4rcc() {
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
        return 4;
    }

    public boolean isCompatible(DalvInsn insn) {
        boolean z = true;
        if (!(insn instanceof MultiCstInsn)) {
            return false;
        }
        MultiCstInsn mci = (MultiCstInsn) insn;
        int methodIdx = mci.getIndex(0);
        int protoIdx = mci.getIndex(1);
        if (!InsnFormat.unsignedFitsInShort(methodIdx) || !InsnFormat.unsignedFitsInShort(protoIdx) || !(mci.getConstant(0) instanceof CstMethodRef) || !(mci.getConstant(1) instanceof CstProtoRef)) {
            return false;
        }
        RegisterSpecList regs = mci.getRegisters();
        int sz = regs.size();
        if (sz == 0) {
            return true;
        }
        if (!(InsnFormat.unsignedFitsInByte(regs.getWordCount()) && InsnFormat.unsignedFitsInShort(sz) && InsnFormat.unsignedFitsInShort(regs.get(0).getReg()) && InsnFormat.isRegListSequential(regs))) {
            z = false;
        }
        return z;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        MultiCstInsn mci = (MultiCstInsn) insn;
        short regB = (short) mci.getIndex(0);
        short regH = (short) mci.getIndex(1);
        RegisterSpecList regs = insn.getRegisters();
        short regC = (short) 0;
        if (regs.size() > 0) {
            regC = (short) regs.get(0).getReg();
        }
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, regs.getWordCount()), regB, regC, regH);
    }
}
