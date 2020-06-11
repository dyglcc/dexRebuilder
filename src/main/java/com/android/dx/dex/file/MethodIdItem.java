package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstBaseMethodRef;

public final class MethodIdItem extends MemberIdItem {
    public MethodIdItem(CstBaseMethodRef method) {
        super(method);
    }

    public ItemType itemType() {
        return ItemType.TYPE_METHOD_ID_ITEM;
    }

    public void addContents(DexFile file) {
        super.addContents(file);
        file.getProtoIds().intern(getMethodRef().getPrototype());
    }

    public CstBaseMethodRef getMethodRef() {
        return (CstBaseMethodRef) getRef();
    }

    protected int getTypoidIdx(DexFile file) {
        return file.getProtoIds().indexOf(getMethodRef().getPrototype());
    }

    protected String getTypoidName() {
        return "proto_idx";
    }
}
