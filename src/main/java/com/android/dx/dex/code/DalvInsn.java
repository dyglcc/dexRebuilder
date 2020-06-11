package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.ssa.RegisterMapper;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import com.android.dx.util.TwoColumnOutput;
import java.util.BitSet;

public abstract class DalvInsn {
    private int address;
    private final Dop opcode;
    private final SourcePosition position;
    private final RegisterSpecList registers;

    protected abstract String argString();

    public abstract int codeSize();

    protected abstract String listingString0(boolean z);

    public abstract DalvInsn withOpcode(Dop dop);

    public abstract DalvInsn withRegisterOffset(int i);

    public abstract DalvInsn withRegisters(RegisterSpecList registerSpecList);

    public abstract void writeTo(AnnotatedOutput annotatedOutput);

    public static SimpleInsn makeMove(SourcePosition position, RegisterSpec dest, RegisterSpec src) {
        Dop opcode;
        boolean category1 = true;
        if (dest.getCategory() != 1) {
            category1 = false;
        }
        boolean reference = dest.getType().isReference();
        int destReg = dest.getReg();
        if ((src.getReg() | destReg) < 16) {
            opcode = reference ? Dops.MOVE_OBJECT : category1 ? Dops.MOVE : Dops.MOVE_WIDE;
        } else if (destReg < 256) {
            opcode = reference ? Dops.MOVE_OBJECT_FROM16 : category1 ? Dops.MOVE_FROM16 : Dops.MOVE_WIDE_FROM16;
        } else {
            opcode = reference ? Dops.MOVE_OBJECT_16 : category1 ? Dops.MOVE_16 : Dops.MOVE_WIDE_16;
        }
        return new SimpleInsn(opcode, position, RegisterSpecList.make(dest, src));
    }

    public DalvInsn(Dop opcode, SourcePosition position, RegisterSpecList registers) {
        if (opcode == null) {
            throw new NullPointerException("opcode == null");
        } else if (position == null) {
            throw new NullPointerException("position == null");
        } else if (registers == null) {
            throw new NullPointerException("registers == null");
        } else {
            this.address = -1;
            this.opcode = opcode;
            this.position = position;
            this.registers = registers;
        }
    }

    public final String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(identifierString());
        sb.append(' ');
        sb.append(this.position);
        sb.append(": ");
        sb.append(this.opcode.getName());
        boolean needComma = false;
        if (this.registers.size() != 0) {
            sb.append(this.registers.toHuman(" ", ", ", null));
            needComma = true;
        }
        String extra = argString();
        if (extra != null) {
            if (needComma) {
                sb.append(',');
            }
            sb.append(' ');
            sb.append(extra);
        }
        return sb.toString();
    }

    public final boolean hasAddress() {
        return this.address >= 0;
    }

    public final int getAddress() {
        if (this.address >= 0) {
            return this.address;
        }
        throw new RuntimeException("address not yet known");
    }

    public final Dop getOpcode() {
        return this.opcode;
    }

    public final SourcePosition getPosition() {
        return this.position;
    }

    public final RegisterSpecList getRegisters() {
        return this.registers;
    }

    public final boolean hasResult() {
        return this.opcode.hasResult();
    }

    public final int getMinimumRegisterRequirement(BitSet compatRegs) {
        int i = 0;
        boolean hasResult = hasResult();
        int regSz = this.registers.size();
        int resultRequirement = 0;
        int sourceRequirement = 0;
        if (hasResult && !compatRegs.get(0)) {
            resultRequirement = this.registers.get(0).getCategory();
        }
        if (hasResult) {
            i = 1;
        }
        while (i < regSz) {
            if (!compatRegs.get(i)) {
                sourceRequirement += this.registers.get(i).getCategory();
            }
            i++;
        }
        return Math.max(sourceRequirement, resultRequirement);
    }

    public DalvInsn getLowRegVersion() {
        return withRegisters(this.registers.withExpandedRegisters(0, hasResult(), null));
    }

    public DalvInsn expandedPrefix(BitSet compatRegs) {
        RegisterSpecList regs = this.registers;
        boolean firstBit = compatRegs.get(0);
        if (hasResult()) {
            compatRegs.set(0);
        }
        regs = regs.subset(compatRegs);
        if (hasResult()) {
            compatRegs.set(0, firstBit);
        }
        if (regs.size() == 0) {
            return null;
        }
        return new HighRegisterPrefix(this.position, regs);
    }

    public DalvInsn expandedSuffix(BitSet compatRegs) {
        if (!hasResult() || compatRegs.get(0)) {
            return null;
        }
        RegisterSpec r = this.registers.get(0);
        return makeMove(this.position, r, r.withReg(0));
    }

    public DalvInsn expandedVersion(BitSet compatRegs) {
        return withRegisters(this.registers.withExpandedRegisters(0, hasResult(), compatRegs));
    }

    public final String identifierString() {
        if (this.address == -1) {
            return Hex.u4(System.identityHashCode(this));
        }
        return String.format("%04x", new Object[]{Integer.valueOf(this.address)});
    }

    public final String listingString(String prefix, int width, boolean noteIndices) {
        String insnPerSe = listingString0(noteIndices);
        if (insnPerSe == null) {
            return null;
        }
        String addr = prefix + identifierString() + ": ";
        int w1 = addr.length();
        return TwoColumnOutput.toString(addr, w1, "", insnPerSe, width == 0 ? insnPerSe.length() : width - w1);
    }

    public final void setAddress(int address) {
        if (address < 0) {
            throw new IllegalArgumentException("address < 0");
        }
        this.address = address;
    }

    public final int getNextAddress() {
        return getAddress() + codeSize();
    }

    public DalvInsn withMapper(RegisterMapper mapper) {
        return withRegisters(mapper.map(getRegisters()));
    }

    public String cstString() {
        throw new UnsupportedOperationException("Not supported.");
    }

    public String cstComment() {
        throw new UnsupportedOperationException("Not supported.");
    }
}
