package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;
import com.android.dx.util.Hex;

public final class CstChar extends CstLiteral32 {
    public static final CstChar VALUE_0 = make('\u0000');

    public static CstChar make(char value) {
        return new CstChar(value);
    }

    public static CstChar make(int value) {
        char cast = (char) value;
        if (cast == value) {
            return make(cast);
        }
        throw new IllegalArgumentException("bogus char value: " + value);
    }

    private CstChar(char value) {
        super(value);
    }

    public String toString() {
        int value = getIntBits();
        return "char{0x" + Hex.u2(value) + " / " + value + '}';
    }

    public Type getType() {
        return Type.CHAR;
    }

    public String typeName() {
        return "char";
    }

    public String toHuman() {
        return Integer.toString(getIntBits());
    }

    public char getValue() {
        return (char) getIntBits();
    }
}
