package com.android.dx.dex.file;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstMethodHandle;
import java.util.Collection;
import java.util.TreeMap;

public final class MethodHandlesSection extends UniformItemSection {
    private final TreeMap<CstMethodHandle, MethodHandleItem> methodHandles = new TreeMap();

    public MethodHandlesSection(DexFile dexFile) {
        super("method_handles", dexFile, 8);
    }

    public IndexedItem get(Constant cst) {
        if (cst == null) {
            throw new NullPointerException("cst == null");
        }
        throwIfNotPrepared();
        IndexedItem result = (IndexedItem) this.methodHandles.get((CstMethodHandle) cst);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("not found");
    }

    protected void orderItems() {
        int index = 0;
        for (MethodHandleItem item : this.methodHandles.values()) {
            int index2 = index + 1;
            item.setIndex(index);
            index = index2;
        }
    }

    public Collection<? extends Item> items() {
        return this.methodHandles.values();
    }

    public void intern(CstMethodHandle methodHandle) {
        if (methodHandle == null) {
            throw new NullPointerException("methodHandle == null");
        }
        throwIfPrepared();
        if (((MethodHandleItem) this.methodHandles.get(methodHandle)) == null) {
            this.methodHandles.put(methodHandle, new MethodHandleItem(methodHandle));
        }
    }

    int indexOf(CstMethodHandle cstMethodHandle) {
        return ((MethodHandleItem) this.methodHandles.get(cstMethodHandle)).getIndex();
    }
}
