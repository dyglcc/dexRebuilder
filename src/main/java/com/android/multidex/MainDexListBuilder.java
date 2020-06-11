package com.android.multidex;

import com.android.dx.cf.attrib.AttRuntimeVisibleAnnotations;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.iface.Attribute;
import com.android.dx.cf.iface.FieldList;
import com.android.dx.cf.iface.HasAttribute;
import com.android.dx.cf.iface.MethodList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

public class MainDexListBuilder {
    private static final String CLASS_EXTENSION = ".class";
    private static final String DISABLE_ANNOTATION_RESOLUTION_WORKAROUND = "--disable-annotation-resolution-workaround";
    private static final String EOL = System.getProperty("line.separator");
    private static final int STATUS_ERROR = 1;
    private static final String USAGE_MESSAGE = ("Usage:" + EOL + EOL + "Short version: Don't use this." + EOL + EOL + "Slightly longer version: This tool is used by mainDexClasses script to build" + EOL + "the main dex list." + EOL);
    private Set<String> filesToKeep = new HashSet();

    public static void main(String[] args) {
        int argIndex = 0;
        boolean keepAnnotated = true;
        while (argIndex < args.length - 2) {
            if (args[argIndex].equals(DISABLE_ANNOTATION_RESOLUTION_WORKAROUND)) {
                keepAnnotated = false;
            } else {
                System.err.println("Invalid option " + args[argIndex]);
                printUsage();
                System.exit(1);
            }
            argIndex++;
        }
        if (args.length - argIndex != 2) {
            printUsage();
            System.exit(1);
        }
        try {
            printList(new MainDexListBuilder(keepAnnotated, args[argIndex], args[argIndex + 1]).getMainDexList());
        } catch (IOException e) {
            System.err.println("A fatal error occured: " + e.getMessage());
            System.exit(1);
        }
    }

    public MainDexListBuilder(boolean keepAnnotated, String rootJar, String pathString) throws IOException {
        Throwable th;
        ZipFile jarOfRoots = null;
        Path path = null;
        try {
            ZipFile jarOfRoots2 = new ZipFile(rootJar);
            try {
                Path path2 = new Path(pathString);
                try {
                    ClassReferenceListBuilder mainListBuilder = new ClassReferenceListBuilder(path2);
                    mainListBuilder.addRoots(jarOfRoots2);
                    for (String className : mainListBuilder.getClassNames()) {
                        this.filesToKeep.add(className + CLASS_EXTENSION);
                    }
                    if (keepAnnotated) {
                        keepAnnotated(path2);
                    }
                    try {
                        jarOfRoots2.close();
                    } catch (IOException e) {
                    }
                    if (path2 != null) {
                        for (ClassPathElement element : path2.elements) {
                            try {
                                element.close();
                            } catch (IOException e2) {
                            }
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    path = path2;
                    jarOfRoots = jarOfRoots2;
                    try {
                        jarOfRoots.close();
                    } catch (IOException e3) {
                    }
                    if (path != null) {
                        for (ClassPathElement element2 : path.elements) {
                            try {
                                element2.close();
                            } catch (IOException e4) {
                            }
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                jarOfRoots = jarOfRoots2;
                jarOfRoots.close();
                if (path != null) {
                    while (r9.hasNext()) {
                        element2.close();
                    }
                }
                throw th;
            }
        } catch (IOException e5) {
            throw new IOException("\"" + rootJar + "\" can not be read as a zip archive. (" + e5.getMessage() + ")", e5);
        } catch (Throwable th4) {
            th = th4;
            jarOfRoots.close();
            if (path != null) {
                while (r9.hasNext()) {
                    element2.close();
                }
            }
            throw th;
        }
    }

    public Set<String> getMainDexList() {
        return this.filesToKeep;
    }

    private static void printUsage() {
        System.err.print(USAGE_MESSAGE);
    }

    private static void printList(Set<String> fileNames) {
        for (String fileName : fileNames) {
            System.out.println(fileName);
        }
    }

    private void keepAnnotated(Path path) throws FileNotFoundException {
        for (ClassPathElement element : path.getElements()) {
            for (String name : element.list()) {
                if (name.endsWith(CLASS_EXTENSION)) {
                    DirectClassFile clazz = path.getClass(name);
                    if (hasRuntimeVisibleAnnotation(clazz)) {
                        this.filesToKeep.add(name);
                    } else {
                        int i;
                        MethodList methods = clazz.getMethods();
                        for (i = 0; i < methods.size(); i++) {
                            if (hasRuntimeVisibleAnnotation(methods.get(i))) {
                                this.filesToKeep.add(name);
                                break;
                            }
                        }
                        FieldList fields = clazz.getFields();
                        for (i = 0; i < fields.size(); i++) {
                            if (hasRuntimeVisibleAnnotation(fields.get(i))) {
                                this.filesToKeep.add(name);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean hasRuntimeVisibleAnnotation(HasAttribute element) {
        Attribute att = element.getAttributes().findFirst(AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME);
        return att != null && ((AttRuntimeVisibleAnnotations) att).getAnnotations().size() > 0;
    }
}
