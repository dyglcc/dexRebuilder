package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.TargetInsn;
import com.android.dx.util.AnnotatedOutput;

public final class Form20t extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form20t();

    private Form20t() {
    }

    public String insnArgString(DalvInsn insn) {
        return InsnFormat.branchString(insn);
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return InsnFormat.branchComment(insn);
    }

    public int codeSize() {
        return 2;
    }

    public boolean isCompatible(DalvInsn insn) {
        if (!(insn instanceof TargetInsn) || insn.getRegisters().size() != 0) {
            return false;
        }
        TargetInsn ti = (TargetInsn) insn;
        return ti.hasTargetOffset() ? branchFits(ti) : true;
    }

    public boolean branchFits(TargetInsn insn) {
        int offset = insn.getTargetOffset();
        return offset != 0 && InsnFormat.signedFitsInShort(offset);
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, 0), (short) ((TargetInsn) insn).getTargetOffset());
    }
}
