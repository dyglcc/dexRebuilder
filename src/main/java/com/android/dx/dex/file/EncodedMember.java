package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstString;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.ToHuman;
import java.io.PrintWriter;

public abstract class EncodedMember implements ToHuman {
    private final int accessFlags;

    public abstract void addContents(DexFile dexFile);

    public abstract void debugPrint(PrintWriter printWriter, boolean z);

    public abstract int encode(DexFile dexFile, AnnotatedOutput annotatedOutput, int i, int i2);

    public abstract CstString getName();

    public EncodedMember(int accessFlags) {
        this.accessFlags = accessFlags;
    }

    public final int getAccessFlags() {
        return this.accessFlags;
    }
}
