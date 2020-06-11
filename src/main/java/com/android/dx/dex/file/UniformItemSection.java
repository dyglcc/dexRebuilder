package com.android.dx.dex.file;

import com.android.dx.rop.cst.Constant;
import com.android.dx.util.AnnotatedOutput;
import java.util.Collection;

public abstract class UniformItemSection extends Section {
    public abstract IndexedItem get(Constant constant);

    protected abstract void orderItems();

    public UniformItemSection(String name, DexFile file, int alignment) {
        super(name, file, alignment);
    }

    public final int writeSize() {
        Collection<? extends Item> items = items();
        int sz = items.size();
        if (sz == 0) {
            return 0;
        }
        return ((Item) items.iterator().next()).writeSize() * sz;
    }

    protected final void prepare0() {
        DexFile file = getFile();
        orderItems();
        for (Item one : items()) {
            one.addContents(file);
        }
    }

    protected final void writeTo0(AnnotatedOutput out) {
        DexFile file = getFile();
        int alignment = getAlignment();
        for (Item one : items()) {
            one.writeTo(file, out);
            out.alignTo(alignment);
        }
    }

    public final int getAbsoluteItemOffset(Item item) {
        IndexedItem ii = (IndexedItem) item;
        return getAbsoluteOffset(ii.getIndex() * ii.writeSize());
    }
}
