package com.android.dx.dex.file;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public final class MixedItemSection extends Section {
    private static final Comparator<OffsettedItem> TYPE_SORTER = new Comparator<OffsettedItem>() {
        public int compare(OffsettedItem item1, OffsettedItem item2) {
            return item1.itemType().compareTo(item2.itemType());
        }
    };
    private final HashMap<OffsettedItem, OffsettedItem> interns = new HashMap(100);
    private final ArrayList<OffsettedItem> items = new ArrayList(100);
    private final SortType sort;
    private int writeSize;

    enum SortType {
        NONE,
        TYPE,
        INSTANCE
    }

    public MixedItemSection(String name, DexFile file, int alignment, SortType sort) {
        super(name, file, alignment);
        this.sort = sort;
        this.writeSize = -1;
    }

    public Collection<? extends Item> items() {
        return this.items;
    }

    public int writeSize() {
        throwIfNotPrepared();
        return this.writeSize;
    }

    public int getAbsoluteItemOffset(Item item) {
        return ((OffsettedItem) item).getAbsoluteOffset();
    }

    public int size() {
        return this.items.size();
    }

    public void writeHeaderPart(AnnotatedOutput out) {
        throwIfNotPrepared();
        if (this.writeSize == -1) {
            throw new RuntimeException("write size not yet set");
        }
        int sz = this.writeSize;
        int offset = sz == 0 ? 0 : getFileOffset();
        String name = getName();
        if (name == null) {
            name = "<unnamed>";
        }
        char[] spaceArr = new char[(15 - name.length())];
        Arrays.fill(spaceArr, ' ');
        String spaces = new String(spaceArr);
        if (out.annotates()) {
            out.annotate(4, name + "_size:" + spaces + Hex.u4(sz));
            out.annotate(4, name + "_off: " + spaces + Hex.u4(offset));
        }
        out.writeInt(sz);
        out.writeInt(offset);
    }

    public void add(OffsettedItem item) {
        throwIfPrepared();
        try {
            if (item.getAlignment() > getAlignment()) {
                throw new IllegalArgumentException("incompatible item alignment");
            }
            this.items.add(item);
        } catch (NullPointerException e) {
            throw new NullPointerException("item == null");
        }
    }

    public synchronized <T extends OffsettedItem> T intern(T item) {
        T result;
        throwIfPrepared();
        result = (OffsettedItem) this.interns.get(item);
        if (result == null) {
            add(item);
            this.interns.put(item, item);
            result = item;
        }
        return result;
    }

    public <T extends OffsettedItem> T get(T item) {
        throwIfNotPrepared();
        OffsettedItem result = (OffsettedItem) this.interns.get(item);
        if (result != null) {
            return result;
        }
        throw new NoSuchElementException(item.toString());
    }

    public void writeIndexAnnotation(AnnotatedOutput out, ItemType itemType, String intro) {
        throwIfNotPrepared();
        TreeMap<String, OffsettedItem> index = new TreeMap();
        Iterator it = this.items.iterator();
        while (it.hasNext()) {
            OffsettedItem item = (OffsettedItem) it.next();
            if (item.itemType() == itemType) {
                index.put(item.toHuman(), item);
            }
        }
        if (index.size() != 0) {
            out.annotate(0, intro);
            for (Entry<String, OffsettedItem> entry : index.entrySet()) {
                out.annotate(0, ((OffsettedItem) entry.getValue()).offsetString() + ' ' + ((String) entry.getKey()) + '\n');
            }
        }
    }

    protected void prepare0() {
        DexFile file = getFile();
        int i = 0;
        while (true) {
            int sz = this.items.size();
            if (i < sz) {
                while (i < sz) {
                    ((OffsettedItem) this.items.get(i)).addContents(file);
                    i++;
                }
            } else {
                return;
            }
        }
    }

    public void placeItems() {
        throwIfNotPrepared();
        switch (this.sort) {
            case INSTANCE:
                Collections.sort(this.items);
                break;
            case TYPE:
                Collections.sort(this.items, TYPE_SORTER);
                break;
        }
        int sz = this.items.size();
        int outAt = 0;
        int i = 0;
        while (i < sz) {
            OffsettedItem one = (OffsettedItem) this.items.get(i);
            try {
                int placedAt = one.place(this, outAt);
                if (placedAt < outAt) {
                    throw new RuntimeException("bogus place() result for " + one);
                }
                outAt = placedAt + one.writeSize();
                i++;
            } catch (RuntimeException ex) {
                throw ExceptionWithContext.withContext(ex, "...while placing " + one);
            }
        }
        this.writeSize = outAt;
    }

    protected void writeTo0(AnnotatedOutput out) {
        boolean annotates = out.annotates();
        boolean first = true;
        DexFile file = getFile();
        int at = 0;
        Iterator it = this.items.iterator();
        while (it.hasNext()) {
            OffsettedItem one = (OffsettedItem) it.next();
            if (annotates) {
                if (first) {
                    first = false;
                } else {
                    out.annotate(0, "\n");
                }
            }
            int alignMask = one.getAlignment() - 1;
            int writeAt = (at + alignMask) & (alignMask ^ -1);
            if (at != writeAt) {
                out.writeZeroes(writeAt - at);
                at = writeAt;
            }
            one.writeTo(file, out);
            at += one.writeSize();
        }
        if (at != this.writeSize) {
            throw new RuntimeException("output size mismatch");
        }
    }
}
