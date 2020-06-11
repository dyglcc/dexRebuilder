package com.android.dx.cf.iface;

import com.android.dx.cf.attrib.AttConstantValue;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.TypedConstant;

public final class StdField extends StdMember implements Field {
    public StdField(CstType definingClass, int accessFlags, CstNat nat, AttributeList attributes) {
        super(definingClass, accessFlags, nat, attributes);
    }

    public TypedConstant getConstantValue() {
        AttConstantValue cval = (AttConstantValue) getAttributes().findFirst(AttConstantValue.ATTRIBUTE_NAME);
        if (cval == null) {
            return null;
        }
        return cval.getConstantValue();
    }
}
