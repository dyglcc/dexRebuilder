package com.android.dx.io.instructions;

import com.android.dx.io.IndexType;

public final class TwoRegisterDecodedInstruction extends DecodedInstruction {
    private final int a;
    private final int b;

    public TwoRegisterDecodedInstruction(InstructionCodec format, int opcode, int index, IndexType indexType, int target, long literal, int a, int b) {
        super(format, opcode, index, indexType, target, literal);
        this.a = a;
        this.b = b;
    }

    public int getRegisterCount() {
        return 2;
    }

    public int getA() {
        return this.a;
    }

    public int getB() {
        return this.b;
    }

    public DecodedInstruction withIndex(int newIndex) {
        return new TwoRegisterDecodedInstruction(getFormat(), getOpcode(), newIndex, getIndexType(), getTarget(), getLiteral(), this.a, this.b);
    }
}
