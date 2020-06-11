package com.android.dx.io.instructions;

import com.android.dex.DexException;
import com.android.dx.io.IndexType;
import com.android.dx.io.OpcodeInfo;
import com.android.dx.io.Opcodes;
import com.android.dx.util.Hex;
import java.io.EOFException;

public abstract class DecodedInstruction {
    private final InstructionCodec format;
    private final int index;
    private final IndexType indexType;
    private final long literal;
    private final int opcode;
    private final int target;

    public abstract int getRegisterCount();

    public abstract DecodedInstruction withIndex(int i);

    public static DecodedInstruction decode(CodeInput in) throws EOFException {
        int opcodeUnit = in.read();
        return OpcodeInfo.getFormat(Opcodes.extractOpcodeFromUnit(opcodeUnit)).decode(opcodeUnit, in);
    }

    public static DecodedInstruction[] decodeAll(short[] encodedInstructions) {
        DecodedInstruction[] decoded = new DecodedInstruction[encodedInstructions.length];
        ShortArrayCodeInput in = new ShortArrayCodeInput(encodedInstructions);
        while (in.hasMore()) {
            try {
                decoded[in.cursor()] = decode(in);
            } catch (Throwable ex) {
                throw new DexException(ex);
            }
        }
        return decoded;
    }

    public DecodedInstruction(InstructionCodec format, int opcode, int index, IndexType indexType, int target, long literal) {
        if (format == null) {
            throw new NullPointerException("format == null");
        } else if (Opcodes.isValidShape(opcode)) {
            this.format = format;
            this.opcode = opcode;
            this.index = index;
            this.indexType = indexType;
            this.target = target;
            this.literal = literal;
        } else {
            throw new IllegalArgumentException("invalid opcode");
        }
    }

    public final InstructionCodec getFormat() {
        return this.format;
    }

    public final int getOpcode() {
        return this.opcode;
    }

    public final short getOpcodeUnit() {
        return (short) this.opcode;
    }

    public final int getIndex() {
        return this.index;
    }

    public final short getIndexUnit() {
        return (short) this.index;
    }

    public final IndexType getIndexType() {
        return this.indexType;
    }

    public final int getTarget() {
        return this.target;
    }

    public final int getTarget(int baseAddress) {
        return this.target - baseAddress;
    }

    public final short getTargetUnit(int baseAddress) {
        short relativeTarget = getTarget(baseAddress);
        if (relativeTarget == ((short) relativeTarget)) {
            return (short) relativeTarget;
        }
        throw new DexException("Target out of range: " + Hex.s4(relativeTarget));
    }

    public final int getTargetByte(int baseAddress) {
        byte relativeTarget = getTarget(baseAddress);
        if (relativeTarget == ((byte) relativeTarget)) {
            return relativeTarget & Opcodes.CONST_METHOD_TYPE;
        }
        throw new DexException("Target out of range: " + Hex.s4(relativeTarget));
    }

    public final long getLiteral() {
        return this.literal;
    }

    public final int getLiteralInt() {
        if (this.literal == ((long) ((int) this.literal))) {
            return (int) this.literal;
        }
        throw new DexException("Literal out of range: " + Hex.u8(this.literal));
    }

    public final short getLiteralUnit() {
        if (this.literal == ((long) ((short) ((int) this.literal)))) {
            return (short) ((int) this.literal);
        }
        throw new DexException("Literal out of range: " + Hex.u8(this.literal));
    }

    public final int getLiteralByte() {
        if (this.literal == ((long) ((byte) ((int) this.literal)))) {
            return ((int) this.literal) & Opcodes.CONST_METHOD_TYPE;
        }
        throw new DexException("Literal out of range: " + Hex.u8(this.literal));
    }

    public final int getLiteralNibble() {
        if (this.literal >= -8 && this.literal <= 7) {
            return ((int) this.literal) & 15;
        }
        throw new DexException("Literal out of range: " + Hex.u8(this.literal));
    }

    public int getA() {
        return 0;
    }

    public int getB() {
        return 0;
    }

    public int getC() {
        return 0;
    }

    public int getD() {
        return 0;
    }

    public int getE() {
        return 0;
    }

    public final short getRegisterCountUnit() {
        int registerCount = getRegisterCount();
        if ((-65536 & registerCount) == 0) {
            return (short) registerCount;
        }
        throw new DexException("Register count out of range: " + Hex.u8((long) registerCount));
    }

    public final short getAUnit() {
        int a = getA();
        if ((-65536 & a) == 0) {
            return (short) a;
        }
        throw new DexException("Register A out of range: " + Hex.u8((long) a));
    }

    public final short getAByte() {
        int a = getA();
        if ((a & -256) == 0) {
            return (short) a;
        }
        throw new DexException("Register A out of range: " + Hex.u8((long) a));
    }

    public final short getANibble() {
        int a = getA();
        if ((a & -16) == 0) {
            return (short) a;
        }
        throw new DexException("Register A out of range: " + Hex.u8((long) a));
    }

    public final short getBUnit() {
        int b = getB();
        if ((-65536 & b) == 0) {
            return (short) b;
        }
        throw new DexException("Register B out of range: " + Hex.u8((long) b));
    }

    public final short getBByte() {
        int b = getB();
        if ((b & -256) == 0) {
            return (short) b;
        }
        throw new DexException("Register B out of range: " + Hex.u8((long) b));
    }

    public final short getBNibble() {
        int b = getB();
        if ((b & -16) == 0) {
            return (short) b;
        }
        throw new DexException("Register B out of range: " + Hex.u8((long) b));
    }

    public final short getCUnit() {
        int c = getC();
        if ((-65536 & c) == 0) {
            return (short) c;
        }
        throw new DexException("Register C out of range: " + Hex.u8((long) c));
    }

    public final short getCByte() {
        int c = getC();
        if ((c & -256) == 0) {
            return (short) c;
        }
        throw new DexException("Register C out of range: " + Hex.u8((long) c));
    }

    public final short getCNibble() {
        int c = getC();
        if ((c & -16) == 0) {
            return (short) c;
        }
        throw new DexException("Register C out of range: " + Hex.u8((long) c));
    }

    public final short getDUnit() {
        int d = getD();
        if ((-65536 & d) == 0) {
            return (short) d;
        }
        throw new DexException("Register D out of range: " + Hex.u8((long) d));
    }

    public final short getDByte() {
        int d = getD();
        if ((d & -256) == 0) {
            return (short) d;
        }
        throw new DexException("Register D out of range: " + Hex.u8((long) d));
    }

    public final short getDNibble() {
        int d = getD();
        if ((d & -16) == 0) {
            return (short) d;
        }
        throw new DexException("Register D out of range: " + Hex.u8((long) d));
    }

    public final short getENibble() {
        int e = getE();
        if ((e & -16) == 0) {
            return (short) e;
        }
        throw new DexException("Register E out of range: " + Hex.u8((long) e));
    }

    public final void encode(CodeOutput out) {
        this.format.encode(this, out);
    }

    public DecodedInstruction withProtoIndex(int newIndex, int newProtoIndex) {
        throw new IllegalStateException(getClass().toString());
    }

    public short getProtoIndex() {
        throw new IllegalStateException(getClass().toString());
    }
}
