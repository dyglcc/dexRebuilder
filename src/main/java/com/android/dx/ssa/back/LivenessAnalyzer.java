package com.android.dx.ssa.back;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.ssa.PhiInsn;
import com.android.dx.ssa.SsaBasicBlock;
import com.android.dx.ssa.SsaInsn;
import com.android.dx.ssa.SsaMethod;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class LivenessAnalyzer {
    private SsaBasicBlock blockN;
    private final InterferenceGraph interference;
    private final BitSet liveOutBlocks;
    private NextFunction nextFunction;
    private final int regV;
    private final SsaMethod ssaMeth;
    private int statementIndex;
    private final BitSet visitedBlocks;

    private enum NextFunction {
        LIVE_IN_AT_STATEMENT,
        LIVE_OUT_AT_STATEMENT,
        LIVE_OUT_AT_BLOCK,
        DONE
    }

    public static InterferenceGraph constructInterferenceGraph(SsaMethod ssaMeth) {
        int szRegs = ssaMeth.getRegCount();
        InterferenceGraph interference = new InterferenceGraph(szRegs);
        for (int i = 0; i < szRegs; i++) {
            new LivenessAnalyzer(ssaMeth, i, interference).run();
        }
        coInterferePhis(ssaMeth, interference);
        return interference;
    }

    private LivenessAnalyzer(SsaMethod ssaMeth, int reg, InterferenceGraph interference) {
        int blocksSz = ssaMeth.getBlocks().size();
        this.ssaMeth = ssaMeth;
        this.regV = reg;
        this.visitedBlocks = new BitSet(blocksSz);
        this.liveOutBlocks = new BitSet(blocksSz);
        this.interference = interference;
    }

    private void handleTailRecursion() {
        while (this.nextFunction != NextFunction.DONE) {
            switch (this.nextFunction) {
                case LIVE_IN_AT_STATEMENT:
                    this.nextFunction = NextFunction.DONE;
                    liveInAtStatement();
                    break;
                case LIVE_OUT_AT_STATEMENT:
                    this.nextFunction = NextFunction.DONE;
                    liveOutAtStatement();
                    break;
                case LIVE_OUT_AT_BLOCK:
                    this.nextFunction = NextFunction.DONE;
                    liveOutAtBlock();
                    break;
                default:
                    break;
            }
        }
    }

    public void run() {
        for (SsaInsn insn : this.ssaMeth.getUseListForRegister(this.regV)) {
            this.nextFunction = NextFunction.DONE;
            if (insn instanceof PhiInsn) {
                for (SsaBasicBlock pred : ((PhiInsn) insn).predBlocksForReg(this.regV, this.ssaMeth)) {
                    this.blockN = pred;
                    this.nextFunction = NextFunction.LIVE_OUT_AT_BLOCK;
                    handleTailRecursion();
                }
            } else {
                this.blockN = insn.getBlock();
                this.statementIndex = this.blockN.getInsns().indexOf(insn);
                if (this.statementIndex < 0) {
                    throw new RuntimeException("insn not found in it's own block");
                }
                this.nextFunction = NextFunction.LIVE_IN_AT_STATEMENT;
                handleTailRecursion();
            }
        }
        while (true) {
            int nextLiveOutBlock = this.liveOutBlocks.nextSetBit(0);
            if (nextLiveOutBlock >= 0) {
                this.blockN = (SsaBasicBlock) this.ssaMeth.getBlocks().get(nextLiveOutBlock);
                this.liveOutBlocks.clear(nextLiveOutBlock);
                this.nextFunction = NextFunction.LIVE_OUT_AT_BLOCK;
                handleTailRecursion();
            } else {
                return;
            }
        }
    }

    private void liveOutAtBlock() {
        if (!this.visitedBlocks.get(this.blockN.getIndex())) {
            this.visitedBlocks.set(this.blockN.getIndex());
            this.blockN.addLiveOut(this.regV);
            this.statementIndex = this.blockN.getInsns().size() - 1;
            this.nextFunction = NextFunction.LIVE_OUT_AT_STATEMENT;
        }
    }

    private void liveInAtStatement() {
        if (this.statementIndex == 0) {
            this.blockN.addLiveIn(this.regV);
            this.liveOutBlocks.or(this.blockN.getPredecessors());
            return;
        }
        this.statementIndex--;
        this.nextFunction = NextFunction.LIVE_OUT_AT_STATEMENT;
    }

    private void liveOutAtStatement() {
        SsaInsn statement = (SsaInsn) this.blockN.getInsns().get(this.statementIndex);
        RegisterSpec rs = statement.getResult();
        if (!statement.isResultReg(this.regV)) {
            if (rs != null) {
                this.interference.add(this.regV, rs.getReg());
            }
            this.nextFunction = NextFunction.LIVE_IN_AT_STATEMENT;
        }
    }

    private static void coInterferePhis(SsaMethod ssaMeth, InterferenceGraph interference) {
        Iterator it = ssaMeth.getBlocks().iterator();
        while (it.hasNext()) {
            List<SsaInsn> phis = ((SsaBasicBlock) it.next()).getPhiInsns();
            int szPhis = phis.size();
            for (int i = 0; i < szPhis; i++) {
                for (int j = 0; j < szPhis; j++) {
                    if (i != j) {
                        SsaInsn first = (SsaInsn) phis.get(i);
                        SsaInsn second = (SsaInsn) phis.get(j);
                        coInterferePhiRegisters(interference, first.getResult(), second.getSources());
                        coInterferePhiRegisters(interference, second.getResult(), first.getSources());
                        interference.add(first.getResult().getReg(), second.getResult().getReg());
                    }
                }
            }
        }
    }

    private static void coInterferePhiRegisters(InterferenceGraph interference, RegisterSpec result, RegisterSpecList sources) {
        int resultReg = result.getReg();
        for (int i = 0; i < sources.size(); i++) {
            interference.add(resultReg, sources.get(i).getReg());
        }
    }
}
