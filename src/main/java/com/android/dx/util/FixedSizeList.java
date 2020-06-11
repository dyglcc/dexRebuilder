package com.android.dx.util;

import java.util.Arrays;

public class FixedSizeList extends MutabilityControl implements ToHuman {
    private Object[] arr;

    public FixedSizeList(int size) {
        super(size != 0);
        try {
            this.arr = new Object[size];
        } catch (NegativeArraySizeException e) {
            throw new IllegalArgumentException("size < 0");
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return Arrays.equals(this.arr, ((FixedSizeList) other).arr);
    }

    public int hashCode() {
        return Arrays.hashCode(this.arr);
    }

    public String toString() {
        String name = getClass().getName();
        return toString0(name.substring(name.lastIndexOf(46) + 1) + '{', ", ", "}", false);
    }

    public String toHuman() {
        String name = getClass().getName();
        return toString0(name.substring(name.lastIndexOf(46) + 1) + '{', ", ", "}", true);
    }

    public String toString(String prefix, String separator, String suffix) {
        return toString0(prefix, separator, suffix, false);
    }

    public String toHuman(String prefix, String separator, String suffix) {
        return toString0(prefix, separator, suffix, true);
    }

    public final int size() {
        return this.arr.length;
    }

    public void shrinkToFit() {
        int newSz = 0;
        for (Object obj : this.arr) {
            if (obj != null) {
                newSz++;
            }
        }
        if (sz != newSz) {
            throwIfImmutable();
            Object[] newa = new Object[newSz];
            int at = 0;
            for (Object one : this.arr) {
                if (one != null) {
                    newa[at] = one;
                    at++;
                }
            }
            this.arr = newa;
            if (newSz == 0) {
                setImmutable();
            }
        }
    }

    protected final Object get0(int n) {
        try {
            Object obj = this.arr[n];
            if (obj != null) {
                return obj;
            }
            throw new NullPointerException("unset: " + n);
        } catch (ArrayIndexOutOfBoundsException e) {
            return throwIndex(n);
        }
    }

    protected final Object getOrNull0(int n) {
        return this.arr[n];
    }

    protected final void set0(int n, Object obj) {
        throwIfImmutable();
        try {
            this.arr[n] = obj;
        } catch (ArrayIndexOutOfBoundsException e) {
            throwIndex(n);
        }
    }

    private Object throwIndex(int n) {
        if (n < 0) {
            throw new IndexOutOfBoundsException("n < 0");
        }
        throw new IndexOutOfBoundsException("n >= size()");
    }

    private String toString0(String prefix, String separator, String suffix, boolean human) {
        int len = this.arr.length;
        StringBuilder sb = new StringBuilder((len * 10) + 10);
        if (prefix != null) {
            sb.append(prefix);
        }
        for (int i = 0; i < len; i++) {
            if (!(i == 0 || separator == null)) {
                sb.append(separator);
            }
            if (human) {
                sb.append(((ToHuman) this.arr[i]).toHuman());
            } else {
                sb.append(this.arr[i]);
            }
        }
        if (suffix != null) {
            sb.append(suffix);
        }
        return sb.toString();
    }
}
