package com.android.dx.rop.cst;

import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.Type;

public class CstCallSiteRef extends Constant {
    private final int id;
    private final CstInvokeDynamic invokeDynamic;

    CstCallSiteRef(CstInvokeDynamic invokeDynamic, int id) {
        if (invokeDynamic == null) {
            throw new NullPointerException("invokeDynamic == null");
        }
        this.invokeDynamic = invokeDynamic;
        this.id = id;
    }

    public boolean isCategory2() {
        return false;
    }

    public String typeName() {
        return "CallSiteRef";
    }

    protected int compareTo0(Constant other) {
        CstCallSiteRef o = (CstCallSiteRef) other;
        int result = this.invokeDynamic.compareTo(o.invokeDynamic);
        return result != 0 ? result : Integer.compare(this.id, o.id);
    }

    public String toHuman() {
        return getCallSite().toHuman();
    }

    public String toString() {
        return getCallSite().toString();
    }

    public Prototype getPrototype() {
        return this.invokeDynamic.getPrototype();
    }

    public Type getReturnType() {
        return this.invokeDynamic.getReturnType();
    }

    public CstCallSite getCallSite() {
        return this.invokeDynamic.getCallSite();
    }
}
