package com.android.dx.dex.file;

import com.android.dex.Leb128;
import com.android.dx.dex.code.DalvCode;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.io.PrintWriter;

public final class EncodedMethod extends EncodedMember implements Comparable<EncodedMethod> {
    private final CodeItem code;
    private final CstMethodRef method;

    public EncodedMethod(CstMethodRef method, int accessFlags, DalvCode code, TypeList throwsList) {
        super(accessFlags);
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.method = method;
        if (code == null) {
            this.code = null;
        } else {
            this.code = new CodeItem(method, code, (accessFlags & 8) != 0, throwsList);
        }
    }

    public boolean equals(Object other) {
        if ((other instanceof EncodedMethod) && compareTo((EncodedMethod) other) == 0) {
            return true;
        }
        return false;
    }

    public int compareTo(EncodedMethod other) {
        return this.method.compareTo(other.method);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(getClass().getName());
        sb.append('{');
        sb.append(Hex.u2(getAccessFlags()));
        sb.append(' ');
        sb.append(this.method);
        if (this.code != null) {
            sb.append(' ');
            sb.append(this.code);
        }
        sb.append('}');
        return sb.toString();
    }

    public void addContents(DexFile file) {
        MethodIdsSection methodIds = file.getMethodIds();
        MixedItemSection wordData = file.getWordData();
        methodIds.intern(this.method);
        if (this.code != null) {
            wordData.add(this.code);
        }
    }

    public final String toHuman() {
        return this.method.toHuman();
    }

    public final CstString getName() {
        return this.method.getNat().getName();
    }

    public void debugPrint(PrintWriter out, boolean verbose) {
        if (this.code == null) {
            out.println(getRef().toHuman() + ": abstract or native");
        } else {
            this.code.debugPrint(out, "  ", verbose);
        }
    }

    public final CstMethodRef getRef() {
        return this.method;
    }

    public int encode(DexFile file, AnnotatedOutput out, int lastIndex, int dumpSeq) {
        boolean hasCode;
        boolean shouldHaveCode;
        int methodIdx = file.getMethodIds().indexOf(this.method);
        int diff = methodIdx - lastIndex;
        int accessFlags = getAccessFlags();
        int codeOff = OffsettedItem.getAbsoluteOffsetOr0(this.code);
        if (codeOff != 0) {
            hasCode = true;
        } else {
            hasCode = false;
        }
        if ((accessFlags & 1280) == 0) {
            shouldHaveCode = true;
        } else {
            shouldHaveCode = false;
        }
        if (hasCode != shouldHaveCode) {
            throw new UnsupportedOperationException("code vs. access_flags mismatch");
        }
        if (out.annotates()) {
            out.annotate(0, String.format("  [%x] %s", new Object[]{Integer.valueOf(dumpSeq), this.method.toHuman()}));
            out.annotate(Leb128.unsignedLeb128Size(diff), "    method_idx:   " + Hex.u4(methodIdx));
            out.annotate(Leb128.unsignedLeb128Size(accessFlags), "    access_flags: " + AccessFlags.methodString(accessFlags));
            out.annotate(Leb128.unsignedLeb128Size(codeOff), "    code_off:     " + Hex.u4(codeOff));
        }
        out.writeUleb128(diff);
        out.writeUleb128(accessFlags);
        out.writeUleb128(codeOff);
        return methodIdx;
    }
}
