package com.android.dx.dex.code;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.io.Opcodes;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstBaseMethodRef;
import com.android.dx.rop.cst.CstCallSiteRef;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.FixedSizeList;
import com.android.dx.util.IndentingWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public final class DalvInsnList extends FixedSizeList {
    private final int regCount;

    public static DalvInsnList makeImmutable(ArrayList<DalvInsn> list, int regCount) {
        int size = list.size();
        DalvInsnList result = new DalvInsnList(size, regCount);
        for (int i = 0; i < size; i++) {
            result.set(i, (DalvInsn) list.get(i));
        }
        result.setImmutable();
        return result;
    }

    public DalvInsnList(int size, int regCount) {
        super(size);
        this.regCount = regCount;
    }

    public DalvInsn get(int n) {
        return (DalvInsn) get0(n);
    }

    public void set(int n, DalvInsn insn) {
        set0(n, insn);
    }

    public int codeSize() {
        int sz = size();
        if (sz == 0) {
            return 0;
        }
        return get(sz - 1).getNextAddress();
    }

    public void writeTo(AnnotatedOutput out) {
        int i;
        DalvInsn insn;
        int startCursor = out.getCursor();
        int sz = size();
        if (out.annotates()) {
            boolean verbose = out.isVerbose();
            for (i = 0; i < sz; i++) {
                String s;
                insn = (DalvInsn) get0(i);
                int codeBytes = insn.codeSize() * 2;
                if (codeBytes != 0 || verbose) {
                    s = insn.listingString("  ", out.getAnnotationWidth(), true);
                } else {
                    s = null;
                }
                if (s != null) {
                    out.annotate(codeBytes, s);
                } else if (codeBytes != 0) {
                    out.annotate(codeBytes, "");
                }
            }
        }
        i = 0;
        while (i < sz) {
            insn = (DalvInsn) get0(i);
            try {
                insn.writeTo(out);
                i++;
            } catch (RuntimeException ex) {
                throw ExceptionWithContext.withContext(ex, "...while writing " + insn);
            }
        }
        int written = (out.getCursor() - startCursor) / 2;
        if (written != codeSize()) {
            throw new RuntimeException("write length mismatch; expected " + codeSize() + " but actually wrote " + written);
        }
    }

    public int getRegistersSize() {
        return this.regCount;
    }

    public int getOutsSize() {
        int sz = size();
        int result = 0;
        for (int i = 0; i < sz; i++) {
            DalvInsn insn = (DalvInsn) get0(i);
            int count = 0;
            if (insn instanceof CstInsn) {
                Constant cst = ((CstInsn) insn).getConstant();
                if (cst instanceof CstBaseMethodRef) {
                    count = ((CstBaseMethodRef) cst).getParameterWordCount(insn.getOpcode().getFamily() == 113);
                } else if (cst instanceof CstCallSiteRef) {
                    count = ((CstCallSiteRef) cst).getPrototype().getParameterTypes().getWordCount();
                }
            } else if (!(insn instanceof MultiCstInsn)) {
                continue;
            } else if (insn.getOpcode().getFamily() != Opcodes.INVOKE_POLYMORPHIC) {
                throw new RuntimeException("Expecting invoke-polymorphic");
            } else {
                count = ((CstProtoRef) ((MultiCstInsn) insn).getConstant(1)).getPrototype().getParameterTypes().getWordCount() + 1;
            }
            if (count > result) {
                result = count;
            }
        }
        return result;
    }

    public void debugPrint(Writer out, String prefix, boolean verbose) {
        IndentingWriter iw = new IndentingWriter(out, 0, prefix);
        int sz = size();
        int i = 0;
        while (i < sz) {
            try {
                String s;
                DalvInsn insn = (DalvInsn) get0(i);
                if (insn.codeSize() != 0 || verbose) {
                    s = insn.listingString("", 0, verbose);
                } else {
                    s = null;
                }
                if (s != null) {
                    iw.write(s);
                }
                i++;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        iw.flush();
    }

    public void debugPrint(OutputStream out, String prefix, boolean verbose) {
        Writer w = new OutputStreamWriter(out);
        debugPrint(w, prefix, verbose);
        try {
            w.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
