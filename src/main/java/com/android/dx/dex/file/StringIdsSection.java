package com.android.dx.dex.file;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.util.Collection;
import java.util.TreeMap;

public final class StringIdsSection extends UniformItemSection {
    private final TreeMap<CstString, StringIdItem> strings = new TreeMap();

    public StringIdsSection(DexFile file) {
        super("string_ids", file, 4);
    }

    public Collection<? extends Item> items() {
        return this.strings.values();
    }

    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();
        IndexedItem result = (IndexedItem) this.strings.get((CstString) cst);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("not found");
    }

    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();
        int sz = this.strings.size();
        int offset = sz == 0 ? 0 : getFileOffset();
        if (out.annotates()) {
            out.annotate(4, "string_ids_size: " + Hex.u4(sz));
            out.annotate(4, "string_ids_off:  " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }

    public StringIdItem intern(String string) {
        return intern(new StringIdItem(new CstString(string)));
    }

    public StringIdItem intern(CstString string) {
        return intern(new StringIdItem(string));
    }

    public synchronized StringIdItem intern(StringIdItem string) {
        StringIdItem already;
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        throwIfPrepared();
        CstString value = string.getValue();
        already = (StringIdItem) this.strings.get(value);
        if (already == null) {
            this.strings.put(value, string);
            already = string;
        }
        return already;
    }

    public synchronized void intern(CstNat nat) {
        intern(nat.getName());
        intern(nat.getDescriptor());
    }

    public int indexOf(CstString string) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        throwIfNotPrepared();
        StringIdItem s = (StringIdItem) this.strings.get(string);
        if (s != null) {
            return s.getIndex();
        }
        throw new IllegalArgumentException("not found");
    }

    protected void orderItems() {
        int idx = 0;
        for (StringIdItem s : this.strings.values()) {
            s.setIndex(idx);
            idx++;
        }
    }
}
