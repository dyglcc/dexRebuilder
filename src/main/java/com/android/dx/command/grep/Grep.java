package com.android.dx.command.grep;

import com.android.dex.ClassData;
import com.android.dex.ClassData.Method;
import com.android.dex.ClassDef;
import com.android.dex.Dex;
import com.android.dex.EncodedValueReader;
import com.android.dex.MethodId;
import com.android.dx.io.CodeReader;
import com.android.dx.io.CodeReader.Visitor;
import com.android.dx.io.instructions.DecodedInstruction;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class Grep {
    private final CodeReader codeReader = new CodeReader();
    private int count = 0;
    private ClassDef currentClass;
    private Method currentMethod;
    private final Dex dex;
    private final PrintWriter out;
    private final Set<Integer> stringIds;

    public Grep(Dex dex, Pattern pattern, PrintWriter out) {
        this.dex = dex;
        this.out = out;
        this.stringIds = getStringIds(dex, pattern);
        this.codeReader.setStringVisitor(new Visitor() {
            public void visit(DecodedInstruction[] all, DecodedInstruction one) {
                Grep.this.encounterString(one.getIndex());
            }
        });
    }

    private void readArray(EncodedValueReader reader) {
        int size = reader.readArray();
        for (int i = 0; i < size; i++) {
            switch (reader.peek()) {
                case 23:
                    encounterString(reader.readString());
                    break;
                case 28:
                    readArray(reader);
                    break;
                default:
                    break;
            }
        }
    }

    private void encounterString(int index) {
        if (this.stringIds.contains(Integer.valueOf(index))) {
            this.out.println(location() + " " + ((String) this.dex.strings().get(index)));
            this.count++;
        }
    }

    private String location() {
        String className = (String) this.dex.typeNames().get(this.currentClass.getTypeIndex());
        if (this.currentMethod == null) {
            return className;
        }
        return className + "." + ((String) this.dex.strings().get(((MethodId) this.dex.methodIds().get(this.currentMethod.getMethodIndex())).getNameIndex()));
    }

    public int grep() {
        for (ClassDef classDef : this.dex.classDefs()) {
            this.currentClass = classDef;
            this.currentMethod = null;
            if (classDef.getClassDataOffset() != 0) {
                ClassData classData = this.dex.readClassData(classDef);
                int staticValuesOffset = classDef.getStaticValuesOffset();
                if (staticValuesOffset != 0) {
                    readArray(new EncodedValueReader(this.dex.open(staticValuesOffset)));
                }
                for (Method method : classData.allMethods()) {
                    this.currentMethod = method;
                    if (method.getCodeOffset() != 0) {
                        this.codeReader.visitAll(this.dex.readCode(method).getInstructions());
                    }
                }
            }
        }
        this.currentClass = null;
        this.currentMethod = null;
        return this.count;
    }

    private Set<Integer> getStringIds(Dex dex, Pattern pattern) {
        Set<Integer> stringIds = new HashSet();
        int stringIndex = 0;
        for (String s : dex.strings()) {
            if (pattern.matcher(s).find()) {
                stringIds.add(Integer.valueOf(stringIndex));
            }
            stringIndex++;
        }
        return stringIds;
    }
}
