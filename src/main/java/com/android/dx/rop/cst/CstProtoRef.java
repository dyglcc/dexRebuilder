package com.android.dx.rop.cst;

import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.Type;

public final class CstProtoRef extends TypedConstant {
    private final Prototype prototype;

    public CstProtoRef(Prototype prototype) {
        this.prototype = prototype;
    }

    public static CstProtoRef make(CstString descriptor) {
        return new CstProtoRef(Prototype.fromDescriptor(descriptor.getString()));
    }

    public boolean equals(Object other) {
        if (!(other instanceof CstProtoRef)) {
            return false;
        }
        return getPrototype().equals(((CstProtoRef) other).getPrototype());
    }

    public int hashCode() {
        return this.prototype.hashCode();
    }

    public boolean isCategory2() {
        return false;
    }

    public String typeName() {
        return "proto";
    }

    protected int compareTo0(Constant other) {
        return this.prototype.compareTo(((CstProtoRef) other).getPrototype());
    }

    public String toHuman() {
        return this.prototype.getDescriptor();
    }

    public final String toString() {
        return typeName() + "{" + toHuman() + '}';
    }

    public Prototype getPrototype() {
        return this.prototype;
    }

    public Type getType() {
        return Type.METHOD_TYPE;
    }
}
