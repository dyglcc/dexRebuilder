package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;
import com.android.dx.util.Hex;

public final class CstLong extends CstLiteral64 {
    public static final CstLong VALUE_0 = make(0);
    public static final CstLong VALUE_1 = make(1);

    public static CstLong make(long value) {
        return new CstLong(value);
    }

    private CstLong(long value) {
        super(value);
    }

    public String toString() {
        long value = getLongBits();
        return "long{0x" + Hex.u8(value) + " / " + value + '}';
    }

    public Type getType() {
        return Type.LONG;
    }

    public String typeName() {
        return "long";
    }

    public String toHuman() {
        return Long.toString(getLongBits());
    }

    public long getValue() {
        return getLongBits();
    }
}
