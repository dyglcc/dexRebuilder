package com.android.dx.dex.file;

import com.android.dx.util.AnnotatedOutput;
import java.util.Collection;

public abstract class Section {
    private final int alignment;
    private final DexFile file;
    private int fileOffset;
    private final String name;
    private boolean prepared;

    public abstract int getAbsoluteItemOffset(Item item);

    public abstract Collection<? extends Item> items();

    protected abstract void prepare0();

    public abstract int writeSize();

    protected abstract void writeTo0(AnnotatedOutput annotatedOutput);

    public static void validateAlignment(int alignment) {
        if (alignment <= 0 || ((alignment - 1) & alignment) != 0) {
            throw new IllegalArgumentException("invalid alignment");
        }
    }

    public Section(String name, DexFile file, int alignment) {
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        validateAlignment(alignment);
        this.name = name;
        this.file = file;
        this.alignment = alignment;
        this.fileOffset = -1;
        this.prepared = false;
    }

    public final DexFile getFile() {
        return this.file;
    }

    public final int getAlignment() {
        return this.alignment;
    }

    public final int getFileOffset() {
        if (this.fileOffset >= 0) {
            return this.fileOffset;
        }
        throw new RuntimeException("fileOffset not set");
    }

    public final int setFileOffset(int fileOffset) {
        if (fileOffset < 0) {
            throw new IllegalArgumentException("fileOffset < 0");
        } else if (this.fileOffset >= 0) {
            throw new RuntimeException("fileOffset already set");
        } else {
            int mask = this.alignment - 1;
            fileOffset = (fileOffset + mask) & (mask ^ -1);
            this.fileOffset = fileOffset;
            return fileOffset;
        }
    }

    public final void writeTo(AnnotatedOutput out) {
        throwIfNotPrepared();
        align(out);
        int cursor = out.getCursor();
        if (this.fileOffset < 0) {
            this.fileOffset = cursor;
        } else if (this.fileOffset != cursor) {
            throw new RuntimeException("alignment mismatch: for " + this + ", at " + cursor + ", but expected " + this.fileOffset);
        }
        if (out.annotates()) {
            if (this.name != null) {
                out.annotate(0, "\n" + this.name + ":");
            } else if (cursor != 0) {
                out.annotate(0, "\n");
            }
        }
        writeTo0(out);
    }

    public final int getAbsoluteOffset(int relative) {
        if (relative < 0) {
            throw new IllegalArgumentException("relative < 0");
        } else if (this.fileOffset >= 0) {
            return this.fileOffset + relative;
        } else {
            throw new RuntimeException("fileOffset not yet set");
        }
    }

    public final void prepare() {
        throwIfPrepared();
        prepare0();
        this.prepared = true;
    }

    protected final void throwIfNotPrepared() {
        if (!this.prepared) {
            throw new RuntimeException("not prepared");
        }
    }

    protected final void throwIfPrepared() {
        if (this.prepared) {
            throw new RuntimeException("already prepared");
        }
    }

    protected final void align(AnnotatedOutput out) {
        out.alignTo(this.alignment);
    }

    protected final String getName() {
        return this.name;
    }
}
