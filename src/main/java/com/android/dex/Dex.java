package com.android.dex;

import com.android.dex.ClassData.Field;
import com.android.dex.ClassData.Method;
import com.android.dex.Code.CatchHandler;
import com.android.dex.Code.Try;
import com.android.dex.MethodHandle.MethodHandleType;
import com.android.dex.util.ByteInput;
import com.android.dex.util.ByteOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.zip.Adler32;

public final class Dex {
    private static final int CHECKSUM_OFFSET = 8;
    private static final int CHECKSUM_SIZE = 4;
    static final short[] EMPTY_SHORT_ARRAY = new short[0];
    private static final int SIGNATURE_OFFSET = 12;
    private static final int SIGNATURE_SIZE = 20;
    private ByteBuffer data;
    private final FieldIdTable fieldIds;
    private final MethodIdTable methodIds;
    private int nextSectionStart;
    private final ProtoIdTable protoIds;
    private final StringTable strings;
    private final TableOfContents tableOfContents;
    private final TypeIndexToDescriptorIndexTable typeIds;
    private final TypeIndexToDescriptorTable typeNames;

    private final class ClassDefIterable implements Iterable<ClassDef> {
        private ClassDefIterable() {
        }

        public Iterator<ClassDef> iterator() {
            if (Dex.this.tableOfContents.classDefs.exists()) {
                return new ClassDefIterator();
            }
            return Collections.emptySet().iterator();
        }
    }

    private final class ClassDefIterator implements Iterator<ClassDef> {
        private int count;
        private final Section in;

        private ClassDefIterator() {
            this.in = Dex.this.open(Dex.this.tableOfContents.classDefs.off);
            this.count = 0;
        }

        public boolean hasNext() {
            return this.count < Dex.this.tableOfContents.classDefs.size;
        }

        public ClassDef next() {
            if (hasNext()) {
                this.count++;
                return this.in.readClassDef();
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class FieldIdTable extends AbstractList<FieldId> implements RandomAccess {
        private FieldIdTable() {
        }

        public FieldId get(int index) {
            Dex.checkBounds(index, Dex.this.tableOfContents.fieldIds.size);
            return Dex.this.open(Dex.this.tableOfContents.fieldIds.off + (index * 8)).readFieldId();
        }

        public int size() {
            return Dex.this.tableOfContents.fieldIds.size;
        }
    }

    private final class MethodIdTable extends AbstractList<MethodId> implements RandomAccess {
        private MethodIdTable() {
        }

        public MethodId get(int index) {
            Dex.checkBounds(index, Dex.this.tableOfContents.methodIds.size);
            return Dex.this.open(Dex.this.tableOfContents.methodIds.off + (index * 8)).readMethodId();
        }

        public int size() {
            return Dex.this.tableOfContents.methodIds.size;
        }
    }

    private final class ProtoIdTable extends AbstractList<ProtoId> implements RandomAccess {
        private ProtoIdTable() {
        }

        public ProtoId get(int index) {
            Dex.checkBounds(index, Dex.this.tableOfContents.protoIds.size);
            return Dex.this.open(Dex.this.tableOfContents.protoIds.off + (index * 12)).readProtoId();
        }

        public int size() {
            return Dex.this.tableOfContents.protoIds.size;
        }
    }

    public final class Section implements ByteInput, ByteOutput {
        private final ByteBuffer data;
        private final int initialPosition;
        private final String name;

        private Section(String name, ByteBuffer data) {
            this.name = name;
            this.data = data;
            this.initialPosition = data.position();
        }

        public int getPosition() {
            return this.data.position();
        }

        public int readInt() {
            return this.data.getInt();
        }

        public short readShort() {
            return this.data.getShort();
        }

        public int readUnsignedShort() {
            return readShort() & 65535;
        }

        public byte readByte() {
            return this.data.get();
        }

        public byte[] readByteArray(int length) {
            byte[] result = new byte[length];
            this.data.get(result);
            return result;
        }

        public short[] readShortArray(int length) {
            if (length == 0) {
                return Dex.EMPTY_SHORT_ARRAY;
            }
            short[] result = new short[length];
            for (int i = 0; i < length; i++) {
                result[i] = readShort();
            }
            return result;
        }

        public int readUleb128() {
            return Leb128.readUnsignedLeb128(this);
        }

        public int readUleb128p1() {
            return Leb128.readUnsignedLeb128(this) - 1;
        }

        public int readSleb128() {
            return Leb128.readSignedLeb128(this);
        }

        public void writeUleb128p1(int i) {
            writeUleb128(i + 1);
        }

        public TypeList readTypeList() {
            short[] types = readShortArray(readInt());
            alignToFourBytes();
            return new TypeList(Dex.this, types);
        }

        public String readString() {
            int offset = readInt();
            int savedPosition = this.data.position();
            int savedLimit = this.data.limit();
            this.data.position(offset);
            this.data.limit(this.data.capacity());
            try {
                int expectedLength = readUleb128();
                String result = Mutf8.decode(this, new char[expectedLength]);
                if (result.length() != expectedLength) {
                    throw new DexException("Declared length " + expectedLength + " doesn't match decoded length of " + result.length());
                }
                this.data.position(savedPosition);
                this.data.limit(savedLimit);
                return result;
            } catch (Throwable e) {
                throw new DexException(e);
            } catch (Throwable th) {
                this.data.position(savedPosition);
                this.data.limit(savedLimit);
            }
        }

        public FieldId readFieldId() {
            return new FieldId(Dex.this, readUnsignedShort(), readUnsignedShort(), readInt());
        }

        public MethodId readMethodId() {
            return new MethodId(Dex.this, readUnsignedShort(), readUnsignedShort(), readInt());
        }

        public ProtoId readProtoId() {
            return new ProtoId(Dex.this, readInt(), readInt(), readInt());
        }

        public CallSiteId readCallSiteId() {
            return new CallSiteId(Dex.this, readInt());
        }

        public MethodHandle readMethodHandle() {
            return new MethodHandle(Dex.this, MethodHandleType.fromValue(readUnsignedShort()), readUnsignedShort(), readUnsignedShort(), readUnsignedShort());
        }

        public ClassDef readClassDef() {
            return new ClassDef(Dex.this, getPosition(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt());
        }

        private Code readCode() {
            CatchHandler[] catchHandlers;
            Try[] tries;
            int registersSize = readUnsignedShort();
            int insSize = readUnsignedShort();
            int outsSize = readUnsignedShort();
            int triesSize = readUnsignedShort();
            int debugInfoOffset = readInt();
            short[] instructions = readShortArray(readInt());
            if (triesSize > 0) {
                if (instructions.length % 2 == 1) {
                    readShort();
                }
                Section triesSection = Dex.this.open(this.data.position());
                skip(triesSize * 8);
                catchHandlers = readCatchHandlers();
                tries = triesSection.readTries(triesSize, catchHandlers);
            } else {
                tries = new Try[0];
                catchHandlers = new CatchHandler[0];
            }
            return new Code(registersSize, insSize, outsSize, debugInfoOffset, instructions, tries, catchHandlers);
        }

        private CatchHandler[] readCatchHandlers() {
            int baseOffset = this.data.position();
            int catchHandlersSize = readUleb128();
            CatchHandler[] result = new CatchHandler[catchHandlersSize];
            for (int i = 0; i < catchHandlersSize; i++) {
                result[i] = readCatchHandler(this.data.position() - baseOffset);
            }
            return result;
        }

        private Try[] readTries(int triesSize, CatchHandler[] catchHandlers) {
            Try[] result = new Try[triesSize];
            for (int i = 0; i < triesSize; i++) {
                result[i] = new Try(readInt(), readUnsignedShort(), findCatchHandlerIndex(catchHandlers, readUnsignedShort()));
            }
            return result;
        }

        private int findCatchHandlerIndex(CatchHandler[] catchHandlers, int offset) {
            for (int i = 0; i < catchHandlers.length; i++) {
                if (catchHandlers[i].getOffset() == offset) {
                    return i;
                }
            }
            throw new IllegalArgumentException();
        }

        private CatchHandler readCatchHandler(int offset) {
            int size = readSleb128();
            int handlersCount = Math.abs(size);
            int[] typeIndexes = new int[handlersCount];
            int[] addresses = new int[handlersCount];
            for (int i = 0; i < handlersCount; i++) {
                typeIndexes[i] = readUleb128();
                addresses[i] = readUleb128();
            }
            return new CatchHandler(typeIndexes, addresses, size <= 0 ? readUleb128() : -1, offset);
        }

        private ClassData readClassData() {
            return new ClassData(readFields(readUleb128()), readFields(readUleb128()), readMethods(readUleb128()), readMethods(readUleb128()));
        }

        private Field[] readFields(int count) {
            Field[] result = new Field[count];
            int fieldIndex = 0;
            for (int i = 0; i < count; i++) {
                fieldIndex += readUleb128();
                result[i] = new Field(fieldIndex, readUleb128());
            }
            return result;
        }

        private Method[] readMethods(int count) {
            Method[] result = new Method[count];
            int methodIndex = 0;
            for (int i = 0; i < count; i++) {
                methodIndex += readUleb128();
                result[i] = new Method(methodIndex, readUleb128(), readUleb128());
            }
            return result;
        }

        private byte[] getBytesFrom(int start) {
            byte[] result = new byte[(this.data.position() - start)];
            this.data.position(start);
            this.data.get(result);
            return result;
        }

        public Annotation readAnnotation() {
            byte visibility = readByte();
            int start = this.data.position();
            new EncodedValueReader((ByteInput) this, 29).skipValue();
            return new Annotation(Dex.this, visibility, new EncodedValue(getBytesFrom(start)));
        }

        public EncodedValue readEncodedArray() {
            int start = this.data.position();
            new EncodedValueReader((ByteInput) this, 28).skipValue();
            return new EncodedValue(getBytesFrom(start));
        }

        public void skip(int count) {
            if (count < 0) {
                throw new IllegalArgumentException();
            }
            this.data.position(this.data.position() + count);
        }

        public void alignToFourBytes() {
            this.data.position((this.data.position() + 3) & -4);
        }

        public void alignToFourBytesWithZeroFill() {
            while ((this.data.position() & 3) != 0) {
                this.data.put((byte) 0);
            }
        }

        public void assertFourByteAligned() {
            if ((this.data.position() & 3) != 0) {
                throw new IllegalStateException("Not four byte aligned!");
            }
        }

        public void write(byte[] bytes) {
            this.data.put(bytes);
        }

        public void writeByte(int b) {
            this.data.put((byte) b);
        }

        public void writeShort(short i) {
            this.data.putShort(i);
        }

        public void writeUnsignedShort(int i) {
            short s = (short) i;
            if (i != (65535 & s)) {
                throw new IllegalArgumentException("Expected an unsigned short: " + i);
            }
            writeShort(s);
        }

        public void write(short[] shorts) {
            for (short s : shorts) {
                writeShort(s);
            }
        }

        public void writeInt(int i) {
            this.data.putInt(i);
        }

        public void writeUleb128(int i) {
            try {
                Leb128.writeUnsignedLeb128(this, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new DexException("Section limit " + this.data.limit() + " exceeded by " + this.name);
            }
        }

        public void writeSleb128(int i) {
            try {
                Leb128.writeSignedLeb128(this, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new DexException("Section limit " + this.data.limit() + " exceeded by " + this.name);
            }
        }

        public void writeStringData(String value) {
            try {
                writeUleb128(value.length());
                write(Mutf8.encode(value));
                writeByte(0);
            } catch (UTFDataFormatException e) {
                throw new AssertionError();
            }
        }

        public void writeTypeList(TypeList typeList) {
            short[] types = typeList.getTypes();
            writeInt(types.length);
            for (short type : types) {
                writeShort(type);
            }
            alignToFourBytesWithZeroFill();
        }

        public int used() {
            return this.data.position() - this.initialPosition;
        }
    }

    private final class StringTable extends AbstractList<String> implements RandomAccess {
        private StringTable() {
        }

        public String get(int index) {
            Dex.checkBounds(index, Dex.this.tableOfContents.stringIds.size);
            return Dex.this.open(Dex.this.tableOfContents.stringIds.off + (index * 4)).readString();
        }

        public int size() {
            return Dex.this.tableOfContents.stringIds.size;
        }
    }

    private final class TypeIndexToDescriptorIndexTable extends AbstractList<Integer> implements RandomAccess {
        private TypeIndexToDescriptorIndexTable() {
        }

        public Integer get(int index) {
            return Integer.valueOf(Dex.this.descriptorIndexFromTypeIndex(index));
        }

        public int size() {
            return Dex.this.tableOfContents.typeIds.size;
        }
    }

    private final class TypeIndexToDescriptorTable extends AbstractList<String> implements RandomAccess {
        private TypeIndexToDescriptorTable() {
        }

        public String get(int index) {
            return Dex.this.strings.get(Dex.this.descriptorIndexFromTypeIndex(index));
        }

        public int size() {
            return Dex.this.tableOfContents.typeIds.size;
        }
    }

    public Dex(byte[] data) throws IOException {
        this(ByteBuffer.wrap(data));
    }

    private Dex(ByteBuffer data) throws IOException {
        this.tableOfContents = new TableOfContents();
        this.nextSectionStart = 0;
        this.strings = new StringTable();
        this.typeIds = new TypeIndexToDescriptorIndexTable();
        this.typeNames = new TypeIndexToDescriptorTable();
        this.protoIds = new ProtoIdTable();
        this.fieldIds = new FieldIdTable();
        this.methodIds = new MethodIdTable();
        this.data = data;
        this.data.order(ByteOrder.LITTLE_ENDIAN);
        this.tableOfContents.readFrom(this);
    }

    public Dex(int byteCount) throws IOException {
        this.tableOfContents = new TableOfContents();
        this.nextSectionStart = 0;
        this.strings = new StringTable();
        this.typeIds = new TypeIndexToDescriptorIndexTable();
        this.typeNames = new TypeIndexToDescriptorTable();
        this.protoIds = new ProtoIdTable();
        this.fieldIds = new FieldIdTable();
        this.methodIds = new MethodIdTable();
        this.data = ByteBuffer.wrap(new byte[byteCount]);
        this.data.order(ByteOrder.LITTLE_ENDIAN);
    }

    public Dex(InputStream in) throws IOException {
        this.tableOfContents = new TableOfContents();
        this.nextSectionStart = 0;
        this.strings = new StringTable();
        this.typeIds = new TypeIndexToDescriptorIndexTable();
        this.typeNames = new TypeIndexToDescriptorTable();
        this.protoIds = new ProtoIdTable();
        this.fieldIds = new FieldIdTable();
        this.methodIds = new MethodIdTable();
        try {
            loadFrom(in);
        } finally {
            in.close();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Dex(java.io.File r7) throws IOException {
        /*
        r6 = this;
        r4 = 0;
        r6.<init>();
        r3 = new com.android.dex.TableOfContents;
        r3.<init>();
        r6.tableOfContents = r3;
        r3 = 0;
        r6.nextSectionStart = r3;
        r3 = new com.android.dex.Dex$StringTable;
        r3.<init>();
        r6.strings = r3;
        r3 = new com.android.dex.Dex$TypeIndexToDescriptorIndexTable;
        r3.<init>();
        r6.typeIds = r3;
        r3 = new com.android.dex.Dex$TypeIndexToDescriptorTable;
        r3.<init>();
        r6.typeNames = r3;
        r3 = new com.android.dex.Dex$ProtoIdTable;
        r3.<init>();
        r6.protoIds = r3;
        r3 = new com.android.dex.Dex$FieldIdTable;
        r3.<init>();
        r6.fieldIds = r3;
        r3 = new com.android.dex.Dex$MethodIdTable;
        r3.<init>();
        r6.methodIds = r3;
        r3 = r7.getName();
        r3 = com.android.dex.util.FileUtils.hasArchiveSuffix(r3);
        if (r3 == 0) goto L_0x0081;
    L_0x0042:
        r2 = new java.util.zip.ZipFile;
        r2.<init>(r7);
        r3 = "classes.dex";
        r0 = r2.getEntry(r3);
        if (r0 == 0) goto L_0x0068;
    L_0x004f:
        r1 = r2.getInputStream(r0);
        r6.loadFrom(r1);	 Catch:{ Throwable -> 0x005f }
        if (r1 == 0) goto L_0x005b;
    L_0x0058:
        $closeResource(r4, r1);
    L_0x005b:
        r2.close();
    L_0x005e:
        return;
    L_0x005f:
        r4 = move-exception;
        throw r4;	 Catch:{ all -> 0x0061 }
    L_0x0061:
        r3 = move-exception;
        if (r1 == 0) goto L_0x0067;
    L_0x0064:
        $closeResource(r4, r1);
    L_0x0067:
        throw r3;
    L_0x0068:
        r3 = new com.android.dex.DexException;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "Expected classes.dex in ";
        r4 = r4.append(r5);
        r4 = r4.append(r7);
        r4 = r4.toString();
        r3.<init>(r4);
        throw r3;
    L_0x0081:
        r3 = r7.getName();
        r5 = ".dex";
        r3 = r3.endsWith(r5);
        if (r3 == 0) goto L_0x00a0;
    L_0x008d:
        r1 = new java.io.FileInputStream;
        r1.<init>(r7);
        r6.loadFrom(r1);	 Catch:{ Throwable -> 0x0099 }
        $closeResource(r4, r1);
        goto L_0x005e;
    L_0x0099:
        r4 = move-exception;
        throw r4;	 Catch:{ all -> 0x009b }
    L_0x009b:
        r3 = move-exception;
        $closeResource(r4, r1);
        throw r3;
    L_0x00a0:
        r3 = new com.android.dex.DexException;
        r4 = new java.lang.StringBuilder;
        r4.<init>();
        r5 = "unknown output extension: ";
        r4 = r4.append(r5);
        r4 = r4.append(r7);
        r4 = r4.toString();
        r3.<init>(r4);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.dex.Dex.<init>(java.io.File):void");
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private void loadFrom(InputStream in) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (true) {
            int count = in.read(buffer);
            if (count != -1) {
                bytesOut.write(buffer, 0, count);
            } else {
                this.data = ByteBuffer.wrap(bytesOut.toByteArray());
                this.data.order(ByteOrder.LITTLE_ENDIAN);
                this.tableOfContents.readFrom(this);
                return;
            }
        }
    }

    private static void checkBounds(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index:" + index + ", length=" + length);
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        ByteBuffer data = this.data.duplicate();
        data.clear();
        while (data.hasRemaining()) {
            int count = Math.min(buffer.length, data.remaining());
            data.get(buffer, 0, count);
            out.write(buffer, 0, count);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void writeTo(java.io.File r4) throws IOException {
        /*
        r3 = this;
        r0 = new java.io.FileOutputStream;
        r0.<init>(r4);
        r2 = 0;
        r3.writeTo(r0);	 Catch:{ Throwable -> 0x000d }
        $closeResource(r2, r0);
        return;
    L_0x000d:
        r2 = move-exception;
        throw r2;	 Catch:{ all -> 0x000f }
    L_0x000f:
        r1 = move-exception;
        $closeResource(r2, r0);
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.dex.Dex.writeTo(java.io.File):void");
    }

    public TableOfContents getTableOfContents() {
        return this.tableOfContents;
    }

    public Section open(int position) {
        if (position < 0 || position >= this.data.capacity()) {
            throw new IllegalArgumentException("position=" + position + " length=" + this.data.capacity());
        }
        ByteBuffer sectionData = this.data.duplicate();
        sectionData.order(ByteOrder.LITTLE_ENDIAN);
        sectionData.position(position);
        sectionData.limit(this.data.capacity());
        return new Section("section", sectionData);
    }

    public Section appendSection(int maxByteCount, String name) {
        if ((maxByteCount & 3) != 0) {
            throw new IllegalStateException("Not four byte aligned!");
        }
        int limit = this.nextSectionStart + maxByteCount;
        ByteBuffer sectionData = this.data.duplicate();
        sectionData.order(ByteOrder.LITTLE_ENDIAN);
        sectionData.position(this.nextSectionStart);
        sectionData.limit(limit);
        Section result = new Section(name, sectionData);
        this.nextSectionStart = limit;
        return result;
    }

    public int getLength() {
        return this.data.capacity();
    }

    public int getNextSectionStart() {
        return this.nextSectionStart;
    }

    public byte[] getBytes() {
        ByteBuffer data = this.data.duplicate();
        byte[] result = new byte[data.capacity()];
        data.position(0);
        data.get(result);
        return result;
    }

    public List<String> strings() {
        return this.strings;
    }

    public List<Integer> typeIds() {
        return this.typeIds;
    }

    public List<String> typeNames() {
        return this.typeNames;
    }

    public List<ProtoId> protoIds() {
        return this.protoIds;
    }

    public List<FieldId> fieldIds() {
        return this.fieldIds;
    }

    public List<MethodId> methodIds() {
        return this.methodIds;
    }

    public Iterable<ClassDef> classDefs() {
        return new ClassDefIterable();
    }

    public TypeList readTypeList(int offset) {
        if (offset == 0) {
            return TypeList.EMPTY;
        }
        return open(offset).readTypeList();
    }

    public ClassData readClassData(ClassDef classDef) {
        int offset = classDef.getClassDataOffset();
        if (offset != 0) {
            return open(offset).readClassData();
        }
        throw new IllegalArgumentException("offset == 0");
    }

    public Code readCode(Method method) {
        int offset = method.getCodeOffset();
        if (offset != 0) {
            return open(offset).readCode();
        }
        throw new IllegalArgumentException("offset == 0");
    }

    public byte[] computeSignature() throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            ByteBuffer data = this.data.duplicate();
            data.limit(data.capacity());
            data.position(32);
            while (data.hasRemaining()) {
                int count = Math.min(buffer.length, data.remaining());
                data.get(buffer, 0, count);
                digest.update(buffer, 0, count);
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
    }

    public int computeChecksum() throws IOException {
        Adler32 adler32 = new Adler32();
        byte[] buffer = new byte[8192];
        ByteBuffer data = this.data.duplicate();
        data.limit(data.capacity());
        data.position(12);
        while (data.hasRemaining()) {
            int count = Math.min(buffer.length, data.remaining());
            data.get(buffer, 0, count);
            adler32.update(buffer, 0, count);
        }
        return (int) adler32.getValue();
    }

    public void writeHashes() throws IOException {
        open(12).write(computeSignature());
        open(8).writeInt(computeChecksum());
    }

    public int descriptorIndexFromTypeIndex(int typeIndex) {
        checkBounds(typeIndex, this.tableOfContents.typeIds.size);
        return this.data.getInt(this.tableOfContents.typeIds.off + (typeIndex * 4));
    }
}
