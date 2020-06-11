package com.android.dx.rop.cst;

import com.android.dx.util.ToHuman;

public abstract class Constant implements ToHuman, Comparable<Constant> {
    protected abstract int compareTo0(Constant constant);

    public abstract boolean isCategory2();

    public abstract String typeName();

    public final int compareTo(Constant other) {
        Class clazz = getClass();
        Class otherClazz = other.getClass();
        if (clazz != otherClazz) {
            return clazz.getName().compareTo(otherClazz.getName());
        }
        return compareTo0(other);
    }
}
