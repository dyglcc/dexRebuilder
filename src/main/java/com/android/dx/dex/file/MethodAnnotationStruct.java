package com.android.dx.dex.file;

import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import com.android.dx.util.ToHuman;

public final class MethodAnnotationStruct implements ToHuman, Comparable<MethodAnnotationStruct> {
    private AnnotationSetItem annotations;
    private final CstMethodRef method;

    public MethodAnnotationStruct(CstMethodRef method, AnnotationSetItem annotations) {
        if (method == null) {
            throw new NullPointerException("method == null");
        } else if (annotations == null) {
            throw new NullPointerException("annotations == null");
        } else {
            this.method = method;
            this.annotations = annotations;
        }
    }

    public int hashCode() {
        return this.method.hashCode();
    }

    public boolean equals(Object other) {
        if (other instanceof MethodAnnotationStruct) {
            return this.method.equals(((MethodAnnotationStruct) other).method);
        }
        return false;
    }

    public int compareTo(MethodAnnotationStruct other) {
        return this.method.compareTo(other.method);
    }

    public void addContents(DexFile file) {
        MethodIdsSection methodIds = file.getMethodIds();
        MixedItemSection wordData = file.getWordData();
        methodIds.intern(this.method);
        this.annotations = (AnnotationSetItem) wordData.intern(this.annotations);
    }

    public void writeTo(DexFile file, AnnotatedOutput out) {
        int methodIdx = file.getMethodIds().indexOf(this.method);
        int annotationsOff = this.annotations.getAbsoluteOffset();
        if (out.annotates()) {
            out.annotate(0, "    " + this.method.toHuman());
            out.annotate(4, "      method_idx:      " + Hex.u4(methodIdx));
            out.annotate(4, "      annotations_off: " + Hex.u4(annotationsOff));
        }
        out.writeInt(methodIdx);
        out.writeInt(annotationsOff);
    }

    public String toHuman() {
        return this.method.toHuman() + ": " + this.annotations;
    }

    public CstMethodRef getMethod() {
        return this.method;
    }

    public Annotations getAnnotations() {
        return this.annotations.getAnnotations();
    }
}
