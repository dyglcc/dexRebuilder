package com.android.dx.cf.attrib;

import com.android.dx.rop.type.TypeList;
import com.android.dx.util.MutabilityException;

public final class AttExceptions extends BaseAttribute {
    public static final String ATTRIBUTE_NAME = "Exceptions";
    private final TypeList exceptions;

    public AttExceptions(TypeList exceptions) {
        super(ATTRIBUTE_NAME);
        try {
            if (exceptions.isMutable()) {
                throw new MutabilityException("exceptions.isMutable()");
            }
            this.exceptions = exceptions;
        } catch (NullPointerException e) {
            throw new NullPointerException("exceptions == null");
        }
    }

    public int byteLength() {
        return (this.exceptions.size() * 2) + 8;
    }

    public TypeList getExceptions() {
        return this.exceptions;
    }
}
