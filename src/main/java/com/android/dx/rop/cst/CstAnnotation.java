package com.android.dx.rop.cst;

import com.android.dx.rop.annotation.Annotation;

public final class CstAnnotation extends Constant {
    private final Annotation annotation;

    public CstAnnotation(Annotation annotation) {
        if (annotation == null) {
            throw new NullPointerException("annotation == null");
        }
        annotation.throwIfMutable();
        this.annotation = annotation;
    }

    public boolean equals(Object other) {
        if (other instanceof CstAnnotation) {
            return this.annotation.equals(((CstAnnotation) other).annotation);
        }
        return false;
    }

    public int hashCode() {
        return this.annotation.hashCode();
    }

    protected int compareTo0(Constant other) {
        return this.annotation.compareTo(((CstAnnotation) other).annotation);
    }

    public String toString() {
        return this.annotation.toString();
    }

    public String typeName() {
        return "annotation";
    }

    public boolean isCategory2() {
        return false;
    }

    public String toHuman() {
        return this.annotation.toString();
    }

    public Annotation getAnnotation() {
        return this.annotation;
    }
}
