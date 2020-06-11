package com.android.dx.dex.cf;

import com.android.dx.cf.attrib.AttAnnotationDefault;
import com.android.dx.cf.attrib.AttEnclosingMethod;
import com.android.dx.cf.attrib.AttExceptions;
import com.android.dx.cf.attrib.AttInnerClasses;
import com.android.dx.cf.attrib.AttRuntimeInvisibleAnnotations;
import com.android.dx.cf.attrib.AttRuntimeInvisibleParameterAnnotations;
import com.android.dx.cf.attrib.AttRuntimeVisibleAnnotations;
import com.android.dx.cf.attrib.AttRuntimeVisibleParameterAnnotations;
import com.android.dx.cf.attrib.AttSignature;
import com.android.dx.cf.attrib.AttSourceDebugExtension;
import com.android.dx.cf.attrib.InnerClassList;
import com.android.dx.cf.attrib.InnerClassList.Item;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.iface.AttributeList;
import com.android.dx.cf.iface.Method;
import com.android.dx.cf.iface.MethodList;
import com.android.dx.dex.file.AnnotationUtils;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.rop.annotation.AnnotationVisibility;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.annotation.NameValuePair;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.Warning;
import java.util.ArrayList;

class AttributeTranslator {
    private AttributeTranslator() {
    }

    public static TypeList getExceptions(Method method) {
        AttExceptions exceptions = (AttExceptions) method.getAttributes().findFirst(AttExceptions.ATTRIBUTE_NAME);
        if (exceptions == null) {
            return StdTypeList.EMPTY;
        }
        return exceptions.getExceptions();
    }

    public static Annotations getAnnotations(AttributeList attribs) {
        Annotations result = getAnnotations0(attribs);
        Annotation signature = getSignature(attribs);
        Annotation sourceDebugExtension = getSourceDebugExtension(attribs);
        if (signature != null) {
            result = Annotations.combine(result, signature);
        }
        if (sourceDebugExtension != null) {
            return Annotations.combine(result, sourceDebugExtension);
        }
        return result;
    }

    public static Annotations getClassAnnotations(DirectClassFile cf, CfOptions args) {
        CstType thisClass = cf.getThisClass();
        AttributeList attribs = cf.getAttributes();
        Annotations result = getAnnotations(attribs);
        Annotation enclosingMethod = translateEnclosingMethod(attribs);
        try {
            Annotations innerClassAnnotations = translateInnerClasses(thisClass, attribs, enclosingMethod == null);
            if (innerClassAnnotations != null) {
                result = Annotations.combine(result, innerClassAnnotations);
            }
        } catch (Warning warn) {
            args.warn.println("warning: " + warn.getMessage());
        }
        if (enclosingMethod != null) {
            result = Annotations.combine(result, enclosingMethod);
        }
        if (!AccessFlags.isAnnotation(cf.getAccessFlags())) {
            return result;
        }
        Annotation annotationDefault = translateAnnotationDefaults(cf);
        if (annotationDefault != null) {
            return Annotations.combine(result, annotationDefault);
        }
        return result;
    }

    public static Annotations getMethodAnnotations(Method method) {
        Annotations result = getAnnotations(method.getAttributes());
        TypeList exceptions = getExceptions(method);
        if (exceptions.size() != 0) {
            return Annotations.combine(result, AnnotationUtils.makeThrows(exceptions));
        }
        return result;
    }

    private static Annotations getAnnotations0(AttributeList attribs) {
        AttRuntimeVisibleAnnotations visible = (AttRuntimeVisibleAnnotations) attribs.findFirst(AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME);
        AttRuntimeInvisibleAnnotations invisible = (AttRuntimeInvisibleAnnotations) attribs.findFirst(AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME);
        if (visible == null) {
            if (invisible == null) {
                return Annotations.EMPTY;
            }
            return invisible.getAnnotations();
        } else if (invisible == null) {
            return visible.getAnnotations();
        } else {
            return Annotations.combine(visible.getAnnotations(), invisible.getAnnotations());
        }
    }

    private static Annotation getSignature(AttributeList attribs) {
        AttSignature signature = (AttSignature) attribs.findFirst(AttSignature.ATTRIBUTE_NAME);
        if (signature == null) {
            return null;
        }
        return AnnotationUtils.makeSignature(signature.getSignature());
    }

    private static Annotation getSourceDebugExtension(AttributeList attribs) {
        AttSourceDebugExtension extension = (AttSourceDebugExtension) attribs.findFirst(AttSourceDebugExtension.ATTRIBUTE_NAME);
        if (extension == null) {
            return null;
        }
        return AnnotationUtils.makeSourceDebugExtension(extension.getSmapString());
    }

    private static Annotation translateEnclosingMethod(AttributeList attribs) {
        AttEnclosingMethod enclosingMethod = (AttEnclosingMethod) attribs.findFirst(AttEnclosingMethod.ATTRIBUTE_NAME);
        if (enclosingMethod == null) {
            return null;
        }
        CstType enclosingClass = enclosingMethod.getEnclosingClass();
        CstNat nat = enclosingMethod.getMethod();
        if (nat == null) {
            return AnnotationUtils.makeEnclosingClass(enclosingClass);
        }
        return AnnotationUtils.makeEnclosingMethod(new CstMethodRef(enclosingClass, nat));
    }

    private static Annotations translateInnerClasses(CstType thisClass, AttributeList attribs, boolean needEnclosingClass) {
        AttInnerClasses innerClasses = (AttInnerClasses) attribs.findFirst(AttInnerClasses.ATTRIBUTE_NAME);
        if (innerClasses == null) {
            return null;
        }
        int i;
        InnerClassList list = innerClasses.getInnerClasses();
        int size = list.size();
        Item foundThisClass = null;
        ArrayList<Type> membersList = new ArrayList();
        for (i = 0; i < size; i++) {
            Item item = list.get(i);
            CstType innerClass = item.getInnerClass();
            if (innerClass.equals(thisClass)) {
                foundThisClass = item;
            } else {
                if (thisClass.equals(item.getOuterClass())) {
                    membersList.add(innerClass.getClassType());
                }
            }
        }
        int membersSize = membersList.size();
        if (foundThisClass == null && membersSize == 0) {
            return null;
        }
        Annotations result = new Annotations();
        if (foundThisClass != null) {
            result.add(AnnotationUtils.makeInnerClass(foundThisClass.getInnerName(), foundThisClass.getAccessFlags()));
            if (needEnclosingClass) {
                if (foundThisClass.getOuterClass() == null) {
                    throw new Warning("Ignoring InnerClasses attribute for an anonymous inner class\n(" + thisClass.toHuman() + ") that doesn't come with an\nassociated EnclosingMethod attribute. This class was probably produced by a\ncompiler that did not target the modern .class file format. The recommended\nsolution is to recompile the class from source, using an up-to-date compiler\nand without specifying any \"-target\" type options. The consequence of ignoring\nthis warning is that reflective operations on this class will incorrectly\nindicate that it is *not* an inner class.");
                }
                result.add(AnnotationUtils.makeEnclosingClass(foundThisClass.getOuterClass()));
            }
        }
        if (membersSize != 0) {
            StdTypeList typeList = new StdTypeList(membersSize);
            for (i = 0; i < membersSize; i++) {
                typeList.set(i, (Type) membersList.get(i));
            }
            typeList.setImmutable();
            result.add(AnnotationUtils.makeMemberClasses(typeList));
        }
        result.setImmutable();
        return result;
    }

    public static AnnotationsList getParameterAnnotations(Method method) {
        AttributeList attribs = method.getAttributes();
        AttRuntimeVisibleParameterAnnotations visible = (AttRuntimeVisibleParameterAnnotations) attribs.findFirst(AttRuntimeVisibleParameterAnnotations.ATTRIBUTE_NAME);
        AttRuntimeInvisibleParameterAnnotations invisible = (AttRuntimeInvisibleParameterAnnotations) attribs.findFirst(AttRuntimeInvisibleParameterAnnotations.ATTRIBUTE_NAME);
        if (visible == null) {
            if (invisible == null) {
                return AnnotationsList.EMPTY;
            }
            return invisible.getParameterAnnotations();
        } else if (invisible == null) {
            return visible.getParameterAnnotations();
        } else {
            return AnnotationsList.combine(visible.getParameterAnnotations(), invisible.getParameterAnnotations());
        }
    }

    private static Annotation translateAnnotationDefaults(DirectClassFile cf) {
        CstType thisClass = cf.getThisClass();
        MethodList methods = cf.getMethods();
        int sz = methods.size();
        Annotation result = new Annotation(thisClass, AnnotationVisibility.EMBEDDED);
        boolean any = false;
        for (int i = 0; i < sz; i++) {
            Method one = methods.get(i);
            AttAnnotationDefault oneDefault = (AttAnnotationDefault) one.getAttributes().findFirst(AttAnnotationDefault.ATTRIBUTE_NAME);
            if (oneDefault != null) {
                result.add(new NameValuePair(one.getNat().getName(), oneDefault.getValue()));
                any = true;
            }
        }
        if (!any) {
            return null;
        }
        result.setImmutable();
        return AnnotationUtils.makeAnnotationDefault(result);
    }
}
