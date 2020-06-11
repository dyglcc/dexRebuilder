package com.android.dx.cf.code;

import com.android.dx.util.Hex;
import com.android.dx.util.IntList;
import com.android.dx.util.LabeledItem;

public final class ByteBlock implements LabeledItem {
    private final ByteCatchList catches;
    private final int end;
    private final int label;
    private final int start;
    private final IntList successors;

    public ByteBlock(int label, int start, int end, IntList successors, ByteCatchList catches) {
        if (label < 0) {
            throw new IllegalArgumentException("label < 0");
        } else if (start < 0) {
            throw new IllegalArgumentException("start < 0");
        } else if (end <= start) {
            throw new IllegalArgumentException("end <= start");
        } else if (successors == null) {
            throw new NullPointerException("targets == null");
        } else {
            int sz = successors.size();
            for (int i = 0; i < sz; i++) {
                if (successors.get(i) < 0) {
                    throw new IllegalArgumentException("successors[" + i + "] == " + successors.get(i));
                }
            }
            if (catches == null) {
                throw new NullPointerException("catches == null");
            }
            this.label = label;
            this.start = start;
            this.end = end;
            this.successors = successors;
            this.catches = catches;
        }
    }

    public String toString() {
        return '{' + Hex.u2(this.label) + ": " + Hex.u2(this.start) + ".." + Hex.u2(this.end) + '}';
    }

    public int getLabel() {
        return this.label;
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;
    }

    public IntList getSuccessors() {
        return this.successors;
    }

    public ByteCatchList getCatches() {
        return this.catches;
    }
}
