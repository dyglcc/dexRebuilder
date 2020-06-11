package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;
import com.android.dx.util.Hex;

public final class CstDouble extends CstLiteral64 {
    public static final CstDouble VALUE_0 = new CstDouble(Double.doubleToLongBits(0.0d));
    public static final CstDouble VALUE_1 = new CstDouble(Double.doubleToLongBits(1.0d));

    public static CstDouble make(long bits) {
        return new CstDouble(bits);
    }

    private CstDouble(long bits) {
        super(bits);
    }

    public String toString() {
        long bits = getLongBits();
        return "double{0x" + Hex.u8(bits) + " / " + Double.longBitsToDouble(bits) + '}';
    }

    public Type getType() {
        return Type.DOUBLE;
    }

    public String typeName() {
        return "double";
    }

    public String toHuman() {
        return Double.toString(Double.longBitsToDouble(getLongBits()));
    }

    public double getValue() {
        return Double.longBitsToDouble(getLongBits());
    }
}
