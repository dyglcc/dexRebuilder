package com.android.dx.command.annotool;

import com.android.dx.cf.attrib.AttRuntimeInvisibleAnnotations;
import com.android.dx.cf.attrib.AttRuntimeVisibleAnnotations;
import com.android.dx.cf.attrib.BaseAnnotations;
import com.android.dx.cf.direct.ClassPathOpener;
import com.android.dx.cf.direct.ClassPathOpener.Consumer;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.iface.Attribute;
import com.android.dx.cf.iface.AttributeList;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.util.ByteArray;
import java.io.File;
import java.lang.annotation.ElementType;
import java.util.HashSet;
import java.util.Iterator;

class AnnotationLister {
    private static final String PACKAGE_INFO = "package-info";
    private final Arguments args;
    HashSet<String> matchInnerClassesOf = new HashSet();
    HashSet<String> matchPackages = new HashSet();

    AnnotationLister(Arguments args) {
        this.args = args;
    }

    void process() {
        for (String path : this.args.files) {
            new ClassPathOpener(path, true, new Consumer() {
                public boolean processFileBytes(String name, long lastModified, byte[] bytes) {
                    if (name.endsWith(".class")) {
                        DirectClassFile cf = new DirectClassFile(new ByteArray(bytes), name, true);
                        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
                        AttributeList attributes = cf.getAttributes();
                        String cfClassName = cf.getThisClass().getClassType().getClassName();
                        Attribute att;
                        if (cfClassName.endsWith(AnnotationLister.PACKAGE_INFO)) {
                            for (att = attributes.findFirst(AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME); att != null; att = attributes.findNext(att)) {
                                AnnotationLister.this.visitPackageAnnotation(cf, (BaseAnnotations) att);
                            }
                            for (att = attributes.findFirst(AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME); att != null; att = attributes.findNext(att)) {
                                AnnotationLister.this.visitPackageAnnotation(cf, (BaseAnnotations) att);
                            }
                        } else if (AnnotationLister.this.isMatchingInnerClass(cfClassName) || AnnotationLister.this.isMatchingPackage(cfClassName)) {
                            AnnotationLister.this.printMatch(cf);
                        } else {
                            for (att = attributes.findFirst(AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME); att != null; att = attributes.findNext(att)) {
                                AnnotationLister.this.visitClassAnnotation(cf, (BaseAnnotations) att);
                            }
                            for (att = attributes.findFirst(AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME); att != null; att = attributes.findNext(att)) {
                                AnnotationLister.this.visitClassAnnotation(cf, (BaseAnnotations) att);
                            }
                        }
                    }
                    return true;
                }

                public void onException(Exception ex) {
                    throw new RuntimeException(ex);
                }

                public void onProcessArchiveStart(File file) {
                }
            }).process();
        }
    }

    private void visitClassAnnotation(DirectClassFile cf, BaseAnnotations ann) {
        if (this.args.eTypes.contains(ElementType.TYPE)) {
            for (Annotation anAnn : ann.getAnnotations().getAnnotations()) {
                if (this.args.aclass.equals(anAnn.getType().getClassType().getClassName())) {
                    printMatch(cf);
                }
            }
        }
    }

    private void visitPackageAnnotation(DirectClassFile cf, BaseAnnotations ann) {
        if (this.args.eTypes.contains(ElementType.PACKAGE)) {
            String packageName = cf.getThisClass().getClassType().getClassName();
            int slashIndex = packageName.lastIndexOf(47);
            if (slashIndex == -1) {
                packageName = "";
            } else {
                packageName = packageName.substring(0, slashIndex);
            }
            for (Annotation anAnn : ann.getAnnotations().getAnnotations()) {
                if (this.args.aclass.equals(anAnn.getType().getClassType().getClassName())) {
                    printMatchPackage(packageName);
                }
            }
        }
    }

    private void printMatchPackage(String packageName) {
        Iterator it = this.args.printTypes.iterator();
        while (it.hasNext()) {
            switch ((PrintType) it.next()) {
                case CLASS:
                case INNERCLASS:
                case METHOD:
                    this.matchPackages.add(packageName);
                    break;
                case PACKAGE:
                    System.out.println(packageName.replace(ClassPathElement.SEPARATOR_CHAR, '.'));
                    break;
                default:
                    break;
            }
        }
    }

    private void printMatch(DirectClassFile cf) {
        Iterator it = this.args.printTypes.iterator();
        while (it.hasNext()) {
            switch ((PrintType) it.next()) {
                case CLASS:
                    System.out.println(cf.getThisClass().getClassType().getClassName().replace(ClassPathElement.SEPARATOR_CHAR, '.'));
                    break;
                case INNERCLASS:
                    this.matchInnerClassesOf.add(cf.getThisClass().getClassType().getClassName());
                    break;
                case METHOD:
                    break;
                default:
                    break;
            }
        }
    }

    private boolean isMatchingInnerClass(String s) {
        do {
            int i = s.lastIndexOf(36);
            if (i <= 0) {
                return false;
            }
            s = s.substring(0, i);
        } while (!this.matchInnerClassesOf.contains(s));
        return true;
    }

    private boolean isMatchingPackage(String s) {
        String packageName;
        int slashIndex = s.lastIndexOf(47);
        if (slashIndex == -1) {
            packageName = "";
        } else {
            packageName = s.substring(0, slashIndex);
        }
        return this.matchPackages.contains(packageName);
    }
}
