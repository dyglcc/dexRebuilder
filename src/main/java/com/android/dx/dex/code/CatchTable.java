package com.android.dx.dex.code;

import com.android.dx.util.FixedSizeList;

public final class CatchTable extends FixedSizeList implements Comparable<CatchTable> {
    public static final CatchTable EMPTY = new CatchTable(0);

    public static class Entry implements Comparable<Entry> {
        private final int end;
        private final CatchHandlerList handlers;
        private final int start;

        public Entry(int start, int end, CatchHandlerList handlers) {
            if (start < 0) {
                throw new IllegalArgumentException("start < 0");
            } else if (end <= start) {
                throw new IllegalArgumentException("end <= start");
            } else if (handlers.isMutable()) {
                throw new IllegalArgumentException("handlers.isMutable()");
            } else {
                this.start = start;
                this.end = end;
                this.handlers = handlers;
            }
        }

        public int hashCode() {
            return (((this.start * 31) + this.end) * 31) + this.handlers.hashCode();
        }

        public boolean equals(Object other) {
            if ((other instanceof Entry) && compareTo((Entry) other) == 0) {
                return true;
            }
            return false;
        }

        public int compareTo(Entry other) {
            if (this.start < other.start) {
                return -1;
            }
            if (this.start > other.start) {
                return 1;
            }
            if (this.end < other.end) {
                return -1;
            }
            if (this.end > other.end) {
                return 1;
            }
            return this.handlers.compareTo(other.handlers);
        }

        public int getStart() {
            return this.start;
        }

        public int getEnd() {
            return this.end;
        }

        public CatchHandlerList getHandlers() {
            return this.handlers;
        }
    }

    public CatchTable(int size) {
        super(size);
    }

    public Entry get(int n) {
        return (Entry) get0(n);
    }

    public void set(int n, Entry entry) {
        set0(n, entry);
    }

    public int compareTo(CatchTable other) {
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
