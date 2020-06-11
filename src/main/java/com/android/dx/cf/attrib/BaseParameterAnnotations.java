package com.android.dx.cf.attrib;

import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.util.MutabilityException;

public abstract class BaseParameterAnnotations extends BaseAttribute {
    private final int byteLength;
    private final AnnotationsList parameterAnnotations;

    public BaseParameterAnnotations(String attributeName, AnnotationsList parameterAnnotations, int byteLength) {
        super(attributeName);
        try {
            if (parameterAnnotations.isMutable()) {
                throw new MutabilityException("parameterAnnotations.isMutable()");
            }
            this.parameterAnnotations = parameterAnnotations;
            this.byteLength = byteLength;
        } catch (NullPointerException e) {
            throw new NullPointerException("parameterAnnotations == null");
        }
    }

    public final int byteLength() {
        return this.byteLength + 6;
    }

    public final AnnotationsList getParameterAnnotations() {
        return this.parameterAnnotations;
    }
}
