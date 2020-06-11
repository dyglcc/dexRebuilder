package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstCallSite;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.ByteArrayAnnotatedOutput;

public final class CallSiteItem extends OffsettedItem {
    private byte[] encodedForm;
    private final CstCallSite value;

    public CallSiteItem(CstCallSite value) {
        super(1, writeSize(value));
        this.value = value;
    }

    private static int writeSize(CstCallSite value) {
        return -1;
    }

    protected void place0(Section addedTo, int offset) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        new ValueEncoder(addedTo.getFile(), out).writeArray(this.value, true);
        this.encodedForm = out.toByteArray();
        setWriteSize(this.encodedForm.length);
    }

    public String toHuman() {
        return this.value.toHuman();
    }

    public String toString() {
        return this.value.toString();
    }

    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(0, offsetString() + " call site");
            new ValueEncoder(file, out).writeArray(this.value, true);
            return;
        }
        out.write(this.encodedForm);
    }

    public ItemType itemType() {
        return ItemType.TYPE_ENCODED_ARRAY_ITEM;
    }

    public void addContents(DexFile file) {
        ValueEncoder.addContents(file, this.value);
    }
}
