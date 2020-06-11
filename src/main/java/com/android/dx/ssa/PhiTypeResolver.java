package com.android.dx.ssa;

import com.android.dx.cf.code.Merger;
import com.android.dx.rop.code.LocalItem;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.type.TypeBearer;
import java.util.BitSet;
import java.util.List;

public class PhiTypeResolver {
    SsaMethod ssaMeth;
    private final BitSet worklist;

    public static void process(SsaMethod ssaMeth) {
        new PhiTypeResolver(ssaMeth).run();
    }

    private PhiTypeResolver(SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
        this.worklist = new BitSet(ssaMeth.getRegCount());
    }

    private void run() {
        int reg;
        int regCount = this.ssaMeth.getRegCount();
        for (reg = 0; reg < regCount; reg++) {
            SsaInsn definsn = this.ssaMeth.getDefinitionForRegister(reg);
            if (definsn != null && definsn.getResult().getBasicType() == 0) {
                this.worklist.set(reg);
            }
        }
        while (true) {
            reg = this.worklist.nextSetBit(0);
            if (reg >= 0) {
                this.worklist.clear(reg);
                if (resolveResultType((PhiInsn) this.ssaMeth.getDefinitionForRegister(reg))) {
                    List<SsaInsn> useList = this.ssaMeth.getUseListForRegister(reg);
                    int sz = useList.size();
                    for (int i = 0; i < sz; i++) {
                        SsaInsn useInsn = (SsaInsn) useList.get(i);
                        RegisterSpec resultReg = useInsn.getResult();
                        if (resultReg != null && (useInsn instanceof PhiInsn)) {
                            this.worklist.set(resultReg.getReg());
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    private static boolean equalsHandlesNulls(LocalItem a, LocalItem b) {
        return a == b || (a != null && a.equals(b));
    }

    boolean resolveResultType(PhiInsn insn) {
        int i;
        insn.updateSourcesToDefinitions(this.ssaMeth);
        RegisterSpecList sources = insn.getSources();
        RegisterSpec first = null;
        int firstIndex = -1;
        int szSources = sources.size();
        for (i = 0; i < szSources; i++) {
            RegisterSpec rs = sources.get(i);
            if (rs.getBasicType() != 0) {
                first = rs;
                firstIndex = i;
            }
        }
        if (first == null) {
            return false;
        }
        LocalItem firstLocal = first.getLocalItem();
        TypeBearer mergedType = first.getType();
        boolean sameLocals = true;
        for (i = 0; i < szSources; i++) {
            if (i != firstIndex) {
                rs = sources.get(i);
                if (rs.getBasicType() != 0) {
                    sameLocals = sameLocals && equalsHandlesNulls(firstLocal, rs.getLocalItem());
                    mergedType = Merger.mergeType(mergedType, rs.getType());
                }
            }
        }
        if (mergedType != null) {
            TypeBearer newResultType = mergedType;
            LocalItem newLocal = sameLocals ? firstLocal : null;
            RegisterSpec result = insn.getResult();
            if (result.getTypeBearer() == newResultType && equalsHandlesNulls(newLocal, result.getLocalItem())) {
                return false;
            }
            insn.changeResultType(newResultType, newLocal);
            return true;
        }
        StringBuilder sb = new StringBuilder();
        for (i = 0; i < szSources; i++) {
            sb.append(sources.get(i).toString());
            sb.append(' ');
        }
        throw new RuntimeException("Couldn't map types in phi insn:" + sb);
    }
}
