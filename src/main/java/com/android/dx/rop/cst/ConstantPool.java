package com.android.dx.rop.cst;

public interface ConstantPool {
    Constant get(int i);

    Constant get0Ok(int i);

    Constant[] getEntries();

    Constant getOrNull(int i);

    int size();
}
