package com.android.dx.rop.cst;

import com.android.dx.util.FixedSizeList;

public class CstArray extends Constant {
    private final List list;

    public static final class List extends FixedSizeList implements Comparable<List> {
        public List(int size) {
            super(size);
        }

        public int compareTo(List other) {
            int compareSize;
            int thisSize = size();
            int otherSize = other.size();
            if (thisSize < otherSize) {
                compareSize = thisSize;
            } else {
                compareSize = otherSize;
            }
            for (int i = 0; i < compareSize; i++) {
                int compare = ((Constant) get0(i)).compareTo((Constant) other.get0(i));
                if (compare != 0) {
                    return compare;
                }
            }
            if (thisSize < otherSize) {
                return -1;
            }
            if (thisSize > otherSize) {
                return 1;
            }
            return 0;
        }

        public Constant get(int n) {
            return (Constant) get0(n);
        }

        public void set(int n, Constant a) {
            set0(n, a);
        }
    }

    public CstArray(List list) {
        if (list == null) {
            throw new NullPointerException("list == null");
        }
        list.throwIfMutable();
        this.list = list;
    }

    public boolean equals(Object other) {
        if (other instanceof CstArray) {
            return this.list.equals(((CstArray) other).list);
        }
        return false;
    }

    public int hashCode() {
        return this.list.hashCode();
    }

    protected int compareTo0(Constant other) {
        return this.list.compareTo(((CstArray) other).list);
    }

    public String toString() {
        return this.list.toString("array{", ", ", "}");
    }

    public String typeName() {
        return "array";
    }

    public boolean isCategory2() {
        return false;
    }

    public String toHuman() {
        return this.list.toHuman("{", ", ", "}");
    }

    public List getList() {
        return this.list;
    }
}
