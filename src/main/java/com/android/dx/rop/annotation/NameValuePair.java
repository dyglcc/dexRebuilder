package com.android.dx.rop.annotation;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstString;

public final class NameValuePair implements Comparable<NameValuePair> {
    private final CstString name;
    private final Constant value;

    public NameValuePair(CstString name, Constant value) {
        if (name == null) {
            throw new NullPointerException("name == null");
        } else if (value == null) {
            throw new NullPointerException("value == null");
        } else {
            this.name = name;
            this.value = value;
        }
    }

    public String toString() {
        return this.name.toHuman() + ":" + this.value;
    }

    public int hashCode() {
        return (this.name.hashCode() * 31) + this.value.hashCode();
    }

    public boolean equals(Object other) {
        if (!(other instanceof NameValuePair)) {
            return false;
        }
        NameValuePair otherPair = (NameValuePair) other;
        if (this.name.equals(otherPair.name) && this.value.equals(otherPair.value)) {
            return true;
        }
        return false;
    }

    public int compareTo(NameValuePair other) {
        int result = this.name.compareTo(other.name);
        return result != 0 ? result : this.value.compareTo(other.value);
    }

    public CstString getName() {
        return this.name;
    }

    public Constant getValue() {
        return this.value;
    }
}
