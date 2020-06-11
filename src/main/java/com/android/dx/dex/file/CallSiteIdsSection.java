package com.android.dx.dex.file;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstCallSite;
import com.android.dx.rop.cst.CstCallSiteRef;
import java.util.Collection;
import java.util.TreeMap;

public final class CallSiteIdsSection extends UniformItemSection {
    private final TreeMap<CstCallSiteRef, CallSiteIdItem> callSiteIds = new TreeMap();
    private final TreeMap<CstCallSite, CallSiteItem> callSites = new TreeMap();

    public CallSiteIdsSection(DexFile dexFile) {
        super("call_site_ids", dexFile, 4);
    }

    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();
        IndexedItem result = (IndexedItem) this.callSiteIds.get((CstCallSiteRef) cst);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("not found");
    }

    protected void orderItems() {
        int index = 0;
        for (CallSiteIdItem callSiteId : this.callSiteIds.values()) {
            int index2 = index + 1;
            callSiteId.setIndex(index);
            index = index2;
        }
    }

    public Collection<? extends Item> items() {
        return this.callSiteIds.values();
    }

    public synchronized void intern(CstCallSiteRef cstRef) {
        if (cstRef == null) {
            throw new NullPointerException("cstRef");
        }
        throwIfPrepared();
        if (((CallSiteIdItem) this.callSiteIds.get(cstRef)) == null) {
            this.callSiteIds.put(cstRef, new CallSiteIdItem(cstRef));
        }
    }

    void addCallSiteItem(CstCallSite callSite, CallSiteItem callSiteItem) {
        if (callSite == null) {
            throw new NullPointerException("callSite == null");
        } else if (callSiteItem == null) {
            throw new NullPointerException("callSiteItem == null");
        } else {
            this.callSites.put(callSite, callSiteItem);
        }
    }

    CallSiteItem getCallSiteItem(CstCallSite callSite) {
        if (callSite != null) {
            return (CallSiteItem) this.callSites.get(callSite);
        }
        throw new NullPointerException("callSite == null");
    }
}
