package com.android.dx.cf.iface;

public interface FieldList {
    Field get(int i);

    boolean isMutable();

    int size();
}
