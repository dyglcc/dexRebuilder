package com.android.dx.dex.code;

import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;
import com.android.dx.util.IntList;

public final class SwitchData extends VariableSizeInsn {
    private final IntList cases;
    private final boolean packed;
    private final CodeAddress[] targets;
    private final CodeAddress user;

    public SwitchData(SourcePosition position, CodeAddress user, IntList cases, CodeAddress[] targets) {
        super(position, RegisterSpecList.EMPTY);
        if (user == null) {
            throw new NullPointerException("user == null");
        } else if (cases == null) {
            throw new NullPointerException("cases == null");
        } else if (targets == null) {
            throw new NullPointerException("targets == null");
        } else {
            int sz = cases.size();
            if (sz != targets.length) {
                throw new IllegalArgumentException("cases / targets mismatch");
            } else if (sz > 65535) {
                throw new IllegalArgumentException("too many cases");
            } else {
                this.user = user;
                this.cases = cases;
                this.targets = targets;
                this.packed = shouldPack(cases);
            }
        }
    }

    public int codeSize() {
        if (this.packed) {
            return (int) packedCodeSize(this.cases);
        }
        return (int) sparseCodeSize(this.cases);
    }

    public void writeTo(AnnotatedOutput out) {
        int lastCase = 0;
        int baseAddress = this.user.getAddress();
        int defaultTarget = Dops.PACKED_SWITCH.getFormat().codeSize();
        int i;
        if (this.packed) {
            int firstCase = sz == 0 ? 0 : this.cases.get(0);
            if (sz != 0) {
                lastCase = this.cases.get(sz - 1);
            }
            int outSz = (lastCase - firstCase) + 1;
            out.writeShort(256);
            out.writeShort(outSz);
            out.writeInt(firstCase);
            int caseAt = 0;
            for (i = 0; i < outSz; i++) {
                int relTarget;
                if (this.cases.get(caseAt) > firstCase + i) {
                    relTarget = defaultTarget;
                } else {
                    relTarget = this.targets[caseAt].getAddress() - baseAddress;
                    caseAt++;
                }
                out.writeInt(relTarget);
            }
            return;
        }
        out.writeShort(512);
        out.writeShort(sz);
        for (i = 0; i < sz; i++) {
            out.writeInt(this.cases.get(i));
        }
        for (CodeAddress address : this.targets) {
            out.writeInt(address.getAddress() - baseAddress);
        }
    }

    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new SwitchData(getPosition(), this.user, this.cases, this.targets);
    }

    public boolean isPacked() {
        return this.packed;
    }

    protected String argString() {
        StringBuilder sb = new StringBuilder(100);
        int sz = this.targets.length;
        for (int i = 0; i < sz; i++) {
            sb.append("\n    ");
            sb.append(this.cases.get(i));
            sb.append(": ");
            sb.append(this.targets[i]);
        }
        return sb.toString();
    }

    protected String listingString0(boolean noteIndices) {
        int baseAddress = this.user.getAddress();
        StringBuilder sb = new StringBuilder(100);
        int sz = this.targets.length;
        sb.append(this.packed ? "packed" : "sparse");
        sb.append("-switch-payload // for switch @ ");
        sb.append(Hex.u2(baseAddress));
        for (int i = 0; i < sz; i++) {
            int absTarget = this.targets[i].getAddress();
            int relTarget = absTarget - baseAddress;
            sb.append("\n  ");
            sb.append(this.cases.get(i));
            sb.append(": ");
            sb.append(Hex.u4(absTarget));
            sb.append(" // ");
            sb.append(Hex.s4(relTarget));
        }
        return sb.toString();
    }

    private static long packedCodeSize(IntList cases) {
        long result = (((((long) cases.get(cases.size() - 1)) - ((long) cases.get(0))) + 1) * 2) + 4;
        return result <= 2147483647L ? result : -1;
    }

    private static long sparseCodeSize(IntList cases) {
        return (((long) cases.size()) * 4) + 2;
    }

    private static boolean shouldPack(IntList cases) {
        if (cases.size() < 2) {
            return true;
        }
        long packedSize = packedCodeSize(cases);
        long sparseSize = sparseCodeSize(cases);
        if (packedSize < 0 || packedSize > (5 * sparseSize) / 4) {
            return false;
        }
        return true;
    }
}
