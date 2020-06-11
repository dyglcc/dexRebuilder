package com.android.dx.rop.cst;

import com.android.dx.cf.code.BootstrapMethodArgumentsList;
import com.android.dx.rop.cst.CstArray.List;
import com.android.dx.rop.type.Prototype;

public final class CstCallSite extends CstArray {
    public static CstCallSite make(CstMethodHandle bootstrapHandle, CstNat nat, BootstrapMethodArgumentsList optionalArguments) {
        if (bootstrapHandle == null) {
            throw new NullPointerException("bootstrapMethodHandle == null");
        } else if (nat == null) {
            throw new NullPointerException("nat == null");
        } else {
            List list = new List(optionalArguments.size() + 3);
            list.set(0, bootstrapHandle);
            list.set(1, nat.getName());
            list.set(2, new CstProtoRef(Prototype.fromDescriptor(nat.getDescriptor().getString())));
            if (optionalArguments != null) {
                for (int i = 0; i < optionalArguments.size(); i++) {
                    list.set(i + 3, optionalArguments.get(i));
                }
            }
            list.setImmutable();
            return new CstCallSite(list);
        }
    }

    private CstCallSite(List list) {
        super(list);
    }

    public boolean equals(Object other) {
        if (other instanceof CstCallSite) {
            return getList().equals(((CstCallSite) other).getList());
        }
        return false;
    }

    public int hashCode() {
        return getList().hashCode();
    }

    protected int compareTo0(Constant other) {
        return getList().compareTo(((CstCallSite) other).getList());
    }

    public String toString() {
        return getList().toString("call site{", ", ", "}");
    }

    public String typeName() {
        return "call site";
    }

    public boolean isCategory2() {
        return false;
    }

    public String toHuman() {
        return getList().toHuman("{", ", ", "}");
    }
}
