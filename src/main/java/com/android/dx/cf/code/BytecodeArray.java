package com.android.dx.cf.code;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstDouble;
import com.android.dx.rop.cst.CstFloat;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstInvokeDynamic;
import com.android.dx.rop.cst.CstKnownNull;
import com.android.dx.rop.cst.CstLiteralBits;
import com.android.dx.rop.cst.CstLong;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.util.Bits;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;
import java.util.ArrayList;

public final class BytecodeArray {
    public static final Visitor EMPTY_VISITOR = new BaseVisitor();
    private final ByteArray bytes;
    private final ConstantPool pool;

    public interface Visitor {
        int getPreviousOffset();

        void setPreviousOffset(int i);

        void visitBranch(int i, int i2, int i3, int i4);

        void visitConstant(int i, int i2, int i3, Constant constant, int i4);

        void visitInvalid(int i, int i2, int i3);

        void visitLocal(int i, int i2, int i3, int i4, Type type, int i5);

        void visitNewarray(int i, int i2, CstType cstType, ArrayList<Constant> arrayList);

        void visitNoArgs(int i, int i2, int i3, Type type);

        void visitSwitch(int i, int i2, int i3, SwitchList switchList, int i4);
    }

    public static class BaseVisitor implements Visitor {
        private int previousOffset = -1;

        BaseVisitor() {
        }

        public void visitInvalid(int opcode, int offset, int length) {
        }

        public void visitNoArgs(int opcode, int offset, int length, Type type) {
        }

        public void visitLocal(int opcode, int offset, int length, int idx, Type type, int value) {
        }

        public void visitConstant(int opcode, int offset, int length, Constant cst, int value) {
        }

        public void visitBranch(int opcode, int offset, int length, int target) {
        }

        public void visitSwitch(int opcode, int offset, int length, SwitchList cases, int padding) {
        }

        public void visitNewarray(int offset, int length, CstType type, ArrayList<Constant> arrayList) {
        }

        public void setPreviousOffset(int offset) {
            this.previousOffset = offset;
        }

        public int getPreviousOffset() {
            return this.previousOffset;
        }
    }

    class ConstantParserVisitor extends BaseVisitor {
        Constant cst;
        int length;
        int value;

        ConstantParserVisitor() {
        }

        private void clear() {
            this.length = 0;
        }

        public void visitInvalid(int opcode, int offset, int length) {
            clear();
        }

        public void visitNoArgs(int opcode, int offset, int length, Type type) {
            clear();
        }

        public void visitLocal(int opcode, int offset, int length, int idx, Type type, int value) {
            clear();
        }

        public void visitConstant(int opcode, int offset, int length, Constant cst, int value) {
            this.cst = cst;
            this.length = length;
            this.value = value;
        }

        public void visitBranch(int opcode, int offset, int length, int target) {
            clear();
        }

        public void visitSwitch(int opcode, int offset, int length, SwitchList cases, int padding) {
            clear();
        }

        public void visitNewarray(int offset, int length, CstType type, ArrayList<Constant> arrayList) {
            clear();
        }

        public void setPreviousOffset(int offset) {
        }

        public int getPreviousOffset() {
            return -1;
        }
    }

    public BytecodeArray(ByteArray bytes, ConstantPool pool) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        } else if (pool == null) {
            throw new NullPointerException("pool == null");
        } else {
            this.bytes = bytes;
            this.pool = pool;
        }
    }

    public ByteArray getBytes() {
        return this.bytes;
    }

    public int size() {
        return this.bytes.size();
    }

    public int byteLength() {
        return this.bytes.size() + 4;
    }

    public void forEach(Visitor visitor) {
        int at = 0;
        while (at < this.bytes.size()) {
            at += parseInstruction(at, visitor);
        }
    }

    public int[] getInstructionOffsets() {
        int sz = this.bytes.size();
        int[] result = Bits.makeBitSet(sz);
        int at = 0;
        while (at < sz) {
            Bits.set(result, at, true);
            at += parseInstruction(at, null);
        }
        return result;
    }

    public void processWorkSet(int[] workSet, Visitor visitor) {
        if (visitor == null) {
            throw new NullPointerException("visitor == null");
        }
        while (true) {
            int offset = Bits.findFirst(workSet, 0);
            if (offset >= 0) {
                Bits.clear(workSet, offset);
                parseInstruction(offset, visitor);
                visitor.setPreviousOffset(offset);
            } else {
                return;
            }
        }
    }

    public int parseInstruction(int offset, Visitor visitor) {
        if (visitor == null) {
            visitor = EMPTY_VISITOR;
        }
        try {
            int opcode = this.bytes.getUnsignedByte(offset);
            int fmt = ByteOps.opInfo(opcode) & 31;
            int value;
            Constant cst;
            switch (opcode) {
                case 0:
                    visitor.visitNoArgs(opcode, offset, 1, Type.VOID);
                    return 1;
                case 1:
                    visitor.visitConstant(18, offset, 1, CstKnownNull.THE_ONE, 0);
                    return 1;
                case 2:
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_M1, -1);
                    return 1;
                case 3:
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_0, 0);
                    return 1;
                case 4:
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_1, 1);
                    return 1;
                case 5:
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_2, 2);
                    return 1;
                case 6:
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_3, 3);
                    return 1;
                case 7:
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_4, 4);
                    return 1;
                case 8:
                    visitor.visitConstant(18, offset, 1, CstInteger.VALUE_5, 5);
                    return 1;
                case 9:
                    visitor.visitConstant(18, offset, 1, CstLong.VALUE_0, 0);
                    return 1;
                case 10:
                    visitor.visitConstant(18, offset, 1, CstLong.VALUE_1, 0);
                    return 1;
                case 11:
                    visitor.visitConstant(18, offset, 1, CstFloat.VALUE_0, 0);
                    return 1;
                case 12:
                    visitor.visitConstant(18, offset, 1, CstFloat.VALUE_1, 0);
                    return 1;
                case 13:
                    visitor.visitConstant(18, offset, 1, CstFloat.VALUE_2, 0);
                    return 1;
                case 14:
                    visitor.visitConstant(18, offset, 1, CstDouble.VALUE_0, 0);
                    return 1;
                case 15:
                    visitor.visitConstant(18, offset, 1, CstDouble.VALUE_1, 0);
                    return 1;
                case 16:
                    value = this.bytes.getByte(offset + 1);
                    visitor.visitConstant(18, offset, 2, CstInteger.make(value), value);
                    return 2;
                case 17:
                    value = this.bytes.getShort(offset + 1);
                    visitor.visitConstant(18, offset, 3, CstInteger.make(value), value);
                    return 3;
                case 18:
                    cst = this.pool.get(this.bytes.getUnsignedByte(offset + 1));
                    visitor.visitConstant(18, offset, 2, cst, cst instanceof CstInteger ? ((CstInteger) cst).getValue() : 0);
                    return 2;
                case 19:
                    cst = this.pool.get(this.bytes.getUnsignedShort(offset + 1));
                    visitor.visitConstant(18, offset, 3, cst, cst instanceof CstInteger ? ((CstInteger) cst).getValue() : 0);
                    return 3;
                case 20:
                    visitor.visitConstant(20, offset, 3, this.pool.get(this.bytes.getUnsignedShort(offset + 1)), 0);
                    return 3;
                case 21:
                    visitor.visitLocal(21, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.INT, 0);
                    return 2;
                case 22:
                    visitor.visitLocal(21, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.LONG, 0);
                    return 2;
                case 23:
                    visitor.visitLocal(21, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.FLOAT, 0);
                    return 2;
                case 24:
                    visitor.visitLocal(21, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.DOUBLE, 0);
                    return 2;
                case 25:
                    visitor.visitLocal(21, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.OBJECT, 0);
                    return 2;
                case 26:
                case 27:
                case 28:
                case 29:
                    visitor.visitLocal(21, offset, 1, opcode - 26, Type.INT, 0);
                    return 1;
                case 30:
                case 31:
                case 32:
                case 33:
                    visitor.visitLocal(21, offset, 1, opcode - 30, Type.LONG, 0);
                    return 1;
                case 34:
                case 35:
                case 36:
                case 37:
                    visitor.visitLocal(21, offset, 1, opcode - 34, Type.FLOAT, 0);
                    return 1;
                case 38:
                case 39:
                case 40:
                case 41:
                    visitor.visitLocal(21, offset, 1, opcode - 38, Type.DOUBLE, 0);
                    return 1;
                case 42:
                case 43:
                case 44:
                case 45:
                    visitor.visitLocal(21, offset, 1, opcode - 42, Type.OBJECT, 0);
                    return 1;
                case 46:
                    visitor.visitNoArgs(46, offset, 1, Type.INT);
                    return 1;
                case 47:
                    visitor.visitNoArgs(46, offset, 1, Type.LONG);
                    return 1;
                case 48:
                    visitor.visitNoArgs(46, offset, 1, Type.FLOAT);
                    return 1;
                case 49:
                    visitor.visitNoArgs(46, offset, 1, Type.DOUBLE);
                    return 1;
                case 50:
                    visitor.visitNoArgs(46, offset, 1, Type.OBJECT);
                    return 1;
                case 51:
                    visitor.visitNoArgs(46, offset, 1, Type.BYTE);
                    return 1;
                case 52:
                    visitor.visitNoArgs(46, offset, 1, Type.CHAR);
                    return 1;
                case 53:
                    visitor.visitNoArgs(46, offset, 1, Type.SHORT);
                    return 1;
                case 54:
                    visitor.visitLocal(54, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.INT, 0);
                    return 2;
                case 55:
                    visitor.visitLocal(54, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.LONG, 0);
                    return 2;
                case 56:
                    visitor.visitLocal(54, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.FLOAT, 0);
                    return 2;
                case 57:
                    visitor.visitLocal(54, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.DOUBLE, 0);
                    return 2;
                case 58:
                    visitor.visitLocal(54, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.OBJECT, 0);
                    return 2;
                case 59:
                case 60:
                case 61:
                case ByteOps.ISTORE_3 /*62*/:
                    visitor.visitLocal(54, offset, 1, opcode - 59, Type.INT, 0);
                    return 1;
                case ByteOps.LSTORE_0 /*63*/:
                case 64:
                case ByteOps.LSTORE_2 /*65*/:
                case ByteOps.LSTORE_3 /*66*/:
                    visitor.visitLocal(54, offset, 1, opcode - 63, Type.LONG, 0);
                    return 1;
                case ByteOps.FSTORE_0 /*67*/:
                case 68:
                case 69:
                case 70:
                    visitor.visitLocal(54, offset, 1, opcode - 67, Type.FLOAT, 0);
                    return 1;
                case 71:
                case 72:
                case 73:
                case 74:
                    visitor.visitLocal(54, offset, 1, opcode - 71, Type.DOUBLE, 0);
                    return 1;
                case 75:
                case 76:
                case 77:
                case 78:
                    visitor.visitLocal(54, offset, 1, opcode - 75, Type.OBJECT, 0);
                    return 1;
                case 79:
                    visitor.visitNoArgs(79, offset, 1, Type.INT);
                    return 1;
                case 80:
                    visitor.visitNoArgs(79, offset, 1, Type.LONG);
                    return 1;
                case 81:
                    visitor.visitNoArgs(79, offset, 1, Type.FLOAT);
                    return 1;
                case 82:
                    visitor.visitNoArgs(79, offset, 1, Type.DOUBLE);
                    return 1;
                case 83:
                    visitor.visitNoArgs(79, offset, 1, Type.OBJECT);
                    return 1;
                case 84:
                    visitor.visitNoArgs(79, offset, 1, Type.BYTE);
                    return 1;
                case 85:
                    visitor.visitNoArgs(79, offset, 1, Type.CHAR);
                    return 1;
                case 86:
                    visitor.visitNoArgs(79, offset, 1, Type.SHORT);
                    return 1;
                case 87:
                case 88:
                case 89:
                case 90:
                case 91:
                case 92:
                case 93:
                case 94:
                case 95:
                    visitor.visitNoArgs(opcode, offset, 1, Type.VOID);
                    return 1;
                case 96:
                case 100:
                case 104:
                case 108:
                case 112:
                case 116:
                case 120:
                case ByteOps.ISHR /*122*/:
                case 124:
                case 126:
                case 128:
                case 130:
                    visitor.visitNoArgs(opcode, offset, 1, Type.INT);
                    return 1;
                case 97:
                case 101:
                case 105:
                case 109:
                case 113:
                case 117:
                case ByteOps.LSHL /*121*/:
                case 123:
                case 125:
                case 127:
                case 129:
                case 131:
                    visitor.visitNoArgs(opcode - 1, offset, 1, Type.LONG);
                    return 1;
                case 98:
                case 102:
                case 106:
                case 110:
                case 114:
                case 118:
                    visitor.visitNoArgs(opcode - 2, offset, 1, Type.FLOAT);
                    return 1;
                case 99:
                case 103:
                case 107:
                case 111:
                case ByteOps.DREM /*115*/:
                case 119:
                    visitor.visitNoArgs(opcode - 3, offset, 1, Type.DOUBLE);
                    return 1;
                case 132:
                    visitor.visitLocal(opcode, offset, 3, this.bytes.getUnsignedByte(offset + 1), Type.INT, this.bytes.getByte(offset + 2));
                    return 3;
                case 133:
                case 140:
                case 143:
                    visitor.visitNoArgs(opcode, offset, 1, Type.LONG);
                    return 1;
                case 134:
                case 137:
                case 144:
                    visitor.visitNoArgs(opcode, offset, 1, Type.FLOAT);
                    return 1;
                case 135:
                case 138:
                case 141:
                    visitor.visitNoArgs(opcode, offset, 1, Type.DOUBLE);
                    return 1;
                case 136:
                case 139:
                case 142:
                case 145:
                case 146:
                case 147:
                case 148:
                case 149:
                case 150:
                case 151:
                case 152:
                case 190:
                    visitor.visitNoArgs(opcode, offset, 1, Type.INT);
                    return 1;
                case 153:
                case 154:
                case 155:
                case 156:
                case 157:
                case 158:
                case 159:
                case 160:
                case 161:
                case 162:
                case 163:
                case 164:
                case 165:
                case 166:
                case 167:
                case 168:
                case 198:
                case 199:
                    visitor.visitBranch(opcode, offset, 3, offset + this.bytes.getShort(offset + 1));
                    return 3;
                case 169:
                    visitor.visitLocal(opcode, offset, 2, this.bytes.getUnsignedByte(offset + 1), Type.RETURN_ADDRESS, 0);
                    return 2;
                case 170:
                    return parseTableswitch(offset, visitor);
                case 171:
                    return parseLookupswitch(offset, visitor);
                case 172:
                    visitor.visitNoArgs(172, offset, 1, Type.INT);
                    return 1;
                case 173:
                    visitor.visitNoArgs(172, offset, 1, Type.LONG);
                    return 1;
                case 174:
                    visitor.visitNoArgs(172, offset, 1, Type.FLOAT);
                    return 1;
                case 175:
                    visitor.visitNoArgs(172, offset, 1, Type.DOUBLE);
                    return 1;
                case 176:
                    visitor.visitNoArgs(172, offset, 1, Type.OBJECT);
                    return 1;
                case 177:
                case 191:
                case 194:
                case 195:
                    visitor.visitNoArgs(opcode, offset, 1, Type.VOID);
                    return 1;
                case 178:
                case 179:
                case 180:
                case 181:
                case 182:
                case 183:
                case 184:
                case 187:
                case 189:
                case 192:
                case 193:
                    visitor.visitConstant(opcode, offset, 3, this.pool.get(this.bytes.getUnsignedShort(offset + 1)), 0);
                    return 3;
                case 185:
                    visitor.visitConstant(opcode, offset, 5, this.pool.get(this.bytes.getUnsignedShort(offset + 1)), this.bytes.getUnsignedByte(offset + 3) | (this.bytes.getUnsignedByte(offset + 4) << 8));
                    return 5;
                case 186:
                    visitor.visitConstant(opcode, offset, 5, (CstInvokeDynamic) this.pool.get(this.bytes.getUnsignedShort(offset + 1)), 0);
                    return 5;
                case 188:
                    return parseNewarray(offset, visitor);
                case 196:
                    return parseWide(offset, visitor);
                case 197:
                    visitor.visitConstant(opcode, offset, 4, this.pool.get(this.bytes.getUnsignedShort(offset + 1)), this.bytes.getUnsignedByte(offset + 3));
                    return 4;
                case 200:
                case 201:
                    int newop;
                    int target = offset + this.bytes.getInt(offset + 1);
                    if (opcode == 200) {
                        newop = 167;
                    } else {
                        newop = 168;
                    }
                    visitor.visitBranch(newop, offset, 5, target);
                    return 5;
                default:
                    visitor.visitInvalid(opcode, offset, 1);
                    return 1;
            }
        } catch (SimException ex) {
            ex.addContext("...at bytecode offset " + Hex.u4(offset));
            throw ex;
        } catch (Throwable ex2) {
            SimException simException = new SimException(ex2);
            simException.addContext("...at bytecode offset " + Hex.u4(offset));
            throw simException;
        }
    }

    private int parseTableswitch(int offset, Visitor visitor) {
        int i;
        int at = (offset + 4) & -4;
        int padding = 0;
        for (i = offset + 1; i < at; i++) {
            padding = (padding << 8) | this.bytes.getUnsignedByte(i);
        }
        int defaultTarget = offset + this.bytes.getInt(at);
        int low = this.bytes.getInt(at + 4);
        int high = this.bytes.getInt(at + 8);
        int count = (high - low) + 1;
        at += 12;
        if (low > high) {
            throw new SimException("low / high inversion");
        }
        SwitchList cases = new SwitchList(count);
        for (i = 0; i < count; i++) {
            int target = offset + this.bytes.getInt(at);
            at += 4;
            cases.add(low + i, target);
        }
        cases.setDefaultTarget(defaultTarget);
        cases.removeSuperfluousDefaults();
        cases.setImmutable();
        int length = at - offset;
        visitor.visitSwitch(171, offset, length, cases, padding);
        return length;
    }

    private int parseLookupswitch(int offset, Visitor visitor) {
        int i;
        int at = (offset + 4) & -4;
        int padding = 0;
        for (i = offset + 1; i < at; i++) {
            padding = (padding << 8) | this.bytes.getUnsignedByte(i);
        }
        int defaultTarget = offset + this.bytes.getInt(at);
        int npairs = this.bytes.getInt(at + 4);
        at += 8;
        SwitchList cases = new SwitchList(npairs);
        for (i = 0; i < npairs; i++) {
            at += 8;
            cases.add(this.bytes.getInt(at), offset + this.bytes.getInt(at + 4));
        }
        cases.setDefaultTarget(defaultTarget);
        cases.removeSuperfluousDefaults();
        cases.setImmutable();
        int length = at - offset;
        visitor.visitSwitch(171, offset, length, cases, padding);
        return length;
    }

    private int parseNewarray(int offset, Visitor visitor) {
        CstType type;
        int value = this.bytes.getUnsignedByte(offset + 1);
        switch (value) {
            case 4:
                type = CstType.BOOLEAN_ARRAY;
                break;
            case 5:
                type = CstType.CHAR_ARRAY;
                break;
            case 6:
                type = CstType.FLOAT_ARRAY;
                break;
            case 7:
                type = CstType.DOUBLE_ARRAY;
                break;
            case 8:
                type = CstType.BYTE_ARRAY;
                break;
            case 9:
                type = CstType.SHORT_ARRAY;
                break;
            case 10:
                type = CstType.INT_ARRAY;
                break;
            case 11:
                type = CstType.LONG_ARRAY;
                break;
            default:
                throw new SimException("bad newarray code " + Hex.u1(value));
        }
        int previousOffset = visitor.getPreviousOffset();
        ConstantParserVisitor constantVisitor = new ConstantParserVisitor();
        int arrayLength = 0;
        if (previousOffset >= 0) {
            parseInstruction(previousOffset, constantVisitor);
            if ((constantVisitor.cst instanceof CstInteger) && constantVisitor.length + previousOffset == offset) {
                arrayLength = constantVisitor.value;
            }
        }
        int nInit = 0;
        int curOffset = offset + 2;
        int lastOffset = curOffset;
        ArrayList<Constant> initVals = new ArrayList();
        if (arrayLength != 0) {
            while (true) {
                boolean punt = false;
                int curOffset2 = curOffset + 1;
                if (this.bytes.getUnsignedByte(curOffset) != 89) {
                    curOffset = curOffset2;
                } else {
                    parseInstruction(curOffset2, constantVisitor);
                    if (constantVisitor.length == 0 || !(constantVisitor.cst instanceof CstInteger)) {
                    } else if (constantVisitor.value != nInit) {
                        curOffset = curOffset2;
                    } else {
                        curOffset = curOffset2 + constantVisitor.length;
                        parseInstruction(curOffset, constantVisitor);
                        if (constantVisitor.length != 0 && (constantVisitor.cst instanceof CstLiteralBits)) {
                            curOffset += constantVisitor.length;
                            initVals.add(constantVisitor.cst);
                            curOffset2 = curOffset + 1;
                            int nextByte = this.bytes.getUnsignedByte(curOffset);
                            switch (value) {
                                case 4:
                                case 8:
                                    if (nextByte != 84) {
                                        punt = true;
                                        break;
                                    }
                                    break;
                                case 5:
                                    if (nextByte != 85) {
                                        punt = true;
                                        break;
                                    }
                                    break;
                                case 6:
                                    if (nextByte != 81) {
                                        punt = true;
                                        break;
                                    }
                                    break;
                                case 7:
                                    if (nextByte != 82) {
                                        punt = true;
                                        break;
                                    }
                                    break;
                                case 9:
                                    if (nextByte != 86) {
                                        punt = true;
                                        break;
                                    }
                                    break;
                                case 10:
                                    if (nextByte != 79) {
                                        punt = true;
                                        break;
                                    }
                                    break;
                                case 11:
                                    if (nextByte != 80) {
                                        punt = true;
                                        break;
                                    }
                                    break;
                                default:
                                    punt = true;
                                    break;
                            }
                            if (punt) {
                                curOffset = curOffset2;
                            } else {
                                lastOffset = curOffset2;
                                nInit++;
                                curOffset = curOffset2;
                            }
                        }
                    }
                }
            }
        }
        if (nInit < 2 || nInit != arrayLength) {
            visitor.visitNewarray(offset, 2, type, null);
            return 2;
        }
        visitor.visitNewarray(offset, lastOffset - offset, type, initVals);
        return lastOffset - offset;
    }

    private int parseWide(int offset, Visitor visitor) {
        int opcode = this.bytes.getUnsignedByte(offset + 1);
        int idx = this.bytes.getUnsignedShort(offset + 2);
        switch (opcode) {
            case 21:
                visitor.visitLocal(21, offset, 4, idx, Type.INT, 0);
                return 4;
            case 22:
                visitor.visitLocal(21, offset, 4, idx, Type.LONG, 0);
                return 4;
            case 23:
                visitor.visitLocal(21, offset, 4, idx, Type.FLOAT, 0);
                return 4;
            case 24:
                visitor.visitLocal(21, offset, 4, idx, Type.DOUBLE, 0);
                return 4;
            case 25:
                visitor.visitLocal(21, offset, 4, idx, Type.OBJECT, 0);
                return 4;
            case 54:
                visitor.visitLocal(54, offset, 4, idx, Type.INT, 0);
                return 4;
            case 55:
                visitor.visitLocal(54, offset, 4, idx, Type.LONG, 0);
                return 4;
            case 56:
                visitor.visitLocal(54, offset, 4, idx, Type.FLOAT, 0);
                return 4;
            case 57:
                visitor.visitLocal(54, offset, 4, idx, Type.DOUBLE, 0);
                return 4;
            case 58:
                visitor.visitLocal(54, offset, 4, idx, Type.OBJECT, 0);
                return 4;
            case 132:
                Visitor visitor2 = visitor;
                int i = opcode;
                int i2 = offset;
                visitor2.visitLocal(i, i2, 6, idx, Type.INT, this.bytes.getShort(offset + 4));
                return 6;
            case 169:
                visitor.visitLocal(opcode, offset, 4, idx, Type.RETURN_ADDRESS, 0);
                return 4;
            default:
                visitor.visitInvalid(196, offset, 1);
                return 1;
        }
    }
}
