package com.android.dx.cf.attrib;

import com.android.dx.cf.code.LineNumberList;
import com.android.dx.util.MutabilityException;

public final class AttLineNumberTable extends BaseAttribute {
    public static final String ATTRIBUTE_NAME = "LineNumberTable";
    private final LineNumberList lineNumbers;

    public AttLineNumberTable(LineNumberList lineNumbers) {
        super(ATTRIBUTE_NAME);
        try {
            if (lineNumbers.isMutable()) {
                throw new MutabilityException("lineNumbers.isMutable()");
            }
            this.lineNumbers = lineNumbers;
        } catch (NullPointerException e) {
            throw new NullPointerException("lineNumbers == null");
        }
    }

    public int byteLength() {
        return (this.lineNumbers.size() * 4) + 8;
    }

    public LineNumberList getLineNumbers() {
        return this.lineNumbers;
    }
}
