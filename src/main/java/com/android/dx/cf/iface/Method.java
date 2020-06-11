package com.android.dx.cf.iface;

import com.android.dx.rop.type.Prototype;

public interface Method extends Member {
    Prototype getEffectiveDescriptor();
}
