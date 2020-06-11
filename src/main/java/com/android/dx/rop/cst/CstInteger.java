package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;
import com.android.dx.util.Hex;

public final class CstInteger extends CstLiteral32 {
    public static final CstInteger VALUE_0 = make(0);
    public static final CstInteger VALUE_1 = make(1);
    public static final CstInteger VALUE_2 = make(2);
    public static final CstInteger VALUE_3 = make(3);
    public static final CstInteger VALUE_4 = make(4);
    public static final CstInteger VALUE_5 = make(5);
    public static final CstInteger VALUE_M1 = make(-1);
    private static final CstInteger[] cache = new CstInteger[511];

    public static CstInteger make(int value) {
        int idx = (Integer.MAX_VALUE & value) % cache.length;
        CstInteger obj = cache[idx];
        if (obj != null && obj.getValue() == value) {
            return obj;
        }
        obj = new CstInteger(value);
        cache[idx] = obj;
        return obj;
    }

    private CstInteger(int value) {
        super(value);
    }

    public String toString() {
        int value = getIntBits();
        return "int{0x" + Hex.u4(value) + " / " + value + '}';
    }

    public Type getType() {
        return Type.INT;
    }

    public String typeName() {
        return "int";
    }

    public String toHuman() {
        return Integer.toString(getIntBits());
    }

    public int getValue() {
        return getIntBits();
    }
}
