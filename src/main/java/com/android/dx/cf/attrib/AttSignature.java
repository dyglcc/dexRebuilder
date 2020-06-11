package com.android.dx.cf.attrib;

import com.android.dx.rop.cst.CstString;

public final class AttSignature extends BaseAttribute {
    public static final String ATTRIBUTE_NAME = "Signature";
    private final CstString signature;

    public AttSignature(CstString signature) {
        super(ATTRIBUTE_NAME);
        if (signature == null) {
            throw new NullPointerException("signature == null");
        }
        this.signature = signature;
    }

    public int byteLength() {
        return 8;
    }

    public CstString getSignature() {
        return this.signature;
    }
}
