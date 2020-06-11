package com.android.dx.dex.code.form;

import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.dex.code.MultiCstInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.rop.type.Type;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form45cc extends InsnFormat {
    private static final int MAX_NUM_OPS = 5;
    public static final InsnFormat THE_ONE = new Form45cc();

    private Form45cc() {
    }

    public String insnArgString(DalvInsn insn) {
        return InsnFormat.regListString(explicitize(insn.getRegisters())) + ", " + insn.cstString();
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
        if (mci.getNumberOfConstants() != 2) {
            return false;
        }
        int methodIdx = mci.getIndex(0);
        int protoIdx = mci.getIndex(1);
        if (!InsnFormat.unsignedFitsInShort(methodIdx) || !InsnFormat.unsignedFitsInShort(protoIdx) || !(mci.getConstant(0) instanceof CstMethodRef) || !(mci.getConstant(1) instanceof CstProtoRef)) {
            return false;
        }
        if (wordCount(mci.getRegisters()) < 0) {
            z = false;
        }
        return z;
    }

    public BitSet compatibleRegs(DalvInsn insn) {
        RegisterSpecList regs = insn.getRegisters();
        int sz = regs.size();
        BitSet bits = new BitSet(sz);
        for (int i = 0; i < sz; i++) {
            RegisterSpec reg = regs.get(i);
            bits.set(i, InsnFormat.unsignedFitsInNibble((reg.getReg() + reg.getCategory()) - 1));
        }
        return bits;
    }

    public void writeTo(AnnotatedOutput out, DalvInsn insn) {
        MultiCstInsn mci = (MultiCstInsn) insn;
        short regB = (short) mci.getIndex(0);
        short regH = (short) mci.getIndex(1);
        RegisterSpecList regs = explicitize(insn.getRegisters());
        int regA = regs.size();
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(regA > 4 ? regs.get(4).getReg() : 0, regA)), regB, InsnFormat.codeUnit(regA > 0 ? regs.get(0).getReg() : 0, regA > 1 ? regs.get(1).getReg() : 0, regA > 2 ? regs.get(2).getReg() : 0, regA > 3 ? regs.get(3).getReg() : 0), regH);
    }

    private static int wordCount(RegisterSpecList regs) {
        int sz = regs.size();
        if (sz > 5) {
            return -1;
        }
        int result = 0;
        for (int i = 0; i < sz; i++) {
            RegisterSpec one = regs.get(i);
            result += one.getCategory();
            if (!InsnFormat.unsignedFitsInNibble((one.getReg() + one.getCategory()) - 1)) {
                return -1;
            }
        }
        if (result > 5) {
            result = -1;
        }
        return result;
    }

    private static RegisterSpecList explicitize(RegisterSpecList orig) {
        int wordCount = wordCount(orig);
        int sz = orig.size();
        if (wordCount == sz) {
            return orig;
        }
        RegisterSpecList result = new RegisterSpecList(wordCount);
        int wordAt = 0;
        for (int i = 0; i < sz; i++) {
            RegisterSpec one = orig.get(i);
            result.set(wordAt, one);
            if (one.getCategory() == 2) {
                result.set(wordAt + 1, RegisterSpec.make(one.getReg() + 1, Type.VOID));
                wordAt += 2;
            } else {
                wordAt++;
            }
        }
        result.setImmutable();
        return result;
    }
}
