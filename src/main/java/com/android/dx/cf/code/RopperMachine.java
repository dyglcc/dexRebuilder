package com.android.dx.cf.code;

import com.android.dx.cf.iface.Method;
import com.android.dx.cf.iface.MethodList;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.FillArrayDataInsn;
import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.InvokePolymorphicInsn;
import com.android.dx.rop.code.PlainCstInsn;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.code.SwitchInsn;
import com.android.dx.rop.code.ThrowingCstInsn;
import com.android.dx.rop.code.ThrowingInsn;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstCallSiteRef;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.IntList;
import java.util.ArrayList;

final class RopperMachine extends ValueAwareMachine {
    private static final CstType ARRAY_REFLECT_TYPE = new CstType(Type.internClassName("java/lang/reflect/Array"));
    private static final CstMethodRef MULTIANEWARRAY_METHOD = new CstMethodRef(ARRAY_REFLECT_TYPE, new CstNat(new CstString("newInstance"), new CstString("(Ljava/lang/Class;[I)Ljava/lang/Object;")));
    private final TranslationAdvice advice;
    private boolean blockCanThrow;
    private TypeList catches;
    private boolean catchesUsed;
    private int extraBlockCount;
    private boolean hasJsr;
    private final ArrayList<Insn> insns;
    private final int maxLocals;
    private final ConcreteMethod method;
    private final MethodList methods;
    private int primarySuccessorIndex;
    private ReturnAddress returnAddress;
    private Rop returnOp;
    private SourcePosition returnPosition;
    private boolean returns;
    private final Ropper ropper;

    public RopperMachine(Ropper ropper, ConcreteMethod method, TranslationAdvice advice, MethodList methods) {
        super(method.getEffectiveDescriptor());
        if (methods == null) {
            throw new NullPointerException("methods == null");
        } else if (ropper == null) {
            throw new NullPointerException("ropper == null");
        } else if (advice == null) {
            throw new NullPointerException("advice == null");
        } else {
            this.ropper = ropper;
            this.method = method;
            this.methods = methods;
            this.advice = advice;
            this.maxLocals = method.getMaxLocals();
            this.insns = new ArrayList(25);
            this.catches = null;
            this.catchesUsed = false;
            this.returns = false;
            this.primarySuccessorIndex = -1;
            this.extraBlockCount = 0;
            this.blockCanThrow = false;
            this.returnOp = null;
            this.returnPosition = null;
        }
    }

    public ArrayList<Insn> getInsns() {
        return this.insns;
    }

    public Rop getReturnOp() {
        return this.returnOp;
    }

    public SourcePosition getReturnPosition() {
        return this.returnPosition;
    }

    public void startBlock(TypeList catches) {
        this.catches = catches;
        this.insns.clear();
        this.catchesUsed = false;
        this.returns = false;
        this.primarySuccessorIndex = 0;
        this.extraBlockCount = 0;
        this.blockCanThrow = false;
        this.hasJsr = false;
        this.returnAddress = null;
    }

    public boolean wereCatchesUsed() {
        return this.catchesUsed;
    }

    public boolean returns() {
        return this.returns;
    }

    public int getPrimarySuccessorIndex() {
        return this.primarySuccessorIndex;
    }

    public int getExtraBlockCount() {
        return this.extraBlockCount;
    }

    public boolean canThrow() {
        return this.blockCanThrow;
    }

    public boolean hasJsr() {
        return this.hasJsr;
    }

    public boolean hasRet() {
        return this.returnAddress != null;
    }

    public ReturnAddress getReturnAddress() {
        return this.returnAddress;
    }

    public void run(Frame frame, int offset, int opcode) {
        RegisterSpec dest;
        int i;
        TypeBearer type;
        Insn insn;
        RegisterSpec dest2;
        int stackPointer = this.maxLocals + frame.getStack().size();
        RegisterSpecList sources = getSources(opcode, stackPointer);
        int sourceCount = sources.size();
        super.run(frame, offset, opcode);
        SourcePosition pos = this.method.makeSourcePosistion(offset);
        RegisterSpec localTarget = getLocalTarget(opcode == 54);
        int destCount = resultCount();
        if (destCount == 0) {
            switch (opcode) {
                case 87:
                case 88:
                    return;
                default:
                    dest = null;
                    break;
            }
        } else if (localTarget != null) {
            dest = localTarget;
        } else if (destCount == 1) {
            dest = RegisterSpec.make(stackPointer, result(0));
        } else {
            RegisterSpec scratch;
            int scratchAt = this.ropper.getFirstTempStackReg();
            RegisterSpec[] scratchRegs = new RegisterSpec[sourceCount];
            for (i = 0; i < sourceCount; i++) {
                RegisterSpec src = sources.get(i);
                type = src.getTypeBearer();
                scratch = src.withReg(scratchAt);
                this.insns.add(new PlainInsn(Rops.opMove(type), pos, scratch, src));
                scratchRegs[i] = scratch;
                scratchAt += src.getCategory();
            }
            for (int pattern = getAuxInt(); pattern != 0; pattern >>= 4) {
                scratch = scratchRegs[(pattern & 15) - 1];
                type = scratch.getTypeBearer();
                this.insns.add(new PlainInsn(Rops.opMove(type), pos, scratch.withReg(stackPointer), scratch));
                stackPointer += type.getType().getCategory();
            }
            return;
        }
        TypeBearer destType = dest != null ? dest : Type.VOID;
        Constant cst = getAuxCst();
        if (opcode == 197) {
            this.blockCanThrow = true;
            this.extraBlockCount = 6;
            RegisterSpec dimsReg = RegisterSpec.make(dest.getNextReg(), Type.INT_ARRAY);
            this.insns.add(new ThrowingCstInsn(Rops.opFilledNewArray(Type.INT_ARRAY, sourceCount), pos, sources, this.catches, CstType.INT_ARRAY));
            this.insns.add(new PlainInsn(Rops.opMoveResult(Type.INT_ARRAY), pos, dimsReg, RegisterSpecList.EMPTY));
            Type componentType = ((CstType) cst).getClassType();
            for (i = 0; i < sourceCount; i++) {
                componentType = componentType.getComponentType();
            }
            RegisterSpec classReg = RegisterSpec.make(dest.getReg(), Type.CLASS);
            if (componentType.isPrimitive()) {
                Insn throwingCstInsn = new ThrowingCstInsn(Rops.GET_STATIC_OBJECT, pos, RegisterSpecList.EMPTY, this.catches, CstFieldRef.forPrimitiveType(componentType));
            } else {
                Insn throwingCstInsn2 = new ThrowingCstInsn(Rops.CONST_OBJECT, pos, RegisterSpecList.EMPTY, this.catches, new CstType(componentType));
            }
            this.insns.add(insn);
            this.insns.add(new PlainInsn(Rops.opMoveResultPseudo(classReg.getType()), pos, classReg, RegisterSpecList.EMPTY));
            RegisterSpec objectReg = RegisterSpec.make(dest.getReg(), Type.OBJECT);
            this.insns.add(new ThrowingCstInsn(Rops.opInvokeStatic(MULTIANEWARRAY_METHOD.getPrototype()), pos, RegisterSpecList.make(classReg, dimsReg), this.catches, MULTIANEWARRAY_METHOD));
            this.insns.add(new PlainInsn(Rops.opMoveResult(MULTIANEWARRAY_METHOD.getPrototype().getReturnType()), pos, objectReg, RegisterSpecList.EMPTY));
            opcode = 192;
            sources = RegisterSpecList.make(objectReg);
        } else if (opcode == 168) {
            this.hasJsr = true;
            return;
        } else if (opcode == 169) {
            try {
                this.returnAddress = (ReturnAddress) arg(0);
                return;
            } catch (Throwable ex) {
                throw new RuntimeException("Argument to RET was not a ReturnAddress", ex);
            }
        }
        int ropOpcode = jopToRopOpcode(opcode, cst);
        Rop rop = Rops.ropFor(ropOpcode, destType, sources, cst);
        Insn moveResult = null;
        Insn plainInsn;
        if (dest != null && rop.isCallLike()) {
            Type returnType;
            this.extraBlockCount++;
            if (rop.getOpcode() == 59) {
                returnType = ((CstCallSiteRef) cst).getReturnType();
            } else {
                returnType = ((CstMethodRef) cst).getPrototype().getReturnType();
            }
            plainInsn = new PlainInsn(Rops.opMoveResult(returnType), pos, dest, RegisterSpecList.EMPTY);
            dest2 = null;
        } else if (dest == null || !rop.canThrow()) {
            dest2 = dest;
        } else {
            this.extraBlockCount++;
            plainInsn = new PlainInsn(Rops.opMoveResultPseudo(dest.getTypeBearer()), pos, dest, RegisterSpecList.EMPTY);
            dest2 = null;
        }
        if (ropOpcode == 41) {
            cst = CstType.intern(rop.getResult());
        } else if (cst == null && sourceCount == 2) {
            TypeBearer firstType = sources.get(0).getTypeBearer();
            TypeBearer lastType = sources.get(1).getTypeBearer();
            if ((lastType.isConstant() || firstType.isConstant()) && this.advice.hasConstantOperation(rop, sources.get(0), sources.get(1))) {
                if (lastType.isConstant()) {
                    cst = (Constant) lastType;
                    sources = sources.withoutLast();
                    if (rop.getOpcode() == 15) {
                        ropOpcode = 14;
                        cst = CstInteger.make(-((CstInteger) lastType).getValue());
                    }
                } else {
                    cst = (Constant) firstType;
                    sources = sources.withoutFirst();
                }
                rop = Rops.ropFor(ropOpcode, destType, sources, cst);
            }
        }
        SwitchList cases = getAuxCases();
        ArrayList<Constant> initValues = getInitValues();
        boolean canThrow = rop.canThrow();
        this.blockCanThrow |= canThrow;
        if (cases != null) {
            if (cases.size() == 0) {
                insn = new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY);
                this.primarySuccessorIndex = 0;
            } else {
                IntList values = cases.getValues();
                throwingCstInsn2 = new SwitchInsn(rop, pos, dest2, sources, values);
                this.primarySuccessorIndex = values.size();
            }
        } else if (ropOpcode == 33) {
            if (sources.size() != 0) {
                RegisterSpec source = sources.get(0);
                type = source.getTypeBearer();
                if (source.getReg() != 0) {
                    this.insns.add(new PlainInsn(Rops.opMove(type), pos, RegisterSpec.make(0, type), source));
                }
            }
            insn = new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY);
            this.primarySuccessorIndex = 0;
            updateReturnOp(rop, pos);
            this.returns = true;
        } else if (cst != null) {
            if (canThrow) {
                if (rop.getOpcode() == 58) {
                    insn = makeInvokePolymorphicInsn(rop, pos, sources, this.catches, cst);
                } else {
                    insn = new ThrowingCstInsn(rop, pos, sources, this.catches, cst);
                }
                this.catchesUsed = true;
                this.primarySuccessorIndex = this.catches.size();
            } else {
                Insn plainCstInsn = new PlainCstInsn(rop, pos, dest2, sources, cst);
            }
        } else if (canThrow) {
            insn = new ThrowingInsn(rop, pos, sources, this.catches);
            this.catchesUsed = true;
            if (opcode == 191) {
                this.primarySuccessorIndex = -1;
            } else {
                this.primarySuccessorIndex = this.catches.size();
            }
        } else {
            insn = new PlainInsn(rop, pos, dest2, sources);
        }
        this.insns.add(insn);
        if (moveResult != null) {
            this.insns.add(moveResult);
        }
        if (initValues != null) {
            this.extraBlockCount++;
            this.insns.add(new FillArrayDataInsn(Rops.FILL_ARRAY_DATA, pos, RegisterSpecList.make(moveResult.getResult()), initValues, cst));
        }
    }

    private RegisterSpecList getSources(int opcode, int stackPointer) {
        int count = argCount();
        if (count == 0) {
            return RegisterSpecList.EMPTY;
        }
        RegisterSpecList sources;
        int localIndex = getLocalIndex();
        if (localIndex < 0) {
            sources = new RegisterSpecList(count);
            int regAt = stackPointer;
            for (int i = 0; i < count; i++) {
                RegisterSpec spec = RegisterSpec.make(regAt, arg(i));
                sources.set(i, spec);
                regAt += spec.getCategory();
            }
            switch (opcode) {
                case 79:
                    if (count == 3) {
                        RegisterSpec array = sources.get(0);
                        RegisterSpec index = sources.get(1);
                        sources.set(0, sources.get(2));
                        sources.set(1, array);
                        sources.set(2, index);
                        break;
                    }
                    throw new RuntimeException("shouldn't happen");
                case 181:
                    if (count == 2) {
                        RegisterSpec obj = sources.get(0);
                        sources.set(0, sources.get(1));
                        sources.set(1, obj);
                        break;
                    }
                    throw new RuntimeException("shouldn't happen");
                default:
                    break;
            }
        }
        sources = new RegisterSpecList(1);
        sources.set(0, RegisterSpec.make(localIndex, arg(0)));
        sources.setImmutable();
        return sources;
    }

    private void updateReturnOp(Rop op, SourcePosition pos) {
        if (op == null) {
            throw new NullPointerException("op == null");
        } else if (pos == null) {
            throw new NullPointerException("pos == null");
        } else if (this.returnOp == null) {
            this.returnOp = op;
            this.returnPosition = pos;
        } else if (this.returnOp != op) {
            throw new SimException("return op mismatch: " + op + ", " + this.returnOp);
        } else if (pos.getLine() > this.returnPosition.getLine()) {
            this.returnPosition = pos;
        }
    }

    private int jopToRopOpcode(int jop, Constant cst) {
        CstMethodRef ref;
        switch (jop) {
            case 0:
                return 1;
            case 18:
            case 20:
                return 5;
            case 21:
            case 54:
                return 2;
            case 46:
                return 38;
            case 79:
                return 39;
            case 96:
            case 132:
                return 14;
            case 100:
                return 15;
            case 104:
                return 16;
            case 108:
                return 17;
            case 112:
                return 18;
            case 116:
                return 19;
            case 120:
                return 23;
            case ByteOps.ISHR /*122*/:
                return 24;
            case 124:
                return 25;
            case 126:
                return 20;
            case 128:
                return 21;
            case 130:
                return 22;
            case 133:
            case 134:
            case 135:
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
                return 29;
            case 145:
                return 30;
            case 146:
                return 31;
            case 147:
                return 32;
            case 148:
            case 149:
            case 151:
                return 27;
            case 150:
            case 152:
                return 28;
            case 153:
            case 159:
            case 165:
            case 198:
                return 7;
            case 154:
            case 160:
            case 166:
            case 199:
                return 8;
            case 155:
            case 161:
                return 9;
            case 156:
            case 162:
                return 10;
            case 157:
            case 163:
                return 12;
            case 158:
            case 164:
                return 11;
            case 167:
                return 6;
            case 171:
                return 13;
            case 172:
            case 177:
                return 33;
            case 178:
                return 46;
            case 179:
                return 48;
            case 180:
                return 45;
            case 181:
                return 47;
            case 182:
                ref = (CstMethodRef) cst;
                if (ref.getDefiningClass().equals(this.method.getDefiningClass())) {
                    for (int i = 0; i < this.methods.size(); i++) {
                        Method m = this.methods.get(i);
                        if (AccessFlags.isPrivate(m.getAccessFlags()) && ref.getNat().equals(m.getNat())) {
                            return 52;
                        }
                    }
                }
                if (ref.isSignaturePolymorphic()) {
                    return 58;
                }
                return 50;
            case 183:
                ref = (CstMethodRef) cst;
                if (ref.isInstanceInit() || ref.getDefiningClass().equals(this.method.getDefiningClass())) {
                    return 52;
                }
                return 51;
            case 184:
                return 49;
            case 185:
                return 53;
            case 186:
                return 59;
            case 187:
                return 40;
            case 188:
            case 189:
                return 41;
            case 190:
                return 34;
            case 191:
                return 35;
            case 192:
                return 43;
            case 193:
                return 44;
            case 194:
                return 36;
            case 195:
                return 37;
            default:
                throw new RuntimeException("shouldn't happen");
        }
    }

    private Insn makeInvokePolymorphicInsn(Rop rop, SourcePosition pos, RegisterSpecList sources, TypeList catches, Constant cst) {
        return new InvokePolymorphicInsn(rop, pos, sources, catches, (CstMethodRef) cst);
    }
}
