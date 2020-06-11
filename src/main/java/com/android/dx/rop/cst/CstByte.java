package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;
import com.android.dx.util.Hex;

public final class CstByte extends CstLiteral32 {
    public static final CstByte VALUE_0 = make((byte) 0);

    public static CstByte make(byte value) {
        return new CstByte(value);
    }

    public static CstByte make(int value) {
        byte cast = (byte) value;
        if (cast == value) {
            return make(cast);
        }
        throw new IllegalArgumentException("bogus byte value: " + value);
    }

    private CstByte(byte value) {
        super(value);
    }

    public String toString() {
        int value = getIntBits();
        return "byte{0x" + Hex.u1(value) + " / " + value + '}';
    }

    public Type getType() {
        return Type.BYTE;
    }

    public String typeName() {
        return "byte";
    }

    public String toHuman() {
        return Integer.toString(getIntBits());
    }

    public byte getValue() {
        return (byte) getIntBits();
    }
}
