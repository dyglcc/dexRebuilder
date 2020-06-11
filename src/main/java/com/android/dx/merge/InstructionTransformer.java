package com.android.dx.merge;

import com.android.dex.DexException;
import com.android.dex.DexIndexOverflowException;
import com.android.dx.io.CodeReader;
import com.android.dx.io.CodeReader.Visitor;
import com.android.dx.io.instructions.DecodedInstruction;
import com.android.dx.io.instructions.ShortArrayCodeOutput;

final class InstructionTransformer {
    private IndexMap indexMap;
    private int mappedAt;
    private DecodedInstruction[] mappedInstructions;
    private final CodeReader reader = new CodeReader();

    private class CallSiteVisitor implements Visitor {
        private CallSiteVisitor() {
        }

        public void visit(DecodedInstruction[] all, DecodedInstruction one) {
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt = InstructionTransformer.this.mappedAt + 1] = one.withIndex(InstructionTransformer.this.indexMap.adjustCallSite(one.getIndex()));
        }
    }

    private class FieldVisitor implements Visitor {
        private FieldVisitor() {
        }

        public void visit(DecodedInstruction[] all, DecodedInstruction one) {
            int mappedId = InstructionTransformer.this.indexMap.adjustField(one.getIndex());
            InstructionTransformer.jumboCheck(one.getOpcode() == 27, mappedId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt = InstructionTransformer.this.mappedAt + 1] = one.withIndex(mappedId);
        }
    }

    private class GenericVisitor implements Visitor {
        private GenericVisitor() {
        }

        public void visit(DecodedInstruction[] all, DecodedInstruction one) {
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt = InstructionTransformer.this.mappedAt + 1] = one;
        }
    }

    private class MethodAndProtoVisitor implements Visitor {
        private MethodAndProtoVisitor() {
        }

        public void visit(DecodedInstruction[] all, DecodedInstruction one) {
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt = InstructionTransformer.this.mappedAt + 1] = one.withProtoIndex(InstructionTransformer.this.indexMap.adjustMethod(one.getIndex()), InstructionTransformer.this.indexMap.adjustProto(one.getProtoIndex()));
        }
    }

    private class MethodVisitor implements Visitor {
        private MethodVisitor() {
        }

        public void visit(DecodedInstruction[] all, DecodedInstruction one) {
            int mappedId = InstructionTransformer.this.indexMap.adjustMethod(one.getIndex());
            InstructionTransformer.jumboCheck(one.getOpcode() == 27, mappedId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt = InstructionTransformer.this.mappedAt + 1] = one.withIndex(mappedId);
        }
    }

    private class StringVisitor implements Visitor {
        private StringVisitor() {
        }

        public void visit(DecodedInstruction[] all, DecodedInstruction one) {
            int mappedId = InstructionTransformer.this.indexMap.adjustString(one.getIndex());
            InstructionTransformer.jumboCheck(one.getOpcode() == 27, mappedId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt = InstructionTransformer.this.mappedAt + 1] = one.withIndex(mappedId);
        }
    }

    private class TypeVisitor implements Visitor {
        private TypeVisitor() {
        }

        public void visit(DecodedInstruction[] all, DecodedInstruction one) {
            int mappedId = InstructionTransformer.this.indexMap.adjustType(one.getIndex());
            InstructionTransformer.jumboCheck(one.getOpcode() == 27, mappedId);
            InstructionTransformer.this.mappedInstructions[InstructionTransformer.this.mappedAt = InstructionTransformer.this.mappedAt + 1] = one.withIndex(mappedId);
        }
    }

    public InstructionTransformer() {
        this.reader.setAllVisitors(new GenericVisitor());
        this.reader.setStringVisitor(new StringVisitor());
        this.reader.setTypeVisitor(new TypeVisitor());
        this.reader.setFieldVisitor(new FieldVisitor());
        this.reader.setMethodVisitor(new MethodVisitor());
        this.reader.setMethodAndProtoVisitor(new MethodAndProtoVisitor());
        this.reader.setCallSiteVisitor(new CallSiteVisitor());
    }

    public short[] transform(IndexMap indexMap, short[] encodedInstructions) throws DexException {
        int i = 0;
        DecodedInstruction[] decodedInstructions = DecodedInstruction.decodeAll(encodedInstructions);
        int size = decodedInstructions.length;
        this.indexMap = indexMap;
        this.mappedInstructions = new DecodedInstruction[size];
        this.mappedAt = 0;
        this.reader.visitAll(decodedInstructions);
        ShortArrayCodeOutput out = new ShortArrayCodeOutput(size);
        DecodedInstruction[] decodedInstructionArr = this.mappedInstructions;
        int length = decodedInstructionArr.length;
        while (i < length) {
            DecodedInstruction instruction = decodedInstructionArr[i];
            if (instruction != null) {
                instruction.encode(out);
            }
            i++;
        }
        this.indexMap = null;
        return out.getArray();
    }

    private static void jumboCheck(boolean isJumbo, int newIndex) {
        if (!isJumbo && newIndex > 65535) {
            throw new DexIndexOverflowException("Cannot merge new index " + newIndex + " into a non-jumbo instruction!");
        }
    }
}
