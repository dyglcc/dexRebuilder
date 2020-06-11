package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;
import com.android.dx.util.Hex;

public final class CstShort extends CstLiteral32 {
    public static final CstShort VALUE_0 = make((short) 0);

    public static CstShort make(short value) {
        return new CstShort(value);
    }

    public static CstShort make(int value) {
        short cast = (short) value;
        if (cast == value) {
            return make(cast);
        }
        throw new IllegalArgumentException("bogus short value: " + value);
    }

    private CstShort(short value) {
        super(value);
    }

    public String toString() {
        int value = getIntBits();
        return "short{0x" + Hex.u2(value) + " / " + value + '}';
    }

    public Type getType() {
        return Type.SHORT;
    }

    public String typeName() {
        return "short";
    }

    public String toHuman() {
        return Integer.toString(getIntBits());
    }

    public short getValue() {
        return (short) getIntBits();
    }
}
