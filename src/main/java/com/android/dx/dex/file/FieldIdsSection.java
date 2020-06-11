package com.android.dx.dex.file;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.util.Collection;
import java.util.TreeMap;

public final class FieldIdsSection extends MemberIdsSection {
    private final TreeMap<CstFieldRef, FieldIdItem> fieldIds = new TreeMap();

    public FieldIdsSection(DexFile file) {
        super("field_ids", file);
    }

    public Collection<? extends Item> items() {
        return this.fieldIds.values();
    }

    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();
        IndexedItem result = (IndexedItem) this.fieldIds.get((CstFieldRef) cst);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("not found");
    }

    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();
        int sz = this.fieldIds.size();
        int offset = sz == 0 ? 0 : getFileOffset();
        if (out.annotates()) {
            out.annotate(4, "field_ids_size:  " + Hex.u4(sz));
            out.annotate(4, "field_ids_off:   " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }

    public synchronized FieldIdItem intern(CstFieldRef field) {
        FieldIdItem result;
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        throwIfPrepared();
        result = (FieldIdItem) this.fieldIds.get(field);
        if (result == null) {
            result = new FieldIdItem(field);
            this.fieldIds.put(field, result);
        }
        return result;
    }

    public int indexOf(CstFieldRef ref) {
        if (ref == null) {
            throw new NullPointerException("ref == null");
        }
        throwIfNotPrepared();
        FieldIdItem item = (FieldIdItem) this.fieldIds.get(ref);
        if (item != null) {
            return item.getIndex();
        }
        throw new IllegalArgumentException("not found");
    }
}
