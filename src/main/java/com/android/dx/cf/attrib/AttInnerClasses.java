package com.android.dx.cf.attrib;

import com.android.dx.util.MutabilityException;

public final class AttInnerClasses extends BaseAttribute {
    public static final String ATTRIBUTE_NAME = "InnerClasses";
    private final InnerClassList innerClasses;

    public AttInnerClasses(InnerClassList innerClasses) {
        super(ATTRIBUTE_NAME);
        try {
            if (innerClasses.isMutable()) {
                throw new MutabilityException("innerClasses.isMutable()");
            }
            this.innerClasses = innerClasses;
        } catch (NullPointerException e) {
            throw new NullPointerException("innerClasses == null");
        }
    }

    public int byteLength() {
        return (this.innerClasses.size() * 8) + 8;
    }

    public InnerClassList getInnerClasses() {
        return this.innerClasses;
    }
}
