package com.android.dx.io.instructions;

import com.android.dx.io.IndexType;

public class InvokePolymorphicDecodedInstruction extends DecodedInstruction {
    private final int protoIndex;
    private final int[] registers;

    public InvokePolymorphicDecodedInstruction(InstructionCodec format, int opcode, int methodIndex, IndexType indexType, int protoIndex, int[] registers) {
        super(format, opcode, methodIndex, indexType, 0, 0);
        if (protoIndex != ((short) protoIndex)) {
            throw new IllegalArgumentException("protoIndex doesn't fit in a short: " + protoIndex);
        }
        this.protoIndex = protoIndex;
        this.registers = registers;
    }

    public int getRegisterCount() {
        return this.registers.length;
    }

    public DecodedInstruction withIndex(int newIndex) {
        throw new UnsupportedOperationException("use withProtoIndex to update both the method and proto indices for invoke-polymorphic");
    }

    public DecodedInstruction withProtoIndex(int newIndex, int newProtoIndex) {
        return new InvokePolymorphicDecodedInstruction(getFormat(), getOpcode(), newIndex, getIndexType(), newProtoIndex, this.registers);
    }

    public int getC() {
        return this.registers.length > 0 ? this.registers[0] : 0;
    }

    public int getD() {
        return this.registers.length > 1 ? this.registers[1] : 0;
    }

    public int getE() {
        return this.registers.length > 2 ? this.registers[2] : 0;
    }

    public int getF() {
        return this.registers.length > 3 ? this.registers[3] : 0;
    }

    public int getG() {
        return this.registers.length > 4 ? this.registers[4] : 0;
    }

    public short getProtoIndex() {
        return (short) this.protoIndex;
    }
}
