package com.android.dx.rop.cst;

public final class CstInterfaceMethodRef extends CstBaseMethodRef {
    private CstMethodRef methodRef = null;

    public CstInterfaceMethodRef(CstType definingClass, CstNat nat) {
        super(definingClass, nat);
    }

    public String typeName() {
        return "ifaceMethod";
    }

    public CstMethodRef toMethodRef() {
        if (this.methodRef == null) {
            this.methodRef = new CstMethodRef(getDefiningClass(), getNat());
        }
        return this.methodRef;
    }
}
