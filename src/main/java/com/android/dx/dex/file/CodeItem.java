package com.android.dx.dex.file;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.dex.code.DalvCode;
import com.android.dx.dex.code.DalvCode.AssignIndicesCallback;
import com.android.dx.dex.code.DalvInsnList;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;

public final class CodeItem extends OffsettedItem {
    private static final int ALIGNMENT = 4;
    private static final int HEADER_SIZE = 16;
    private CatchStructs catches;
    private final DalvCode code;
    private DebugInfoItem debugInfo;
    private final boolean isStatic;
    private final CstMethodRef ref;
    private final TypeList throwsList;

    public CodeItem(CstMethodRef ref, DalvCode code, boolean isStatic, TypeList throwsList) {
        super(4, -1);
        if (ref == null) {
            throw new NullPointerException("ref == null");
        } else if (code == null) {
            throw new NullPointerException("code == null");
        } else if (throwsList == null) {
            throw new NullPointerException("throwsList == null");
        } else {
            this.ref = ref;
            this.code = code;
            this.isStatic = isStatic;
            this.throwsList = throwsList;
            this.catches = null;
            this.debugInfo = null;
        }
    }

    public ItemType itemType() {
        return ItemType.TYPE_CODE_ITEM;
    }

    public void addContents(DexFile file) {
        Iterator it;
        MixedItemSection byteData = file.getByteData();
        TypeIdsSection typeIds = file.getTypeIds();
        if (this.code.hasPositions() || this.code.hasLocals()) {
            this.debugInfo = new DebugInfoItem(this.code, this.isStatic, this.ref);
            byteData.add(this.debugInfo);
        }
        if (this.code.hasAnyCatches()) {
            it = this.code.getCatchTypes().iterator();
            while (it.hasNext()) {
                typeIds.intern((Type) it.next());
            }
            this.catches = new CatchStructs(this.code);
        }
        it = this.code.getInsnConstants().iterator();
        while (it.hasNext()) {
            file.internIfAppropriate((Constant) it.next());
        }
    }

    public String toString() {
        return "CodeItem{" + toHuman() + "}";
    }

    public String toHuman() {
        return this.ref.toHuman();
    }

    public CstMethodRef getRef() {
        return this.ref;
    }

    public void debugPrint(PrintWriter out, String prefix, boolean verbose) {
        out.println(this.ref.toHuman() + ":");
        DalvInsnList insns = this.code.getInsns();
        out.println("regs: " + Hex.u2(getRegistersSize()) + "; ins: " + Hex.u2(getInsSize()) + "; outs: " + Hex.u2(getOutsSize()));
        insns.debugPrint((Writer) out, prefix, verbose);
        String prefix2 = prefix + "  ";
        if (this.catches != null) {
            out.print(prefix);
            out.println("catches");
            this.catches.debugPrint(out, prefix2);
        }
        if (this.debugInfo != null) {
            out.print(prefix);
            out.println("debug info");
            this.debugInfo.debugPrint(out, prefix2);
        }
    }

    protected void place0(Section addedTo, int offset) {
        int catchesSize;
        final DexFile file = addedTo.getFile();
        this.code.assignIndices(new AssignIndicesCallback() {
            public int getIndex(Constant cst) {
                IndexedItem item = file.findItemOrNull(cst);
                if (item == null) {
                    return -1;
                }
                return item.getIndex();
            }
        });
        if (this.catches != null) {
            this.catches.encode(file);
            catchesSize = this.catches.writeSize();
        } else {
            catchesSize = 0;
        }
        int insnsSize = this.code.getInsns().codeSize();
        if ((insnsSize & 1) != 0) {
            insnsSize++;
        }
        setWriteSize(((insnsSize * 2) + 16) + catchesSize);
    }

    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        boolean annotates = out.annotates();
        int regSz = getRegistersSize();
        int outsSz = getOutsSize();
        int insSz = getInsSize();
        int insnsSz = this.code.getInsns().codeSize();
        boolean needPadding = (insnsSz & 1) != 0;
        int triesSz = this.catches == null ? 0 : this.catches.triesSize();
        int debugOff = this.debugInfo == null ? 0 : this.debugInfo.getAbsoluteOffset();
        if (annotates) {
            out.annotate(0, offsetString() + ' ' + this.ref.toHuman());
            out.annotate(2, "  registers_size: " + Hex.u2(regSz));
            out.annotate(2, "  ins_size:       " + Hex.u2(insSz));
            out.annotate(2, "  outs_size:      " + Hex.u2(outsSz));
            out.annotate(2, "  tries_size:     " + Hex.u2(triesSz));
            out.annotate(4, "  debug_off:      " + Hex.u4(debugOff));
            out.annotate(4, "  insns_size:     " + Hex.u4(insnsSz));
            if (this.throwsList.size() != 0) {
                out.annotate(0, "  throws " + StdTypeList.toHuman(this.throwsList));
            }
        }
        out.writeShort(regSz);
        out.writeShort(insSz);
        out.writeShort(outsSz);
        out.writeShort(triesSz);
        out.writeInt(debugOff);
        out.writeInt(insnsSz);
        writeCodes(file, out);
        if (this.catches != null) {
            if (needPadding) {
                if (annotates) {
                    out.annotate(2, "  padding: 0");
                }
                out.writeShort(0);
            }
            this.catches.writeTo(file, out);
        }
        if (annotates && this.debugInfo != null) {
            out.annotate(0, "  debug info");
            this.debugInfo.annotateTo(file, out, "    ");
        }
    }

    private void writeCodes(DexFile file, AnnotatedOutput out) {
        try {
            this.code.getInsns().writeTo(out);
        } catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex, "...while writing instructions for " + this.ref.toHuman());
        }
    }

    private int getInsSize() {
        return this.ref.getParameterWordCount(this.isStatic);
    }

    private int getOutsSize() {
        return this.code.getInsns().getOutsSize();
    }

    private int getRegistersSize() {
        return this.code.getInsns().getRegistersSize();
    }
}
