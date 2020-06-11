package com.android.dx.cf.attrib;

import com.android.dx.cf.code.ByteCatchList;
import com.android.dx.cf.code.BytecodeArray;
import com.android.dx.cf.iface.AttributeList;
import com.android.dx.util.MutabilityException;

public final class AttCode extends BaseAttribute {
    public static final String ATTRIBUTE_NAME = "Code";
    private final AttributeList attributes;
    private final ByteCatchList catches;
    private final BytecodeArray code;
    private final int maxLocals;
    private final int maxStack;

    public AttCode(int maxStack, int maxLocals, BytecodeArray code, ByteCatchList catches, AttributeList attributes) {
        super(ATTRIBUTE_NAME);
        if (maxStack < 0) {
            throw new IllegalArgumentException("maxStack < 0");
        } else if (maxLocals < 0) {
            throw new IllegalArgumentException("maxLocals < 0");
        } else if (code == null) {
            throw new NullPointerException("code == null");
        } else {
            try {
                if (catches.isMutable()) {
                    throw new MutabilityException("catches.isMutable()");
                }
                try {
                    if (attributes.isMutable()) {
                        throw new MutabilityException("attributes.isMutable()");
                    }
                    this.maxStack = maxStack;
                    this.maxLocals = maxLocals;
                    this.code = code;
                    this.catches = catches;
                    this.attributes = attributes;
                } catch (NullPointerException e) {
                    throw new NullPointerException("attributes == null");
                }
            } catch (NullPointerException e2) {
                throw new NullPointerException("catches == null");
            }
        }
    }

    public int byteLength() {
        return ((this.code.byteLength() + 10) + this.catches.byteLength()) + this.attributes.byteLength();
    }

    public int getMaxStack() {
        return this.maxStack;
    }

    public int getMaxLocals() {
        return this.maxLocals;
    }

    public BytecodeArray getCode() {
        return this.code;
    }

    public ByteCatchList getCatches() {
        return this.catches;
    }

    public AttributeList getAttributes() {
        return this.attributes;
    }
}
