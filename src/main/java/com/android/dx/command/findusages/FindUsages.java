package com.android.dx.command.findusages;

import com.android.dex.ClassData;
import com.android.dex.ClassData.Field;
import com.android.dex.ClassData.Method;
import com.android.dex.ClassDef;
import com.android.dex.Dex;
import com.android.dex.FieldId;
import com.android.dex.MethodId;
import com.android.dx.io.CodeReader;
import com.android.dx.io.CodeReader.Visitor;
import com.android.dx.io.OpcodeInfo;
import com.android.dx.io.instructions.DecodedInstruction;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class FindUsages {
    private final CodeReader codeReader = new CodeReader();
    private ClassDef currentClass;
    private Method currentMethod;
    private final Dex dex;
    private final Set<Integer> fieldIds;
    private final Set<Integer> methodIds;
    private final PrintWriter out;

    public FindUsages(final Dex dex, String declaredBy, String memberName, PrintWriter out) {
        this.dex = dex;
        this.out = out;
        Set<Integer> typeStringIndexes = new HashSet();
        Set<Integer> memberNameIndexes = new HashSet();
        Pattern declaredByPattern = Pattern.compile(declaredBy);
        Pattern memberNamePattern = Pattern.compile(memberName);
        List<String> strings = dex.strings();
        for (int i = 0; i < strings.size(); i++) {
            String string = (String) strings.get(i);
            if (declaredByPattern.matcher(string).matches()) {
                typeStringIndexes.add(Integer.valueOf(i));
            }
            if (memberNamePattern.matcher(string).matches()) {
                memberNameIndexes.add(Integer.valueOf(i));
            }
        }
        if (typeStringIndexes.isEmpty() || memberNameIndexes.isEmpty()) {
            this.fieldIds = null;
            this.methodIds = null;
            return;
        }
        this.methodIds = new HashSet();
        this.fieldIds = new HashSet();
        for (Integer intValue : typeStringIndexes) {
            int typeIndex = Collections.binarySearch(dex.typeIds(), Integer.valueOf(intValue.intValue()));
            if (typeIndex >= 0) {
                this.methodIds.addAll(getMethodIds(dex, memberNameIndexes, typeIndex));
                this.fieldIds.addAll(getFieldIds(dex, memberNameIndexes, typeIndex));
            }
        }
        final PrintWriter printWriter = out;
        this.codeReader.setFieldVisitor(new Visitor() {
            public void visit(DecodedInstruction[] all, DecodedInstruction one) {
                int fieldId = one.getIndex();
                if (FindUsages.this.fieldIds.contains(Integer.valueOf(fieldId))) {
                    printWriter.println(FindUsages.this.location() + ": field reference " + dex.fieldIds().get(fieldId) + " (" + OpcodeInfo.getName(one.getOpcode()) + ")");
                }
            }
        });
        printWriter = out;
        this.codeReader.setMethodVisitor(new Visitor() {
            public void visit(DecodedInstruction[] all, DecodedInstruction one) {
                int methodId = one.getIndex();
                if (FindUsages.this.methodIds.contains(Integer.valueOf(methodId))) {
                    printWriter.println(FindUsages.this.location() + ": method reference " + dex.methodIds().get(methodId) + " (" + OpcodeInfo.getName(one.getOpcode()) + ")");
                }
            }
        });
    }

    private String location() {
        String className = (String) this.dex.typeNames().get(this.currentClass.getTypeIndex());
        if (this.currentMethod == null) {
            return className;
        }
        return className + "." + ((String) this.dex.strings().get(((MethodId) this.dex.methodIds().get(this.currentMethod.getMethodIndex())).getNameIndex()));
    }

    public void findUsages() {
        if (this.fieldIds != null && this.methodIds != null) {
            for (ClassDef classDef : this.dex.classDefs()) {
                this.currentClass = classDef;
                this.currentMethod = null;
                if (classDef.getClassDataOffset() != 0) {
                    ClassData classData = this.dex.readClassData(classDef);
                    for (Field field : classData.allFields()) {
                        int fieldIndex = field.getFieldIndex();
                        if (this.fieldIds.contains(Integer.valueOf(fieldIndex))) {
                            this.out.println(location() + " field declared " + this.dex.fieldIds().get(fieldIndex));
                        }
                    }
                    for (Method method : classData.allMethods()) {
                        this.currentMethod = method;
                        int methodIndex = method.getMethodIndex();
                        if (this.methodIds.contains(Integer.valueOf(methodIndex))) {
                            this.out.println(location() + " method declared " + this.dex.methodIds().get(methodIndex));
                        }
                        if (method.getCodeOffset() != 0) {
                            this.codeReader.visitAll(this.dex.readCode(method).getInstructions());
                        }
                    }
                }
            }
            this.currentClass = null;
            this.currentMethod = null;
        }
    }

    private Set<Integer> getFieldIds(Dex dex, Set<Integer> memberNameIndexes, int declaringType) {
        Set<Integer> fields = new HashSet();
        int fieldIndex = 0;
        for (FieldId fieldId : dex.fieldIds()) {
            if (memberNameIndexes.contains(Integer.valueOf(fieldId.getNameIndex())) && declaringType == fieldId.getDeclaringClassIndex()) {
                fields.add(Integer.valueOf(fieldIndex));
            }
            fieldIndex++;
        }
        return fields;
    }

    private Set<Integer> getMethodIds(Dex dex, Set<Integer> memberNameIndexes, int declaringType) {
        Set<Integer> subtypes = findAssignableTypes(dex, declaringType);
        Set<Integer> methods = new HashSet();
        int methodIndex = 0;
        for (MethodId method : dex.methodIds()) {
            if (memberNameIndexes.contains(Integer.valueOf(method.getNameIndex())) && subtypes.contains(Integer.valueOf(method.getDeclaringClassIndex()))) {
                methods.add(Integer.valueOf(methodIndex));
            }
            methodIndex++;
        }
        return methods;
    }

    private Set<Integer> findAssignableTypes(Dex dex, int typeIndex) {
        Set<Integer> assignableTypes = new HashSet();
        assignableTypes.add(Integer.valueOf(typeIndex));
        for (ClassDef classDef : dex.classDefs()) {
            if (assignableTypes.contains(Integer.valueOf(classDef.getSupertypeIndex()))) {
                assignableTypes.add(Integer.valueOf(classDef.getTypeIndex()));
            } else {
                for (int implemented : classDef.getInterfaces()) {
                    if (assignableTypes.contains(Integer.valueOf(implemented))) {
                        assignableTypes.add(Integer.valueOf(classDef.getTypeIndex()));
                        break;
                    }
                }
            }
        }
        return assignableTypes;
    }
}
