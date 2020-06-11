package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstArray;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.ByteArrayAnnotatedOutput;

public final class EncodedArrayItem extends OffsettedItem {
    private static final int ALIGNMENT = 1;
    private final CstArray array;
    private byte[] encodedForm;

    public EncodedArrayItem(CstArray array) {
        super(1, -1);
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        this.array = array;
        this.encodedForm = null;
    }

    public ItemType itemType() {
        return ItemType.TYPE_ENCODED_ARRAY_ITEM;
    }

    public int hashCode() {
        return this.array.hashCode();
    }

    protected int compareTo0(OffsettedItem other) {
        return this.array.compareTo(((EncodedArrayItem) other).array);
    }

    public String toHuman() {
        return this.array.toHuman();
    }

    public void addContents(DexFile file) {
        ValueEncoder.addContents(file, this.array);
    }

    protected void place0(Section addedTo, int offset) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        new ValueEncoder(addedTo.getFile(), out).writeArray(this.array, false);
        this.encodedForm = out.toByteArray();
        setWriteSize(this.encodedForm.length);
    }

    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(0, offsetString() + " encoded array");
            new ValueEncoder(file, out).writeArray(this.array, true);
            return;
        }
        out.write(this.encodedForm);
    }
}
