package com.android.dx.dex.code.form;

import com.android.dx.dex.code.CstInsn;
import com.android.dx.dex.code.DalvInsn;
import com.android.dx.dex.code.InsnFormat;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstCallSiteRef;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.util.AnnotatedOutput;
import java.util.BitSet;

public final class Form35c extends InsnFormat {
    private static final int MAX_NUM_OPS = 5;
    public static final InsnFormat THE_ONE = new Form35c();

    private Form35c() {
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
        return 3;
    }

    public boolean isCompatible(DalvInsn insn) {
        if (!(insn instanceof CstInsn)) {
            return false;
        }
        CstInsn ci = (CstInsn) insn;
        if (!InsnFormat.unsignedFitsInShort(ci.getIndex())) {
            return false;
        }
        Constant cst = ci.getConstant();
        if (((cst instanceof CstMethodRef) || (cst instanceof CstType) || (cst instanceof CstCallSiteRef)) && wordCount(ci.getRegisters()) >= 0) {
            return true;
        }
        return false;
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
        int r0;
        int r1;
        int r2;
        int r3;
        int r4 = 0;
        int cpi = ((CstInsn) insn).getIndex();
        RegisterSpecList regs = explicitize(insn.getRegisters());
        int sz = regs.size();
        if (sz > 0) {
            r0 = regs.get(0).getReg();
        } else {
            r0 = 0;
        }
        if (sz > 1) {
            r1 = regs.get(1).getReg();
        } else {
            r1 = 0;
        }
        if (sz > 2) {
            r2 = regs.get(2).getReg();
        } else {
            r2 = 0;
        }
        if (sz > 3) {
            r3 = regs.get(3).getReg();
        } else {
            r3 = 0;
        }
        if (sz > 4) {
            r4 = regs.get(4).getReg();
        }
        InsnFormat.write(out, InsnFormat.opcodeUnit(insn, InsnFormat.makeByte(r4, sz)), (short) cpi, InsnFormat.codeUnit(r0, r1, r2, r3));
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
