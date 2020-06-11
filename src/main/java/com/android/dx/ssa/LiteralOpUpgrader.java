package com.android.dx.ssa;

import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.PlainCstInsn;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegOps;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstLiteralBits;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.ssa.SsaInsn.Visitor;
import java.util.ArrayList;
import java.util.List;

public class LiteralOpUpgrader {
    private final SsaMethod ssaMeth;

    public static void process(SsaMethod ssaMethod) {
        new LiteralOpUpgrader(ssaMethod).run();
    }

    private LiteralOpUpgrader(SsaMethod ssaMethod) {
        this.ssaMeth = ssaMethod;
    }

    private static boolean isConstIntZeroOrKnownNull(RegisterSpec spec) {
        TypeBearer tb = spec.getTypeBearer();
        if ((tb instanceof CstLiteralBits) && ((CstLiteralBits) tb).getLongBits() == 0) {
            return true;
        }
        return false;
    }

    private void run() {
        final TranslationAdvice advice = Optimizer.getAdvice();
        this.ssaMeth.forEachInsn(new Visitor() {
            public void visitMoveInsn(NormalSsaInsn insn) {
            }

            public void visitPhiInsn(PhiInsn insn) {
            }

            public void visitNonMoveInsn(NormalSsaInsn insn) {
                Rop opcode = insn.getOriginalRopInsn().getOpcode();
                RegisterSpecList sources = insn.getSources();
                if (LiteralOpUpgrader.this.tryReplacingWithConstant(insn) || sources.size() != 2) {
                    return;
                }
                if (opcode.getBranchingness() == 4) {
                    if (LiteralOpUpgrader.isConstIntZeroOrKnownNull(sources.get(0))) {
                        LiteralOpUpgrader.this.replacePlainInsn(insn, sources.withoutFirst(), RegOps.flippedIfOpcode(opcode.getOpcode()), null);
                    } else if (LiteralOpUpgrader.isConstIntZeroOrKnownNull(sources.get(1))) {
                        LiteralOpUpgrader.this.replacePlainInsn(insn, sources.withoutLast(), opcode.getOpcode(), null);
                    }
                } else if (advice.hasConstantOperation(opcode, sources.get(0), sources.get(1))) {
                    insn.upgradeToLiteral();
                } else if (opcode.isCommutative() && advice.hasConstantOperation(opcode, sources.get(1), sources.get(0))) {
                    insn.setNewSources(RegisterSpecList.make(sources.get(1), sources.get(0)));
                    insn.upgradeToLiteral();
                }
            }
        });
    }

    private boolean tryReplacingWithConstant(NormalSsaInsn insn) {
        Rop opcode = insn.getOriginalRopInsn().getOpcode();
        RegisterSpec result = insn.getResult();
        if (result == null || this.ssaMeth.isRegALocal(result) || opcode.getOpcode() == 5) {
            return false;
        }
        TypeBearer type = insn.getResult().getTypeBearer();
        if (!type.isConstant() || type.getBasicType() != 6) {
            return false;
        }
        replacePlainInsn(insn, RegisterSpecList.EMPTY, 5, (Constant) type);
        if (opcode.getOpcode() == 56) {
            ArrayList<SsaInsn> predInsns = ((SsaBasicBlock) this.ssaMeth.getBlocks().get(insn.getBlock().getPredecessors().nextSetBit(0))).getInsns();
            replacePlainInsn((NormalSsaInsn) predInsns.get(predInsns.size() - 1), RegisterSpecList.EMPTY, 6, null);
        }
        return true;
    }

    private void replacePlainInsn(NormalSsaInsn insn, RegisterSpecList newSources, int newOpcode, Constant cst) {
        Insn newRopInsn;
        Insn originalRopInsn = insn.getOriginalRopInsn();
        Rop newRop = Rops.ropFor(newOpcode, insn.getResult(), newSources, cst);
        if (cst == null) {
            newRopInsn = new PlainInsn(newRop, originalRopInsn.getPosition(), insn.getResult(), newSources);
        } else {
            newRopInsn = new PlainCstInsn(newRop, originalRopInsn.getPosition(), insn.getResult(), newSources, cst);
        }
        NormalSsaInsn newInsn = new NormalSsaInsn(newRopInsn, insn.getBlock());
        List<SsaInsn> insns = insn.getBlock().getInsns();
        this.ssaMeth.onInsnRemoved(insn);
        insns.set(insns.lastIndexOf(insn), newInsn);
        this.ssaMeth.onInsnAdded(newInsn);
    }
}
