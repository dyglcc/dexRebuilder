package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstFieldRef;

public final class FieldIdItem extends MemberIdItem {
    public FieldIdItem(CstFieldRef field) {
        super(field);
    }

    public ItemType itemType() {
        return ItemType.TYPE_FIELD_ID_ITEM;
    }

    public void addContents(DexFile file) {
        super.addContents(file);
        file.getTypeIds().intern(getFieldRef().getType());
    }

    public CstFieldRef getFieldRef() {
        return (CstFieldRef) getRef();
    }

    protected int getTypoidIdx(DexFile file) {
        return file.getTypeIds().indexOf(getFieldRef().getType());
    }

    protected String getTypoidName() {
        return "type_idx";
    }
}
