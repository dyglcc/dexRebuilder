package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;

public final class CstNat extends Constant {
    public static final CstNat PRIMITIVE_TYPE_NAT = new CstNat(new CstString("TYPE"), new CstString("Ljava/lang/Class;"));
    private final CstString descriptor;
    private final CstString name;

    public CstNat(CstString name, CstString descriptor) {
        if (name == null) {
            throw new NullPointerException("name == null");
        } else if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        } else {
            this.name = name;
            this.descriptor = descriptor;
        }
    }

    public boolean equals(Object other) {
        if (!(other instanceof CstNat)) {
            return false;
        }
        CstNat otherNat = (CstNat) other;
        if (this.name.equals(otherNat.name) && this.descriptor.equals(otherNat.descriptor)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (this.name.hashCode() * 31) ^ this.descriptor.hashCode();
    }

    protected int compareTo0(Constant other) {
        CstNat otherNat = (CstNat) other;
        int cmp = this.name.compareTo(otherNat.name);
        return cmp != 0 ? cmp : this.descriptor.compareTo(otherNat.descriptor);
    }

    public String toString() {
        return "nat{" + toHuman() + '}';
    }

    public String typeName() {
        return "nat";
    }

    public boolean isCategory2() {
        return false;
    }

    public CstString getName() {
        return this.name;
    }

    public CstString getDescriptor() {
        return this.descriptor;
    }

    public String toHuman() {
        return this.name.toHuman() + ':' + this.descriptor.toHuman();
    }

    public Type getFieldType() {
        return Type.intern(this.descriptor.getString());
    }

    public final boolean isInstanceInit() {
        return this.name.getString().equals("<init>");
    }

    public final boolean isClassInit() {
        return this.name.getString().equals("<clinit>");
    }
}
