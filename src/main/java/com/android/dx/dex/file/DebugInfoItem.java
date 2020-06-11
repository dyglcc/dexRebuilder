package com.android.dx.dex.file;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.dex.code.DalvCode;
import com.android.dx.dex.code.DalvInsnList;
import com.android.dx.dex.code.LocalList;
import com.android.dx.dex.code.PositionList;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.util.AnnotatedOutput;
import java.io.PrintWriter;

public class DebugInfoItem extends OffsettedItem {
    private static final int ALIGNMENT = 1;
    private static final boolean ENABLE_ENCODER_SELF_CHECK = false;
    private final DalvCode code;
    private byte[] encoded;
    private final boolean isStatic;
    private final CstMethodRef ref;

    public DebugInfoItem(DalvCode code, boolean isStatic, CstMethodRef ref) {
        super(1, -1);
        if (code == null) {
            throw new NullPointerException("code == null");
        }
        this.code = code;
        this.isStatic = isStatic;
        this.ref = ref;
    }

    public ItemType itemType() {
        return ItemType.TYPE_DEBUG_INFO_ITEM;
    }

    public void addContents(DexFile file) {
    }

    protected void place0(Section addedTo, int offset) {
        try {
            this.encoded = encode(addedTo.getFile(), null, null, null, false);
            setWriteSize(this.encoded.length);
        } catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex, "...while placing debug info for " + this.ref.toHuman());
        }
    }

    public String toHuman() {
        throw new RuntimeException("unsupported");
    }

    public void annotateTo(DexFile file, AnnotatedOutput out, String prefix) {
        encode(file, prefix, null, out, false);
    }

    public void debugPrint(PrintWriter out, String prefix) {
        encode(null, prefix, out, null, false);
    }

    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(offsetString() + " debug info");
            encode(file, null, null, out, true);
        }
        out.write(this.encoded);
    }

    private byte[] encode(DexFile file, String prefix, PrintWriter debugPrint, AnnotatedOutput out, boolean consume) {
        return encode0(file, prefix, debugPrint, out, consume);
    }

    private byte[] encode0(DexFile file, String prefix, PrintWriter debugPrint, AnnotatedOutput out, boolean consume) {
        PositionList positions = this.code.getPositions();
        LocalList locals = this.code.getLocals();
        DalvInsnList insns = this.code.getInsns();
        DebugInfoEncoder encoder = new DebugInfoEncoder(positions, locals, file, insns.codeSize(), insns.getRegistersSize(), this.isStatic, this.ref);
        if (debugPrint == null && out == null) {
            return encoder.convert();
        }
        return encoder.convertAndAnnotate(prefix, debugPrint, out, consume);
    }
}
