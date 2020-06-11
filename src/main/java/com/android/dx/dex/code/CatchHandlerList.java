package com.android.dx.dex.code;

import com.android.dx.rop.cst.CstType;
import com.android.dx.util.FixedSizeList;
import com.android.dx.util.Hex;

public final class CatchHandlerList extends FixedSizeList implements Comparable<CatchHandlerList> {
    public static final CatchHandlerList EMPTY = new CatchHandlerList(0);

    public static class Entry implements Comparable<Entry> {
        private final CstType exceptionType;
        private final int handler;

        public Entry(CstType exceptionType, int handler) {
            if (handler < 0) {
                throw new IllegalArgumentException("handler < 0");
            } else if (exceptionType == null) {
                throw new NullPointerException("exceptionType == null");
            } else {
                this.handler = handler;
                this.exceptionType = exceptionType;
            }
        }

        public int hashCode() {
            return (this.handler * 31) + this.exceptionType.hashCode();
        }

        public boolean equals(Object other) {
            if ((other instanceof Entry) && compareTo((Entry) other) == 0) {
                return true;
            }
            return false;
        }

        public int compareTo(Entry other) {
            if (this.handler < other.handler) {
                return -1;
            }
            if (this.handler > other.handler) {
                return 1;
            }
            return this.exceptionType.compareTo(other.exceptionType);
        }

        public CstType getExceptionType() {
            return this.exceptionType;
        }

        public int getHandler() {
            return this.handler;
        }
    }

    public CatchHandlerList(int size) {
        super(size);
    }

    public Entry get(int n) {
        return (Entry) get0(n);
    }

    public String toHuman() {
        return toHuman("", "");
    }

    public String toHuman(String prefix, String header) {
        StringBuilder sb = new StringBuilder(100);
        int size = size();
        sb.append(prefix);
        sb.append(header);
        sb.append("catch ");
        for (int i = 0; i < size; i++) {
            Entry entry = get(i);
            if (i != 0) {
                sb.append(",\n");
                sb.append(prefix);
                sb.append("  ");
            }
            if (i == size - 1 && catchesAll()) {
                sb.append("<any>");
            } else {
                sb.append(entry.getExceptionType().toHuman());
            }
            sb.append(" -> ");
            sb.append(Hex.u2or4(entry.getHandler()));
        }
        return sb.toString();
    }

    public boolean catchesAll() {
        int size = size();
        if (size == 0) {
            return false;
        }
        return get(size - 1).getExceptionType().equals(CstType.OBJECT);
    }

    public void set(int n, CstType exceptionType, int handler) {
        set0(n, new Entry(exceptionType, handler));
    }

    public void set(int n, Entry entry) {
        set0(n, entry);
    }

    public int compareTo(CatchHandlerList other) {
        if (this == other) {
            return 0;
        }
        int thisSize = size();
        int otherSize = other.size();
        int checkSize = Math.min(thisSize, otherSize);
        for (int i = 0; i < checkSize; i++) {
            int compare = get(i).compareTo(other.get(i));
            if (compare != 0) {
                return compare;
            }
        }
        if (thisSize < otherSize) {
            return -1;
        }
        return thisSize > otherSize ? 1 : 0;
    }
}
