package com.android.dx.io.instructions;

import com.android.dx.io.IndexType;

public final class FourRegisterDecodedInstruction extends DecodedInstruction {
    private final int a;
    private final int b;
    private final int c;
    private final int d;

    public FourRegisterDecodedInstruction(InstructionCodec format, int opcode, int index, IndexType indexType, int target, long literal, int a, int b, int c, int d) {
        super(format, opcode, index, indexType, target, literal);
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public int getRegisterCount() {
        return 4;
    }

    public int getA() {
        return this.a;
    }

    public int getB() {
        return this.b;
    }

    public int getC() {
        return this.c;
    }

    public int getD() {
        return this.d;
    }

    public DecodedInstruction withIndex(int newIndex) {
        return new FourRegisterDecodedInstruction(getFormat(), getOpcode(), newIndex, getIndexType(), getTarget(), getLiteral(), this.a, this.b, this.c, this.d);
    }
}
