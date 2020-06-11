package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.SimpleInsn;
import com.android.dx.util.AnnotatedOutput;

public final class Form10x extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form10x();

    private Form10x() {
    }

    public String insnArgString(DalvInsn insn) {
        return "";
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return "";
    }

    public int codeSize() {
        return 1;
    }

    public boolean isCompatible(DalvInsn insn) {
        return (insn instanceof SimpleInsn) && insn.getRegisters().size() == 0;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, 0));
    }
}
