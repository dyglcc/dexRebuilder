package com.android.dx.merge;

import com.android.dex.Annotation;
import com.android.dex.CallSiteId;
import com.android.dex.ClassDef;
import com.android.dex.Dex;
import com.android.dex.DexException;
import com.android.dex.EncodedValue;
import com.android.dex.EncodedValueCodec;
import com.android.dex.EncodedValueReader;
import com.android.dex.FieldId;
import com.android.dex.Leb128;
import com.android.dex.MethodHandle;
import com.android.dex.MethodHandle.MethodHandleType;
import com.android.dex.MethodId;
import com.android.dex.ProtoId;
import com.android.dex.TableOfContents;
import com.android.dex.TypeList;
import com.android.dex.util.ByteOutput;
import com.android.dx.util.ByteArrayAnnotatedOutput;
import java.util.HashMap;

public final class IndexMap {
    private final HashMap<Integer, Integer> annotationDirectoryOffsets = new HashMap();
    private final HashMap<Integer, Integer> annotationOffsets = new HashMap();
    private final HashMap<Integer, Integer> annotationSetOffsets = new HashMap();
    private final HashMap<Integer, Integer> annotationSetRefListOffsets = new HashMap();
    public final int[] callSiteIds;
    private final HashMap<Integer, Integer> encodedArrayValueOffset = new HashMap();
    public final short[] fieldIds;
    public final HashMap<Integer, Integer> methodHandleIds = new HashMap();
    public final short[] methodIds;
    public final short[] protoIds;
    public final int[] stringIds;
    private final Dex target;
    public final short[] typeIds;
    private final HashMap<Integer, Integer> typeListOffsets = new HashMap();

    private final class EncodedValueTransformer {
        private final ByteOutput out;

        public EncodedValueTransformer(ByteOutput out) {
            this.out = out;
        }

        public void transform(EncodedValueReader reader) {
            int i = 0;
            switch (reader.peek()) {
                case 0:
                    EncodedValueCodec.writeSignedIntegralValue(this.out, 0, (long) reader.readByte());
                    return;
                case 2:
                    EncodedValueCodec.writeSignedIntegralValue(this.out, 2, (long) reader.readShort());
                    return;
                case 3:
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 3, (long) reader.readChar());
                    return;
                case 4:
                    EncodedValueCodec.writeSignedIntegralValue(this.out, 4, (long) reader.readInt());
                    return;
                case 6:
                    EncodedValueCodec.writeSignedIntegralValue(this.out, 6, reader.readLong());
                    return;
                case 16:
                    EncodedValueCodec.writeRightZeroExtendedValue(this.out, 16, ((long) Float.floatToIntBits(reader.readFloat())) << 32);
                    return;
                case 17:
                    EncodedValueCodec.writeRightZeroExtendedValue(this.out, 17, Double.doubleToLongBits(reader.readDouble()));
                    return;
                case 21:
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 21, (long) IndexMap.this.adjustProto(reader.readMethodType()));
                    return;
                case 22:
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 22, (long) IndexMap.this.adjustMethodHandle(reader.readMethodHandle()));
                    return;
                case 23:
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 23, (long) IndexMap.this.adjustString(reader.readString()));
                    return;
                case 24:
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 24, (long) IndexMap.this.adjustType(reader.readType()));
                    return;
                case 25:
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 25, (long) IndexMap.this.adjustField(reader.readField()));
                    return;
                case 26:
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 26, (long) IndexMap.this.adjustMethod(reader.readMethod()));
                    return;
                case 27:
                    EncodedValueCodec.writeUnsignedIntegralValue(this.out, 27, (long) IndexMap.this.adjustField(reader.readEnum()));
                    return;
                case 28:
                    writeTypeAndArg(28, 0);
                    transformArray(reader);
                    return;
                case 29:
                    writeTypeAndArg(29, 0);
                    transformAnnotation(reader);
                    return;
                case 30:
                    reader.readNull();
                    writeTypeAndArg(30, 0);
                    return;
                case 31:
                    if (reader.readBoolean()) {
                        i = 1;
                    }
                    writeTypeAndArg(31, i);
                    return;
                default:
                    throw new DexException("Unexpected type: " + Integer.toHexString(reader.peek()));
            }
        }

        private void transformAnnotation(EncodedValueReader reader) {
            int fieldCount = reader.readAnnotation();
            Leb128.writeUnsignedLeb128(this.out, IndexMap.this.adjustType(reader.getAnnotationType()));
            Leb128.writeUnsignedLeb128(this.out, fieldCount);
            for (int i = 0; i < fieldCount; i++) {
                Leb128.writeUnsignedLeb128(this.out, IndexMap.this.adjustString(reader.readAnnotationName()));
                transform(reader);
            }
        }

        private void transformArray(EncodedValueReader reader) {
            int size = reader.readArray();
            Leb128.writeUnsignedLeb128(this.out, size);
            for (int i = 0; i < size; i++) {
                transform(reader);
            }
        }

        private void writeTypeAndArg(int type, int arg) {
            this.out.writeByte((arg << 5) | type);
        }
    }

    public IndexMap(Dex target, TableOfContents tableOfContents) {
        this.target = target;
        this.stringIds = new int[tableOfContents.stringIds.size];
        this.typeIds = new short[tableOfContents.typeIds.size];
        this.protoIds = new short[tableOfContents.protoIds.size];
        this.fieldIds = new short[tableOfContents.fieldIds.size];
        this.methodIds = new short[tableOfContents.methodIds.size];
        this.callSiteIds = new int[tableOfContents.callSiteIds.size];
        this.typeListOffsets.put(Integer.valueOf(0), Integer.valueOf(0));
        this.annotationSetOffsets.put(Integer.valueOf(0), Integer.valueOf(0));
        this.annotationDirectoryOffsets.put(Integer.valueOf(0), Integer.valueOf(0));
        this.encodedArrayValueOffset.put(Integer.valueOf(0), Integer.valueOf(0));
    }

    public void putTypeListOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.typeListOffsets.put(Integer.valueOf(oldOffset), Integer.valueOf(newOffset));
    }

    public void putAnnotationOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.annotationOffsets.put(Integer.valueOf(oldOffset), Integer.valueOf(newOffset));
    }

    public void putAnnotationSetOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.annotationSetOffsets.put(Integer.valueOf(oldOffset), Integer.valueOf(newOffset));
    }

    public void putAnnotationSetRefListOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.annotationSetRefListOffsets.put(Integer.valueOf(oldOffset), Integer.valueOf(newOffset));
    }

    public void putAnnotationDirectoryOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.annotationDirectoryOffsets.put(Integer.valueOf(oldOffset), Integer.valueOf(newOffset));
    }

    public void putEncodedArrayValueOffset(int oldOffset, int newOffset) {
        if (oldOffset <= 0 || newOffset <= 0) {
            throw new IllegalArgumentException();
        }
        this.encodedArrayValueOffset.put(Integer.valueOf(oldOffset), Integer.valueOf(newOffset));
    }

    public int adjustString(int stringIndex) {
        return stringIndex == -1 ? -1 : this.stringIds[stringIndex];
    }

    public int adjustType(int typeIndex) {
        return typeIndex == -1 ? -1 : this.typeIds[typeIndex] & 65535;
    }

    public TypeList adjustTypeList(TypeList typeList) {
        if (typeList == TypeList.EMPTY) {
            return typeList;
        }
        short[] types = (short[]) typeList.getTypes().clone();
        for (int i = 0; i < types.length; i++) {
            types[i] = (short) adjustType(types[i]);
        }
        return new TypeList(this.target, types);
    }

    public int adjustProto(int protoIndex) {
        return this.protoIds[protoIndex] & 65535;
    }

    public int adjustField(int fieldIndex) {
        return this.fieldIds[fieldIndex] & 65535;
    }

    public int adjustMethod(int methodIndex) {
        return this.methodIds[methodIndex] & 65535;
    }

    public int adjustTypeListOffset(int typeListOffset) {
        return ((Integer) this.typeListOffsets.get(Integer.valueOf(typeListOffset))).intValue();
    }

    public int adjustAnnotation(int annotationOffset) {
        return ((Integer) this.annotationOffsets.get(Integer.valueOf(annotationOffset))).intValue();
    }

    public int adjustAnnotationSet(int annotationSetOffset) {
        return ((Integer) this.annotationSetOffsets.get(Integer.valueOf(annotationSetOffset))).intValue();
    }

    public int adjustAnnotationSetRefList(int annotationSetRefListOffset) {
        return ((Integer) this.annotationSetRefListOffsets.get(Integer.valueOf(annotationSetRefListOffset))).intValue();
    }

    public int adjustAnnotationDirectory(int annotationDirectoryOffset) {
        return ((Integer) this.annotationDirectoryOffsets.get(Integer.valueOf(annotationDirectoryOffset))).intValue();
    }

    public int adjustEncodedArray(int encodedArrayAttribute) {
        return ((Integer) this.encodedArrayValueOffset.get(Integer.valueOf(encodedArrayAttribute))).intValue();
    }

    public int adjustCallSite(int callSiteIndex) {
        return this.callSiteIds[callSiteIndex];
    }

    public int adjustMethodHandle(int methodHandleIndex) {
        return ((Integer) this.methodHandleIds.get(Integer.valueOf(methodHandleIndex))).intValue();
    }

    public MethodId adjust(MethodId methodId) {
        return new MethodId(this.target, adjustType(methodId.getDeclaringClassIndex()), adjustProto(methodId.getProtoIndex()), adjustString(methodId.getNameIndex()));
    }

    public CallSiteId adjust(CallSiteId callSiteId) {
        return new CallSiteId(this.target, adjustEncodedArray(callSiteId.getCallSiteOffset()));
    }

    public MethodHandle adjust(MethodHandle methodHandle) {
        int adjustField;
        Dex dex = this.target;
        MethodHandleType methodHandleType = methodHandle.getMethodHandleType();
        int unused1 = methodHandle.getUnused1();
        if (methodHandle.getMethodHandleType().isField()) {
            adjustField = adjustField(methodHandle.getFieldOrMethodId());
        } else {
            adjustField = adjustMethod(methodHandle.getFieldOrMethodId());
        }
        return new MethodHandle(dex, methodHandleType, unused1, adjustField, methodHandle.getUnused2());
    }

    public FieldId adjust(FieldId fieldId) {
        return new FieldId(this.target, adjustType(fieldId.getDeclaringClassIndex()), adjustType(fieldId.getTypeIndex()), adjustString(fieldId.getNameIndex()));
    }

    public ProtoId adjust(ProtoId protoId) {
        return new ProtoId(this.target, adjustString(protoId.getShortyIndex()), adjustType(protoId.getReturnTypeIndex()), adjustTypeListOffset(protoId.getParametersOffset()));
    }

    public ClassDef adjust(ClassDef classDef) {
        return new ClassDef(this.target, classDef.getOffset(), adjustType(classDef.getTypeIndex()), classDef.getAccessFlags(), adjustType(classDef.getSupertypeIndex()), adjustTypeListOffset(classDef.getInterfacesOffset()), classDef.getSourceFileIndex(), classDef.getAnnotationsOffset(), classDef.getClassDataOffset(), classDef.getStaticValuesOffset());
    }

    public SortableType adjust(SortableType sortableType) {
        return new SortableType(sortableType.getDex(), sortableType.getIndexMap(), adjust(sortableType.getClassDef()));
    }

    public EncodedValue adjustEncodedValue(EncodedValue encodedValue) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transform(new EncodedValueReader(encodedValue));
        return new EncodedValue(out.toByteArray());
    }

    public EncodedValue adjustEncodedArray(EncodedValue encodedArray) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transformArray(new EncodedValueReader(encodedArray, 28));
        return new EncodedValue(out.toByteArray());
    }

    public Annotation adjust(Annotation annotation) {
        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput(32);
        new EncodedValueTransformer(out).transformAnnotation(annotation.getReader());
        return new Annotation(this.target, annotation.getVisibility(), new EncodedValue(out.toByteArray()));
    }
}
