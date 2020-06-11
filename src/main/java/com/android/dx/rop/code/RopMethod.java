package com.android.dx.rop.code;

import com.android.dx.util.Hex;
import com.android.dx.util.IntList;

public final class RopMethod {
    private final BasicBlockList blocks;
    private IntList exitPredecessors;
    private final int firstLabel;
    private IntList[] predecessors;

    public RopMethod(BasicBlockList blocks, int firstLabel) {
        if (blocks == null) {
            throw new NullPointerException("blocks == null");
        } else if (firstLabel < 0) {
            throw new IllegalArgumentException("firstLabel < 0");
        } else {
            this.blocks = blocks;
            this.firstLabel = firstLabel;
            this.predecessors = null;
            this.exitPredecessors = null;
        }
    }

    public BasicBlockList getBlocks() {
        return this.blocks;
    }

    public int getFirstLabel() {
        return this.firstLabel;
    }

    public IntList labelToPredecessors(int label) {
        if (this.exitPredecessors == null) {
            calcPredecessors();
        }
        IntList result = this.predecessors[label];
        if (result != null) {
            return result;
        }
        throw new RuntimeException("no such block: " + Hex.u2(label));
    }

    public IntList getExitPredecessors() {
        if (this.exitPredecessors == null) {
            calcPredecessors();
        }
        return this.exitPredecessors;
    }

    public RopMethod withRegisterOffset(int delta) {
        RopMethod result = new RopMethod(this.blocks.withRegisterOffset(delta), this.firstLabel);
        if (this.exitPredecessors != null) {
            result.exitPredecessors = this.exitPredecessors;
            result.predecessors = this.predecessors;
        }
        return result;
    }

    private void calcPredecessors() {
        int i;
        int maxLabel = this.blocks.getMaxLabel();
        IntList[] predecessors = new IntList[maxLabel];
        IntList exitPredecessors = new IntList(10);
        int sz = this.blocks.size();
        for (i = 0; i < sz; i++) {
            BasicBlock one = this.blocks.get(i);
            int label = one.getLabel();
            IntList successors = one.getSuccessors();
            int ssz = successors.size();
            if (ssz == 0) {
                exitPredecessors.add(label);
            } else {
                for (int j = 0; j < ssz; j++) {
                    int succLabel = successors.get(j);
                    IntList succPreds = predecessors[succLabel];
                    if (succPreds == null) {
                        succPreds = new IntList(10);
                        predecessors[succLabel] = succPreds;
                    }
                    succPreds.add(label);
                }
            }
        }
        for (i = 0; i < maxLabel; i++) {
            IntList preds = predecessors[i];
            if (preds != null) {
                preds.sort();
                preds.setImmutable();
            }
        }
        exitPredecessors.sort();
        exitPredecessors.setImmutable();
        if (predecessors[this.firstLabel] == null) {
            predecessors[this.firstLabel] = IntList.EMPTY;
        }
        this.predecessors = predecessors;
        this.exitPredecessors = exitPredecessors;
    }
}
