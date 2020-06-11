package com.android.dx.dex.cf;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.cf.code.BootstrapMethodsList.Item;
import com.android.dx.cf.code.ConcreteMethod;
import com.android.dx.cf.code.Ropper;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.iface.Field;
import com.android.dx.cf.iface.FieldList;
import com.android.dx.cf.iface.Method;
import com.android.dx.cf.iface.MethodList;
import com.android.dx.command.dexer.DxContext;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.code.DalvCode;
import com.android.dx.dex.code.DalvCode.AssignIndicesCallback;
import com.android.dx.dex.code.RopTranslator;
import com.android.dx.dex.file.CallSiteIdsSection;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.file.EncodedField;
import com.android.dx.dex.file.EncodedMethod;
import com.android.dx.dex.file.FieldIdsSection;
import com.android.dx.dex.file.MethodHandlesSection;
import com.android.dx.dex.file.MethodIdsSection;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.DexTranslationAdvice;
import com.android.dx.rop.code.LocalVariableExtractor;
import com.android.dx.rop.code.LocalVariableInfo;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstBaseMethodRef;
import com.android.dx.rop.cst.CstBoolean;
import com.android.dx.rop.cst.CstByte;
import com.android.dx.rop.cst.CstCallSite;
import com.android.dx.rop.cst.CstCallSiteRef;
import com.android.dx.rop.cst.CstChar;
import com.android.dx.rop.cst.CstEnumRef;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstInterfaceMethodRef;
import com.android.dx.rop.cst.CstInvokeDynamic;
import com.android.dx.rop.cst.CstMethodHandle;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstShort;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.TypedConstant;
import com.android.dx.rop.type.Type;
import com.android.dx.ssa.Optimizer;

public class CfTranslator {
    private static final boolean DEBUG = false;

    private CfTranslator() {
    }

    public static ClassDefItem translate(DxContext context, DirectClassFile cf, byte[] bytes, CfOptions cfOptions, DexOptions dexOptions, DexFile dexFile) {
        try {
            return translate0(context, cf, bytes, cfOptions, dexOptions, dexFile);
        } catch (RuntimeException ex) {
            throw ExceptionWithContext.withContext(ex, "...while processing " + cf.getFilePath());
        }
    }

    private static ClassDefItem translate0(DxContext context, DirectClassFile cf, byte[] bytes, CfOptions cfOptions, DexOptions dexOptions, DexFile dexFile) {
        CstString sourceFile;
        context.optimizerOptions.loadOptimizeLists(cfOptions.optimizeListFile, cfOptions.dontOptimizeListFile);
        CstType thisClass = cf.getThisClass();
        int classAccessFlags = cf.getAccessFlags() & -33;
        if (cfOptions.positionInfo == 1) {
            sourceFile = null;
        } else {
            sourceFile = cf.getSourceFile();
        }
        ClassDefItem out = new ClassDefItem(thisClass, classAccessFlags, cf.getSuperclass(), cf.getInterfaces(), sourceFile);
        Annotations classAnnotations = AttributeTranslator.getClassAnnotations(cf, cfOptions);
        if (classAnnotations.size() != 0) {
            out.setClassAnnotations(classAnnotations, dexFile);
        }
        FieldIdsSection fieldIdsSection = dexFile.getFieldIds();
        MethodIdsSection methodIdsSection = dexFile.getMethodIds();
        MethodHandlesSection methodHandlesSection = dexFile.getMethodHandles();
        CallSiteIdsSection callSiteIds = dexFile.getCallSiteIds();
        processFields(cf, out, dexFile);
        processMethods(context, cf, cfOptions, dexOptions, out, dexFile);
        ConstantPool constantPool = cf.getConstantPool();
        int constantPoolSize = constantPool.size();
        for (int i = 0; i < constantPoolSize; i++) {
            Constant constant = constantPool.getOrNull(i);
            if (constant instanceof CstMethodRef) {
                methodIdsSection.intern((CstBaseMethodRef) constant);
            } else if (constant instanceof CstInterfaceMethodRef) {
                methodIdsSection.intern(((CstInterfaceMethodRef) constant).toMethodRef());
            } else if (constant instanceof CstFieldRef) {
                fieldIdsSection.intern((CstFieldRef) constant);
            } else if (constant instanceof CstEnumRef) {
                fieldIdsSection.intern(((CstEnumRef) constant).getFieldRef());
            } else if (constant instanceof CstMethodHandle) {
                methodHandlesSection.intern((CstMethodHandle) constant);
            } else if (constant instanceof CstInvokeDynamic) {
                CstInvokeDynamic cstInvokeDynamic = (CstInvokeDynamic) constant;
                Item bootstrapMethod = cf.getBootstrapMethods().get(cstInvokeDynamic.getBootstrapMethodIndex());
                CstCallSite callSite = CstCallSite.make(bootstrapMethod.getBootstrapMethodHandle(), cstInvokeDynamic.getNat(), bootstrapMethod.getBootstrapMethodArguments());
                cstInvokeDynamic.setDeclaringClass(cf.getThisClass());
                cstInvokeDynamic.setCallSite(callSite);
                for (CstCallSiteRef ref : cstInvokeDynamic.getReferences()) {
                    callSiteIds.intern(ref);
                }
            }
        }
        return out;
    }

    private static void processFields(DirectClassFile cf, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        FieldList fields = cf.getFields();
        int sz = fields.size();
        int i = 0;
        while (i < sz) {
            Field one = fields.get(i);
            try {
                CstFieldRef field = new CstFieldRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();
                if (AccessFlags.isStatic(accessFlags)) {
                    TypedConstant constVal = one.getConstantValue();
                    EncodedField fi = new EncodedField(field, accessFlags);
                    if (constVal != null) {
                        constVal = coerceConstant(constVal, field.getType());
                    }
                    out.addStaticField(fi, constVal);
                } else {
                    out.addInstanceField(new EncodedField(field, accessFlags));
                }
                Annotations annotations = AttributeTranslator.getAnnotations(one.getAttributes());
                if (annotations.size() != 0) {
                    out.addFieldAnnotations(field, annotations, dexFile);
                }
                dexFile.getFieldIds().intern(field);
                i++;
            } catch (RuntimeException ex) {
                throw ExceptionWithContext.withContext(ex, "...while processing " + one.getName().toHuman() + " " + one.getDescriptor().toHuman());
            }
        }
    }

    private static TypedConstant coerceConstant(TypedConstant constant, Type type) {
        if (constant.getType().equals(type)) {
            return constant;
        }
        switch (type.getBasicType()) {
            case 1:
                return CstBoolean.make(((CstInteger) constant).getValue());
            case 2:
                return CstByte.make(((CstInteger) constant).getValue());
            case 3:
                return CstChar.make(((CstInteger) constant).getValue());
            case 8:
                return CstShort.make(((CstInteger) constant).getValue());
            default:
                throw new UnsupportedOperationException("can't coerce " + constant + " to " + type);
        }
    }

    private static void processMethods(DxContext context, DirectClassFile cf, CfOptions cfOptions, DexOptions dexOptions, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        MethodList methods = cf.getMethods();
        int sz = methods.size();
        for (int i = 0; i < sz; i++) {
            DalvCode code;
            Method one = methods.get(i);
            CstBaseMethodRef cstMethodRef = new CstMethodRef(thisClass, one.getNat());
            int accessFlags = one.getAccessFlags();
            boolean isStatic = AccessFlags.isStatic(accessFlags);
            boolean isPrivate = AccessFlags.isPrivate(accessFlags);
            boolean isNative = AccessFlags.isNative(accessFlags);
            boolean isAbstract = AccessFlags.isAbstract(accessFlags);
            boolean isConstructor = cstMethodRef.isInstanceInit() || cstMethodRef.isClassInit();
            if (isNative || isAbstract) {
                code = null;
            } else {
                ConcreteMethod concreteMethod = new ConcreteMethod(one, cf, cfOptions.positionInfo != 1, cfOptions.localInfo);
                TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
                RopMethod rmeth = Ropper.convert(concreteMethod, advice, methods, dexOptions);
                RopMethod nonOptRmeth = null;
                int paramSize = cstMethodRef.getParameterWordCount(isStatic);
                String canonicalName = thisClass.getClassType().getDescriptor() + "." + one.getName().getString();
                if (cfOptions.optimize && context.optimizerOptions.shouldOptimize(canonicalName)) {
                    nonOptRmeth = rmeth;
                    rmeth = Optimizer.optimize(rmeth, paramSize, isStatic, cfOptions.localInfo, advice);
                    if (cfOptions.statistics) {
                        context.codeStatistics.updateRopStatistics(nonOptRmeth, rmeth);
                    }
                }
                LocalVariableInfo locals = null;
                if (cfOptions.localInfo) {
                    locals = LocalVariableExtractor.extract(rmeth);
                }
                code = RopTranslator.translate(rmeth, cfOptions.positionInfo, locals, paramSize, dexOptions);
                if (cfOptions.statistics && nonOptRmeth != null) {
                    updateDexStatistics(context, cfOptions, dexOptions, rmeth, nonOptRmeth, locals, paramSize, concreteMethod.getCode().size());
                }
            }
            if (AccessFlags.isSynchronized(accessFlags)) {
                accessFlags |= AccessFlags.ACC_DECLARED_SYNCHRONIZED;
                if (!isNative) {
                    accessFlags &= -33;
                }
            }
            if (isConstructor) {
                accessFlags |= AccessFlags.ACC_CONSTRUCTOR;
            }
            EncodedMethod encodedMethod = new EncodedMethod(cstMethodRef, accessFlags, code, AttributeTranslator.getExceptions(one));
            if (cstMethodRef.isInstanceInit() || cstMethodRef.isClassInit() || isStatic || isPrivate) {
                out.addDirectMethod(encodedMethod);
            } else {
                try {
                    out.addVirtualMethod(encodedMethod);
                } catch (Throwable ex) {
                    throw ExceptionWithContext.withContext(ex, "...while processing " + one.getName().toHuman() + " " + one.getDescriptor().toHuman());
                }
            }
            Annotations annotations = AttributeTranslator.getMethodAnnotations(one);
            if (annotations.size() != 0) {
                out.addMethodAnnotations(cstMethodRef, annotations, dexFile);
            }
            AnnotationsList list = AttributeTranslator.getParameterAnnotations(one);
            if (list.size() != 0) {
                out.addParameterAnnotations(cstMethodRef, list, dexFile);
            }
            dexFile.getMethodIds().intern(cstMethodRef);
        }
    }

    private static void updateDexStatistics(DxContext context, CfOptions cfOptions, DexOptions dexOptions, RopMethod optRmeth, RopMethod nonOptRmeth, LocalVariableInfo locals, int paramSize, int originalByteCount) {
        DalvCode optCode = RopTranslator.translate(optRmeth, cfOptions.positionInfo, locals, paramSize, dexOptions);
        DalvCode nonOptCode = RopTranslator.translate(nonOptRmeth, cfOptions.positionInfo, locals, paramSize, dexOptions);
        AssignIndicesCallback callback = new AssignIndicesCallback() {
            public int getIndex(Constant cst) {
                return 0;
            }
        };
        optCode.assignIndices(callback);
        nonOptCode.assignIndices(callback);
        context.codeStatistics.updateDexStatistics(nonOptCode, optCode);
        context.codeStatistics.updateOriginalByteCount(originalByteCount);
    }
}
