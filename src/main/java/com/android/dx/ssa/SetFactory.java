package com.android.dx.ssa;

import com.android.dx.util.BitIntSet;
import com.android.dx.util.IntSet;
import com.android.dx.util.ListIntSet;

public final class SetFactory {
    private static final int DOMFRONT_SET_THRESHOLD_SIZE = 3072;
    private static final int INTERFERENCE_SET_THRESHOLD_SIZE = 3072;
    private static final int LIVENESS_SET_THRESHOLD_SIZE = 3072;

    static IntSet makeDomFrontSet(int szBlocks) {
        if (szBlocks <= 3072) {
            return new BitIntSet(szBlocks);
        }
        return new ListIntSet();
    }

    public static IntSet makeInterferenceSet(int countRegs) {
        if (countRegs <= 3072) {
            return new BitIntSet(countRegs);
        }
        return new ListIntSet();
    }

    static IntSet makeLivenessSet(int countRegs) {
        if (countRegs <= 3072) {
            return new BitIntSet(countRegs);
        }
        return new ListIntSet();
    }
}
