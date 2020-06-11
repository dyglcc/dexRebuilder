package com.android.dx.rop.cst;

public abstract class CstLiteral32 extends CstLiteralBits {
    private final int bits;

    CstLiteral32(int bits) {
        this.bits = bits;
    }

    public final boolean equals(Object other) {
        return other != null && getClass() == other.getClass() && this.bits == ((CstLiteral32) other).bits;
    }

    public final int hashCode() {
        return this.bits;
    }

    protected int compareTo0(Constant other) {
        int otherBits = ((CstLiteral32) other).bits;
        if (this.bits < otherBits) {
            return -1;
        }
        if (this.bits > otherBits) {
            return 1;
        }
        return 0;
    }

    public final boolean isCategory2() {
        return false;
    }

    public final boolean fitsInInt() {
        return true;
    }

    public final int getIntBits() {
        return this.bits;
    }

    public final long getLongBits() {
        return (long) this.bits;
    }
}
