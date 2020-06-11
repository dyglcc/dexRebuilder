package com.android.dx.dex.code;

import com.android.dx.rop.code.BasicBlock;
import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.SourcePosition;

public final class BlockAddresses {
    private final CodeAddress[] ends;
    private final CodeAddress[] lasts;
    private final CodeAddress[] starts;

    public BlockAddresses(RopMethod method) {
        int maxLabel = method.getBlocks().getMaxLabel();
        this.starts = new CodeAddress[maxLabel];
        this.lasts = new CodeAddress[maxLabel];
        this.ends = new CodeAddress[maxLabel];
        setupArrays(method);
    }

    public CodeAddress getStart(BasicBlock block) {
        return this.starts[block.getLabel()];
    }

    public CodeAddress getStart(int label) {
        return this.starts[label];
    }

    public CodeAddress getLast(BasicBlock block) {
        return this.lasts[block.getLabel()];
    }

    public CodeAddress getLast(int label) {
        return this.lasts[label];
    }

    public CodeAddress getEnd(BasicBlock block) {
        return this.ends[block.getLabel()];
    }

    public CodeAddress getEnd(int label) {
        return this.ends[label];
    }

    private void setupArrays(RopMethod method) {
        BasicBlockList blocks = method.getBlocks();
        int sz = blocks.size();
        for (int i = 0; i < sz; i++) {
            BasicBlock one = blocks.get(i);
            int label = one.getLabel();
            this.starts[label] = new CodeAddress(one.getInsns().get(0).getPosition());
            SourcePosition pos = one.getLastInsn().getPosition();
            this.lasts[label] = new CodeAddress(pos);
            this.ends[label] = new CodeAddress(pos);
        }
    }
}
