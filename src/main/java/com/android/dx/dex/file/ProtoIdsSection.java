package com.android.dx.dex.file;

import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.rop.type.Prototype;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.util.Collection;
import java.util.TreeMap;

public final class ProtoIdsSection extends UniformItemSection {
    private final TreeMap<Prototype, ProtoIdItem> protoIds = new TreeMap();

    public ProtoIdsSection(DexFile file) {
        super("proto_ids", file, 4);
    }

    public Collection<? extends Item> items() {
        return this.protoIds.values();
    }

    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        } else if (cst instanceof CstProtoRef) {
            throwIfNotPrepared();
            IndexedItem result = (IndexedItem) this.protoIds.get(((CstProtoRef) cst).getPrototype());
            if (result != null) {
                return result;
            }
            throw new IllegalArgumentException("not found");
        } else {
            throw new IllegalArgumentException("cst not instance of CstProtoRef");
        }
    }

    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();
        int sz = this.protoIds.size();
        int offset = sz == 0 ? 0 : getFileOffset();
        if (sz > AccessFlags.ACC_CONSTRUCTOR) {
            throw new UnsupportedOperationException("too many proto ids");
        }
        if (out.annotates()) {
            out.annotate(4, "proto_ids_size:  " + Hex.u4(sz));
            out.annotate(4, "proto_ids_off:   " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }

    public synchronized ProtoIdItem intern(Prototype prototype) {
        ProtoIdItem result;
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }
        throwIfPrepared();
        result = (ProtoIdItem) this.protoIds.get(prototype);
        if (result == null) {
            result = new ProtoIdItem(prototype);
            this.protoIds.put(prototype, result);
        }
        return result;
    }

    public int indexOf(Prototype prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }
        throwIfNotPrepared();
        ProtoIdItem item = (ProtoIdItem) this.protoIds.get(prototype);
        if (item != null) {
            return item.getIndex();
        }
        throw new IllegalArgumentException("not found");
    }

    protected void orderItems() {
        int idx = 0;
        for (ProtoIdItem i : items()) {
            i.setIndex(idx);
            idx++;
        }
    }
}
