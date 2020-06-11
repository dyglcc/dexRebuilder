package com.android.dx.rop.code;

import com.android.dx.util.MutabilityControl;
import java.util.HashMap;

public final class LocalVariableInfo extends MutabilityControl {
    private final RegisterSpecSet[] blockStarts;
    private final RegisterSpecSet emptySet;
    private final HashMap<Insn, RegisterSpec> insnAssignments;
    private final int regCount;

    public LocalVariableInfo(RopMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        BasicBlockList blocks = method.getBlocks();
        int maxLabel = blocks.getMaxLabel();
        this.regCount = blocks.getRegCount();
        this.emptySet = new RegisterSpecSet(this.regCount);
        this.blockStarts = new RegisterSpecSet[maxLabel];
        this.insnAssignments = new HashMap(blocks.getInstructionCount());
        this.emptySet.setImmutable();
    }

    public void setStarts(int label, RegisterSpecSet specs) {
        throwIfImmutable();
        if (specs == null) {
            throw new NullPointerException("specs == null");
        }
        try {
            this.blockStarts[label] = specs;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("bogus label");
        }
    }

    public boolean mergeStarts(int label, RegisterSpecSet specs) {
        RegisterSpecSet start = getStarts0(label);
        if (start == null) {
            setStarts(label, specs);
            return true;
        }
        RegisterSpecSet newStart = start.mutableCopy();
        if (start.size() != 0) {
            newStart.intersect(specs, true);
        } else {
            newStart = specs.mutableCopy();
        }
        if (start.equals(newStart)) {
            return false;
        }
        newStart.setImmutable();
        setStarts(label, newStart);
        return true;
    }

    public RegisterSpecSet getStarts(int label) {
        RegisterSpecSet result = getStarts0(label);
        return result != null ? result : this.emptySet;
    }

    public RegisterSpecSet getStarts(BasicBlock block) {
        return getStarts(block.getLabel());
    }

    public RegisterSpecSet mutableCopyOfStarts(int label) {
        RegisterSpecSet result = getStarts0(label);
        return result != null ? result.mutableCopy() : new RegisterSpecSet(this.regCount);
    }

    public void addAssignment(Insn insn, RegisterSpec spec) {
        throwIfImmutable();
        if (insn == null) {
            throw new NullPointerException("insn == null");
        } else if (spec == null) {
            throw new NullPointerException("spec == null");
        } else {
            this.insnAssignments.put(insn, spec);
        }
    }

    public RegisterSpec getAssignment(Insn insn) {
        return (RegisterSpec) this.insnAssignments.get(insn);
    }

    public int getAssignmentCount() {
        return this.insnAssignments.size();
    }

    public void debugDump() {
        for (int label = 0; label < this.blockStarts.length; label++) {
            if (this.blockStarts[label] != null) {
                if (this.blockStarts[label] == this.emptySet) {
                    System.out.printf("%04x: empty set\n", new Object[]{Integer.valueOf(label)});
                } else {
                    System.out.printf("%04x: %s\n", new Object[]{Integer.valueOf(label), this.blockStarts[label]});
                }
            }
        }
    }

    private RegisterSpecSet getStarts0(int label) {
        try {
            return this.blockStarts[label];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("bogus label");
        }
    }
}
