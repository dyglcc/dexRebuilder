package com.android.dx.dex.file;

import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;

public final class AnnotationSetRefItem extends OffsettedItem {
    private static final int ALIGNMENT = 4;
    private static final int WRITE_SIZE = 4;
    private AnnotationSetItem annotations;

    public AnnotationSetRefItem(AnnotationSetItem annotations) {
        super(4, 4);
        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        }
        this.annotations = annotations;
    }

    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATION_SET_REF_ITEM;
    }

    public void addContents(DexFile file) {
        this.annotations = (AnnotationSetItem) file.getWordData().intern(this.annotations);
    }

    public String toHuman() {
        return this.annotations.toHuman();
    }

    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        int annotationsOff = this.annotations.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(4, "  annotations_off: " + Hex.u4(annotationsOff));
        }
        out.writeInt(annotationsOff);
    }
}
