package com.android.dx.rop.cst;

import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.Type;

public abstract class CstBaseMethodRef extends CstMemberRef {
    private Prototype instancePrototype;
    private final Prototype prototype;

    CstBaseMethodRef(CstType definingClass, CstNat nat) {
        super(definingClass, nat);
        String descriptor = getNat().getDescriptor().getString();
        if (isSignaturePolymorphic()) {
            this.prototype = Prototype.fromDescriptor(descriptor);
        } else {
            this.prototype = Prototype.intern(descriptor);
        }
        this.instancePrototype = null;
    }

    public final Prototype getPrototype() {
        return this.prototype;
    }

    public final Prototype getPrototype(boolean isStatic) {
        if (isStatic) {
            return this.prototype;
        }
        if (this.instancePrototype == null) {
            this.instancePrototype = this.prototype.withFirstParameter(getDefiningClass().getClassType());
        }
        return this.instancePrototype;
    }

    protected final int compareTo0(Constant other) {
        int cmp = super.compareTo0(other);
        if (cmp != 0) {
            return cmp;
        }
        return this.prototype.compareTo(((CstBaseMethodRef) other).prototype);
    }

    public final Type getType() {
        return this.prototype.getReturnType();
    }

    public final int getParameterWordCount(boolean isStatic) {
        return getPrototype(isStatic).getParameterTypes().getWordCount();
    }

    public final boolean isInstanceInit() {
        return getNat().isInstanceInit();
    }

    public final boolean isClassInit() {
        return getNat().isClassInit();
    }

    public final boolean isSignaturePolymorphic() {
        boolean z = true;
        CstType definingClass = getDefiningClass();
        String string;
        if (!definingClass.equals(CstType.METHOD_HANDLE)) {
            if (definingClass.equals(CstType.VAR_HANDLE)) {
                string = getNat().getName().getString();
                switch (string.hashCode()) {
                    case -1946504908:
                        if (string.equals("getAndBitwiseOrRelease")) {
                            z = true;
                            break;
                        }
                        break;
                    case -1686727776:
                        if (string.equals("getAndBitwiseAndRelease")) {
                            z = true;
                            break;
                        }
                        break;
                    case -1671098288:
                        if (string.equals("compareAndSet")) {
                            z = true;
                            break;
                        }
                        break;
                    case -1292078254:
                        if (string.equals("compareAndExchangeRelease")) {
                            z = true;
                            break;
                        }
                        break;
                    case -1117944904:
                        if (string.equals("weakCompareAndSet")) {
                            z = true;
                            break;
                        }
                        break;
                    case -1103072857:
                        if (string.equals("getAndAddRelease")) {
                            z = true;
                            break;
                        }
                        break;
                    case -1032914329:
                        if (string.equals("getAndBitwiseAnd")) {
                            z = true;
                            break;
                        }
                        break;
                    case -1032892181:
                        if (string.equals("getAndBitwiseXor")) {
                            z = true;
                            break;
                        }
                        break;
                    case -794517348:
                        if (string.equals("getAndBitwiseXorRelease")) {
                            z = true;
                            break;
                        }
                        break;
                    case -567150350:
                        if (string.equals("weakCompareAndSetPlain")) {
                            z = true;
                            break;
                        }
                        break;
                    case -240822786:
                        if (string.equals("weakCompareAndSetAcquire")) {
                            z = true;
                            break;
                        }
                        break;
                    case -230706875:
                        if (string.equals("setRelease")) {
                            z = true;
                            break;
                        }
                        break;
                    case -127361888:
                        if (string.equals("getAcquire")) {
                            z = true;
                            break;
                        }
                        break;
                    case -37641530:
                        if (string.equals("getAndSetRelease")) {
                            z = true;
                            break;
                        }
                        break;
                    case 102230:
                        if (string.equals("get")) {
                            z = true;
                            break;
                        }
                        break;
                    case 113762:
                        if (string.equals("set")) {
                            z = true;
                            break;
                        }
                        break;
                    case 93645315:
                        if (string.equals("getAndBitwiseOrAcquire")) {
                            z = true;
                            break;
                        }
                        break;
                    case 101293086:
                        if (string.equals("setVolatile")) {
                            z = true;
                            break;
                        }
                        break;
                    case 189872914:
                        if (string.equals("getVolatile")) {
                            z = true;
                            break;
                        }
                        break;
                    case 282707520:
                        if (string.equals("getAndAdd")) {
                            z = true;
                            break;
                        }
                        break;
                    case 282724865:
                        if (string.equals("getAndSet")) {
                            z = true;
                            break;
                        }
                        break;
                    case 353422447:
                        if (string.equals("getAndBitwiseAndAcquire")) {
                            z = true;
                            break;
                        }
                        break;
                    case 470702883:
                        if (string.equals("setOpaque")) {
                            z = true;
                            break;
                        }
                        break;
                    case 685319959:
                        if (string.equals("getOpaque")) {
                            z = true;
                            break;
                        }
                        break;
                    case 748071969:
                        if (string.equals("compareAndExchangeAcquire")) {
                            z = true;
                            break;
                        }
                        break;
                    case 937077366:
                        if (string.equals("getAndAddAcquire")) {
                            z = true;
                            break;
                        }
                        break;
                    case 1245632875:
                        if (string.equals("getAndBitwiseXorAcquire")) {
                            z = true;
                            break;
                        }
                        break;
                    case 1352153939:
                        if (string.equals("getAndBitwiseOr")) {
                            z = true;
                            break;
                        }
                        break;
                    case 1483964149:
                        if (string.equals("compareAndExchange")) {
                            z = false;
                            break;
                        }
                        break;
                    case 2002508693:
                        if (string.equals("getAndSetAcquire")) {
                            z = true;
                            break;
                        }
                        break;
                    case 2013994287:
                        if (string.equals("weakCompareAndSetRelease")) {
                            z = true;
                            break;
                        }
                        break;
                }
                switch (z) {
                    case false:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                    case true:
                        return true;
                    default:
                        break;
                }
            }
        }
        string = getNat().getName().getString();
        switch (string.hashCode()) {
            case -1183693704:
                if (string.equals("invoke")) {
                    z = false;
                    break;
                }
                break;
            case 941760871:
                if (string.equals("invokeExact")) {
                    z = true;
                    break;
                }
                break;
        }
        switch (z) {
            case false:
            case true:
                return true;
        }
        return false;
    }
}
