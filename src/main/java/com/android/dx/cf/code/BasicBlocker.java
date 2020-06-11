package com.android.dx.cf.code;

import com.android.dx.cf.code.ByteCatchList.Item;
import com.android.dx.cf.code.BytecodeArray.Visitor;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstInvokeDynamic;
import com.android.dx.rop.cst.CstMemberRef;
import com.android.dx.rop.cst.CstMethodHandle;
import com.android.dx.rop.cst.CstProtoRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.util.Bits;
import com.android.dx.util.IntList;
import java.util.ArrayList;

public final class BasicBlocker implements Visitor {
    private final int[] blockSet;
    private final ByteCatchList[] catchLists;
    private final int[] liveSet;
    private final ConcreteMethod method;
    private int previousOffset;
    private final IntList[] targetLists;
    private final int[] workSet;

    public static ByteBlockList identifyBlocks(ConcreteMethod method) {
        BasicBlocker bb = new BasicBlocker(method);
        bb.doit();
        return bb.getBlockList();
    }

    private BasicBlocker(ConcreteMethod method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }
        this.method = method;
        int sz = method.getCode().size() + 1;
        this.workSet = Bits.makeBitSet(sz);
        this.liveSet = Bits.makeBitSet(sz);
        this.blockSet = Bits.makeBitSet(sz);
        this.targetLists = new IntList[sz];
        this.catchLists = new ByteCatchList[sz];
        this.previousOffset = -1;
    }

    public void visitInvalid(int opcode, int offset, int length) {
        visitCommon(offset, length, true);
    }

    public void visitNoArgs(int opcode, int offset, int length, Type type) {
        switch (opcode) {
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 190:
            case 194:
            case 195:
                visitCommon(offset, length, true);
                visitThrowing(offset, length, true);
                return;
            case 108:
            case 112:
                visitCommon(offset, length, true);
                if (type == Type.INT || type == Type.LONG) {
                    visitThrowing(offset, length, true);
                    return;
                }
                return;
            case 172:
            case 177:
                visitCommon(offset, length, false);
                this.targetLists[offset] = IntList.EMPTY;
                return;
            case 191:
                visitCommon(offset, length, false);
                visitThrowing(offset, length, false);
                return;
            default:
                visitCommon(offset, length, true);
                return;
        }
    }

    public void visitLocal(int opcode, int offset, int length, int idx, Type type, int value) {
        if (opcode == 169) {
            visitCommon(offset, length, false);
            this.targetLists[offset] = IntList.EMPTY;
            return;
        }
        visitCommon(offset, length, true);
    }

    public void visitConstant(int opcode, int offset, int length, Constant cst, int value) {
        visitCommon(offset, length, true);
        if ((cst instanceof CstMemberRef) || (cst instanceof CstType) || (cst instanceof CstString) || (cst instanceof CstInvokeDynamic) || (cst instanceof CstMethodHandle) || (cst instanceof CstProtoRef)) {
            visitThrowing(offset, length, true);
        }
    }

    public void visitBranch(int opcode, int offset, int length, int target) {
        switch (opcode) {
            case 167:
                visitCommon(offset, length, false);
                this.targetLists[offset] = IntList.makeImmutable(target);
                break;
            case 168:
                addWorkIfNecessary(offset, true);
                break;
        }
        int next = offset + length;
        visitCommon(offset, length, true);
        addWorkIfNecessary(next, true);
        this.targetLists[offset] = IntList.makeImmutable(next, target);
        addWorkIfNecessary(target, true);
    }

    public void visitSwitch(int opcode, int offset, int length, SwitchList cases, int padding) {
        visitCommon(offset, length, false);
        addWorkIfNecessary(cases.getDefaultTarget(), true);
        int sz = cases.size();
        for (int i = 0; i < sz; i++) {
            addWorkIfNecessary(cases.getTarget(i), true);
        }
        this.targetLists[offset] = cases.getTargets();
    }

    public void visitNewarray(int offset, int length, CstType type, ArrayList<Constant> arrayList) {
        visitCommon(offset, length, true);
        visitThrowing(offset, length, true);
    }

    private ByteBlockList getBlockList() {
        ByteBlock[] bbs = new ByteBlock[this.method.getCode().size()];
        int count = 0;
        int at = 0;
        while (true) {
            int next = Bits.findFirst(this.blockSet, at + 1);
            if (next < 0) {
                break;
            }
            int i;
            if (Bits.get(this.liveSet, at)) {
                ByteCatchList blockCatches;
                IntList targets = null;
                int targetsAt = -1;
                for (i = next - 1; i >= at; i--) {
                    targets = this.targetLists[i];
                    if (targets != null) {
                        targetsAt = i;
                        break;
                    }
                }
                if (targets == null) {
                    targets = IntList.makeImmutable(next);
                    blockCatches = ByteCatchList.EMPTY;
                } else {
                    blockCatches = this.catchLists[targetsAt];
                    if (blockCatches == null) {
                        blockCatches = ByteCatchList.EMPTY;
                    }
                }
                bbs[count] = new ByteBlock(at, at, next, targets, blockCatches);
                count++;
            }
            at = next;
        }
        ByteBlockList result = new ByteBlockList(count);
        for (i = 0; i < count; i++) {
            result.set(i, bbs[i]);
        }
        return result;
    }

    private void doit() {
        BytecodeArray bytes = this.method.getCode();
        ByteCatchList catches = this.method.getCatches();
        int catchSz = catches.size();
        Bits.set(this.workSet, 0);
        Bits.set(this.blockSet, 0);
        while (!Bits.isEmpty(this.workSet)) {
            try {
                bytes.processWorkSet(this.workSet, this);
                for (int i = 0; i < catchSz; i++) {
                    Item item = catches.get(i);
                    int start = item.getStartPc();
                    int end = item.getEndPc();
                    if (Bits.anyInRange(this.liveSet, start, end)) {
                        Bits.set(this.blockSet, start);
                        Bits.set(this.blockSet, end);
                        addWorkIfNecessary(item.getHandlerPc(), true);
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw new SimException("flow of control falls off end of method", ex);
            }
        }
    }

    private void addWorkIfNecessary(int offset, boolean blockStart) {
        if (!Bits.get(this.liveSet, offset)) {
            Bits.set(this.workSet, offset);
        }
        if (blockStart) {
            Bits.set(this.blockSet, offset);
        }
    }

    private void visitCommon(int offset, int length, boolean nextIsLive) {
        Bits.set(this.liveSet, offset);
        if (nextIsLive) {
            addWorkIfNecessary(offset + length, false);
        } else {
            Bits.set(this.blockSet, offset + length);
        }
    }

    private void visitThrowing(int offset, int length, boolean nextIsLive) {
        int next = offset + length;
        if (nextIsLive) {
            addWorkIfNecessary(next, true);
        }
        ByteCatchList catches = this.method.getCatches().listFor(offset);
        this.catchLists[offset] = catches;
        IntList[] intListArr = this.targetLists;
        if (!nextIsLive) {
            next = -1;
        }
        intListArr[offset] = catches.toTargetList(next);
    }

    public void setPreviousOffset(int offset) {
        this.previousOffset = offset;
    }

    public int getPreviousOffset() {
        return this.previousOffset;
    }
}
