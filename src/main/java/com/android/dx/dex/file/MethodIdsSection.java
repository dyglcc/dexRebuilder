package com.android.dx.dex.file;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstBaseMethodRef;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.util.Collection;
import java.util.TreeMap;

public final class MethodIdsSection extends MemberIdsSection {
    private final TreeMap<CstBaseMethodRef, MethodIdItem> methodIds = new TreeMap();

    public MethodIdsSection(DexFile file) {
        super("method_ids", file);
    }

    public Collection<? extends Item> items() {
        return this.methodIds.values();
    }

    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();
        IndexedItem result = (IndexedItem) this.methodIds.get((CstBaseMethodRef) cst);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("not found");
    }

    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();
        int sz = this.methodIds.size();
        int offset = sz == 0 ? 0 : getFileOffset();
        if (out.annotates()) {
            out.annotate(4, "method_ids_size: " + Hex.u4(sz));
            out.annotate(4, "method_ids_off:  " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }

    public synchronized MethodIdItem intern(CstBaseMethodRef method) {
        MethodIdItem result;
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        throwIfPrepared();
        result = (MethodIdItem) this.methodIds.get(method);
        if (result == null) {
            result = new MethodIdItem(method);
            this.methodIds.put(method, result);
        }
        return result;
    }

    public int indexOf(CstBaseMethodRef ref) {
        if (ref == null) {
            throw new NullPointerException("ref == null");
        }
        throwIfNotPrepared();
        MethodIdItem item = (MethodIdItem) this.methodIds.get(ref);
        if (item != null) {
            return item.getIndex();
        }
        throw new IllegalArgumentException("not found");
    }
}
