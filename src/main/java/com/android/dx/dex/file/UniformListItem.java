package com.android.dx.dex.file;

import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.util.List;

public final class UniformListItem<T extends OffsettedItem> extends OffsettedItem {
    private static final int HEADER_SIZE = 4;
    private final ItemType itemType;
    private final List<T> items;

    public UniformListItem(ItemType itemType, List<T> items) {
        super(getAlignment(items), writeSize(items));
        if (itemType == null) {
            throw new NullPointerException("itemType == null");
        }
        this.items = items;
        this.itemType = itemType;
    }

    private static int getAlignment(List<? extends OffsettedItem> items) {
        try {
            return Math.max(4, ((OffsettedItem) items.get(0)).getAlignment());
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("items.size() == 0");
        } catch (NullPointerException e2) {
            throw new NullPointerException("items == null");
        }
    }

    private static int writeSize(List<? extends OffsettedItem> items) {
        return (items.size() * ((OffsettedItem) items.get(0)).writeSize()) + getAlignment(items);
    }

    public ItemType itemType() {
        return this.itemType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(getClass().getName());
        sb.append(this.items);
        return sb.toString();
    }

    public void addContents(DexFile file) {
        for (OffsettedItem i : this.items) {
            i.addContents(file);
        }
    }

    public final String toHuman() {
        StringBuilder sb = new StringBuilder(100);
        boolean first = true;
        sb.append("{");
        for (OffsettedItem i : this.items) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(i.toHuman());
        }
        sb.append("}");
        return sb.toString();
    }

    public final List<T> getItems() {
        return this.items;
    }

    protected void place0(Section addedTo, int offset) {
        offset += headerSize();
        boolean first = true;
        int theSize = -1;
        int theAlignment = -1;
        for (OffsettedItem i : this.items) {
            int size = i.writeSize();
            if (first) {
                theSize = size;
                theAlignment = i.getAlignment();
                first = false;
            } else if (size != theSize) {
                throw new UnsupportedOperationException("item size mismatch");
            } else if (i.getAlignment() != theAlignment) {
                throw new UnsupportedOperationException("item alignment mismatch");
            }
            offset = i.place(addedTo, offset) + size;
        }
    }

    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        int size = this.items.size();
        if (out.annotates()) {
            out.annotate(0, offsetString() + " " + typeName());
            out.annotate(4, "  size: " + Hex.u4(size));
        }
        out.writeInt(size);
        for (OffsettedItem i : this.items) {
            i.writeTo(file, out);
        }
    }

    private int headerSize() {
        return getAlignment();
    }
}
