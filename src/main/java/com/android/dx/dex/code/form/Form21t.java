package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.TargetInsn;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form21t extends InsnFormat {
    public static final InsnFormat THE_ONE = new Form21t();

    private Form21t() {
    }

    public String insnArgString(DalvInsn insn) {
        return insn.getRegisters().get(0).regString() + ", " + InsnFormat.branchString(insn);
    }

    public String insnCommentString(DalvInsn insn, boolean noteIndices) {
        return InsnFormat.branchComment(insn);
    }

    public int codeSize() {
        return 2;
    }

    public boolean isCompatible(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        if (!(insn instanceof TargetInsn) || regs.size() != 1 || !InsnFormat.unsignedFitsInByte(regs.get(0).getReg())) {
            return false;
        }
        TargetInsn ti = (TargetInsn) insn;
        if (ti.hasTargetOffset()) {
            return branchFits(ti);
        }
        return true;
    }

    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        BitSet bits = new BitSet(1);
        bits.set(0, InsnFormat.unsignedFitsInByte(regs.get(0).getReg()));
        return bits;
    }

    public boolean branchFits(TargetInsn insn) {
        int offset = insn.getTargetOffset();
        return offset != 0 && InsnFormat.signedFitsInShort(offset);
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, insn.getRegisters().get(0).getReg()), (short) ((TargetInsn) insn).getTargetOffset());
    }
}
