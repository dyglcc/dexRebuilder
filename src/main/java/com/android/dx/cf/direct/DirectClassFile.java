package com.android.dx.cf.direct;

import com.android.dx.cf.attrib.AttBootstrapMethods;
import com.android.dx.cf.attrib.AttSourceFile;
import com.android.dx.cf.code.BootstrapMethodsList;
import com.android.dx.cf.cst.ConstantPoolParser;
import com.android.dx.cf.iface.Attribute;
import com.android.dx.cf.iface.AttributeList;
import com.android.dx.cf.iface.ClassFile;
import com.android.dx.cf.iface.FieldList;
import com.android.dx.cf.iface.MethodList;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.cf.iface.ParseObserver;
import com.android.dx.cf.iface.StdAttributeList;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.StdConstantPool;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;

public class DirectClassFile implements ClassFile {
    private static final int CLASS_FILE_MAGIC = -889275714;
    private static final int CLASS_FILE_MAX_MAJOR_VERSION = 53;
    private static final int CLASS_FILE_MAX_MINOR_VERSION = 0;
    private static final int CLASS_FILE_MIN_MAJOR_VERSION = 45;
    private int accessFlags;
    private AttributeFactory attributeFactory;
    private StdAttributeList attributes;
    private final ByteArray bytes;
    private FieldList fields;
    private final String filePath;
    private TypeList interfaces;
    private MethodList methods;
    private ParseObserver observer;
    private StdConstantPool pool;
    private final boolean strictParse;
    private CstType superClass;
    private CstType thisClass;

    private static class DcfTypeList implements TypeList {
        private final ByteArray bytes;
        private final StdConstantPool pool;
        private final int size;

        public DcfTypeList(ByteArray bytes, int offset, int size, StdConstantPool pool, ParseObserver observer) {
            if (size < 0) {
                throw new IllegalArgumentException("size < 0");
            }
            bytes = bytes.slice(offset, (size * 2) + offset);
            this.bytes = bytes;
            this.size = size;
            this.pool = pool;
            int i = 0;
            while (i < size) {
                offset = i * 2;
                try {
                    CstType type = (CstType) pool.get(bytes.getUnsignedShort(offset));
                    if (observer != null) {
                        observer.parsed(bytes, offset, 2, "  " + type);
                    }
                    i++;
                } catch (ClassCastException ex) {
                    throw new RuntimeException("bogus class cpi", ex);
                }
            }
        }

        public boolean isMutable() {
            return false;
        }

        public int size() {
            return this.size;
        }

        public int getWordCount() {
            return this.size;
        }

        public Type getType(int n) {
            return ((CstType) this.pool.get(this.bytes.getUnsignedShort(n * 2))).getClassType();
        }

        public TypeList withAddedType(Type type) {
            throw new UnsupportedOperationException("unsupported");
        }
    }

    public static String stringOrNone(Object obj) {
        if (obj == null) {
            return "(none)";
        }
        return obj.toString();
    }

    public DirectClassFile(ByteArray bytes, String filePath, boolean strictParse) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        } else if (filePath == null) {
            throw new NullPointerException("filePath == null");
        } else {
            this.filePath = filePath;
            this.bytes = bytes;
            this.strictParse = strictParse;
            this.accessFlags = -1;
        }
    }

    public DirectClassFile(byte[] bytes, String filePath, boolean strictParse) {
        this(new ByteArray(bytes), filePath, strictParse);
    }

    public void setObserver(ParseObserver observer) {
        this.observer = observer;
    }

    public void setAttributeFactory(AttributeFactory attributeFactory) {
        if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        }
        this.attributeFactory = attributeFactory;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public ByteArray getBytes() {
        return this.bytes;
    }

    public int getMagic() {
        parseToInterfacesIfNecessary();
        return getMagic0();
    }

    public int getMinorVersion() {
        parseToInterfacesIfNecessary();
        return getMinorVersion0();
    }

    public int getMajorVersion() {
        parseToInterfacesIfNecessary();
        return getMajorVersion0();
    }

    public int getAccessFlags() {
        parseToInterfacesIfNecessary();
        return this.accessFlags;
    }

    public CstType getThisClass() {
        parseToInterfacesIfNecessary();
        return this.thisClass;
    }

    public CstType getSuperclass() {
        parseToInterfacesIfNecessary();
        return this.superClass;
    }

    public ConstantPool getConstantPool() {
        parseToInterfacesIfNecessary();
        return this.pool;
    }

    public TypeList getInterfaces() {
        parseToInterfacesIfNecessary();
        return this.interfaces;
    }

    public FieldList getFields() {
        parseToEndIfNecessary();
        return this.fields;
    }

    public MethodList getMethods() {
        parseToEndIfNecessary();
        return this.methods;
    }

    public AttributeList getAttributes() {
        parseToEndIfNecessary();
        return this.attributes;
    }

    public BootstrapMethodsList getBootstrapMethods() {
        AttBootstrapMethods bootstrapMethodsAttribute = (AttBootstrapMethods) getAttributes().findFirst(AttBootstrapMethods.ATTRIBUTE_NAME);
        if (bootstrapMethodsAttribute != null) {
            return bootstrapMethodsAttribute.getBootstrapMethods();
        }
        return BootstrapMethodsList.EMPTY;
    }

    public CstString getSourceFile() {
        Attribute attSf = getAttributes().findFirst(AttSourceFile.ATTRIBUTE_NAME);
        if (attSf instanceof AttSourceFile) {
            return ((AttSourceFile) attSf).getSourceFile();
        }
        return null;
    }

    public TypeList makeTypeList(int offset, int size) {
        if (size == 0) {
            return StdTypeList.EMPTY;
        }
        if (this.pool == null) {
            throw new IllegalStateException("pool not yet initialized");
        }
        return new DcfTypeList(this.bytes, offset, size, this.pool, this.observer);
    }

    public int getMagic0() {
        return this.bytes.getInt(0);
    }

    public int getMinorVersion0() {
        return this.bytes.getUnsignedShort(4);
    }

    public int getMajorVersion0() {
        return this.bytes.getUnsignedShort(6);
    }

    private void parseToInterfacesIfNecessary() {
        if (this.accessFlags == -1) {
            parse();
        }
    }

    private void parseToEndIfNecessary() {
        if (this.attributes == null) {
            parse();
        }
    }

    private void parse() {
        try {
            parse0();
        } catch (ParseException ex) {
            ex.addContext("...while parsing " + this.filePath);
            throw ex;
        } catch (Throwable ex2) {
            ParseException pe = new ParseException(ex2);
            pe.addContext("...while parsing " + this.filePath);
            throw pe;
        }
    }

    private boolean isGoodMagic(int magic) {
        return magic == CLASS_FILE_MAGIC;
    }

    private boolean isGoodVersion(int minorVersion, int majorVersion) {
        if (minorVersion >= 0) {
            if (majorVersion == 53) {
                if (minorVersion <= 0) {
                    return true;
                }
            } else if (majorVersion < 53 && majorVersion >= 45) {
                return true;
            }
        }
        return false;
    }

    private void parse0() {
        if (this.bytes.size() < 10) {
            throw new ParseException("severely truncated class file");
        }
        if (this.observer != null) {
            this.observer.parsed(this.bytes, 0, 0, "begin classfile");
            this.observer.parsed(this.bytes, 0, 4, "magic: " + Hex.u4(getMagic0()));
            this.observer.parsed(this.bytes, 4, 2, "minor_version: " + Hex.u2(getMinorVersion0()));
            this.observer.parsed(this.bytes, 6, 2, "major_version: " + Hex.u2(getMajorVersion0()));
        }
        if (this.strictParse) {
            if (!isGoodMagic(getMagic0())) {
                throw new ParseException("bad class file magic (" + Hex.u4(getMagic0()) + ")");
            } else if (!isGoodVersion(getMinorVersion0(), getMajorVersion0())) {
                throw new ParseException("unsupported class file version " + getMajorVersion0() + "." + getMinorVersion0());
            }
        }
        ConstantPoolParser cpParser = new ConstantPoolParser(this.bytes);
        cpParser.setObserver(this.observer);
        this.pool = cpParser.getPool();
        this.pool.setImmutable();
        int at = cpParser.getEndOffset();
        int accessFlags = this.bytes.getUnsignedShort(at);
        this.thisClass = (CstType) this.pool.get(this.bytes.getUnsignedShort(at + 2));
        this.superClass = (CstType) this.pool.get0Ok(this.bytes.getUnsignedShort(at + 4));
        int count = this.bytes.getUnsignedShort(at + 6);
        if (this.observer != null) {
            this.observer.parsed(this.bytes, at, 2, "access_flags: " + AccessFlags.classString(accessFlags));
            this.observer.parsed(this.bytes, at + 2, 2, "this_class: " + this.thisClass);
            this.observer.parsed(this.bytes, at + 4, 2, "super_class: " + stringOrNone(this.superClass));
            this.observer.parsed(this.bytes, at + 6, 2, "interfaces_count: " + Hex.u2(count));
            if (count != 0) {
                this.observer.parsed(this.bytes, at + 8, 0, "interfaces:");
            }
        }
        at += 8;
        this.interfaces = makeTypeList(at, count);
        at += count * 2;
        if (this.strictParse) {
            String thisClassName = this.thisClass.getClassType().getClassName();
            if (!(this.filePath.endsWith(".class") && this.filePath.startsWith(thisClassName) && this.filePath.length() == thisClassName.length() + 6)) {
                throw new ParseException("class name (" + thisClassName + ") does not match path (" + this.filePath + ")");
            }
        }
        this.accessFlags = accessFlags;
        FieldListParser flParser = new FieldListParser(this, this.thisClass, at, this.attributeFactory);
        flParser.setObserver(this.observer);
        this.fields = flParser.getList();
        MethodListParser mlParser = new MethodListParser(this, this.thisClass, flParser.getEndOffset(), this.attributeFactory);
        mlParser.setObserver(this.observer);
        this.methods = mlParser.getList();
        AttributeListParser alParser = new AttributeListParser(this, 0, mlParser.getEndOffset(), this.attributeFactory);
        alParser.setObserver(this.observer);
        this.attributes = alParser.getList();
        this.attributes.setImmutable();
        at = alParser.getEndOffset();
        if (at != this.bytes.size()) {
            throw new ParseException("extra bytes at end of class file, at offset " + Hex.u4(at));
        } else if (this.observer != null) {
            this.observer.parsed(this.bytes, at, 0, "end classfile");
        }
    }
}
