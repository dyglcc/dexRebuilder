package com.android.dx.ssa;

import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.ssa.DomFront.DomInfo;
import com.android.dx.util.IntIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

public class SsaConverter {
    public static final boolean DEBUG = false;

    public static SsaMethod convertToSsaMethod(RopMethod rmeth, int paramWidth, boolean isStatic) {
        SsaMethod result = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);
        edgeSplit(result);
        placePhiFunctions(result, LocalVariableExtractor.extract(result), 0);
        new SsaRenamer(result).run();
        result.makeExitBlock();
        return result;
    }

    public static void updateSsaMethod(SsaMethod ssaMeth, int threshold) {
        placePhiFunctions(ssaMeth, LocalVariableExtractor.extract(ssaMeth), threshold);
        new SsaRenamer(ssaMeth, threshold).run();
    }

    public static SsaMethod testEdgeSplit(RopMethod rmeth, int paramWidth, boolean isStatic) {
        SsaMethod result = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);
        edgeSplit(result);
        return result;
    }

    public static SsaMethod testPhiPlacement(RopMethod rmeth, int paramWidth, boolean isStatic) {
        SsaMethod result = SsaMethod.newFromRopMethod(rmeth, paramWidth, isStatic);
        edgeSplit(result);
        placePhiFunctions(result, LocalVariableExtractor.extract(result), 0);
        return result;
    }

    private static void edgeSplit(SsaMethod result) {
        edgeSplitPredecessors(result);
        edgeSplitMoveExceptionsAndResults(result);
        edgeSplitSuccessors(result);
    }

    private static void edgeSplitPredecessors(SsaMethod result) {
        ArrayList<SsaBasicBlock> blocks = result.getBlocks();
        for (int i = blocks.size() - 1; i >= 0; i--) {
            SsaBasicBlock block = (SsaBasicBlock) blocks.get(i);
            if (nodeNeedsUniquePredecessor(block)) {
                block.insertNewPredecessor();
            }
        }
    }

    private static boolean nodeNeedsUniquePredecessor(SsaBasicBlock block) {
        int countPredecessors = block.getPredecessors().cardinality();
        int countSuccessors = block.getSuccessors().cardinality();
        if (countPredecessors <= 1 || countSuccessors <= 1) {
            return false;
        }
        return true;
    }

    private static void edgeSplitMoveExceptionsAndResults(SsaMethod ssaMeth) {
        ArrayList<SsaBasicBlock> blocks = ssaMeth.getBlocks();
        for (int i = blocks.size() - 1; i >= 0; i--) {
            SsaBasicBlock block = (SsaBasicBlock) blocks.get(i);
            if (!block.isExitBlock() && block.getPredecessors().cardinality() > 1 && ((SsaInsn) block.getInsns().get(0)).isMoveException()) {
                BitSet preds = (BitSet) block.getPredecessors().clone();
                for (int j = preds.nextSetBit(0); j >= 0; j = preds.nextSetBit(j + 1)) {
                    ((SsaBasicBlock) blocks.get(j)).insertNewSuccessor(block).getInsns().add(0, ((SsaInsn) block.getInsns().get(0)).clone());
                }
                block.getInsns().remove(0);
            }
        }
    }

    private static void edgeSplitSuccessors(SsaMethod result) {
        ArrayList<SsaBasicBlock> blocks = result.getBlocks();
        for (int i = blocks.size() - 1; i >= 0; i--) {
            SsaBasicBlock block = (SsaBasicBlock) blocks.get(i);
            BitSet successors = (BitSet) block.getSuccessors().clone();
            for (int j = successors.nextSetBit(0); j >= 0; j = successors.nextSetBit(j + 1)) {
                SsaBasicBlock succ = (SsaBasicBlock) blocks.get(j);
                if (needsNewSuccessor(block, succ)) {
                    block.insertNewSuccessor(succ);
                }
            }
        }
    }

    private static boolean needsNewSuccessor(SsaBasicBlock block, SsaBasicBlock succ) {
        ArrayList<SsaInsn> insns = block.getInsns();
        SsaInsn lastInsn = (SsaInsn) insns.get(insns.size() - 1);
        if (block.getSuccessors().cardinality() > 1 && succ.getPredecessors().cardinality() > 1) {
            return true;
        }
        if ((lastInsn.getResult() != null || lastInsn.getSources().size() > 0) && succ.getPredecessors().cardinality() > 1) {
            return true;
        }
        return false;
    }

    private static void placePhiFunctions(SsaMethod ssaMeth, LocalVariableInfo localInfo, int threshold) {
        ArrayList<SsaBasicBlock> ssaBlocks = ssaMeth.getBlocks();
        int blockCount = ssaBlocks.size();
        int regCount = ssaMeth.getRegCount() - threshold;
        DomInfo[] domInfos = new DomFront(ssaMeth).run();
        BitSet[] defsites = new BitSet[regCount];
        BitSet[] phisites = new BitSet[regCount];
        for (int i = 0; i < regCount; i++) {
            defsites[i] = new BitSet(blockCount);
            phisites[i] = new BitSet(blockCount);
        }
        int s = ssaBlocks.size();
        for (int bi = 0; bi < s; bi++) {
            Iterator it = ((SsaBasicBlock) ssaBlocks.get(bi)).getInsns().iterator();
            while (it.hasNext()) {
                RegisterSpec rs = ((SsaInsn) it.next()).getResult();
                if (rs != null && rs.getReg() - threshold >= 0) {
                    defsites[rs.getReg() - threshold].set(bi);
                }
            }
        }
        s = regCount;
        for (int reg = 0; reg < s; reg++) {
            BitSet worklist = (BitSet) defsites[reg].clone();
            while (true) {
                int workBlockIndex = worklist.nextSetBit(0);
                if (workBlockIndex < 0) {
                    break;
                }
                worklist.clear(workBlockIndex);
                IntIterator dfIterator = domInfos[workBlockIndex].dominanceFrontiers.iterator();
                while (dfIterator.hasNext()) {
                    int dfBlockIndex = dfIterator.next();
                    if (!phisites[reg].get(dfBlockIndex)) {
                        phisites[reg].set(dfBlockIndex);
                        int tReg = reg + threshold;
                        rs = localInfo.getStarts(dfBlockIndex).get(tReg);
                        if (rs == null) {
                            ((SsaBasicBlock) ssaBlocks.get(dfBlockIndex)).addPhiInsnForReg(tReg);
                        } else {
                            ((SsaBasicBlock) ssaBlocks.get(dfBlockIndex)).addPhiInsnForReg(rs);
                        }
                        if (!defsites[reg].get(dfBlockIndex)) {
                            worklist.set(dfBlockIndex);
                        }
                    }
                }
            }
        }
    }
}
