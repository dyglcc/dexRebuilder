package com.android.dx.dex.code;

import com.android.dx.dex.code.CatchTable.Entry;
import com.android.dx.rop.code.BasicBlock;
import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.HashSet;

public final class StdCatchBuilder implements CatchBuilder {
    private static final int MAX_CATCH_RANGE = 65535;
    private final BlockAddresses addresses;
    private final RopMethod method;
    private final int[] order;

    public StdCatchBuilder(RopMethod method, int[] order, BlockAddresses addresses) {
        if (method == null) {
            throw new NullPointerException("method == null");
        } else if (order == null) {
            throw new NullPointerException("order == null");
        } else if (addresses == null) {
            throw new NullPointerException("addresses == null");
        } else {
            this.method = method;
            this.order = order;
            this.addresses = addresses;
        }
    }

    public CatchTable build() {
        return build(this.method, this.order, this.addresses);
    }

    public boolean hasAnyCatches() {
        BasicBlockList blocks = this.method.getBlocks();
        int size = blocks.size();
        for (int i = 0; i < size; i++) {
            if (blocks.get(i).getLastInsn().getCatches().size() != 0) {
                return true;
            }
        }
        return false;
    }

    public HashSet<Type> getCatchTypes() {
        HashSet<Type> result = new HashSet(20);
        BasicBlockList blocks = this.method.getBlocks();
        int size = blocks.size();
        for (int i = 0; i < size; i++) {
            TypeList catches = blocks.get(i).getLastInsn().getCatches();
            int catchSize = catches.size();
            for (int j = 0; j < catchSize; j++) {
                result.add(catches.getType(j));
            }
        }
        return result;
    }

    public static CatchTable build(RopMethod method, int[] order, BlockAddresses addresses) {
        int i;
        BasicBlockList blocks = method.getBlocks();
        ArrayList<Entry> resultList = new ArrayList(len);
        CatchHandlerList currentHandlers = CatchHandlerList.EMPTY;
        BasicBlock currentStartBlock = null;
        BasicBlock currentEndBlock = null;
        for (int labelToBlock : order) {
            BasicBlock block = blocks.labelToBlock(labelToBlock);
            if (block.canThrow()) {
                CatchHandlerList handlers = handlersFor(block, addresses);
                if (currentHandlers.size() == 0) {
                    currentStartBlock = block;
                    currentEndBlock = block;
                    currentHandlers = handlers;
                } else if (currentHandlers.equals(handlers) && rangeIsValid(currentStartBlock, block, addresses)) {
                    currentEndBlock = block;
                } else {
                    if (currentHandlers.size() != 0) {
                        resultList.add(makeEntry(currentStartBlock, currentEndBlock, currentHandlers, addresses));
                    }
                    currentStartBlock = block;
                    currentEndBlock = block;
                    currentHandlers = handlers;
                }
            }
        }
        if (currentHandlers.size() != 0) {
            resultList.add(makeEntry(currentStartBlock, currentEndBlock, currentHandlers, addresses));
        }
        int resultSz = resultList.size();
        if (resultSz == 0) {
            return CatchTable.EMPTY;
        }
        CatchTable result = new CatchTable(resultSz);
        for (i = 0; i < resultSz; i++) {
            result.set(i, (Entry) resultList.get(i));
        }
        result.setImmutable();
        return result;
    }

    private static CatchHandlerList handlersFor(BasicBlock block, BlockAddresses addresses) {
        IntList successors = block.getSuccessors();
        int succSize = successors.size();
        int primary = block.getPrimarySuccessor();
        TypeList catches = block.getLastInsn().getCatches();
        int catchSize = catches.size();
        if (catchSize == 0) {
            return CatchHandlerList.EMPTY;
        }
        if ((primary != -1 || succSize == catchSize) && (primary == -1 || (succSize == catchSize + 1 && primary == successors.get(catchSize)))) {
            int i;
            for (i = 0; i < catchSize; i++) {
                if (catches.getType(i).equals(Type.OBJECT)) {
                    catchSize = i + 1;
                    break;
                }
            }
            CatchHandlerList result = new CatchHandlerList(catchSize);
            for (i = 0; i < catchSize; i++) {
                result.set(i, new CstType(catches.getType(i)), addresses.getStart(successors.get(i)).getAddress());
            }
            result.setImmutable();
            return result;
        }
        throw new RuntimeException("shouldn't happen: weird successors list");
    }

    private static Entry makeEntry(BasicBlock start, BasicBlock end, CatchHandlerList handlers, BlockAddresses addresses) {
        return new Entry(addresses.getLast(start).getAddress(), addresses.getEnd(end).getAddress(), handlers);
    }

    private static boolean rangeIsValid(BasicBlock start, BasicBlock end, BlockAddresses addresses) {
        if (start == null) {
            throw new NullPointerException("start == null");
        } else if (end == null) {
            throw new NullPointerException("end == null");
        } else {
            return addresses.getEnd(end).getAddress() - addresses.getLast(start).getAddress() <= 65535;
        }
    }
}
