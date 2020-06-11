package com.android.dx.dex.file;

import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstArray;
import com.android.dx.rop.cst.CstArray.List;
import com.android.dx.rop.cst.CstLiteralBits;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.Zeroes;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.ByteArrayAnnotatedOutput;
import com.android.dx.util.Writers;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public final class ClassDataItem extends OffsettedItem {
    private final ArrayList<EncodedMethod> directMethods;
    private byte[] encodedForm;
    private final ArrayList<EncodedField> instanceFields;
    private final ArrayList<EncodedField> staticFields;
    private final HashMap<EncodedField, Constant> staticValues;
    private CstArray staticValuesConstant;
    private final CstType thisClass;
    private final ArrayList<EncodedMethod> virtualMethods;

    public ClassDataItem(CstType thisClass) {
        super(1, -1);
        if (thisClass == null) {
            throw new NullPointerException("thisClass == null");
        }
        this.thisClass = thisClass;
        this.staticFields = new ArrayList(20);
        this.staticValues = new HashMap(40);
        this.instanceFields = new ArrayList(20);
        this.directMethods = new ArrayList(20);
        this.virtualMethods = new ArrayList(20);
        this.staticValuesConstant = null;
    }

    public ItemType itemType() {
        return ItemType.TYPE_CLASS_DATA_ITEM;
    }

    public String toHuman() {
        return toString();
    }

    public boolean isEmpty() {
        return this.staticFields.isEmpty() && this.instanceFields.isEmpty() && this.directMethods.isEmpty() && this.virtualMethods.isEmpty();
    }

    public void addStaticField(EncodedField field, Constant value) {
        if (field == null) {
            throw new NullPointerException("field == null");
        } else if (this.staticValuesConstant != null) {
            throw new UnsupportedOperationException("static fields already sorted");
        } else {
            this.staticFields.add(field);
            this.staticValues.put(field, value);
        }
    }

    public void addInstanceField(EncodedField field) {
        if (field == null) {
            throw new NullPointerException("field == null");
        }
        this.instanceFields.add(field);
    }

    public void addDirectMethod(EncodedMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.directMethods.add(method);
    }

    public void addVirtualMethod(EncodedMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.virtualMethods.add(method);
    }

    public ArrayList<EncodedMethod> getMethods() {
        ArrayList<EncodedMethod> result = new ArrayList(this.directMethods.size() + this.virtualMethods.size());
        result.addAll(this.directMethods);
        result.addAll(this.virtualMethods);
        return result;
    }

    public void debugPrint(Writer out, boolean verbose) {
        int i;
        PrintWriter pw = Writers.printWriterFor(out);
        int sz = this.staticFields.size();
        for (i = 0; i < sz; i++) {
            pw.println("  sfields[" + i + "]: " + this.staticFields.get(i));
        }
        sz = this.instanceFields.size();
        for (i = 0; i < sz; i++) {
            pw.println("  ifields[" + i + "]: " + this.instanceFields.get(i));
        }
        sz = this.directMethods.size();
        for (i = 0; i < sz; i++) {
            pw.println("  dmeths[" + i + "]:");
            ((EncodedMethod) this.directMethods.get(i)).debugPrint(pw, verbose);
        }
        sz = this.virtualMethods.size();
        for (i = 0; i < sz; i++) {
            pw.println("  vmeths[" + i + "]:");
            ((EncodedMethod) this.virtualMethods.get(i)).debugPrint(pw, verbose);
        }
    }

    public void addContents(DexFile file) {
        Iterator it;
        if (!this.staticFields.isEmpty()) {
            getStaticValuesConstant();
            it = this.staticFields.iterator();
            while (it.hasNext()) {
                ((EncodedField) it.next()).addContents(file);
            }
        }
        if (!this.instanceFields.isEmpty()) {
            Collections.sort(this.instanceFields);
            it = this.instanceFields.iterator();
            while (it.hasNext()) {
                ((EncodedField) it.next()).addContents(file);
            }
        }
        if (!this.directMethods.isEmpty()) {
            Collections.sort(this.directMethods);
            it = this.directMethods.iterator();
            while (it.hasNext()) {
                ((EncodedMethod) it.next()).addContents(file);
            }
        }
        if (!this.virtualMethods.isEmpty()) {
            Collections.sort(this.virtualMethods);
            it = this.virtualMethods.iterator();
            while (it.hasNext()) {
                ((EncodedMethod) it.next()).addContents(file);
            }
        }
    }

    public CstArray getStaticValuesConstant() {
        if (this.staticValuesConstant == null && this.staticFields.size() != 0) {
            this.staticValuesConstant = makeStaticValuesConstant();
        }
        return this.staticValuesConstant;
    }

    private CstArray makeStaticValuesConstant() {
        Collections.sort(this.staticFields);
        int size = this.staticFields.size();
        while (size > 0) {
            Constant cst = (Constant) this.staticValues.get((EncodedField) this.staticFields.get(size - 1));
            if (!(cst instanceof CstLiteralBits)) {
                if (cst != null) {
                    break;
                }
            } else if (((CstLiteralBits) cst).getLongBits() != 0) {
                break;
            }
            size--;
        }
        if (size == 0) {
            return null;
        }
        List list = new List(size);
        for (int i = 0; i < size; i++) {
            EncodedField field = (EncodedField) this.staticFields.get(i);
            cst = (Constant) this.staticValues.get(field);
            if (cst == null) {
                cst = Zeroes.zeroFor(field.getRef().getType());
            }
            list.set(i, cst);
        }
        list.setImmutable();
        return new CstArray(list);
    }

    protected void place0(Section addedTo, int offset) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        encodeOutput(addedTo.getFile(), out);
        this.encodedForm = out.toByteArray();
        setWriteSize(this.encodedForm.length);
    }

    private void encodeOutput(DexFile file, AnnotatedOutput out) {
        boolean annotates = out.annotates();
        if (annotates) {
            out.annotate(0, offsetString() + " class data for " + this.thisClass.toHuman());
        }
        encodeSize(file, out, "static_fields", this.staticFields.size());
        encodeSize(file, out, "instance_fields", this.instanceFields.size());
        encodeSize(file, out, "direct_methods", this.directMethods.size());
        encodeSize(file, out, "virtual_methods", this.virtualMethods.size());
        encodeList(file, out, "static_fields", this.staticFields);
        encodeList(file, out, "instance_fields", this.instanceFields);
        encodeList(file, out, "direct_methods", this.directMethods);
        encodeList(file, out, "virtual_methods", this.virtualMethods);
        if (annotates) {
            out.endAnnotation();
        }
    }

    private static void encodeSize(DexFile file, AnnotatedOutput out, String label, int size) {
        if (out.annotates()) {
            out.annotate(String.format("  %-21s %08x", new Object[]{label + "_size:", Integer.valueOf(size)}));
        }
        out.writeUleb128(size);
    }

    private static void encodeList(DexFile file, AnnotatedOutput out, String label, ArrayList<? extends EncodedMember> list) {
        int size = list.size();
        int lastIndex = 0;
        if (size != 0) {
            if (out.annotates()) {
                out.annotate(0, "  " + label + ":");
            }
            for (int i = 0; i < size; i++) {
                lastIndex = ((EncodedMember) list.get(i)).encode(file, out, lastIndex, i);
            }
        }
    }

    public void writeTo0(DexFile file, AnnotatedOutput out) {
        if (out.annotates()) {
            encodeOutput(file, out);
        } else {
            out.write(this.encodedForm);
        }
    }
}
