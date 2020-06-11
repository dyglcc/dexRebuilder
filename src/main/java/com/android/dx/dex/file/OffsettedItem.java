package com.android.dx.dex.file;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.util.AnnotatedOutput;

public abstract class OffsettedItem extends Item implements Comparable<OffsettedItem> {
    private Section addedTo;
    private final int alignment;
    private int offset;
    private int writeSize;

    public abstract String toHuman();

    protected abstract void writeTo0(DexFile dexFile, AnnotatedOutput annotatedOutput);

    public static int getAbsoluteOffsetOr0(OffsettedItem item) {
        if (item == null) {
            return 0;
        }
        return item.getAbsoluteOffset();
    }

    public OffsettedItem(int alignment, int writeSize) {
        Section.validateAlignment(alignment);
        if (writeSize < -1) {
            throw new IllegalArgumentException("writeSize < -1");
        }
        this.alignment = alignment;
        this.writeSize = writeSize;
        this.addedTo = null;
        this.offset = -1;
    }

    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        OffsettedItem otherItem = (OffsettedItem) other;
        if (itemType() != otherItem.itemType()) {
            return false;
        }
        if (compareTo0(otherItem) != 0) {
            return false;
        }
        return true;
    }

    public final int compareTo(OffsettedItem other) {
        if (this == other) {
            return 0;
        }
        ItemType thisType = itemType();
        ItemType otherType = other.itemType();
        if (thisType != otherType) {
            return thisType.compareTo(otherType);
        }
        return compareTo0(other);
    }

    public final void setWriteSize(int writeSize) {
        if (writeSize < 0) {
            throw new IllegalArgumentException("writeSize < 0");
        } else if (this.writeSize >= 0) {
            throw new UnsupportedOperationException("writeSize already set");
        } else {
            this.writeSize = writeSize;
        }
    }

    public final int writeSize() {
        if (this.writeSize >= 0) {
            return this.writeSize;
        }
        throw new UnsupportedOperationException("writeSize is unknown");
    }

    public final void writeTo(DexFile file, AnnotatedOutput out) {
        out.alignTo(this.alignment);
        try {
            if (this.writeSize < 0) {
                throw new UnsupportedOperationException("writeSize is unknown");
            }
            out.assertCursor(getAbsoluteOffset());
            writeTo0(file, out);
        } catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex, "...while writing " + this);
        }
    }

    public final int getRelativeOffset() {
        if (this.offset >= 0) {
            return this.offset;
        }
        throw new RuntimeException("offset not yet known");
    }

    public final int getAbsoluteOffset() {
        if (this.offset >= 0) {
            return this.addedTo.getAbsoluteOffset(this.offset);
        }
        throw new RuntimeException("offset not yet known");
    }

    public final int place(Section addedTo, int offset) {
        if (addedTo == null) {
            throw new NullPointerException("addedTo == null");
        } else if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        } else if (this.addedTo != null) {
            throw new RuntimeException("already written");
        } else {
            int mask = this.alignment - 1;
            offset = (offset + mask) & (mask ^ -1);
            this.addedTo = addedTo;
            this.offset = offset;
            place0(addedTo, offset);
            return offset;
        }
    }

    public final int getAlignment() {
        return this.alignment;
    }

    public final String offsetString() {
        return '[' + Integer.toHexString(getAbsoluteOffset()) + ']';
    }

    protected int compareTo0(OffsettedItem other) {
        throw new UnsupportedOperationException("unsupported");
    }

    protected void place0(Section addedTo, int offset) {
    }
}
