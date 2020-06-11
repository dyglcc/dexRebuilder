package com.android.dx.dex.file;

import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public final class AnnotationsDirectoryItem extends OffsettedItem {
    private static final int ALIGNMENT = 4;
    private static final int ELEMENT_SIZE = 8;
    private static final int HEADER_SIZE = 16;
    private AnnotationSetItem classAnnotations = null;
    private ArrayList<FieldAnnotationStruct> fieldAnnotations = null;
    private ArrayList<MethodAnnotationStruct> methodAnnotations = null;
    private ArrayList<ParameterAnnotationStruct> parameterAnnotations = null;

    public AnnotationsDirectoryItem() {
        super(4, -1);
    }

    public ItemType itemType() {
        return ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM;
    }

    public boolean isEmpty() {
        return this.classAnnotations == null && this.fieldAnnotations == null && this.methodAnnotations == null && this.parameterAnnotations == null;
    }

    public boolean isInternable() {
        return this.classAnnotations != null && this.fieldAnnotations == null && this.methodAnnotations == null && this.parameterAnnotations == null;
    }

    public int hashCode() {
        if (this.classAnnotations == null) {
            return 0;
        }
        return this.classAnnotations.hashCode();
    }

    public int compareTo0(OffsettedItem other) {
        if (isInternable()) {
            return this.classAnnotations.compareTo(((AnnotationsDirectoryItem) other).classAnnotations);
        }
        throw new UnsupportedOperationException("uninternable instance");
    }

    public void setClassAnnotations(Annotations annotations, DexFile dexFile) {
        if (annotations == null) {
            throw new NullPointerException("annotations == null");
        } else if (this.classAnnotations != null) {
            throw new UnsupportedOperationException("class annotations already set");
        } else {
            this.classAnnotations = new AnnotationSetItem(annotations, dexFile);
        }
    }

    public void addFieldAnnotations(CstFieldRef field, Annotations annotations, DexFile dexFile) {
        if (this.fieldAnnotations == null) {
            this.fieldAnnotations = new ArrayList();
        }
        this.fieldAnnotations.add(new FieldAnnotationStruct(field, new AnnotationSetItem(annotations, dexFile)));
    }

    public void addMethodAnnotations(CstMethodRef method, Annotations annotations, DexFile dexFile) {
        if (this.methodAnnotations == null) {
            this.methodAnnotations = new ArrayList();
        }
        this.methodAnnotations.add(new MethodAnnotationStruct(method, new AnnotationSetItem(annotations, dexFile)));
    }

    public void addParameterAnnotations(CstMethodRef method, AnnotationsList list, DexFile dexFile) {
        if (this.parameterAnnotations == null) {
            this.parameterAnnotations = new ArrayList();
        }
        this.parameterAnnotations.add(new ParameterAnnotationStruct(method, list, dexFile));
    }

    public Annotations getMethodAnnotations(CstMethodRef method) {
        if (this.methodAnnotations == null) {
            return null;
        }
        Iterator it = this.methodAnnotations.iterator();
        while (it.hasNext()) {
            MethodAnnotationStruct item = (MethodAnnotationStruct) it.next();
            if (item.getMethod().equals(method)) {
                return item.getAnnotations();
            }
        }
        return null;
    }

    public AnnotationsList getParameterAnnotations(CstMethodRef method) {
        if (this.parameterAnnotations == null) {
            return null;
        }
        Iterator it = this.parameterAnnotations.iterator();
        while (it.hasNext()) {
            ParameterAnnotationStruct item = (ParameterAnnotationStruct) it.next();
            if (item.getMethod().equals(method)) {
                return item.getAnnotationsList();
            }
        }
        return null;
    }

    public void addContents(DexFile file) {
        Iterator it;
        MixedItemSection wordData = file.getWordData();
        if (this.classAnnotations != null) {
            this.classAnnotations = (AnnotationSetItem) wordData.intern(this.classAnnotations);
        }
        if (this.fieldAnnotations != null) {
            it = this.fieldAnnotations.iterator();
            while (it.hasNext()) {
                ((FieldAnnotationStruct) it.next()).addContents(file);
            }
        }
        if (this.methodAnnotations != null) {
            it = this.methodAnnotations.iterator();
            while (it.hasNext()) {
                ((MethodAnnotationStruct) it.next()).addContents(file);
            }
        }
        if (this.parameterAnnotations != null) {
            it = this.parameterAnnotations.iterator();
            while (it.hasNext()) {
                ((ParameterAnnotationStruct) it.next()).addContents(file);
            }
        }
    }

    public String toHuman() {
        throw new RuntimeException("unsupported");
    }

    protected void place0(Section addedTo, int offset) {
        setWriteSize((((listSize(this.fieldAnnotations) + listSize(this.methodAnnotations)) + listSize(this.parameterAnnotations)) * 8) + 16);
    }

    protected void writeTo0(DexFile file, AnnotatedOutput out) {
        Iterator it;
        boolean annotates = out.annotates();
        int classOff = OffsettedItem.getAbsoluteOffsetOr0(this.classAnnotations);
        int fieldsSize = listSize(this.fieldAnnotations);
        int methodsSize = listSize(this.methodAnnotations);
        int parametersSize = listSize(this.parameterAnnotations);
        if (annotates) {
            out.annotate(0, offsetString() + " annotations directory");
            out.annotate(4, "  class_annotations_off: " + Hex.u4(classOff));
            out.annotate(4, "  fields_size:           " + Hex.u4(fieldsSize));
            out.annotate(4, "  methods_size:          " + Hex.u4(methodsSize));
            out.annotate(4, "  parameters_size:       " + Hex.u4(parametersSize));
        }
        out.writeInt(classOff);
        out.writeInt(fieldsSize);
        out.writeInt(methodsSize);
        out.writeInt(parametersSize);
        if (fieldsSize != 0) {
            Collections.sort(this.fieldAnnotations);
            if (annotates) {
                out.annotate(0, "  fields:");
            }
            it = this.fieldAnnotations.iterator();
            while (it.hasNext()) {
                ((FieldAnnotationStruct) it.next()).writeTo(file, out);
            }
        }
        if (methodsSize != 0) {
            Collections.sort(this.methodAnnotations);
            if (annotates) {
                out.annotate(0, "  methods:");
            }
            it = this.methodAnnotations.iterator();
            while (it.hasNext()) {
                ((MethodAnnotationStruct) it.next()).writeTo(file, out);
            }
        }
        if (parametersSize != 0) {
            Collections.sort(this.parameterAnnotations);
            if (annotates) {
                out.annotate(0, "  parameters:");
            }
            it = this.parameterAnnotations.iterator();
            while (it.hasNext()) {
                ((ParameterAnnotationStruct) it.next()).writeTo(file, out);
            }
        }
    }

    private static int listSize(ArrayList<?> list) {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    void debugPrint(PrintWriter out) {
        Iterator it;
        if (this.classAnnotations != null) {
            out.println("  class annotations: " + this.classAnnotations);
        }
        if (this.fieldAnnotations != null) {
            out.println("  field annotations:");
            it = this.fieldAnnotations.iterator();
            while (it.hasNext()) {
                out.println("    " + ((FieldAnnotationStruct) it.next()).toHuman());
            }
        }
        if (this.methodAnnotations != null) {
            out.println("  method annotations:");
            it = this.methodAnnotations.iterator();
            while (it.hasNext()) {
                out.println("    " + ((MethodAnnotationStruct) it.next()).toHuman());
            }
        }
        if (this.parameterAnnotations != null) {
            out.println("  parameter annotations:");
            it = this.parameterAnnotations.iterator();
            while (it.hasNext()) {
                out.println("    " + ((ParameterAnnotationStruct) it.next()).toHuman());
            }
        }
    }
}
