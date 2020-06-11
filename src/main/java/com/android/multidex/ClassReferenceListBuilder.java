package com.android.multidex;

import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.iface.FieldList;
import com.android.dx.cf.iface.MethodList;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstBaseMethodRef;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.TypeList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassReferenceListBuilder {
    static final /* synthetic */ boolean $assertionsDisabled = (!ClassReferenceListBuilder.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final String CLASS_EXTENSION = ".class";
    private final Set<String> classNames = new HashSet();
    private final Path path;

    public ClassReferenceListBuilder(Path path) {
        this.path = path;
    }

    @Deprecated
    public static void main(String[] args) {
        MainDexListBuilder.main(args);
    }

    public void addRoots(ZipFile jarOfRoots) throws IOException {
        Enumeration<? extends ZipEntry> entries = jarOfRoots.entries();
        while (entries.hasMoreElements()) {
            String name = ((ZipEntry) entries.nextElement()).getName();
            if (name.endsWith(CLASS_EXTENSION)) {
                this.classNames.add(name.substring(0, name.length() - CLASS_EXTENSION.length()));
            }
        }
        entries = jarOfRoots.entries();
        while (entries.hasMoreElements()) {
            name = ((ZipEntry) entries.nextElement()).getName();
            if (name.endsWith(CLASS_EXTENSION)) {
                try {
                    addDependencies(this.path.getClass(name));
                } catch (FileNotFoundException e) {
                    throw new IOException("Class " + name + " is missing form original class path " + this.path, e);
                }
            }
        }
    }

    Set<String> getClassNames() {
        return this.classNames;
    }

    private void addDependencies(DirectClassFile classFile) {
        int i;
        for (Constant constant : classFile.getConstantPool().getEntries()) {
            if (constant instanceof CstType) {
                checkDescriptor(((CstType) constant).getClassType().getDescriptor());
            } else if (constant instanceof CstFieldRef) {
                checkDescriptor(((CstFieldRef) constant).getType().getDescriptor());
            } else if (constant instanceof CstBaseMethodRef) {
                checkPrototype(((CstBaseMethodRef) constant).getPrototype());
            }
        }
        FieldList fields = classFile.getFields();
        int nbField = fields.size();
        for (i = 0; i < nbField; i++) {
            checkDescriptor(fields.get(i).getDescriptor().getString());
        }
        MethodList methods = classFile.getMethods();
        int nbMethods = methods.size();
        for (i = 0; i < nbMethods; i++) {
            checkPrototype(Prototype.intern(methods.get(i).getDescriptor().getString()));
        }
    }

    private void checkPrototype(Prototype proto) {
        checkDescriptor(proto.getReturnType().getDescriptor());
        StdTypeList args = proto.getParameterTypes();
        for (int i = 0; i < args.size(); i++) {
            checkDescriptor(args.get(i).getDescriptor());
        }
    }

    private void checkDescriptor(String typeDescriptor) {
        if (typeDescriptor.endsWith(";")) {
            int lastBrace = typeDescriptor.lastIndexOf(91);
            if (lastBrace < 0) {
                addClassWithHierachy(typeDescriptor.substring(1, typeDescriptor.length() - 1));
            } else if ($assertionsDisabled || (typeDescriptor.length() > lastBrace + 3 && typeDescriptor.charAt(lastBrace + 1) == 'L')) {
                addClassWithHierachy(typeDescriptor.substring(lastBrace + 2, typeDescriptor.length() - 1));
            } else {
                throw new AssertionError();
            }
        }
    }

    private void addClassWithHierachy(String classBinaryName) {
        if (!this.classNames.contains(classBinaryName)) {
            try {
                DirectClassFile classFile = this.path.getClass(classBinaryName + CLASS_EXTENSION);
                this.classNames.add(classBinaryName);
                CstType superClass = classFile.getSuperclass();
                if (superClass != null) {
                    addClassWithHierachy(superClass.getClassType().getClassName());
                }
                TypeList interfaceList = classFile.getInterfaces();
                int interfaceNumber = interfaceList.size();
                for (int i = 0; i < interfaceNumber; i++) {
                    addClassWithHierachy(interfaceList.getType(i).getClassName());
                }
            } catch (FileNotFoundException e) {
            }
        }
    }
}
