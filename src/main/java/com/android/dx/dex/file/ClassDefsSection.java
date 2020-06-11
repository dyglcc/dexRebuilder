package com.android.dx.dex.file;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

public final class ClassDefsSection extends UniformItemSection {
    private final TreeMap<Type, ClassDefItem> classDefs = new TreeMap();
    private ArrayList<ClassDefItem> orderedDefs = null;

    public ClassDefsSection(DexFile file) {
        super("class_defs", file, 4);
    }

    public Collection<? extends Item> items() {
        if (this.orderedDefs != null) {
            return this.orderedDefs;
        }
        return this.classDefs.values();
    }

    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();
        IndexedItem result = (IndexedItem) this.classDefs.get(((CstType) cst).getClassType());
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("not found");
    }

    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();
        int sz = this.classDefs.size();
        int offset = sz == 0 ? 0 : getFileOffset();
        if (out.annotates()) {
            out.annotate(4, "class_defs_size: " + Hex.u4(sz));
            out.annotate(4, "class_defs_off:  " + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }

    public void add(ClassDefItem clazz) {
        try {
            Type type = clazz.getThisClass().getClassType();
            throwIfPrepared();
            if (this.classDefs.get(type) != null) {
                throw new IllegalArgumentException("already added: " + type);
            }
            this.classDefs.put(type, clazz);
        } catch (NullPointerException e) {
            throw new NullPointerException("clazz == null");
        }
    }

    protected void orderItems() {
        int sz = this.classDefs.size();
        int idx = 0;
        this.orderedDefs = new ArrayList(sz);
        for (Type type : this.classDefs.keySet()) {
            idx = orderItems0(type, idx, sz - idx);
        }
    }

    private int orderItems0(Type type, int idx, int maxDepth) {
        ClassDefItem c = (ClassDefItem) this.classDefs.get(type);
        if (c == null || c.hasIndex()) {
            return idx;
        }
        if (maxDepth < 0) {
            throw new RuntimeException("class circularity with " + type);
        }
        maxDepth--;
        CstType superclassCst = c.getSuperclass();
        if (superclassCst != null) {
            idx = orderItems0(superclassCst.getClassType(), idx, maxDepth);
        }
        TypeList interfaces = c.getInterfaces();
        int sz = interfaces.size();
        for (int i = 0; i < sz; i++) {
            idx = orderItems0(interfaces.getType(i), idx, maxDepth);
        }
        c.setIndex(idx);
        this.orderedDefs.add(c);
        return idx + 1;
    }
}
