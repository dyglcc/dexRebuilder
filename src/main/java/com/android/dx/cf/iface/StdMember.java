package com.android.dx.cf.iface;

import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;

public abstract class StdMember implements Member {
    private final int accessFlags;
    private final AttributeList attributes;
    private final CstType definingClass;
    private final CstNat nat;

    public StdMember(CstType definingClass, int accessFlags, CstNat nat, AttributeList attributes) {
        if (definingClass == null) {
            throw new NullPointerException("definingClass == null");
        } else if (nat == null) {
            throw new NullPointerException("nat == null");
        } else if (attributes == null) {
            throw new NullPointerException("attributes == null");
        } else {
            this.definingClass = definingClass;
            this.accessFlags = accessFlags;
            this.nat = nat;
            this.attributes = attributes;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(getClass().getName());
        sb.append('{');
        sb.append(this.nat.toHuman());
        sb.append('}');
        return sb.toString();
    }

    public final CstType getDefiningClass() {
        return this.definingClass;
    }

    public final int getAccessFlags() {
        return this.accessFlags;
    }

    public final CstNat getNat() {
        return this.nat;
    }

    public final CstString getName() {
        return this.nat.getName();
    }

    public final CstString getDescriptor() {
        return this.nat.getDescriptor();
    }

    public final AttributeList getAttributes() {
        return this.attributes;
    }
}
