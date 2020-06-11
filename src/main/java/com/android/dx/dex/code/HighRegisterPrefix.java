package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.util.AnnotatedOutput;

public final class HighRegisterPrefix extends VariableSizeInsn {
    private SimpleInsn[] insns;

    public HighRegisterPrefix(SourcePosition position, RegisterSpecList registers) {
        super(position, registers);
        if (registers.size() == 0) {
            throw new IllegalArgumentException("registers.size() == 0");
        }
        this.insns = null;
    }

    public int codeSize() {
        int result = 0;
        calculateInsnsIfNecessary();
        for (SimpleInsn insn : this.insns) {
            result += insn.codeSize();
        }
        return result;
    }

    public void writeTo(AnnotatedOutput out) {
        calculateInsnsIfNecessary();
        for (SimpleInsn insn : this.insns) {
            insn.writeTo(out);
        }
    }

    private void calculateInsnsIfNecessary() {
        if (this.insns == null) {
            RegisterSpecList registers = getRegisters();
            int sz = registers.size();
            this.insns = new SimpleInsn[sz];
            int outAt = 0;
            for (int i = 0; i < sz; i++) {
                RegisterSpec src = registers.get(i);
                this.insns[i] = moveInsnFor(src, outAt);
                outAt += src.getCategory();
            }
        }
    }

    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new HighRegisterPrefix(getPosition(), registers);
    }

    protected String argString() {
        return null;
    }

    protected String listingString0(boolean noteIndices) {
        RegisterSpecList registers = getRegisters();
        int sz = registers.size();
        StringBuilder sb = new StringBuilder(100);
        int outAt = 0;
        for (int i = 0; i < sz; i++) {
            RegisterSpec src = registers.get(i);
            SimpleInsn insn = moveInsnFor(src, outAt);
            if (i != 0) {
                sb.append('\n');
            }
            sb.append(insn.listingString0(noteIndices));
            outAt += src.getCategory();
        }
        return sb.toString();
    }

    private static SimpleInsn moveInsnFor(RegisterSpec src, int destIndex) {
        return DalvInsn.makeMove(SourcePosition.NO_INFO, RegisterSpec.make(destIndex, src.getType()), src);
    }
}
