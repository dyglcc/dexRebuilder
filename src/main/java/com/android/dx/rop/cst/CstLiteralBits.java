package com.android.dx.rop.cst;

public abstract class CstLiteralBits extends TypedConstant {
    public abstract boolean fitsInInt();

    public abstract int getIntBits();

    public abstract long getLongBits();

    public boolean fitsIn16Bits() {
        if (!fitsInInt()) {
            return false;
        }
        short bits = getIntBits();
        if (((short) bits) == bits) {
            return true;
        }
        return false;
    }

    public boolean fitsIn8Bits() {
        if (!fitsInInt()) {
            return false;
        }
        byte bits = getIntBits();
        if (((byte) bits) == bits) {
            return true;
        }
        return false;
    }
}
