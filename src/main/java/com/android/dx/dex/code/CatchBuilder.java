package com.android.dx.dex.code;

import com.android.dx.rop.type.Type;
import java.util.HashSet;

public interface CatchBuilder {
    CatchTable build();

    HashSet<Type> getCatchTypes();

    boolean hasAnyCatches();
}
