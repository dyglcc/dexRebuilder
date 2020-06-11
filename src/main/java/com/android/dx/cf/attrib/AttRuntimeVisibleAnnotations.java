package com.android.dx.cf.attrib;

import com.android.dx.rop.annotation.Annotations;

public final class AttRuntimeVisibleAnnotations extends BaseAnnotations {
    public static final String ATTRIBUTE_NAME = "RuntimeVisibleAnnotations";

    public AttRuntimeVisibleAnnotations(Annotations annotations, int byteLength) {
        super(ATTRIBUTE_NAME, annotations, byteLength);
    }
}
