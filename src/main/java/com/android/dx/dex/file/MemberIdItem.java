package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstMemberRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;

public abstract class MemberIdItem extends IdItem {
    private final CstMemberRef cst;

    protected abstract int getTypoidIdx(DexFile dexFile);

    protected abstract String getTypoidName();

    public MemberIdItem(CstMemberRef cst) {
        super(cst.getDefiningClass());
        this.cst = cst;
    }

    public int writeSize() {
        return 8;
    }

    public void addContents(DexFile file) {
        super.addContents(file);
        file.getStringIds().intern(getRef().getNat().getName());
    }

    public final void writeTo(DexFile file, AnnotatedOutput out) {
        TypeIdsSection typeIds = file.getTypeIds();
        StringIdsSection stringIds = file.getStringIds();
        CstNat nat = this.cst.getNat();
        int classIdx = typeIds.indexOf(getDefiningClass());
        int nameIdx = stringIds.indexOf(nat.getName());
        int typoidIdx = getTypoidIdx(file);
        if (out.annotates()) {
            out.annotate(0, indexString() + ' ' + this.cst.toHuman());
            out.annotate(2, "  class_idx: " + Hex.u2(classIdx));
            out.annotate(2, String.format("  %-10s %s", new Object[]{getTypoidName() + ':', Hex.u2(typoidIdx)}));
            out.annotate(4, "  name_idx:  " + Hex.u4(nameIdx));
        }
        out.writeShort(classIdx);
        out.writeShort(typoidIdx);
        out.writeInt(nameIdx);
    }

    public final CstMemberRef getRef() {
        return this.cst;
    }
}
