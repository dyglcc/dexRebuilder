package com.android.dx.cf.code;

import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.FixedSizeList;
import com.android.dx.util.IntList;

public final class ByteCatchList extends FixedSizeList {
    public static final ByteCatchList EMPTY = new ByteCatchList(0);

    public static class Item {
        private final int endPc;
        private final CstType exceptionClass;
        private final int handlerPc;
        private final int startPc;

        public Item(int startPc, int endPc, int handlerPc, CstType exceptionClass) {
            if (startPc < 0) {
                throw new IllegalArgumentException("startPc < 0");
            } else if (endPc < startPc) {
                throw new IllegalArgumentException("endPc < startPc");
            } else if (handlerPc < 0) {
                throw new IllegalArgumentException("handlerPc < 0");
            } else {
                this.startPc = startPc;
                this.endPc = endPc;
                this.handlerPc = handlerPc;
                this.exceptionClass = exceptionClass;
            }
        }

        public int getStartPc() {
            return this.startPc;
        }

        public int getEndPc() {
            return this.endPc;
        }

        public int getHandlerPc() {
            return this.handlerPc;
        }

        public CstType getExceptionClass() {
            return this.exceptionClass != null ? this.exceptionClass : CstType.OBJECT;
        }

        public boolean covers(int pc) {
            return pc >= this.startPc && pc < this.endPc;
        }
    }

    public ByteCatchList(int count) {
        super(count);
    }

    public int byteLength() {
        return (size() * 8) + 2;
    }

    public Item get(int n) {
        return (Item) get0(n);
    }

    public void set(int n, Item item) {
        if (item == null) {
            throw new NullPointerException("item == null");
        }
        set0(n, item);
    }

    public void set(int n, int startPc, int endPc, int handlerPc, CstType exceptionClass) {
        set0(n, new Item(startPc, endPc, handlerPc, exceptionClass));
    }

    public ByteCatchList listFor(int pc) {
        int i;
        int sz = size();
        Item[] resultArr = new Item[sz];
        int resultSz = 0;
        for (i = 0; i < sz; i++) {
            Item one = get(i);
            if (one.covers(pc) && typeNotFound(one, resultArr, resultSz)) {
                resultArr[resultSz] = one;
                resultSz++;
            }
        }
        if (resultSz == 0) {
            return EMPTY;
        }
        ByteCatchList result = new ByteCatchList(resultSz);
        for (i = 0; i < resultSz; i++) {
            result.set(i, resultArr[i]);
        }
        result.setImmutable();
        return result;
    }

    private static boolean typeNotFound(Item item, Item[] arr, int count) {
        CstType type = item.getExceptionClass();
        for (int i = 0; i < count; i++) {
            CstType one = arr[i].getExceptionClass();
            if (one == type || one == CstType.OBJECT) {
                return false;
            }
        }
        return true;
    }

    public IntList toTargetList(int noException) {
        int i = 1;
        if (noException < -1) {
            throw new IllegalArgumentException("noException < -1");
        }
        boolean hasDefault;
        if (noException >= 0) {
            hasDefault = true;
        } else {
            hasDefault = false;
        }
        int sz = size();
        if (sz != 0) {
            if (!hasDefault) {
                i = 0;
            }
            IntList result = new IntList(i + sz);
            for (int i2 = 0; i2 < sz; i2++) {
                result.add(get(i2).getHandlerPc());
            }
            if (hasDefault) {
                result.add(noException);
            }
            result.setImmutable();
            return result;
        } else if (hasDefault) {
            return IntList.makeImmutable(noException);
        } else {
            return IntList.EMPTY;
        }
    }

    public TypeList toRopCatchList() {
        int sz = size();
        if (sz == 0) {
            return StdTypeList.EMPTY;
        }
        TypeList result = new StdTypeList(sz);
        for (int i = 0; i < sz; i++) {
            result.set(i, get(i).getExceptionClass().getClassType());
        }
        result.setImmutable();
        return result;
    }
}
