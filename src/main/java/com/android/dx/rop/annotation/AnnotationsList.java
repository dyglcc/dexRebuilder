package com.android.dx.rop.annotation;

import com.android.dx.util.FixedSizeList;

public final class AnnotationsList extends FixedSizeList {
    public static final AnnotationsList EMPTY = new AnnotationsList(0);

    public static AnnotationsList combine(AnnotationsList list1, AnnotationsList list2) {
        int size = list1.size();
        if (size != list2.size()) {
            throw new IllegalArgumentException("list1.size() != list2.size()");
        }
        AnnotationsList result = new AnnotationsList(size);
        for (int i = 0; i < size; i++) {
            result.set(i, Annotations.combine(list1.get(i), list2.get(i)));
        }
        result.setImmutable();
        return result;
    }

    public AnnotationsList(int size) {
        super(size);
    }

    public Annotations get(int n) {
        return (Annotations) get0(n);
    }

    public void set(int n, Annotations a) {
        a.throwIfMutable();
        set0(n, a);
    }
}
