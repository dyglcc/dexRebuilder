package com.android.dx.cf.direct;

import com.android.dx.cf.code.ByteOps;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.cf.iface.ParseObserver;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.rop.annotation.AnnotationVisibility;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.annotation.NameValuePair;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstAnnotation;
import com.android.dx.rop.cst.CstArray;
import com.android.dx.rop.cst.CstArray.List;
import com.android.dx.rop.cst.CstBoolean;
import com.android.dx.rop.cst.CstByte;
import com.android.dx.rop.cst.CstChar;
import com.android.dx.rop.cst.CstDouble;
import com.android.dx.rop.cst.CstEnumRef;
import com.android.dx.rop.cst.CstFloat;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstLong;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstShort;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.util.ByteArray;
import com.android.dx.util.ByteArray.MyDataInputStream;
import com.android.dx.util.Hex;
import java.io.IOException;

public final class AnnotationParser {
    private final ByteArray bytes;
    private final DirectClassFile cf;
    private final MyDataInputStream input;
    private final ParseObserver observer;
    private int parseCursor;
    private final ConstantPool pool;

    public AnnotationParser(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }
        this.cf = cf;
        this.pool = cf.getConstantPool();
        this.observer = observer;
        this.bytes = cf.getBytes().slice(offset, offset + length);
        this.input = this.bytes.makeDataInputStream();
        this.parseCursor = 0;
    }

    public Constant parseValueAttribute() {
        try {
            Constant result = parseValue();
            if (this.input.available() == 0) {
                return result;
            }
            throw new ParseException("extra data in attribute");
        } catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
    }

    public AnnotationsList parseParameterAttribute(AnnotationVisibility visibility) {
        try {
            AnnotationsList result = parseAnnotationsList(visibility);
            if (this.input.available() == 0) {
                return result;
            }
            throw new ParseException("extra data in attribute");
        } catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
    }

    public Annotations parseAnnotationAttribute(AnnotationVisibility visibility) {
        try {
            Annotations result = parseAnnotations(visibility);
            if (this.input.available() == 0) {
                return result;
            }
            throw new ParseException("extra data in attribute");
        } catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
    }

    private AnnotationsList parseAnnotationsList(AnnotationVisibility visibility) throws IOException {
        int count = this.input.readUnsignedByte();
        if (this.observer != null) {
            parsed(1, "num_parameters: " + Hex.u1(count));
        }
        AnnotationsList outerList = new AnnotationsList(count);
        for (int i = 0; i < count; i++) {
            if (this.observer != null) {
                parsed(0, "parameter_annotations[" + i + "]:");
                changeIndent(1);
            }
            outerList.set(i, parseAnnotations(visibility));
            if (this.observer != null) {
                this.observer.changeIndent(-1);
            }
        }
        outerList.setImmutable();
        return outerList;
    }

    private Annotations parseAnnotations(AnnotationVisibility visibility) throws IOException {
        int count = this.input.readUnsignedShort();
        if (this.observer != null) {
            parsed(2, "num_annotations: " + Hex.u2(count));
        }
        Annotations annotations = new Annotations();
        for (int i = 0; i < count; i++) {
            if (this.observer != null) {
                parsed(0, "annotations[" + i + "]:");
                changeIndent(1);
            }
            annotations.add(parseAnnotation(visibility));
            if (this.observer != null) {
                this.observer.changeIndent(-1);
            }
        }
        annotations.setImmutable();
        return annotations;
    }

    private Annotation parseAnnotation(AnnotationVisibility visibility) throws IOException {
        requireLength(4);
        int typeIndex = this.input.readUnsignedShort();
        int numElements = this.input.readUnsignedShort();
        CstType type = new CstType(Type.intern(((CstString) this.pool.get(typeIndex)).getString()));
        if (this.observer != null) {
            parsed(2, "type: " + type.toHuman());
            parsed(2, "num_elements: " + numElements);
        }
        Annotation annotation = new Annotation(type, visibility);
        for (int i = 0; i < numElements; i++) {
            if (this.observer != null) {
                parsed(0, "elements[" + i + "]:");
                changeIndent(1);
            }
            annotation.add(parseElement());
            if (this.observer != null) {
                changeIndent(-1);
            }
        }
        annotation.setImmutable();
        return annotation;
    }

    private NameValuePair parseElement() throws IOException {
        requireLength(5);
        CstString elementName = (CstString) this.pool.get(this.input.readUnsignedShort());
        if (this.observer != null) {
            parsed(2, "element_name: " + elementName.toHuman());
            parsed(0, "value: ");
            changeIndent(1);
        }
        Constant value = parseValue();
        if (this.observer != null) {
            changeIndent(-1);
        }
        return new NameValuePair(elementName, value);
    }

    private Constant parseValue() throws IOException {
        int tag = this.input.readUnsignedByte();
        if (this.observer != null) {
            parsed(1, "tag: " + new CstString(Character.toString((char) tag)).toQuoted());
        }
        switch (tag) {
            case 64:
                return new CstAnnotation(parseAnnotation(AnnotationVisibility.EMBEDDED));
            case ByteOps.LSTORE_3 /*66*/:
                return CstByte.make(((CstInteger) parseConstant()).getValue());
            case ByteOps.FSTORE_0 /*67*/:
                CstInteger value = (CstInteger) parseConstant();
                int intValue = value.getValue();
                return CstChar.make(value.getValue());
            case 68:
                return (CstDouble) parseConstant();
            case 70:
                return (CstFloat) parseConstant();
            case 73:
                return (CstInteger) parseConstant();
            case 74:
                return (CstLong) parseConstant();
            case 83:
                return CstShort.make(((CstInteger) parseConstant()).getValue());
            case 90:
                return CstBoolean.make(((CstInteger) parseConstant()).getValue());
            case 91:
                requireLength(2);
                int numValues = this.input.readUnsignedShort();
                List list = new List(numValues);
                if (this.observer != null) {
                    parsed(2, "num_values: " + numValues);
                    changeIndent(1);
                }
                for (int i = 0; i < numValues; i++) {
                    if (this.observer != null) {
                        changeIndent(-1);
                        parsed(0, "element_value[" + i + "]:");
                        changeIndent(1);
                    }
                    list.set(i, parseValue());
                }
                if (this.observer != null) {
                    changeIndent(-1);
                }
                list.setImmutable();
                return new CstArray(list);
            case 99:
                Type type = Type.internReturnType(((CstString) this.pool.get(this.input.readUnsignedShort())).getString());
                if (this.observer != null) {
                    parsed(2, "class_info: " + type.toHuman());
                }
                return new CstType(type);
            case 101:
                requireLength(4);
                int typeNameIndex = this.input.readUnsignedShort();
                CstString typeName = (CstString) this.pool.get(typeNameIndex);
                CstString constName = (CstString) this.pool.get(this.input.readUnsignedShort());
                if (this.observer != null) {
                    parsed(2, "type_name: " + typeName.toHuman());
                    parsed(2, "const_name: " + constName.toHuman());
                }
                return new CstEnumRef(new CstNat(constName, typeName));
            case ByteOps.DREM /*115*/:
                return parseConstant();
            default:
                throw new ParseException("unknown annotation tag: " + Hex.u1(tag));
        }
    }

    private Constant parseConstant() throws IOException {
        Constant value = this.pool.get(this.input.readUnsignedShort());
        if (this.observer != null) {
            String human;
            if (value instanceof CstString) {
                human = ((CstString) value).toQuoted();
            } else {
                human = value.toHuman();
            }
            parsed(2, "constant_value: " + human);
        }
        return value;
    }

    private void requireLength(int requiredLength) throws IOException {
        if (this.input.available() < requiredLength) {
            throw new ParseException("truncated annotation attribute");
        }
    }

    private void parsed(int length, String message) {
        this.observer.parsed(this.bytes, this.parseCursor, length, message);
        this.parseCursor += length;
    }

    private void changeIndent(int indent) {
        this.observer.changeIndent(indent);
    }
}
