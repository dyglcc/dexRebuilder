package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;

public final class TypeIdItem extends IdItem {
    public TypeIdItem(CstType type) {
        super(type);
    }

    public ItemType itemType() {
        return ItemType.TYPE_TYPE_ID_ITEM;
    }

    public int writeSize() {
        return 4;
    }

    public void addContents(DexFile file) {
        file.getStringIds().intern(getDefiningClass().getDescriptor());
    }

    public void writeTo(DexFile file, AnnotatedOutput out) {
        CstString descriptor = getDefiningClass().getDescriptor();
        int idx = file.getStringIds().indexOf(descriptor);
        if (out.annotates()) {
            out.annotate(0, indexString() + ' ' + descriptor.toHuman());
            out.annotate(4, "  descriptor_idx: " + Hex.u4(idx));
        }
        out.writeInt(idx);
    }
}
