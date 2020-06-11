package com.android.dx.dex.file;

import com.android.dex.Leb128;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.io.PrintWriter;

public final class EncodedField extends EncodedMember implements Comparable<EncodedField> {
    private final CstFieldRef field;

    public EncodedField(CstFieldRef field, int accessFlags) {
        super(accessFlags);
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        this.field = field;
    }

    public int hashCode() {
        return this.field.hashCode();
    }

    public boolean equals(Object other) {
        if ((other instanceof EncodedField) && compareTo((EncodedField) other) == 0) {
            return true;
        }
        return false;
    }

    public int compareTo(EncodedField other) {
        return this.field.compareTo(other.field);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(getClass().getName());
        sb.append('{');
        sb.append(Hex.u2(getAccessFlags()));
        sb.append(' ');
        sb.append(this.field);
        sb.append('}');
        return sb.toString();
    }

    public void addContents(DexFile file) {
        file.getFieldIds().intern(this.field);
    }

    public CstString getName() {
        return this.field.getNat().getName();
    }

    public String toHuman() {
        return this.field.toHuman();
    }

    public void debugPrint(PrintWriter out, boolean verbose) {
        out.println(toString());
    }

    public CstFieldRef getRef() {
        return this.field;
    }

    public int encode(DexFile file, AnnotatedOutput out, int lastIndex, int dumpSeq) {
        int fieldIdx = file.getFieldIds().indexOf(this.field);
        int diff = fieldIdx - lastIndex;
        int accessFlags = getAccessFlags();
        if (out.annotates()) {
            out.annotate(0, String.format("  [%x] %s", new Object[]{Integer.valueOf(dumpSeq), this.field.toHuman()}));
            out.annotate(Leb128.unsignedLeb128Size(diff), "    field_idx:    " + Hex.u4(fieldIdx));
            out.annotate(Leb128.unsignedLeb128Size(accessFlags), "    access_flags: " + AccessFlags.fieldString(accessFlags));
        }
        out.writeUleb128(diff);
        out.writeUleb128(accessFlags);
        return fieldIdx;
    }
}
