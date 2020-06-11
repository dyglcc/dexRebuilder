package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstCallSite;
import com.android.dx.rop.cst.CstCallSiteRef;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;

public final class CallSiteIdItem extends IndexedItem implements Comparable {
    private static final int ITEM_SIZE = 4;
    CallSiteItem data = null;
    final CstCallSiteRef invokeDynamicRef;

    public CallSiteIdItem(CstCallSiteRef invokeDynamicRef) {
        this.invokeDynamicRef = invokeDynamicRef;
    }

    public ItemType itemType() {
        return ItemType.TYPE_CALL_SITE_ID_ITEM;
    }

    public int writeSize() {
        return 4;
    }

    public void addContents(DexFile file) {
        CstCallSite callSite = this.invokeDynamicRef.getCallSite();
        CallSiteIdsSection callSiteIds = file.getCallSiteIds();
        CallSiteItem callSiteItem = callSiteIds.getCallSiteItem(callSite);
        if (callSiteItem == null) {
            MixedItemSection byteData = file.getByteData();
            callSiteItem = new CallSiteItem(callSite);
            byteData.add(callSiteItem);
            callSiteIds.addCallSiteItem(callSite, callSiteItem);
        }
        this.data = callSiteItem;
    }

    public void writeTo(DexFile file, AnnotatedOutput out) {
        int offset = this.data.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(0, indexString() + ' ' + this.invokeDynamicRef.toString());
            out.annotate(4, "call_site_off: " + Hex.u4(offset));
        }
        out.writeInt(offset);
    }

    public int compareTo(Object o) {
        return this.invokeDynamicRef.compareTo(((CallSiteIdItem) o).invokeDynamicRef);
    }
}
