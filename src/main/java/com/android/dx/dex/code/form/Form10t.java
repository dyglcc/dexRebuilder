package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.TargetInsn;
import com.android.dx.io.Opcodes;
import com.android.dx.util.AnnotatedOutput;

public final class Form10t extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form10t();

    private Form10t() {
    }

    public String insnArgString(DalvInsn insn) {
        return InsnFormat.branchString(insn);
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return InsnFormat.branchComment(insn);
    }

    public int codeSize() {
        return 1;
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
        return offset != 0 && InsnFormat.signedFitsInByte(offset);
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, ((TargetInsn) insn).getTargetOffset() & Opcodes.CONST_METHOD_TYPE));
    }
}
