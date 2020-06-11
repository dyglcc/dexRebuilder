package com.android.dx.rop.code;

import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.FixedSizeList;
import java.util.BitSet;

public final class RegisterSpecList extends FixedSizeList implements TypeList {
    public static final RegisterSpecList EMPTY = new RegisterSpecList(0);

    private static class Expander {
        private int base;
        private final BitSet compatRegs;
        private boolean duplicateFirst;
        private final RegisterSpecList regSpecList;
        private final RegisterSpecList result;

        private Expander(RegisterSpecList regSpecList, BitSet compatRegs, int base, boolean duplicateFirst) {
            this.regSpecList = regSpecList;
            this.compatRegs = compatRegs;
            this.base = base;
            this.result = new RegisterSpecList(regSpecList.size());
            this.duplicateFirst = duplicateFirst;
        }

        private void expandRegister(int regIdx) {
            expandRegister(regIdx, (RegisterSpec) this.regSpecList.get0(regIdx));
        }

        private void expandRegister(int regIdx, RegisterSpec registerToExpand) {
            RegisterSpec expandedReg;
            boolean replace = true;
            if (this.compatRegs != null && this.compatRegs.get(regIdx)) {
                replace = false;
            }
            if (replace) {
                expandedReg = registerToExpand.withReg(this.base);
                if (!this.duplicateFirst) {
                    this.base += expandedReg.getCategory();
                }
            } else {
                expandedReg = registerToExpand;
            }
            this.duplicateFirst = false;
            this.result.set0(regIdx, expandedReg);
        }

        private RegisterSpecList getResult() {
            if (this.regSpecList.isImmutable()) {
                this.result.setImmutable();
            }
            return this.result;
        }
    }

    public static RegisterSpecList make(RegisterSpec spec) {
        RegisterSpecList result = new RegisterSpecList(1);
        result.set(0, spec);
        return result;
    }

    public static RegisterSpecList make(RegisterSpec spec0, RegisterSpec spec1) {
        RegisterSpecList result = new RegisterSpecList(2);
        result.set(0, spec0);
        result.set(1, spec1);
        return result;
    }

    public static RegisterSpecList make(RegisterSpec spec0, RegisterSpec spec1, RegisterSpec spec2) {
        RegisterSpecList result = new RegisterSpecList(3);
        result.set(0, spec0);
        result.set(1, spec1);
        result.set(2, spec2);
        return result;
    }

    public static RegisterSpecList make(RegisterSpec spec0, RegisterSpec spec1, RegisterSpec spec2, RegisterSpec spec3) {
        RegisterSpecList result = new RegisterSpecList(4);
        result.set(0, spec0);
        result.set(1, spec1);
        result.set(2, spec2);
        result.set(3, spec3);
        return result;
    }

    public RegisterSpecList(int size) {
        super(size);
    }

    public Type getType(int n) {
        return get(n).getType().getType();
    }

    public int getWordCount() {
        int result = 0;
        for (int i = 0; i < size(); i++) {
            result += getType(i).getCategory();
        }
        return result;
    }

    public TypeList withAddedType(Type type) {
        throw new UnsupportedOperationException("unsupported");
    }

    public RegisterSpec get(int n) {
        return (RegisterSpec) get0(n);
    }

    public RegisterSpec specForRegister(int reg) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            RegisterSpec rs = get(i);
            if (rs.getReg() == reg) {
                return rs;
            }
        }
        return null;
    }

    public int indexOfRegister(int reg) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            if (get(i).getReg() == reg) {
                return i;
            }
        }
        return -1;
    }

    public void set(int n, RegisterSpec spec) {
        set0(n, spec);
    }

    public int getRegistersSize() {
        int sz = size();
        int result = 0;
        for (int i = 0; i < sz; i++) {
            RegisterSpec spec = (RegisterSpec) get0(i);
            if (spec != null) {
                int min = spec.getNextReg();
                if (min > result) {
                    result = min;
                }
            }
        }
        return result;
    }

    public RegisterSpecList withFirst(RegisterSpec spec) {
        int sz = size();
        RegisterSpecList result = new RegisterSpecList(sz + 1);
        for (int i = 0; i < sz; i++) {
            result.set0(i + 1, get0(i));
        }
        result.set0(0, spec);
        if (isImmutable()) {
            result.setImmutable();
        }
        return result;
    }

    public RegisterSpecList withoutFirst() {
        int newSize = size() - 1;
        if (newSize == 0) {
            return EMPTY;
        }
        RegisterSpecList result = new RegisterSpecList(newSize);
        for (int i = 0; i < newSize; i++) {
            result.set0(i, get0(i + 1));
        }
        if (!isImmutable()) {
            return result;
        }
        result.setImmutable();
        return result;
    }

    public RegisterSpecList withoutLast() {
        int newSize = size() - 1;
        if (newSize == 0) {
            return EMPTY;
        }
        RegisterSpecList result = new RegisterSpecList(newSize);
        for (int i = 0; i < newSize; i++) {
            result.set0(i, get0(i));
        }
        if (!isImmutable()) {
            return result;
        }
        result.setImmutable();
        return result;
    }

    public RegisterSpecList subset(BitSet exclusionSet) {
        int newSize = size() - exclusionSet.cardinality();
        if (newSize == 0) {
            return EMPTY;
        }
        RegisterSpecList result = new RegisterSpecList(newSize);
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < size(); oldIndex++) {
            if (!exclusionSet.get(oldIndex)) {
                result.set0(newIndex, get0(oldIndex));
                newIndex++;
            }
        }
        if (!isImmutable()) {
            return result;
        }
        result.setImmutable();
        return result;
    }

    public RegisterSpecList withOffset(int delta) {
        int sz = size();
        if (sz == 0) {
            return this;
        }
        RegisterSpecList result = new RegisterSpecList(sz);
        for (int i = 0; i < sz; i++) {
            RegisterSpec one = (RegisterSpec) get0(i);
            if (one != null) {
                result.set0(i, one.withOffset(delta));
            }
        }
        if (isImmutable()) {
            result.setImmutable();
        }
        return result;
    }

    public RegisterSpecList withExpandedRegisters(int base, boolean duplicateFirst, BitSet compatRegs) {
        int sz = size();
        if (sz == 0) {
            return this;
        }
        Expander expander = new Expander(compatRegs, base, duplicateFirst);
        for (int regIdx = 0; regIdx < sz; regIdx++) {
            expander.expandRegister(regIdx);
        }
        return expander.getResult();
    }
}
