package com.android.dx.util;

import java.util.NoSuchElementException;

public class ListIntSet implements IntSet {
    final IntList ints = new IntList();

    public ListIntSet() {
        this.ints.sort();
    }

    public void add(int value) {
        int index = this.ints.binarysearch(value);
        if (index < 0) {
            this.ints.insert(-(index + 1), value);
        }
    }

    public void remove(int value) {
        int index = this.ints.indexOf(value);
        if (index >= 0) {
            this.ints.removeIndex(index);
        }
    }

    public boolean has(int value) {
        return this.ints.indexOf(value) >= 0;
    }

    public void merge(IntSet other) {
        int i;
        if (other instanceof ListIntSet) {
            int j;
            ListIntSet o = (ListIntSet) other;
            int szThis = this.ints.size();
            int szOther = o.ints.size();
            i = 0;
            int j2 = 0;
            while (j2 < szOther && i < szThis) {
                j = j2;
                while (j < szOther && o.ints.get(j) < this.ints.get(i)) {
                    j2 = j + 1;
                    add(o.ints.get(j));
                    j = j2;
                }
                if (j == szOther) {
                    break;
                }
                while (i < szThis && o.ints.get(j) >= this.ints.get(i)) {
                    i++;
                }
                j2 = j;
            }
            j = j2;
            while (j < szOther) {
                j2 = j + 1;
                add(o.ints.get(j));
                j = j2;
            }
            this.ints.sort();
        } else if (other instanceof BitIntSet) {
            BitIntSet o2 = (BitIntSet) other;
            for (i = 0; i >= 0; i = Bits.findFirst(o2.bits, i + 1)) {
                this.ints.add(i);
            }
            this.ints.sort();
        } else {
            IntIterator iter = other.iterator();
            while (iter.hasNext()) {
                add(iter.next());
            }
        }
    }

    public int elements() {
        return this.ints.size();
    }

    public IntIterator iterator() {
        return new IntIterator() {
            private int idx = 0;

            public boolean hasNext() {
                return this.idx < ListIntSet.this.ints.size();
            }

            public int next() {
                if (hasNext()) {
                    IntList intList = ListIntSet.this.ints;
                    int i = this.idx;
                    this.idx = i + 1;
                    return intList.get(i);
                }
                throw new NoSuchElementException();
            }
        };
    }

    public String toString() {
        return this.ints.toString();
    }
}
