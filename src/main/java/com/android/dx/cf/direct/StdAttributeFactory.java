package com.android.dx.cf.direct;

import com.android.dx.cf.attrib.AttAnnotationDefault;
import com.android.dx.cf.attrib.AttBootstrapMethods;
import com.android.dx.cf.attrib.AttCode;
import com.android.dx.cf.attrib.AttConstantValue;
import com.android.dx.cf.attrib.AttDeprecated;
import com.android.dx.cf.attrib.AttEnclosingMethod;
import com.android.dx.cf.attrib.AttExceptions;
import com.android.dx.cf.attrib.AttInnerClasses;
import com.android.dx.cf.attrib.AttLineNumberTable;
import com.android.dx.cf.attrib.AttLocalVariableTable;
import com.android.dx.cf.attrib.AttLocalVariableTypeTable;
import com.android.dx.cf.attrib.AttRuntimeInvisibleAnnotations;
import com.android.dx.cf.attrib.AttRuntimeInvisibleParameterAnnotations;
import com.android.dx.cf.attrib.AttRuntimeVisibleAnnotations;
import com.android.dx.cf.attrib.AttRuntimeVisibleParameterAnnotations;
import com.android.dx.cf.attrib.AttSignature;
import com.android.dx.cf.attrib.AttSourceDebugExtension;
import com.android.dx.cf.attrib.AttSourceFile;
import com.android.dx.cf.attrib.AttSynthetic;
import com.android.dx.cf.attrib.InnerClassList;
import com.android.dx.cf.code.BootstrapMethodArgumentsList;
import com.android.dx.cf.code.BootstrapMethodsList;
import com.android.dx.cf.code.ByteCatchList;
import com.android.dx.cf.code.BytecodeArray;
import com.android.dx.cf.code.LineNumberList;
import com.android.dx.cf.code.LocalVariableList;
import com.android.dx.cf.iface.Attribute;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.cf.iface.ParseObserver;
import com.android.dx.cf.iface.StdAttributeList;
import com.android.dx.rop.annotation.AnnotationVisibility;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstMethodHandle;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.TypedConstant;
import com.android.dx.util.ByteArray;
import com.android.dx.util.ByteArray.MyDataInputStream;
import com.android.dx.util.Hex;
import java.io.IOException;

public class StdAttributeFactory extends AttributeFactory {
    public static final StdAttributeFactory THE_ONE = new StdAttributeFactory();

    protected Attribute parse0(DirectClassFile cf, int context, String name, int offset, int length, ParseObserver observer) {
        switch (context) {
            case 0:
                if (name == AttBootstrapMethods.ATTRIBUTE_NAME) {
                    return bootstrapMethods(cf, offset, length, observer);
                }
                if (name == AttDeprecated.ATTRIBUTE_NAME) {
                    return deprecated(cf, offset, length, observer);
                }
                if (name == AttEnclosingMethod.ATTRIBUTE_NAME) {
                    return enclosingMethod(cf, offset, length, observer);
                }
                if (name == AttInnerClasses.ATTRIBUTE_NAME) {
                    return innerClasses(cf, offset, length, observer);
                }
                if (name == AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeInvisibleAnnotations(cf, offset, length, observer);
                }
                if (name == AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeVisibleAnnotations(cf, offset, length, observer);
                }
                if (name == AttSynthetic.ATTRIBUTE_NAME) {
                    return synthetic(cf, offset, length, observer);
                }
                if (name == AttSignature.ATTRIBUTE_NAME) {
                    return signature(cf, offset, length, observer);
                }
                if (name == AttSourceDebugExtension.ATTRIBUTE_NAME) {
                    return sourceDebugExtension(cf, offset, length, observer);
                }
                if (name == AttSourceFile.ATTRIBUTE_NAME) {
                    return sourceFile(cf, offset, length, observer);
                }
                break;
            case 1:
                if (name == AttConstantValue.ATTRIBUTE_NAME) {
                    return constantValue(cf, offset, length, observer);
                }
                if (name == AttDeprecated.ATTRIBUTE_NAME) {
                    return deprecated(cf, offset, length, observer);
                }
                if (name == AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeInvisibleAnnotations(cf, offset, length, observer);
                }
                if (name == AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeVisibleAnnotations(cf, offset, length, observer);
                }
                if (name == AttSignature.ATTRIBUTE_NAME) {
                    return signature(cf, offset, length, observer);
                }
                if (name == AttSynthetic.ATTRIBUTE_NAME) {
                    return synthetic(cf, offset, length, observer);
                }
                break;
            case 2:
                if (name == AttAnnotationDefault.ATTRIBUTE_NAME) {
                    return annotationDefault(cf, offset, length, observer);
                }
                if (name == AttCode.ATTRIBUTE_NAME) {
                    return code(cf, offset, length, observer);
                }
                if (name == AttDeprecated.ATTRIBUTE_NAME) {
                    return deprecated(cf, offset, length, observer);
                }
                if (name == AttExceptions.ATTRIBUTE_NAME) {
                    return exceptions(cf, offset, length, observer);
                }
                if (name == AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeInvisibleAnnotations(cf, offset, length, observer);
                }
                if (name == AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeVisibleAnnotations(cf, offset, length, observer);
                }
                if (name == AttRuntimeInvisibleParameterAnnotations.ATTRIBUTE_NAME) {
                    return runtimeInvisibleParameterAnnotations(cf, offset, length, observer);
                }
                if (name == AttRuntimeVisibleParameterAnnotations.ATTRIBUTE_NAME) {
                    return runtimeVisibleParameterAnnotations(cf, offset, length, observer);
                }
                if (name == AttSignature.ATTRIBUTE_NAME) {
                    return signature(cf, offset, length, observer);
                }
                if (name == AttSynthetic.ATTRIBUTE_NAME) {
                    return synthetic(cf, offset, length, observer);
                }
                break;
            case 3:
                if (name == AttLineNumberTable.ATTRIBUTE_NAME) {
                    return lineNumberTable(cf, offset, length, observer);
                }
                if (name == AttLocalVariableTable.ATTRIBUTE_NAME) {
                    return localVariableTable(cf, offset, length, observer);
                }
                if (name == AttLocalVariableTypeTable.ATTRIBUTE_NAME) {
                    return localVariableTypeTable(cf, offset, length, observer);
                }
                break;
        }
        return super.parse0(cf, context, name, offset, length, observer);
    }

    private Attribute annotationDefault(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        return new AttAnnotationDefault(new AnnotationParser(cf, offset, length, observer).parseValueAttribute(), length);
    }

    private Attribute bootstrapMethods(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        ByteArray bytes = cf.getBytes();
        int numMethods = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "num_boostrap_methods: " + Hex.u2(numMethods));
        }
        return new AttBootstrapMethods(parseBootstrapMethods(bytes, cf.getConstantPool(), cf.getThisClass(), numMethods, offset + 2, length - 2, observer));
    }

    private Attribute code(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 12) {
            return throwSeverelyTruncated();
        }
        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();
        int maxStack = bytes.getUnsignedShort(offset);
        int maxLocals = bytes.getUnsignedShort(offset + 2);
        int codeLength = bytes.getInt(offset + 4);
        int origOffset = offset;
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "max_stack: " + Hex.u2(maxStack));
            observer.parsed(bytes, offset + 2, 2, "max_locals: " + Hex.u2(maxLocals));
            observer.parsed(bytes, offset + 4, 4, "code_length: " + Hex.u4(codeLength));
        }
        offset += 8;
        length -= 8;
        if (length < codeLength + 4) {
            return throwTruncated();
        }
        ByteCatchList catches;
        int codeOffset = offset;
        offset += codeLength;
        length -= codeLength;
        BytecodeArray code = new BytecodeArray(bytes.slice(codeOffset, codeOffset + codeLength), pool);
        if (observer != null) {
            code.forEach(new CodeObserver(code.getBytes(), observer));
        }
        int exceptionTableLength = bytes.getUnsignedShort(offset);
        if (exceptionTableLength == 0) {
            catches = ByteCatchList.EMPTY;
        } else {
            catches = new ByteCatchList(exceptionTableLength);
        }
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "exception_table_length: " + Hex.u2(exceptionTableLength));
        }
        offset += 2;
        length -= 2;
        if (length < (exceptionTableLength * 8) + 2) {
            return throwTruncated();
        }
        for (int i = 0; i < exceptionTableLength; i++) {
            if (observer != null) {
                observer.changeIndent(1);
            }
            int startPc = bytes.getUnsignedShort(offset);
            int endPc = bytes.getUnsignedShort(offset + 2);
            int handlerPc = bytes.getUnsignedShort(offset + 4);
            CstType catchType = (CstType) pool.get0Ok(bytes.getUnsignedShort(offset + 6));
            catches.set(i, startPc, endPc, handlerPc, catchType);
            if (observer != null) {
                String str;
                StringBuilder append = new StringBuilder().append(Hex.u2(startPc)).append("..").append(Hex.u2(endPc)).append(" -> ").append(Hex.u2(handlerPc)).append(" ");
                if (catchType == null) {
                    str = "<any>";
                } else {
                    str = catchType.toHuman();
                }
                observer.parsed(bytes, offset, 8, append.append(str).toString());
            }
            offset += 8;
            length -= 8;
            if (observer != null) {
                observer.changeIndent(-1);
            }
        }
        catches.setImmutable();
        AttributeListParser attributeListParser = new AttributeListParser(cf, 3, offset, this);
        attributeListParser.setObserver(observer);
        StdAttributeList attributes = attributeListParser.getList();
        attributes.setImmutable();
        int attributeByteCount = attributeListParser.getEndOffset() - offset;
        if (attributeByteCount != length) {
            return throwBadLength((offset - origOffset) + attributeByteCount);
        }
        return new AttCode(maxStack, maxLocals, code, catches, attributes);
    }

    private Attribute constantValue(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length != 2) {
            return throwBadLength(2);
        }
        ByteArray bytes = cf.getBytes();
        TypedConstant cst = (TypedConstant) cf.getConstantPool().get(bytes.getUnsignedShort(offset));
        Attribute result = new AttConstantValue(cst);
        if (observer == null) {
            return result;
        }
        observer.parsed(bytes, offset, 2, "value: " + cst);
        return result;
    }

    private Attribute deprecated(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length != 0) {
            return throwBadLength(0);
        }
        return new AttDeprecated();
    }

    private Attribute enclosingMethod(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length != 4) {
            throwBadLength(4);
        }
        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();
        CstType type = (CstType) pool.get(bytes.getUnsignedShort(offset));
        CstNat method = (CstNat) pool.get0Ok(bytes.getUnsignedShort(offset + 2));
        Attribute result = new AttEnclosingMethod(type, method);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "class: " + type);
            observer.parsed(bytes, offset + 2, 2, "method: " + DirectClassFile.stringOrNone(method));
        }
        return result;
    }

    private Attribute exceptions(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        ByteArray bytes = cf.getBytes();
        int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "number_of_exceptions: " + Hex.u2(count));
        }
        offset += 2;
        if (length - 2 != count * 2) {
            throwBadLength((count * 2) + 2);
        }
        return new AttExceptions(cf.makeTypeList(offset, count));
    }

    private Attribute innerClasses(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();
        int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "number_of_classes: " + Hex.u2(count));
        }
        offset += 2;
        if (length - 2 != count * 8) {
            throwBadLength((count * 8) + 2);
        }
        InnerClassList list = new InnerClassList(count);
        for (int i = 0; i < count; i++) {
            int innerClassIdx = bytes.getUnsignedShort(offset);
            int outerClassIdx = bytes.getUnsignedShort(offset + 2);
            int nameIdx = bytes.getUnsignedShort(offset + 4);
            int accessFlags = bytes.getUnsignedShort(offset + 6);
            CstType innerClass = (CstType) pool.get(innerClassIdx);
            CstType outerClass = (CstType) pool.get0Ok(outerClassIdx);
            CstString name = (CstString) pool.get0Ok(nameIdx);
            list.set(i, innerClass, outerClass, name, accessFlags);
            if (observer != null) {
                observer.parsed(bytes, offset, 2, "inner_class: " + DirectClassFile.stringOrNone(innerClass));
                observer.parsed(bytes, offset + 2, 2, "  outer_class: " + DirectClassFile.stringOrNone(outerClass));
                observer.parsed(bytes, offset + 4, 2, "  name: " + DirectClassFile.stringOrNone(name));
                observer.parsed(bytes, offset + 6, 2, "  access_flags: " + AccessFlags.innerClassString(accessFlags));
            }
            offset += 8;
        }
        list.setImmutable();
        return new AttInnerClasses(list);
    }

    private Attribute lineNumberTable(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        ByteArray bytes = cf.getBytes();
        int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "line_number_table_length: " + Hex.u2(count));
        }
        offset += 2;
        if (length - 2 != count * 4) {
            throwBadLength((count * 4) + 2);
        }
        LineNumberList list = new LineNumberList(count);
        for (int i = 0; i < count; i++) {
            int startPc = bytes.getUnsignedShort(offset);
            int lineNumber = bytes.getUnsignedShort(offset + 2);
            list.set(i, startPc, lineNumber);
            if (observer != null) {
                observer.parsed(bytes, offset, 4, Hex.u2(startPc) + " " + lineNumber);
            }
            offset += 4;
        }
        list.setImmutable();
        return new AttLineNumberTable(list);
    }

    private Attribute localVariableTable(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        ByteArray bytes = cf.getBytes();
        int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "local_variable_table_length: " + Hex.u2(count));
        }
        return new AttLocalVariableTable(parseLocalVariables(bytes.slice(offset + 2, offset + length), cf.getConstantPool(), observer, count, false));
    }

    private Attribute localVariableTypeTable(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }
        ByteArray bytes = cf.getBytes();
        int count = bytes.getUnsignedShort(offset);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "local_variable_type_table_length: " + Hex.u2(count));
        }
        return new AttLocalVariableTypeTable(parseLocalVariables(bytes.slice(offset + 2, offset + length), cf.getConstantPool(), observer, count, true));
    }

    private LocalVariableList parseLocalVariables(ByteArray bytes, ConstantPool pool, ParseObserver observer, int count, boolean typeTable) {
        if (bytes.size() != count * 10) {
            throwBadLength((count * 10) + 2);
        }
        MyDataInputStream in = bytes.makeDataInputStream();
        LocalVariableList list = new LocalVariableList(count);
        int i = 0;
        while (i < count) {
            try {
                int startPc = in.readUnsignedShort();
                int length = in.readUnsignedShort();
                int nameIdx = in.readUnsignedShort();
                int typeIdx = in.readUnsignedShort();
                int index = in.readUnsignedShort();
                CstString name = (CstString) pool.get(nameIdx);
                CstString type = (CstString) pool.get(typeIdx);
                CstString descriptor = null;
                CstString signature = null;
                if (typeTable) {
                    signature = type;
                } else {
                    descriptor = type;
                }
                list.set(i, startPc, length, name, descriptor, signature, index);
                if (observer != null) {
                    observer.parsed(bytes, i * 10, 10, Hex.u2(startPc) + ".." + Hex.u2(startPc + length) + " " + Hex.u2(index) + " " + name.toHuman() + " " + type.toHuman());
                }
                i++;
            } catch (IOException ex) {
                throw new RuntimeException("shouldn't happen", ex);
            }
        }
        list.setImmutable();
        return list;
    }

    private Attribute runtimeInvisibleAnnotations(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        return new AttRuntimeInvisibleAnnotations(new AnnotationParser(cf, offset, length, observer).parseAnnotationAttribute(AnnotationVisibility.BUILD), length);
    }

    private Attribute runtimeVisibleAnnotations(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        return new AttRuntimeVisibleAnnotations(new AnnotationParser(cf, offset, length, observer).parseAnnotationAttribute(AnnotationVisibility.RUNTIME), length);
    }

    private Attribute runtimeInvisibleParameterAnnotations(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        return new AttRuntimeInvisibleParameterAnnotations(new AnnotationParser(cf, offset, length, observer).parseParameterAttribute(AnnotationVisibility.BUILD), length);
    }

    private Attribute runtimeVisibleParameterAnnotations(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length < 2) {
            throwSeverelyTruncated();
        }
        return new AttRuntimeVisibleParameterAnnotations(new AnnotationParser(cf, offset, length, observer).parseParameterAttribute(AnnotationVisibility.RUNTIME), length);
    }

    private Attribute signature(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length != 2) {
            throwBadLength(2);
        }
        ByteArray bytes = cf.getBytes();
        CstString cst = (CstString) cf.getConstantPool().get(bytes.getUnsignedShort(offset));
        Attribute result = new AttSignature(cst);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "signature: " + cst);
        }
        return result;
    }

    private Attribute sourceDebugExtension(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        ByteArray bytes = cf.getBytes().slice(offset, offset + length);
        CstString smapString = new CstString(bytes);
        Attribute result = new AttSourceDebugExtension(smapString);
        if (observer != null) {
            observer.parsed(bytes, offset, length, "sourceDebugExtension: " + smapString.getString());
        }
        return result;
    }

    private Attribute sourceFile(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length != 2) {
            throwBadLength(2);
        }
        ByteArray bytes = cf.getBytes();
        CstString cst = (CstString) cf.getConstantPool().get(bytes.getUnsignedShort(offset));
        Attribute result = new AttSourceFile(cst);
        if (observer != null) {
            observer.parsed(bytes, offset, 2, "source: " + cst);
        }
        return result;
    }

    private Attribute synthetic(DirectClassFile cf, int offset, int length, ParseObserver observer) {
        if (length != 0) {
            return throwBadLength(0);
        }
        return new AttSynthetic();
    }

    private static Attribute throwSeverelyTruncated() {
        throw new ParseException("severely truncated attribute");
    }

    private static Attribute throwTruncated() {
        throw new ParseException("truncated attribute");
    }

    private static Attribute throwBadLength(int expected) {
        throw new ParseException("bad attribute length; expected length " + Hex.u4(expected));
    }

    private BootstrapMethodsList parseBootstrapMethods(ByteArray bytes, ConstantPool constantPool, CstType declaringClass, int numMethods, int offset, int length, ParseObserver observer) throws ParseException {
        BootstrapMethodsList methods = new BootstrapMethodsList(numMethods);
        for (int methodIndex = 0; methodIndex < numMethods; methodIndex++) {
            if (length < 4) {
                throwTruncated();
            }
            int methodRef = bytes.getUnsignedShort(offset);
            int numArguments = bytes.getUnsignedShort(offset + 2);
            if (observer != null) {
                observer.parsed(bytes, offset, 2, "bootstrap_method_ref: " + Hex.u2(methodRef));
                observer.parsed(bytes, offset + 2, 2, "num_bootstrap_arguments: " + Hex.u2(numArguments));
            }
            offset += 4;
            length -= 4;
            if (length < numArguments * 2) {
                throwTruncated();
            }
            BootstrapMethodArgumentsList arguments = new BootstrapMethodArgumentsList(numArguments);
            int argIndex = 0;
            while (argIndex < numArguments) {
                int argumentRef = bytes.getUnsignedShort(offset);
                if (observer != null) {
                    observer.parsed(bytes, offset, 2, "bootstrap_arguments[" + argIndex + "]" + Hex.u2(argumentRef));
                }
                arguments.set(argIndex, constantPool.get(argumentRef));
                argIndex++;
                offset += 2;
                length -= 2;
            }
            arguments.setImmutable();
            methods.set(methodIndex, declaringClass, (CstMethodHandle) constantPool.get(methodRef), arguments);
        }
        methods.setImmutable();
        if (length != 0) {
            throwBadLength(length);
        }
        return methods;
    }
}
