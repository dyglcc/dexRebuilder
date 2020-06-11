package com.android.dx.cf.iface;

import com.android.dx.util.FixedSizeList;

public final class StdMethodList extends FixedSizeList implements MethodList {
    public StdMethodList(int size) {
        super(size);
    }

    public Method get(int n) {
        return (Method) get0(n);
    }

    public void set(int n, Method method) {
        set0(n, method);
    }
}
