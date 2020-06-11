package com.android.dx.rop.code;

import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.type.Type;

public final class DexTranslationAdvice implements TranslationAdvice {
    private static final int MIN_INVOKE_IN_ORDER = 6;
    public static final DexTranslationAdvice NO_SOURCES_IN_ORDER = new DexTranslationAdvice(true);
    public static final DexTranslationAdvice THE_ONE = new DexTranslationAdvice();
    private final boolean disableSourcesInOrder;

    private DexTranslationAdvice() {
        this.disableSourcesInOrder = false;
    }

    private DexTranslationAdvice(boolean disableInvokeRange) {
        this.disableSourcesInOrder = disableInvokeRange;
    }

    public boolean hasConstantOperation(Rop opcode, RegisterSpec sourceA, RegisterSpec sourceB) {
        if (sourceA.getType() != Type.INT) {
            return false;
        }
        if (sourceB.getTypeBearer() instanceof CstInteger) {
            CstInteger cst = (CstInteger) sourceB.getTypeBearer();
            switch (opcode.getOpcode()) {
                case 14:
                case 16:
                case 17:
                case 18:
                case 20:
                case 21:
                case 22:
                    return cst.fitsIn16Bits();
                case 15:
                    return CstInteger.make(-cst.getValue()).fitsIn16Bits();
                case 23:
                case 24:
                case 25:
                    return cst.fitsIn8Bits();
                default:
                    return false;
            }
        } else if ((sourceA.getTypeBearer() instanceof CstInteger) && opcode.getOpcode() == 15) {
            return ((CstInteger) sourceA.getTypeBearer()).fitsIn16Bits();
        } else {
            return false;
        }
    }

    public boolean requiresSourcesInOrder(Rop opcode, RegisterSpecList sources) {
        return !this.disableSourcesInOrder && opcode.isCallLike() && totalRopWidth(sources) >= 6;
    }

    private int totalRopWidth(RegisterSpecList sources) {
        int total = 0;
        for (int i = 0; i < sources.size(); i++) {
            total += sources.get(i).getCategory();
        }
        return total;
    }

    public int getMaxOptimalRegisterCount() {
        return 16;
    }
}
