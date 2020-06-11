package com.android.dx.cf.direct;

import com.android.dx.cf.iface.AttributeList;
import com.android.dx.cf.iface.Member;
import com.android.dx.cf.iface.StdField;
import com.android.dx.cf.iface.StdFieldList;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstType;

final class FieldListParser extends MemberListParser {
    private final StdFieldList fields = new StdFieldList(getCount());

    public FieldListParser(DirectClassFile cf, CstType definer, int offset, AttributeFactory attributeFactory) {
        super(cf, definer, offset, attributeFactory);
    }

    public StdFieldList getList() {
        parseIfNecessary();
        return this.fields;
    }

    protected String humanName() {
        return "field";
    }

    protected String humanAccessFlags(int accessFlags) {
        return AccessFlags.fieldString(accessFlags);
    }

    protected int getAttributeContext() {
        return 1;
    }

    protected Member set(int n, int accessFlags, CstNat nat, AttributeList attributes) {
        StdField field = new StdField(getDefiner(), accessFlags, nat, attributes);
        this.fields.set(n, field);
        return field;
    }
}
