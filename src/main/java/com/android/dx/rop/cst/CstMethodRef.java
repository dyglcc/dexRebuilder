package com.android.dx.rop.cst;

public final class CstMethodRef extends CstBaseMethodRef {
    public CstMethodRef(CstType definingClass, CstNat nat) {
        super(definingClass, nat);
    }

    public String typeName() {
        return "method";
    }
}
