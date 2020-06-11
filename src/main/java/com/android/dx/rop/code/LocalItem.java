package com.android.dx.rop.code;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstString;

public class LocalItem implements Comparable<LocalItem> {
    private final CstString name;
    private final CstString signature;

    public static LocalItem make(CstString name, CstString signature) {
        if (name == null && signature == null) {
            return null;
        }
        return new LocalItem(name, signature);
    }

    private LocalItem(CstString name, CstString signature) {
        this.name = name;
        this.signature = signature;
    }

    public boolean equals(Object other) {
        if ((other instanceof LocalItem) && compareTo((LocalItem) other) == 0) {
            return true;
        }
        return false;
    }

    private static int compareHandlesNulls(CstString a, CstString b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo((Constant) b);
    }

    public int compareTo(LocalItem local) {
        int ret = compareHandlesNulls(this.name, local.name);
        if (ret != 0) {
            return ret;
        }
        return compareHandlesNulls(this.signature, local.signature);
    }

    public int hashCode() {
        int i = 0;
        int hashCode = (this.name == null ? 0 : this.name.hashCode()) * 31;
        if (this.signature != null) {
            i = this.signature.hashCode();
        }
        return hashCode + i;
    }

    public String toString() {
        if (this.name != null && this.signature == null) {
            return this.name.toQuoted();
        }
        if (this.name == null && this.signature == null) {
            return "";
        }
        return "[" + (this.name == null ? "" : this.name.toQuoted()) + "|" + (this.signature == null ? "" : this.signature.toQuoted());
    }

    public CstString getName() {
        return this.name;
    }

    public CstString getSignature() {
        return this.signature;
    }
}
