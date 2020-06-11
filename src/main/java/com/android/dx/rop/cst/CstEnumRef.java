package com.android.dx.rop.cst;

import com.android.dx.rop.type.Type;

public final class CstEnumRef extends CstMemberRef {
    private CstFieldRef fieldRef = null;

    public CstEnumRef(CstNat nat) {
        super(new CstType(nat.getFieldType()), nat);
    }

    public String typeName() {
        return "enum";
    }

    public Type getType() {
        return getDefiningClass().getClassType();
    }

    public CstFieldRef getFieldRef() {
        if (this.fieldRef == null) {
            this.fieldRef = new CstFieldRef(getDefiningClass(), getNat());
        }
        return this.fieldRef;
    }
}
