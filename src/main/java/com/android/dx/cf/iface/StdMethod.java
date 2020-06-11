package com.android.dx.cf.iface;

import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Prototype;

public final class StdMethod extends StdMember implements Method {
    private final Prototype effectiveDescriptor;

    public StdMethod(CstType definingClass, int accessFlags, CstNat nat, AttributeList attributes) {
        super(definingClass, accessFlags, nat, attributes);
        this.effectiveDescriptor = Prototype.intern(getDescriptor().getString(), definingClass.getClassType(), AccessFlags.isStatic(accessFlags), nat.isInstanceInit());
    }

    public Prototype getEffectiveDescriptor() {
        return this.effectiveDescriptor;
    }
}
