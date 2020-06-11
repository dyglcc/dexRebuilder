package com.android.dx.rop.cst;

import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.Type;
import java.util.ArrayList;
import java.util.List;

public final class CstInvokeDynamic extends Constant {
    private final int bootstrapMethodIndex;
    private CstCallSite callSite;
    private CstType declaringClass;
    private final CstNat nat;
    private final Prototype prototype;
    private final List<CstCallSiteRef> references = new ArrayList();

    public static CstInvokeDynamic make(int bootstrapMethodIndex, CstNat nat) {
        return new CstInvokeDynamic(bootstrapMethodIndex, nat);
    }

    private CstInvokeDynamic(int bootstrapMethodIndex, CstNat nat) {
        this.bootstrapMethodIndex = bootstrapMethodIndex;
        this.nat = nat;
        this.prototype = Prototype.fromDescriptor(nat.getDescriptor().toHuman());
    }

    public CstCallSiteRef addReference() {
        CstCallSiteRef ref = new CstCallSiteRef(this, this.references.size());
        this.references.add(ref);
        return ref;
    }

    public List<CstCallSiteRef> getReferences() {
        return this.references;
    }

    public String toString() {
        return toHuman();
    }

    public String typeName() {
        return "InvokeDynamic";
    }

    public String toHuman() {
        return "InvokeDynamic(" + (this.declaringClass != null ? this.declaringClass.toHuman() : "Unknown") + ":" + this.bootstrapMethodIndex + ", " + this.nat.toHuman() + ")";
    }

    public boolean isCategory2() {
        return false;
    }

    protected int compareTo0(Constant other) {
        CstInvokeDynamic otherInvoke = (CstInvokeDynamic) other;
        int result = Integer.compare(this.bootstrapMethodIndex, otherInvoke.getBootstrapMethodIndex());
        if (result != 0) {
            return result;
        }
        result = this.nat.compareTo(otherInvoke.getNat());
        if (result != 0) {
            return result;
        }
        result = this.declaringClass.compareTo(otherInvoke.getDeclaringClass());
        if (result != 0) {
            return result;
        }
        return this.callSite.compareTo(otherInvoke.getCallSite());
    }

    public int getBootstrapMethodIndex() {
        return this.bootstrapMethodIndex;
    }

    public CstNat getNat() {
        return this.nat;
    }

    public Prototype getPrototype() {
        return this.prototype;
    }

    public Type getReturnType() {
        return this.prototype.getReturnType();
    }

    public void setDeclaringClass(CstType declaringClass) {
        if (this.declaringClass != null) {
            throw new IllegalArgumentException("already added declaring class");
        } else if (declaringClass == null) {
            throw new NullPointerException("declaringClass == null");
        } else {
            this.declaringClass = declaringClass;
        }
    }

    public CstType getDeclaringClass() {
        return this.declaringClass;
    }

    public void setCallSite(CstCallSite callSite) {
        if (this.callSite != null) {
            throw new IllegalArgumentException("already added call site");
        } else if (callSite == null) {
            throw new NullPointerException("callSite == null");
        } else {
            this.callSite = callSite;
        }
    }

    public CstCallSite getCallSite() {
        return this.callSite;
    }
}
