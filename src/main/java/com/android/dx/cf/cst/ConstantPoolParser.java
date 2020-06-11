package com.android.dx.cf.cst;

import com.android.dx.cf.iface.ParseException;
import com.android.dx.cf.iface.ParseObserver;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstDouble;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstFloat;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstInterfaceMethodRef;
import com.android.dx.rop.cst.CstInvokeDynamic;
import com.android.dx.rop.cst.CstLong;
import com.android.dx.rop.cst.CstMethodHandle;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.StdConstantPool;
import com.android.dx.rop.type.Type;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;
import java.util.BitSet;

public final class ConstantPoolParser {
    private final ByteArray bytes;
    private int endOffset = -1;
    private ParseObserver observer;
    private final int[] offsets;
    private final StdConstantPool pool;

    public ConstantPoolParser(ByteArray bytes) {
        int size = bytes.getUnsignedShort(8);
        this.bytes = bytes;
        this.pool = new StdConstantPool(size);
        this.offsets = new int[size];
    }

    public void setObserver(ParseObserver observer) {
        this.observer = observer;
    }

    public int getEndOffset() {
        parseIfNecessary();
        return this.endOffset;
    }

    public StdConstantPool getPool() {
        parseIfNecessary();
        return this.pool;
    }

    private void parseIfNecessary() {
        if (this.endOffset < 0) {
            parse();
        }
    }

    private void parse() {
        determineOffsets();
        if (this.observer != null) {
            this.observer.parsed(this.bytes, 8, 2, "constant_pool_count: " + Hex.u2(this.offsets.length));
            this.observer.parsed(this.bytes, 10, 0, "\nconstant_pool:");
            this.observer.changeIndent(1);
        }
        BitSet wasUtf8 = new BitSet(this.offsets.length);
        int i = 1;
        while (i < this.offsets.length) {
            if (this.offsets[i] != 0 && this.pool.getOrNull(i) == null) {
                parse0(i, wasUtf8);
            }
            i++;
        }
        if (this.observer != null) {
            for (i = 1; i < this.offsets.length; i++) {
                Constant cst = this.pool.getOrNull(i);
                if (cst != null) {
                    String human;
                    int offset = this.offsets[i];
                    int nextOffset = this.endOffset;
                    for (int j = i + 1; j < this.offsets.length; j++) {
                        int off = this.offsets[j];
                        if (off != 0) {
                            nextOffset = off;
                            break;
                        }
                    }
                    if (wasUtf8.get(i)) {
                        human = Hex.u2(i) + ": utf8{\"" + cst.toHuman() + "\"}";
                    } else {
                        human = Hex.u2(i) + ": " + cst.toString();
                    }
                    this.observer.parsed(this.bytes, offset, nextOffset - offset, human);
                }
            }
            this.observer.changeIndent(-1);
            this.observer.parsed(this.bytes, this.endOffset, 0, "end constant_pool");
        }
    }

    private void determineOffsets() {
        int at = 10;
        int i = 1;
        while (i < this.offsets.length) {
            int lastCategory;
            this.offsets[i] = at;
            int tag = this.bytes.getUnsignedByte(at);
            switch (tag) {
                case 1:
                    lastCategory = 1;
                    at += this.bytes.getUnsignedShort(at + 1) + 3;
                    break;
                case 3:
                case 4:
                case 9:
                case 10:
                case 11:
                case 12:
                    lastCategory = 1;
                    at += 5;
                    break;
                case 5:
                case 6:
                    lastCategory = 2;
                    at += 9;
                    break;
                case 7:
                case 8:
                    lastCategory = 1;
                    at += 3;
                    break;
                case 15:
                    lastCategory = 1;
                    at += 4;
                    break;
                case 16:
                    lastCategory = 1;
                    at += 3;
                    break;
                case 18:
                    lastCategory = 1;
                    at += 5;
                    break;
                default:
                    try {
                        throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                    } catch (ParseException ex) {
                        ex.addContext("...while preparsing cst " + Hex.u2(i) + " at offset " + Hex.u4(at));
                        throw ex;
                    }
            }
            i += lastCategory;
        }
        this.endOffset = at;
    }

    private Constant parse0(int idx, BitSet wasUtf8) {
        Constant cst = this.pool.getOrNull(idx);
        if (cst != null) {
            return cst;
        }
        int at = this.offsets[idx];
        try {
            int tag = this.bytes.getUnsignedByte(at);
            switch (tag) {
                case 1:
                    cst = parseUtf8(at);
                    wasUtf8.set(idx);
                    break;
                case 3:
                    cst = CstInteger.make(this.bytes.getInt(at + 1));
                    break;
                case 4:
                    cst = CstFloat.make(this.bytes.getInt(at + 1));
                    break;
                case 5:
                    cst = CstLong.make(this.bytes.getLong(at + 1));
                    break;
                case 6:
                    cst = CstDouble.make(this.bytes.getLong(at + 1));
                    break;
                case 7:
                    cst = new CstType(Type.internClassName(((CstString) parse0(this.bytes.getUnsignedShort(at + 1), wasUtf8)).getString()));
                    break;
                case 8:
                    cst = parse0(this.bytes.getUnsignedShort(at + 1), wasUtf8);
                    break;
                case 9:
                    cst = new CstFieldRef((CstType) parse0(this.bytes.getUnsignedShort(at + 1), wasUtf8), (CstNat) parse0(this.bytes.getUnsignedShort(at + 3), wasUtf8));
                    break;
                case 10:
                    cst = new CstMethodRef((CstType) parse0(this.bytes.getUnsignedShort(at + 1), wasUtf8), (CstNat) parse0(this.bytes.getUnsignedShort(at + 3), wasUtf8));
                    break;
                case 11:
                    cst = new CstInterfaceMethodRef((CstType) parse0(this.bytes.getUnsignedShort(at + 1), wasUtf8), (CstNat) parse0(this.bytes.getUnsignedShort(at + 3), wasUtf8));
                    break;
                case 12:
                    cst = new CstNat((CstString) parse0(this.bytes.getUnsignedShort(at + 1), wasUtf8), (CstString) parse0(this.bytes.getUnsignedShort(at + 3), wasUtf8));
                    break;
                case 15:
                    Constant ref;
                    int kind = this.bytes.getUnsignedByte(at + 1);
                    int constantIndex = this.bytes.getUnsignedShort(at + 2);
                    switch (kind) {
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                            ref = (CstFieldRef) parse0(constantIndex, wasUtf8);
                            break;
                        case 5:
                        case 8:
                            CstMethodRef ref2 = (CstMethodRef) parse0(constantIndex, wasUtf8);
                            break;
                        case 6:
                        case 7:
                            ref = parse0(constantIndex, wasUtf8);
                            if (!((ref instanceof CstMethodRef) || (ref instanceof CstInterfaceMethodRef))) {
                                throw new ParseException("Unsupported ref constant type for MethodHandle " + ref.getClass());
                            }
                        case 9:
                            CstInterfaceMethodRef ref3 = (CstInterfaceMethodRef) parse0(constantIndex, wasUtf8);
                            break;
                        default:
                            throw new ParseException("Unsupported MethodHandle kind: " + kind);
                    }
                    cst = CstMethodHandle.make(getMethodHandleTypeForKind(kind), ref);
                    break;
                case 16:
                    cst = CstProtoRef.make((CstString) parse0(this.bytes.getUnsignedShort(at + 1), wasUtf8));
                    break;
                case 18:
                    cst = CstInvokeDynamic.make(this.bytes.getUnsignedShort(at + 1), (CstNat) parse0(this.bytes.getUnsignedShort(at + 3), wasUtf8));
                    break;
                default:
                    throw new ParseException("unknown tag byte: " + Hex.u1(tag));
            }
            this.pool.set(idx, cst);
            return cst;
        } catch (ParseException ex) {
            ex.addContext("...while parsing cst " + Hex.u2(idx) + " at offset " + Hex.u4(at));
            throw ex;
        } catch (Throwable ex2) {
            ParseException parseException = new ParseException(ex2);
            parseException.addContext("...while parsing cst " + Hex.u2(idx) + " at offset " + Hex.u4(at));
            throw parseException;
        }
    }

    private CstString parseUtf8(int at) {
        int length = this.bytes.getUnsignedShort(at + 1);
        at += 3;
        try {
            return new CstString(this.bytes.slice(at, at + length));
        } catch (Throwable ex) {
            throw new ParseException(ex);
        }
    }

    private static int getMethodHandleTypeForKind(int kind) {
        switch (kind) {
            case 1:
                return 3;
            case 2:
                return 1;
            case 3:
                return 2;
            case 4:
                return 0;
            case 5:
                return 5;
            case 6:
                return 4;
            case 7:
                return 7;
            case 8:
                return 6;
            case 9:
                return 8;
            default:
                throw new IllegalArgumentException("invalid kind: " + kind);
        }
    }
}
