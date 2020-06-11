package com.android.dx.ssa;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.ssa.back.InterferenceGraph;
import com.android.dx.util.BitIntSet;
import com.android.dx.util.IntSet;
import java.util.ArrayList;

public class InterferenceRegisterMapper extends BasicRegisterMapper {
    private final ArrayList<BitIntSet> newRegInterference = new ArrayList();
    private final InterferenceGraph oldRegInterference;

    public InterferenceRegisterMapper(InterferenceGraph oldRegInterference, int countOldRegisters) {
        super(countOldRegisters);
        this.oldRegInterference = oldRegInterference;
    }

    public void addMapping(int oldReg, int newReg, int category) {
        super.addMapping(oldReg, newReg, category);
        addInterfence(newReg, oldReg);
        if (category == 2) {
            addInterfence(newReg + 1, oldReg);
        }
    }

    public boolean interferes(int oldReg, int newReg, int category) {
        if (newReg >= this.newRegInterference.size()) {
            return false;
        }
        IntSet existing = (IntSet) this.newRegInterference.get(newReg);
        if (existing == null) {
            return false;
        }
        if (category == 1) {
            return existing.has(oldReg);
        }
        if (existing.has(oldReg) || interferes(oldReg, newReg + 1, category - 1)) {
            return true;
        }
        return false;
    }

    public boolean interferes(RegisterSpec oldSpec, int newReg) {
        return interferes(oldSpec.getReg(), newReg, oldSpec.getCategory());
    }

    private void addInterfence(int newReg, int oldReg) {
        this.newRegInterference.ensureCapacity(newReg + 1);
        while (newReg >= this.newRegInterference.size()) {
            this.newRegInterference.add(new BitIntSet(newReg + 1));
        }
        this.oldRegInterference.mergeInterferenceSet(oldReg, (IntSet) this.newRegInterference.get(newReg));
    }

    public boolean areAnyPinned(RegisterSpecList oldSpecs, int newReg, int targetCategory) {
        int sz = oldSpecs.size();
        for (int i = 0; i < sz; i++) {
            RegisterSpec oldSpec = oldSpecs.get(i);
            int r = oldToNew(oldSpec.getReg());
            if (r == newReg || ((oldSpec.getCategory() == 2 && r + 1 == newReg) || (targetCategory == 2 && r == newReg + 1))) {
                return true;
            }
        }
        return false;
    }
}
