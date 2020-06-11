package com.android.dx.rop.code;

import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;

public class InvokePolymorphicInsn extends Insn {
    private static final CstString DEFAULT_DESCRIPTOR = new CstString("([Ljava/lang/Object;)Ljava/lang/Object;");
    private static final CstString VARHANDLE_COMPARE_AND_SET_DESCRIPTOR = new CstString("([Ljava/lang/Object;)Z");
    private static final CstString VARHANDLE_SET_DESCRIPTOR = new CstString("([Ljava/lang/Object;)V");
    private final CstMethodRef callSiteMethod;
    private final CstProtoRef callSiteProto;
    private final TypeList catches;
    private final CstMethodRef polymorphicMethod;

    public InvokePolymorphicInsn(Rop opcode, SourcePosition position, RegisterSpecList sources, TypeList catches, CstMethodRef callSiteMethod) {
        super(opcode, position, null, sources);
        if (opcode.getBranchingness() != 6) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        } else if (catches == null) {
            throw new NullPointerException("catches == null");
        } else {
            this.catches = catches;
            if (callSiteMethod == null) {
                throw new NullPointerException("callSiteMethod == null");
            } else if (callSiteMethod.isSignaturePolymorphic()) {
                this.callSiteMethod = callSiteMethod;
                this.polymorphicMethod = makePolymorphicMethod(callSiteMethod);
                this.callSiteProto = makeCallSiteProto(callSiteMethod);
            } else {
                throw new IllegalArgumentException("callSiteMethod is not signature polymorphic");
            }
        }
    }

    public TypeList getCatches() {
        return this.catches;
    }

    public void accept(Visitor visitor) {
        visitor.visitInvokePolymorphicInsn(this);
    }

    public Insn withAddedCatch(Type type) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(), getSources(), this.catches.withAddedType(type), getCallSiteMethod());
    }

    public Insn withRegisterOffset(int delta) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(), getSources().withOffset(delta), this.catches, getCallSiteMethod());
    }

    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new InvokePolymorphicInsn(getOpcode(), getPosition(), sources, this.catches, getCallSiteMethod());
    }

    public CstMethodRef getCallSiteMethod() {
        return this.callSiteMethod;
    }

    public CstMethodRef getPolymorphicMethod() {
        return this.polymorphicMethod;
    }

    public CstProtoRef getCallSiteProto() {
        return this.callSiteProto;
    }

    public String getInlineString() {
        return getPolymorphicMethod().toString() + " " + getCallSiteProto().toString() + " " + ThrowingInsn.toCatchString(this.catches);
    }

    private static CstMethodRef makePolymorphicMethod(CstMethodRef callSiteMethod) {
        CstType definingClass = callSiteMethod.getDefiningClass();
        CstString cstMethodName = callSiteMethod.getNat().getName();
        String methodName = callSiteMethod.getNat().getName().getString();
        if (definingClass.equals(CstType.METHOD_HANDLE) && (methodName.equals("invoke") || methodName.equals("invokeExact"))) {
            return new CstMethodRef(definingClass, new CstNat(cstMethodName, DEFAULT_DESCRIPTOR));
        }
        if (definingClass.equals(CstType.VAR_HANDLE)) {
            Object obj = -1;
            switch (methodName.hashCode()) {
                case -1946504908:
                    if (methodName.equals("getAndBitwiseOrRelease")) {
                        obj = 13;
                        break;
                    }
                    break;
                case -1686727776:
                    if (methodName.equals("getAndBitwiseAndRelease")) {
                        obj = 10;
                        break;
                    }
                    break;
                case -1671098288:
                    if (methodName.equals("compareAndSet")) {
                        obj = 26;
                        break;
                    }
                    break;
                case -1292078254:
                    if (methodName.equals("compareAndExchangeRelease")) {
                        obj = 2;
                        break;
                    }
                    break;
                case -1117944904:
                    if (methodName.equals("weakCompareAndSet")) {
                        obj = 27;
                        break;
                    }
                    break;
                case -1103072857:
                    if (methodName.equals("getAndAddRelease")) {
                        obj = 7;
                        break;
                    }
                    break;
                case -1032914329:
                    if (methodName.equals("getAndBitwiseAnd")) {
                        obj = 8;
                        break;
                    }
                    break;
                case -1032892181:
                    if (methodName.equals("getAndBitwiseXor")) {
                        obj = 14;
                        break;
                    }
                    break;
                case -794517348:
                    if (methodName.equals("getAndBitwiseXorRelease")) {
                        obj = 16;
                        break;
                    }
                    break;
                case -567150350:
                    if (methodName.equals("weakCompareAndSetPlain")) {
                        obj = 29;
                        break;
                    }
                    break;
                case -240822786:
                    if (methodName.equals("weakCompareAndSetAcquire")) {
                        obj = 28;
                        break;
                    }
                    break;
                case -230706875:
                    if (methodName.equals("setRelease")) {
                        obj = 24;
                        break;
                    }
                    break;
                case -127361888:
                    if (methodName.equals("getAcquire")) {
                        obj = 4;
                        break;
                    }
                    break;
                case -37641530:
                    if (methodName.equals("getAndSetRelease")) {
                        obj = 19;
                        break;
                    }
                    break;
                case 102230:
                    if (methodName.equals("get")) {
                        obj = 3;
                        break;
                    }
                    break;
                case 113762:
                    if (methodName.equals("set")) {
                        obj = 22;
                        break;
                    }
                    break;
                case 93645315:
                    if (methodName.equals("getAndBitwiseOrAcquire")) {
                        obj = 12;
                        break;
                    }
                    break;
                case 101293086:
                    if (methodName.equals("setVolatile")) {
                        obj = 25;
                        break;
                    }
                    break;
                case 189872914:
                    if (methodName.equals("getVolatile")) {
                        obj = 21;
                        break;
                    }
                    break;
                case 282707520:
                    if (methodName.equals("getAndAdd")) {
                        obj = 5;
                        break;
                    }
                    break;
                case 282724865:
                    if (methodName.equals("getAndSet")) {
                        obj = 17;
                        break;
                    }
                    break;
                case 353422447:
                    if (methodName.equals("getAndBitwiseAndAcquire")) {
                        obj = 9;
                        break;
                    }
                    break;
                case 470702883:
                    if (methodName.equals("setOpaque")) {
                        obj = 23;
                        break;
                    }
                    break;
                case 685319959:
                    if (methodName.equals("getOpaque")) {
                        obj = 20;
                        break;
                    }
                    break;
                case 748071969:
                    if (methodName.equals("compareAndExchangeAcquire")) {
                        obj = 1;
                        break;
                    }
                    break;
                case 937077366:
                    if (methodName.equals("getAndAddAcquire")) {
                        obj = 6;
                        break;
                    }
                    break;
                case 1245632875:
                    if (methodName.equals("getAndBitwiseXorAcquire")) {
                        obj = 15;
                        break;
                    }
                    break;
                case 1352153939:
                    if (methodName.equals("getAndBitwiseOr")) {
                        obj = 11;
                        break;
                    }
                    break;
                case 1483964149:
                    if (methodName.equals("compareAndExchange")) {
                        obj = null;
                        break;
                    }
                    break;
                case 2002508693:
                    if (methodName.equals("getAndSetAcquire")) {
                        obj = 18;
                        break;
                    }
                    break;
                case 2013994287:
                    if (methodName.equals("weakCompareAndSetRelease")) {
                        obj = 30;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                    return new CstMethodRef(definingClass, new CstNat(cstMethodName, DEFAULT_DESCRIPTOR));
                case 22:
                case 23:
                case 24:
                case 25:
                    return new CstMethodRef(definingClass, new CstNat(cstMethodName, VARHANDLE_SET_DESCRIPTOR));
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                    return new CstMethodRef(definingClass, new CstNat(cstMethodName, VARHANDLE_COMPARE_AND_SET_DESCRIPTOR));
            }
        }
        throw new IllegalArgumentException("Unknown signature polymorphic method: " + callSiteMethod.toHuman());
    }

    private static CstProtoRef makeCallSiteProto(CstMethodRef callSiteMethod) {
        return new CstProtoRef(callSiteMethod.getPrototype(true));
    }
}
