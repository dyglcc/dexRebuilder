package com.android.dx.cf.code;

import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.util.Hex;

public final class ReturnAddress implements TypeBearer {
    private final int subroutineAddress;

    public ReturnAddress(int subroutineAddress) {
        if (subroutineAddress < 0) {
            throw new IllegalArgumentException("subroutineAddress < 0");
        }
        this.subroutineAddress = subroutineAddress;
    }

    public String toString() {
        return "<addr:" + Hex.u2(this.subroutineAddress) + ">";
    }

    public String toHuman() {
        return toString();
    }

    public Type getType() {
        return Type.RETURN_ADDRESS;
    }

    public TypeBearer getFrameType() {
        return this;
    }

    public int getBasicType() {
        return Type.RETURN_ADDRESS.getBasicType();
    }

    public int getBasicFrameType() {
        return Type.RETURN_ADDRESS.getBasicFrameType();
    }

    public boolean isConstant() {
        return false;
    }

    public boolean equals(Object other) {
        if ((other instanceof ReturnAddress) && this.subroutineAddress == ((ReturnAddress) other).subroutineAddress) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return this.subroutineAddress;
    }

    public int getSubroutineAddress() {
        return this.subroutineAddress;
    }
}
