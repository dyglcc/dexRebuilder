package com.android.dx.cf.code;

import com.android.dx.cf.code.BytecodeArray.Visitor;
import com.android.dx.cf.code.LocalVariableList.Item;
import com.android.dx.dex.DexOptions;
import com.android.dx.rop.code.LocalItem;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstInterfaceMethodRef;
import com.android.dx.rop.cst.CstInvokeDynamic;
import com.android.dx.rop.cst.CstMethodHandle;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.Type;
import com.android.dx.util.Hex;
import java.util.ArrayList;

public class Simulator {
    static final /* synthetic */ boolean $assertionsDisabled = (!Simulator.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String LOCAL_MISMATCH_ERROR = "This is symptomatic of .class transformation tools that ignore local variable information.";
    private final BytecodeArray code;
    private final DexOptions dexOptions;
    private final LocalVariableList localVariables;
    private final Machine machine;
    private ConcreteMethod method;
    private final SimVisitor visitor;

    private class SimVisitor implements Visitor {
        private Frame frame = null;
        private final Machine machine;
        private int previousOffset;

        public SimVisitor() {
            this.machine = Simulator.this.machine;
        }

        public void setFrame(Frame frame) {
            if (frame == null) {
                throw new NullPointerException("frame == null");
            }
            this.frame = frame;
        }

        public void visitInvalid(int opcode, int offset, int length) {
            throw new SimException("invalid opcode " + Hex.u1(opcode));
        }

        public void visitNoArgs(int opcode, int offset, int length, Type type) {
            Type requiredArrayType;
            ExecutionStack stack;
            switch (opcode) {
                case 0:
                    this.machine.clearArgs();
                    break;
                case 46:
                    requiredArrayType = Simulator.requiredArrayTypeFor(type, this.frame.getStack().peekType(1));
                    if (requiredArrayType == Type.KNOWN_NULL) {
                        type = Type.KNOWN_NULL;
                    } else {
                        type = requiredArrayType.getComponentType();
                    }
                    this.machine.popArgs(this.frame, requiredArrayType, Type.INT);
                    break;
                case 79:
                    stack = this.frame.getStack();
                    int peekDepth = type.isCategory1() ? 2 : 3;
                    Type foundArrayType = stack.peekType(peekDepth);
                    boolean foundArrayLocal = stack.peekLocal(peekDepth);
                    requiredArrayType = Simulator.requiredArrayTypeFor(type, foundArrayType);
                    if (foundArrayLocal) {
                        if (requiredArrayType == Type.KNOWN_NULL) {
                            type = Type.KNOWN_NULL;
                        } else {
                            type = requiredArrayType.getComponentType();
                        }
                    }
                    this.machine.popArgs(this.frame, requiredArrayType, Type.INT, type);
                    break;
                case 87:
                    if (!this.frame.getStack().peekType(0).isCategory2()) {
                        this.machine.popArgs(this.frame, 1);
                        break;
                    }
                    throw Simulator.illegalTos();
                case 88:
                case 92:
                    int pattern;
                    stack = this.frame.getStack();
                    if (stack.peekType(0).isCategory2()) {
                        this.machine.popArgs(this.frame, 1);
                        pattern = 17;
                    } else if (stack.peekType(1).isCategory1()) {
                        this.machine.popArgs(this.frame, 2);
                        pattern = 8481;
                    } else {
                        throw Simulator.illegalTos();
                    }
                    if (opcode == 92) {
                        this.machine.auxIntArg(pattern);
                        break;
                    }
                    break;
                case 89:
                    if (!this.frame.getStack().peekType(0).isCategory2()) {
                        this.machine.popArgs(this.frame, 1);
                        this.machine.auxIntArg(17);
                        break;
                    }
                    throw Simulator.illegalTos();
                case 90:
                    stack = this.frame.getStack();
                    if (stack.peekType(0).isCategory1() && stack.peekType(1).isCategory1()) {
                        this.machine.popArgs(this.frame, 2);
                        this.machine.auxIntArg(530);
                        break;
                    }
                    throw Simulator.illegalTos();
                    break;
                case 91:
                    stack = this.frame.getStack();
                    if (stack.peekType(0).isCategory2()) {
                        throw Simulator.illegalTos();
                    } else if (stack.peekType(1).isCategory2()) {
                        this.machine.popArgs(this.frame, 2);
                        this.machine.auxIntArg(530);
                        break;
                    } else if (stack.peekType(2).isCategory1()) {
                        this.machine.popArgs(this.frame, 3);
                        this.machine.auxIntArg(12819);
                        break;
                    } else {
                        throw Simulator.illegalTos();
                    }
                case 93:
                    stack = this.frame.getStack();
                    if (!stack.peekType(0).isCategory2()) {
                        if (!stack.peekType(1).isCategory2() && !stack.peekType(2).isCategory2()) {
                            this.machine.popArgs(this.frame, 3);
                            this.machine.auxIntArg(205106);
                            break;
                        }
                        throw Simulator.illegalTos();
                    } else if (!stack.peekType(2).isCategory2()) {
                        this.machine.popArgs(this.frame, 2);
                        this.machine.auxIntArg(530);
                        break;
                    } else {
                        throw Simulator.illegalTos();
                    }
                    break;
                case 94:
                    stack = this.frame.getStack();
                    if (stack.peekType(0).isCategory2()) {
                        if (stack.peekType(2).isCategory2()) {
                            this.machine.popArgs(this.frame, 2);
                            this.machine.auxIntArg(530);
                            break;
                        } else if (stack.peekType(3).isCategory1()) {
                            this.machine.popArgs(this.frame, 3);
                            this.machine.auxIntArg(12819);
                            break;
                        } else {
                            throw Simulator.illegalTos();
                        }
                    } else if (!stack.peekType(1).isCategory1()) {
                        throw Simulator.illegalTos();
                    } else if (stack.peekType(2).isCategory2()) {
                        this.machine.popArgs(this.frame, 3);
                        this.machine.auxIntArg(205106);
                        break;
                    } else if (stack.peekType(3).isCategory1()) {
                        this.machine.popArgs(this.frame, 4);
                        this.machine.auxIntArg(4399427);
                        break;
                    } else {
                        throw Simulator.illegalTos();
                    }
                case 95:
                    stack = this.frame.getStack();
                    if (stack.peekType(0).isCategory1() && stack.peekType(1).isCategory1()) {
                        this.machine.popArgs(this.frame, 2);
                        this.machine.auxIntArg(18);
                        break;
                    }
                    throw Simulator.illegalTos();
                    break;
                case 96:
                case 100:
                case 104:
                case 108:
                case 112:
                case 126:
                case 128:
                case 130:
                    this.machine.popArgs(this.frame, type, type);
                    break;
                case 116:
                    this.machine.popArgs(this.frame, type);
                    break;
                case 120:
                case ByteOps.ISHR /*122*/:
                case 124:
                    this.machine.popArgs(this.frame, type, Type.INT);
                    break;
                case 133:
                case 134:
                case 135:
                case 145:
                case 146:
                case 147:
                    this.machine.popArgs(this.frame, Type.INT);
                    break;
                case 136:
                case 137:
                case 138:
                    this.machine.popArgs(this.frame, Type.LONG);
                    break;
                case 139:
                case 140:
                case 141:
                    this.machine.popArgs(this.frame, Type.FLOAT);
                    break;
                case 142:
                case 143:
                case 144:
                    this.machine.popArgs(this.frame, Type.DOUBLE);
                    break;
                case 148:
                    this.machine.popArgs(this.frame, Type.LONG, Type.LONG);
                    break;
                case 149:
                case 150:
                    this.machine.popArgs(this.frame, Type.FLOAT, Type.FLOAT);
                    break;
                case 151:
                case 152:
                    this.machine.popArgs(this.frame, Type.DOUBLE, Type.DOUBLE);
                    break;
                case 172:
                    Type checkType = type;
                    if (type == Type.OBJECT) {
                        checkType = this.frame.getStack().peekType(0);
                    }
                    this.machine.popArgs(this.frame, type);
                    checkReturnType(checkType);
                    break;
                case 177:
                    this.machine.clearArgs();
                    checkReturnType(Type.VOID);
                    break;
                case 190:
                    Type arrayType = this.frame.getStack().peekType(0);
                    if (!arrayType.isArrayOrKnownNull()) {
                        Simulator.this.fail("type mismatch: expected array type but encountered " + arrayType.toHuman());
                    }
                    this.machine.popArgs(this.frame, Type.OBJECT);
                    break;
                case 191:
                case 194:
                case 195:
                    this.machine.popArgs(this.frame, Type.OBJECT);
                    break;
                default:
                    visitInvalid(opcode, offset, length);
                    return;
            }
            this.machine.auxType(type);
            this.machine.run(this.frame, offset, opcode);
        }

        private void checkReturnType(Type encountered) {
            Type returnType = this.machine.getPrototype().getReturnType();
            if (!Merger.isPossiblyAssignableFrom(returnType, encountered)) {
                Simulator.this.fail("return type mismatch: prototype indicates " + returnType.toHuman() + ", but encountered type " + encountered.toHuman());
            }
        }

        public void visitLocal(int opcode, int offset, int length, int idx, Type type, int value) {
            int localOffset;
            Type localType;
            LocalItem item = null;
            if (opcode == 54) {
                localOffset = offset + length;
            } else {
                localOffset = offset;
            }
            Item local = Simulator.this.localVariables.pcAndIndexToLocal(localOffset, idx);
            if (local != null) {
                localType = local.getType();
                if (localType.getBasicFrameType() != type.getBasicFrameType()) {
                    local = null;
                    localType = type;
                }
            } else {
                localType = type;
            }
            switch (opcode) {
                case 21:
                case 169:
                    this.machine.localArg(this.frame, idx);
                    this.machine.localInfo(local != null ? true : Simulator.$assertionsDisabled);
                    this.machine.auxType(type);
                    break;
                case 54:
                    if (local != null) {
                        item = local.getLocalItem();
                    }
                    this.machine.popArgs(this.frame, type);
                    this.machine.auxType(type);
                    this.machine.localTarget(idx, localType, item);
                    break;
                case 132:
                    if (local != null) {
                        item = local.getLocalItem();
                    }
                    this.machine.localArg(this.frame, idx);
                    this.machine.localTarget(idx, localType, item);
                    this.machine.auxType(type);
                    this.machine.auxIntArg(value);
                    this.machine.auxCstArg(CstInteger.make(value));
                    break;
                default:
                    visitInvalid(opcode, offset, length);
                    return;
            }
            this.machine.run(this.frame, offset, opcode);
        }

        public void visitConstant(int opcode, int offset, int length, Constant cst, int value) {
            switch (opcode) {
                case 18:
                case 19:
                    if ((cst instanceof CstMethodHandle) || (cst instanceof CstProtoRef)) {
                        Simulator.this.checkConstMethodHandleSupported(cst);
                    }
                    this.machine.clearArgs();
                    break;
                case 179:
                    this.machine.popArgs(this.frame, ((CstFieldRef) cst).getType());
                    break;
                case 180:
                case 192:
                case 193:
                    this.machine.popArgs(this.frame, Type.OBJECT);
                    break;
                case 181:
                    this.machine.popArgs(this.frame, Type.OBJECT, ((CstFieldRef) cst).getType());
                    break;
                case 182:
                case 183:
                case 184:
                case 185:
                    if (cst instanceof CstInterfaceMethodRef) {
                        cst = ((CstInterfaceMethodRef) cst).toMethodRef();
                        Simulator.this.checkInvokeInterfaceSupported(opcode, (CstMethodRef) cst);
                    }
                    if ((cst instanceof CstMethodRef) && ((CstMethodRef) cst).isSignaturePolymorphic()) {
                        Simulator.this.checkInvokeSignaturePolymorphic(opcode);
                    }
                    this.machine.popArgs(this.frame, ((CstMethodRef) cst).getPrototype(opcode == 184 ? true : Simulator.$assertionsDisabled));
                    break;
                case 186:
                    Simulator.this.checkInvokeDynamicSupported(opcode);
                    CstInvokeDynamic invokeDynamicRef = (CstInvokeDynamic) cst;
                    this.machine.popArgs(this.frame, invokeDynamicRef.getPrototype());
                    cst = invokeDynamicRef.addReference();
                    break;
                case 189:
                    this.machine.popArgs(this.frame, Type.INT);
                    break;
                case 197:
                    this.machine.popArgs(this.frame, Prototype.internInts(Type.VOID, value));
                    break;
                default:
                    this.machine.clearArgs();
                    break;
            }
            this.machine.auxIntArg(value);
            this.machine.auxCstArg(cst);
            this.machine.run(this.frame, offset, opcode);
        }

        public void visitBranch(int opcode, int offset, int length, int target) {
            switch (opcode) {
                case 153:
                case 154:
                case 155:
                case 156:
                case 157:
                case 158:
                    this.machine.popArgs(this.frame, Type.INT);
                    break;
                case 159:
                case 160:
                case 161:
                case 162:
                case 163:
                case 164:
                    this.machine.popArgs(this.frame, Type.INT, Type.INT);
                    break;
                case 165:
                case 166:
                    this.machine.popArgs(this.frame, Type.OBJECT, Type.OBJECT);
                    break;
                case 167:
                case 168:
                case 200:
                case 201:
                    this.machine.clearArgs();
                    break;
                case 198:
                case 199:
                    this.machine.popArgs(this.frame, Type.OBJECT);
                    break;
                default:
                    visitInvalid(opcode, offset, length);
                    return;
            }
            this.machine.auxTargetArg(target);
            this.machine.run(this.frame, offset, opcode);
        }

        public void visitSwitch(int opcode, int offset, int length, SwitchList cases, int padding) {
            this.machine.popArgs(this.frame, Type.INT);
            this.machine.auxIntArg(padding);
            this.machine.auxSwitchArg(cases);
            this.machine.run(this.frame, offset, opcode);
        }

        public void visitNewarray(int offset, int length, CstType type, ArrayList<Constant> initValues) {
            this.machine.popArgs(this.frame, Type.INT);
            this.machine.auxInitValues(initValues);
            this.machine.auxCstArg(type);
            this.machine.run(this.frame, offset, 188);
        }

        public void setPreviousOffset(int offset) {
            this.previousOffset = offset;
        }

        public int getPreviousOffset() {
            return this.previousOffset;
        }
    }

    public Simulator(Machine machine, ConcreteMethod method, DexOptions dexOptions) {
        if (machine == null) {
            throw new NullPointerException("machine == null");
        } else if (method == null) {
            throw new NullPointerException("method == null");
        } else if (dexOptions == null) {
            throw new NullPointerException("dexOptions == null");
        } else {
            this.machine = machine;
            this.code = method.getCode();
            this.method = method;
            this.localVariables = method.getLocalVariables();
            this.visitor = new SimVisitor();
            this.dexOptions = dexOptions;
            if (method.isDefaultOrStaticInterfaceMethod()) {
                checkInterfaceMethodDeclaration(method);
            }
        }
    }

    public void simulate(ByteBlock bb, Frame frame) {
        int end = bb.getEnd();
        this.visitor.setFrame(frame);
        try {
            int off = bb.getStart();
            while (off < end) {
                int length = this.code.parseInstruction(off, this.visitor);
                this.visitor.setPreviousOffset(off);
                off += length;
            }
        } catch (SimException ex) {
            frame.annotate(ex);
            throw ex;
        }
    }

    public int simulate(int offset, Frame frame) {
        this.visitor.setFrame(frame);
        return this.code.parseInstruction(offset, this.visitor);
    }

    private static SimException illegalTos() {
        return new SimException("stack mismatch: illegal top-of-stack for opcode");
    }

    private static Type requiredArrayTypeFor(Type impliedType, Type foundArrayType) {
        if (foundArrayType == Type.KNOWN_NULL) {
            if (impliedType.isReference()) {
                return Type.KNOWN_NULL;
            }
            return impliedType.getArrayType();
        } else if (impliedType == Type.OBJECT && foundArrayType.isArray() && foundArrayType.getComponentType().isReference()) {
            return foundArrayType;
        } else {
            if (impliedType == Type.BYTE && foundArrayType == Type.BOOLEAN_ARRAY) {
                return Type.BOOLEAN_ARRAY;
            }
            return impliedType.getArrayType();
        }
    }

    private void checkConstMethodHandleSupported(Constant cst) throws SimException {
        if (!this.dexOptions.apiIsSupported(28)) {
            fail(String.format("invalid constant type %s requires --min-sdk-version >= %d (currently %d)", new Object[]{cst.typeName(), Integer.valueOf(28), Integer.valueOf(this.dexOptions.minSdkVersion)}));
        }
    }

    private void checkInvokeDynamicSupported(int opcode) throws SimException {
        if (!this.dexOptions.apiIsSupported(26)) {
            fail(String.format("invalid opcode %02x - invokedynamic requires --min-sdk-version >= %d (currently %d)", new Object[]{Integer.valueOf(opcode), Integer.valueOf(26), Integer.valueOf(this.dexOptions.minSdkVersion)}));
        }
    }

    private void checkInvokeInterfaceSupported(int opcode, CstMethodRef callee) {
        if (opcode != 185 && !this.dexOptions.apiIsSupported(24)) {
            boolean softFail = this.dexOptions.allowAllInterfaceMethodInvokes;
            if (opcode == 184) {
                softFail &= this.dexOptions.apiIsSupported(21);
            } else if (!($assertionsDisabled || opcode == 183 || opcode == 182)) {
                throw new AssertionError();
            }
            String invokeKind = opcode == 184 ? "static" : "default";
            if (softFail) {
                warn(String.format("invoking a %s interface method %s.%s strictly requires --min-sdk-version >= %d (experimental at current API level %d)", new Object[]{invokeKind, callee.getDefiningClass().toHuman(), callee.getNat().toHuman(), Integer.valueOf(24), Integer.valueOf(this.dexOptions.minSdkVersion)}));
            } else {
                fail(String.format("invoking a %s interface method %s.%s strictly requires --min-sdk-version >= %d (blocked at current API level %d)", new Object[]{invokeKind, callee.getDefiningClass().toHuman(), callee.getNat().toHuman(), Integer.valueOf(24), Integer.valueOf(this.dexOptions.minSdkVersion)}));
            }
        }
    }

    private void checkInterfaceMethodDeclaration(ConcreteMethod declaredMethod) {
        if (!this.dexOptions.apiIsSupported(24)) {
            String str = "defining a %s interface method requires --min-sdk-version >= %d (currently %d) for interface methods: %s.%s";
            Object[] objArr = new Object[5];
            objArr[0] = declaredMethod.isStaticMethod() ? "static" : "default";
            objArr[1] = Integer.valueOf(24);
            objArr[2] = Integer.valueOf(this.dexOptions.minSdkVersion);
            objArr[3] = declaredMethod.getDefiningClass().toHuman();
            objArr[4] = declaredMethod.getNat().toHuman();
            warn(String.format(str, objArr));
        }
    }

    private void checkInvokeSignaturePolymorphic(int opcode) {
        if (!this.dexOptions.apiIsSupported(26)) {
            fail(String.format("invoking a signature-polymorphic requires --min-sdk-version >= %d (currently %d)", new Object[]{Integer.valueOf(26), Integer.valueOf(this.dexOptions.minSdkVersion)}));
        } else if (opcode != 182) {
            fail("Unsupported signature polymorphic invocation (" + ByteOps.opName(opcode) + ")");
        }
    }

    private void fail(String reason) {
        throw new SimException(String.format("ERROR in %s.%s: %s", new Object[]{this.method.getDefiningClass().toHuman(), this.method.getNat().toHuman(), reason}));
    }

    private void warn(String reason) {
        this.dexOptions.err.println(String.format("WARNING in %s.%s: %s", new Object[]{this.method.getDefiningClass().toHuman(), this.method.getNat().toHuman(), reason}));
    }
}
