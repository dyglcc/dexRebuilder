package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.TargetInsn;
import com.android.dx.util.AnnotatedOutput;

public final class Form30t extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form30t();

    private Form30t() {
    }

    public String insnArgString(DalvInsn insn) {
        return InsnFormat.branchString(insn);
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return InsnFormat.branchComment(insn);
    }

    public int codeSize() {
        return 3;
    }

    public boolean isCompatible(DalvInsn insn) {
        if ((insn instanceof TargetInsn) && insn.getRegisters().size() == 0) {
            return true;
        }
        return false;
    }

    public boolean branchFits(TargetInsn insn) {
        return true;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, 0), ((TargetInsn) insn).getTargetOffset());
    }
}
