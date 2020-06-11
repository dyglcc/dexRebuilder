package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;

public final class CstKnownNull extends CstLiteralBits {
    public static final CstKnownNull THE_ONE = new CstKnownNull();

    private CstKnownNull() {
    }

    public boolean equals(Object other) {
        return other instanceof CstKnownNull;
    }

    public int hashCode() {
        return 1147565434;
    }

    protected int compareTo0(Constant other) {
        return 0;
    }

    public String toString() {
        return "known-null";
    }

    public Type getType() {
        return Type.KNOWN_NULL;
    }

    public String typeName() {
        return "known-null";
    }

    public boolean isCategory2() {
        return false;
    }

    public String toHuman() {
        return "null";
    }

    public boolean fitsInInt() {
        return true;
    }

    public int getIntBits() {
        return 0;
    }

    public long getLongBits() {
        return 0;
    }
}
