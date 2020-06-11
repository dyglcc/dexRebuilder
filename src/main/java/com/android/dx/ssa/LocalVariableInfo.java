package com.android.dx.ssa;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecSet;
import com.android.dx.util.MutabilityControl;
import java.util.HashMap;
import java.util.List;

public class LocalVariableInfo extends MutabilityControl {
    private final RegisterSpecSet[] blockStarts;
    private final RegisterSpecSet emptySet;
    private final HashMap<SsaInsn, RegisterSpec> insnAssignments;
    private final int regCount;

    public LocalVariableInfo(SsaMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        List<SsaBasicBlock> blocks = method.getBlocks();
        this.regCount = method.getRegCount();
        this.emptySet = new RegisterSpecSet(this.regCount);
        this.blockStarts = new RegisterSpecSet[blocks.size()];
        this.insnAssignments = new HashMap();
        this.emptySet.setImmutable();
    }

    public void setStarts(int index, RegisterSpecSet specs) {
        throwIfImmutable();
        if (specs == null) {
            throw new NullPointerException("specs == null");
        }
        try {
            this.blockStarts[index] = specs;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("bogus index");
        }
    }

    public boolean mergeStarts(int index, RegisterSpecSet specs) {
        RegisterSpecSet start = getStarts0(index);
        if (start == null) {
            setStarts(index, specs);
            return true;
        }
        RegisterSpecSet newStart = start.mutableCopy();
        newStart.intersect(specs, true);
        if (start.equals(newStart)) {
            return false;
        }
        newStart.setImmutable();
        setStarts(index, newStart);
        return true;
    }

    public RegisterSpecSet getStarts(int index) {
        RegisterSpecSet result = getStarts0(index);
        return result != null ? result : this.emptySet;
    }

    public RegisterSpecSet getStarts(SsaBasicBlock block) {
        return getStarts(block.getIndex());
    }

    public RegisterSpecSet mutableCopyOfStarts(int index) {
        RegisterSpecSet result = getStarts0(index);
        return result != null ? result.mutableCopy() : new RegisterSpecSet(this.regCount);
    }

    public void addAssignment(SsaInsn insn, RegisterSpec spec) {
        throwIfImmutable();
        if (insn == null) {
            throw new NullPointerException("insn == null");
        } else if (spec == null) {
            throw new NullPointerException("spec == null");
        } else {
            this.insnAssignments.put(insn, spec);
        }
    }

    public RegisterSpec getAssignment(SsaInsn insn) {
        return (RegisterSpec) this.insnAssignments.get(insn);
    }

    public int getAssignmentCount() {
        return this.insnAssignments.size();
    }

    public void debugDump() {
        for (int index = 0; index < this.blockStarts.length; index++) {
            if (this.blockStarts[index] != null) {
                if (this.blockStarts[index] == this.emptySet) {
                    System.out.printf("%04x: empty set\n", new Object[]{Integer.valueOf(index)});
                } else {
                    System.out.printf("%04x: %s\n", new Object[]{Integer.valueOf(index), this.blockStarts[index]});
                }
            }
        }
    }

    private RegisterSpecSet getStarts0(int index) {
        try {
            return this.blockStarts[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("bogus index");
        }
    }
}
