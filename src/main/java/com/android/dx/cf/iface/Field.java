package com.android.dx.cf.iface;

import com.android.dx.rop.cst.TypedConstant;

public interface Field extends Member {
    TypedConstant getConstantValue();
}
