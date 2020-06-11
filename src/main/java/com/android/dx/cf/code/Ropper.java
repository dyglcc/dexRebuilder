package com.android.dx.cf.code;

import com.android.dx.cf.code.ByteCatchList.Item;
import com.android.dx.cf.iface.MethodList;
import com.android.dx.dex.DexOptions;
import com.android.dx.rop.code.BasicBlock;
import com.android.dx.rop.code.BasicBlock.Visitor;
import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.InsnList;
import com.android.dx.rop.code.PlainCstInsn;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.code.ThrowingCstInsn;
import com.android.dx.rop.code.ThrowingInsn;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.Bits;
import com.android.dx.util.Hex;
import com.android.dx.util.IntList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Ropper {
    private static final int PARAM_ASSIGNMENT = -1;
    private static final int RETURN = -2;
    private static final int SPECIAL_LABEL_COUNT = 7;
    private static final int SYNCH_CATCH_1 = -6;
    private static final int SYNCH_CATCH_2 = -7;
    private static final int SYNCH_RETURN = -3;
    private static final int SYNCH_SETUP_1 = -4;
    private static final int SYNCH_SETUP_2 = -5;
    private final ByteBlockList blocks;
    private final CatchInfo[] catchInfos;
    private final ExceptionSetupLabelAllocator exceptionSetupLabelAllocator;
    private boolean hasSubroutines;
    private final RopperMachine machine;
    private final int maxLabel;
    private final int maxLocals;
    private final ConcreteMethod method;
    private final ArrayList<BasicBlock> result;
    private final ArrayList<IntList> resultSubroutines;
    private final Simulator sim;
    private final Frame[] startFrames;
    private final Subroutine[] subroutines;
    private boolean synchNeedsExceptionHandler;

    private class CatchInfo {
        private final Map<Type, ExceptionHandlerSetup> setups;

        private CatchInfo() {
            this.setups = new HashMap();
        }

        ExceptionHandlerSetup getSetup(Type caughtType) {
            ExceptionHandlerSetup handler = (ExceptionHandlerSetup) this.setups.get(caughtType);
            if (handler != null) {
                return handler;
            }
            handler = new ExceptionHandlerSetup(caughtType, Ropper.this.exceptionSetupLabelAllocator.getNextLabel());
            this.setups.put(caughtType, handler);
            return handler;
        }

        Collection<ExceptionHandlerSetup> getSetups() {
            return this.setups.values();
        }
    }

    private static class ExceptionHandlerSetup {
        private Type caughtType;
        private int label;

        ExceptionHandlerSetup(Type caughtType, int label) {
            this.caughtType = caughtType;
            this.label = label;
        }

        Type getCaughtType() {
            return this.caughtType;
        }

        public int getLabel() {
            return this.label;
        }
    }

    private static class LabelAllocator {
        int nextAvailableLabel;

        LabelAllocator(int startLabel) {
            this.nextAvailableLabel = startLabel;
        }

        int getNextLabel() {
            int i = this.nextAvailableLabel;
            this.nextAvailableLabel = i + 1;
            return i;
        }
    }

    private class ExceptionSetupLabelAllocator extends LabelAllocator {
        int maxSetupLabel;

        ExceptionSetupLabelAllocator() {
            super(Ropper.this.maxLabel);
            this.maxSetupLabel = Ropper.this.maxLabel + Ropper.this.method.getCatches().size();
        }

        int getNextLabel() {
            if (this.nextAvailableLabel >= this.maxSetupLabel) {
                throw new IndexOutOfBoundsException();
            }
            int i = this.nextAvailableLabel;
            this.nextAvailableLabel = i + 1;
            return i;
        }
    }

    private class Subroutine {
        private BitSet callerBlocks;
        private BitSet retBlocks;
        private int startBlock;

        Subroutine(int startBlock) {
            this.startBlock = startBlock;
            this.retBlocks = new BitSet(Ropper.this.maxLabel);
            this.callerBlocks = new BitSet(Ropper.this.maxLabel);
            Ropper.this.hasSubroutines = true;
        }

        Subroutine(Ropper ropper, int startBlock, int retBlock) {
            this(startBlock);
            addRetBlock(retBlock);
        }

        int getStartBlock() {
            return this.startBlock;
        }

        void addRetBlock(int retBlock) {
            this.retBlocks.set(retBlock);
        }

        void addCallerBlock(int label) {
            this.callerBlocks.set(label);
        }

        IntList getSuccessors() {
            IntList successors = new IntList(this.callerBlocks.size());
            int label = this.callerBlocks.nextSetBit(0);
            while (label >= 0) {
                successors.add(Ropper.this.labelToBlock(label).getSuccessors().get(0));
                label = this.callerBlocks.nextSetBit(label + 1);
            }
            successors.setImmutable();
            return successors;
        }

        void mergeToSuccessors(Frame frame, int[] workSet) {
            int label = this.callerBlocks.nextSetBit(0);
            while (label >= 0) {
                int succLabel = Ropper.this.labelToBlock(label).getSuccessors().get(0);
                Frame subFrame = frame.subFrameForLabel(this.startBlock, label);
                if (subFrame != null) {
                    Ropper.this.mergeAndWorkAsNecessary(succLabel, -1, null, subFrame, workSet);
                } else {
                    Bits.set(workSet, label);
                }
                label = this.callerBlocks.nextSetBit(label + 1);
            }
        }
    }

    private class SubroutineInliner {
        private final LabelAllocator labelAllocator;
        private final ArrayList<IntList> labelToSubroutines;
        private final HashMap<Integer, Integer> origLabelToCopiedLabel = new HashMap();
        private int subroutineStart;
        private int subroutineSuccessor;
        private final BitSet workList;

        SubroutineInliner(LabelAllocator labelAllocator, ArrayList<IntList> labelToSubroutines) {
            this.workList = new BitSet(Ropper.this.maxLabel);
            this.labelAllocator = labelAllocator;
            this.labelToSubroutines = labelToSubroutines;
        }

        void inlineSubroutineCalledFrom(BasicBlock b) {
            this.subroutineSuccessor = b.getSuccessors().get(0);
            this.subroutineStart = b.getSuccessors().get(1);
            int newSubStartLabel = mapOrAllocateLabel(this.subroutineStart);
            int label = this.workList.nextSetBit(0);
            while (label >= 0) {
                this.workList.clear(label);
                int newLabel = ((Integer) this.origLabelToCopiedLabel.get(Integer.valueOf(label))).intValue();
                copyBlock(label, newLabel);
                if (Ropper.this.isSubroutineCaller(Ropper.this.labelToBlock(label))) {
                    new SubroutineInliner(this.labelAllocator, this.labelToSubroutines).inlineSubroutineCalledFrom(Ropper.this.labelToBlock(newLabel));
                }
                label = this.workList.nextSetBit(0);
            }
            Ropper.this.addOrReplaceBlockNoDelete(new BasicBlock(b.getLabel(), b.getInsns(), IntList.makeImmutable(newSubStartLabel), newSubStartLabel), (IntList) this.labelToSubroutines.get(b.getLabel()));
        }

        private void copyBlock(int origLabel, int newLabel) {
            IntList successors;
            BasicBlock origBlock = Ropper.this.labelToBlock(origLabel);
            IntList origSuccessors = origBlock.getSuccessors();
            int primarySuccessor = -1;
            if (Ropper.this.isSubroutineCaller(origBlock)) {
                successors = IntList.makeImmutable(mapOrAllocateLabel(origSuccessors.get(0)), origSuccessors.get(1));
            } else {
                Subroutine subroutine = Ropper.this.subroutineFromRetBlock(origLabel);
                if (subroutine == null) {
                    int origPrimary = origBlock.getPrimarySuccessor();
                    int sz = origSuccessors.size();
                    successors = new IntList(sz);
                    for (int i = 0; i < sz; i++) {
                        int origSuccLabel = origSuccessors.get(i);
                        int newSuccLabel = mapOrAllocateLabel(origSuccLabel);
                        successors.add(newSuccLabel);
                        if (origPrimary == origSuccLabel) {
                            primarySuccessor = newSuccLabel;
                        }
                    }
                    successors.setImmutable();
                } else if (subroutine.startBlock != this.subroutineStart) {
                    throw new RuntimeException("ret instruction returns to label " + Hex.u2(subroutine.startBlock) + " expected: " + Hex.u2(this.subroutineStart));
                } else {
                    successors = IntList.makeImmutable(this.subroutineSuccessor);
                    primarySuccessor = this.subroutineSuccessor;
                }
            }
            Ropper.this.addBlock(new BasicBlock(newLabel, Ropper.this.filterMoveReturnAddressInsns(origBlock.getInsns()), successors, primarySuccessor), (IntList) this.labelToSubroutines.get(newLabel));
        }

        private boolean involvedInSubroutine(int label, int subroutineStart) {
            IntList subroutinesList = (IntList) this.labelToSubroutines.get(label);
            return subroutinesList != null && subroutinesList.size() > 0 && subroutinesList.top() == subroutineStart;
        }

        private int mapOrAllocateLabel(int origLabel) {
            Integer mappedLabel = (Integer) this.origLabelToCopiedLabel.get(Integer.valueOf(origLabel));
            if (mappedLabel != null) {
                return mappedLabel.intValue();
            }
            if (!involvedInSubroutine(origLabel, this.subroutineStart)) {
                return origLabel;
            }
            int resultLabel = this.labelAllocator.getNextLabel();
            this.workList.set(origLabel);
            this.origLabelToCopiedLabel.put(Integer.valueOf(origLabel), Integer.valueOf(resultLabel));
            while (this.labelToSubroutines.size() <= resultLabel) {
                this.labelToSubroutines.add(null);
            }
            this.labelToSubroutines.set(resultLabel, this.labelToSubroutines.get(origLabel));
            return resultLabel;
        }
    }

    public static RopMethod convert(ConcreteMethod method, TranslationAdvice advice, MethodList methods, DexOptions dexOptions) {
        try {
            Ropper r = new Ropper(method, advice, methods, dexOptions);
            r.doit();
            return r.getRopMethod();
        } catch (SimException ex) {
            ex.addContext("...while working on method " + method.getNat().toHuman());
            throw ex;
        }
    }

    private Ropper(ConcreteMethod method, TranslationAdvice advice, MethodList methods, DexOptions dexOptions) {
        if (method == null) {
            throw new NullPointerException("method == null");
        } else if (advice == null) {
            throw new NullPointerException("advice == null");
        } else {
            this.method = method;
            this.blocks = BasicBlocker.identifyBlocks(method);
            this.maxLabel = this.blocks.getMaxLabel();
            this.maxLocals = method.getMaxLocals();
            this.machine = new RopperMachine(this, method, advice, methods);
            this.sim = new Simulator(this.machine, method, dexOptions);
            this.startFrames = new Frame[this.maxLabel];
            this.subroutines = new Subroutine[this.maxLabel];
            this.result = new ArrayList((this.blocks.size() * 2) + 10);
            this.resultSubroutines = new ArrayList((this.blocks.size() * 2) + 10);
            this.catchInfos = new CatchInfo[this.maxLabel];
            this.synchNeedsExceptionHandler = false;
            this.startFrames[0] = new Frame(this.maxLocals, method.getMaxStack());
            this.exceptionSetupLabelAllocator = new ExceptionSetupLabelAllocator();
        }
    }

    int getFirstTempStackReg() {
        int regCount = getNormalRegCount();
        return isSynchronized() ? regCount + 1 : regCount;
    }

    private int getSpecialLabel(int label) {
        return (this.maxLabel + this.method.getCatches().size()) + (label ^ -1);
    }

    private int getMinimumUnreservedLabel() {
        return (this.maxLabel + this.method.getCatches().size()) + 7;
    }

    private int getAvailableLabel() {
        int candidate = getMinimumUnreservedLabel();
        Iterator it = this.result.iterator();
        while (it.hasNext()) {
            int label = ((BasicBlock) it.next()).getLabel();
            if (label >= candidate) {
                candidate = label + 1;
            }
        }
        return candidate;
    }

    private boolean isSynchronized() {
        return (this.method.getAccessFlags() & 32) != 0;
    }

    private boolean isStatic() {
        return (this.method.getAccessFlags() & 8) != 0;
    }

    private int getNormalRegCount() {
        return this.maxLocals + this.method.getMaxStack();
    }

    private RegisterSpec getSynchReg() {
        int reg = getNormalRegCount();
        if (reg < 1) {
            reg = 1;
        }
        return RegisterSpec.make(reg, Type.OBJECT);
    }

    private int labelToResultIndex(int label) {
        int sz = this.result.size();
        for (int i = 0; i < sz; i++) {
            if (((BasicBlock) this.result.get(i)).getLabel() == label) {
                return i;
            }
        }
        return -1;
    }

    private BasicBlock labelToBlock(int label) {
        int idx = labelToResultIndex(label);
        if (idx >= 0) {
            return (BasicBlock) this.result.get(idx);
        }
        throw new IllegalArgumentException("no such label " + Hex.u2(label));
    }

    private void addBlock(BasicBlock block, IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }
        this.result.add(block);
        subroutines.throwIfMutable();
        this.resultSubroutines.add(subroutines);
    }

    private boolean addOrReplaceBlock(BasicBlock block, IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }
        boolean ret;
        int idx = labelToResultIndex(block.getLabel());
        if (idx < 0) {
            ret = false;
        } else {
            removeBlockAndSpecialSuccessors(idx);
            ret = true;
        }
        this.result.add(block);
        subroutines.throwIfMutable();
        this.resultSubroutines.add(subroutines);
        return ret;
    }

    private boolean addOrReplaceBlockNoDelete(BasicBlock block, IntList subroutines) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }
        boolean ret;
        int idx = labelToResultIndex(block.getLabel());
        if (idx < 0) {
            ret = false;
        } else {
            this.result.remove(idx);
            this.resultSubroutines.remove(idx);
            ret = true;
        }
        this.result.add(block);
        subroutines.throwIfMutable();
        this.resultSubroutines.add(subroutines);
        return ret;
    }

    private void removeBlockAndSpecialSuccessors(int idx) {
        int minLabel = getMinimumUnreservedLabel();
        IntList successors = ((BasicBlock) this.result.get(idx)).getSuccessors();
        int sz = successors.size();
        this.result.remove(idx);
        this.resultSubroutines.remove(idx);
        for (int i = 0; i < sz; i++) {
            int label = successors.get(i);
            if (label >= minLabel) {
                idx = labelToResultIndex(label);
                if (idx < 0) {
                    throw new RuntimeException("Invalid label " + Hex.u2(label));
                }
                removeBlockAndSpecialSuccessors(idx);
            }
        }
    }

    private RopMethod getRopMethod() {
        int sz = this.result.size();
        BasicBlockList bbl = new BasicBlockList(sz);
        for (int i = 0; i < sz; i++) {
            bbl.set(i, (BasicBlock) this.result.get(i));
        }
        bbl.setImmutable();
        return new RopMethod(bbl, getSpecialLabel(-1));
    }

    private void doit() {
        int[] workSet = Bits.makeBitSet(this.maxLabel);
        Bits.set(workSet, 0);
        addSetupBlocks();
        setFirstFrame();
        while (true) {
            int offset = Bits.findFirst(workSet, 0);
            if (offset < 0) {
                break;
            }
            Bits.clear(workSet, offset);
            try {
                processBlock(this.blocks.labelToBlock(offset), this.startFrames[offset], workSet);
            } catch (SimException ex) {
                ex.addContext("...while working on block " + Hex.u2(offset));
                throw ex;
            }
        }
        addReturnBlock();
        addSynchExceptionHandlerBlock();
        addExceptionSetupBlocks();
        if (this.hasSubroutines) {
            inlineSubroutines();
        }
    }

    private void setFirstFrame() {
        this.startFrames[0].initializeWithParameters(this.method.getEffectiveDescriptor().getParameterTypes());
        this.startFrames[0].setImmutable();
    }

    private void processBlock(ByteBlock block, Frame frame, int[] workSet) {
        int startSuccessorIndex;
        int primarySucc;
        InsnList insnList;
        Insn lastInsn;
        ByteCatchList catches = block.getCatches();
        this.machine.startBlock(catches.toRopCatchList());
        frame = frame.copy();
        this.sim.simulate(block, frame);
        frame.setImmutable();
        int extraBlockCount = this.machine.getExtraBlockCount();
        ArrayList<Insn> insns = this.machine.getInsns();
        int insnSz = insns.size();
        int catchSz = catches.size();
        IntList successors = block.getSuccessors();
        Subroutine calledSubroutine = null;
        int subroutineLabel;
        if (this.machine.hasJsr()) {
            startSuccessorIndex = 1;
            subroutineLabel = successors.get(1);
            if (this.subroutines[subroutineLabel] == null) {
                this.subroutines[subroutineLabel] = new Subroutine(subroutineLabel);
            }
            this.subroutines[subroutineLabel].addCallerBlock(block.getLabel());
            calledSubroutine = this.subroutines[subroutineLabel];
        } else if (this.machine.hasRet()) {
            subroutineLabel = this.machine.getReturnAddress().getSubroutineAddress();
            if (this.subroutines[subroutineLabel] == null) {
                this.subroutines[subroutineLabel] = new Subroutine(this, subroutineLabel, block.getLabel());
            } else {
                this.subroutines[subroutineLabel].addRetBlock(block.getLabel());
            }
            successors = this.subroutines[subroutineLabel].getSuccessors();
            this.subroutines[subroutineLabel].mergeToSuccessors(frame, workSet);
            startSuccessorIndex = successors.size();
        } else if (this.machine.wereCatchesUsed()) {
            startSuccessorIndex = catchSz;
        } else {
            startSuccessorIndex = 0;
        }
        int succSz = successors.size();
        int i = startSuccessorIndex;
        while (i < succSz) {
            int succ = successors.get(i);
            try {
                mergeAndWorkAsNecessary(succ, block.getLabel(), calledSubroutine, frame, workSet);
                i++;
            } catch (SimException ex) {
                ex.addContext("...while merging to block " + Hex.u2(succ));
                throw ex;
            }
        }
        if (succSz == 0 && this.machine.returns()) {
            successors = IntList.makeImmutable(getSpecialLabel(RETURN));
            succSz = 1;
        }
        if (succSz == 0) {
            primarySucc = -1;
        } else {
            primarySucc = this.machine.getPrimarySuccessorIndex();
            if (primarySucc >= 0) {
                primarySucc = successors.get(primarySucc);
            }
        }
        boolean synch = isSynchronized() && this.machine.canThrow();
        if (synch || catchSz != 0) {
            boolean catchesAny = false;
            IntList intList = new IntList(succSz);
            i = 0;
            while (i < catchSz) {
                Item one = catches.get(i);
                CstType exceptionClass = one.getExceptionClass();
                int targ = one.getHandlerPc();
                catchesAny |= exceptionClass == CstType.OBJECT ? 1 : 0;
                try {
                    mergeAndWorkAsNecessary(targ, block.getLabel(), null, frame.makeExceptionHandlerStartFrame(exceptionClass), workSet);
                    CatchInfo handlers = this.catchInfos[targ];
                    if (handlers == null) {
                        Ropper ropper = this;
                        CatchInfo catchInfo = new CatchInfo();
                        this.catchInfos[targ] = catchInfo;
                    }
                    intList.add(handlers.getSetup(exceptionClass.getClassType()).getLabel());
                    i++;
                } catch (SimException ex2) {
                    ex2.addContext("...while merging exception to block " + Hex.u2(targ));
                    throw ex2;
                }
            }
            if (synch && !catchesAny) {
                intList.add(getSpecialLabel(SYNCH_CATCH_1));
                this.synchNeedsExceptionHandler = true;
                for (i = (insnSz - extraBlockCount) - 1; i < insnSz; i++) {
                    Insn insn = (Insn) insns.get(i);
                    if (insn.canThrow()) {
                        insns.set(i, insn.withAddedCatch(Type.OBJECT));
                    }
                }
            }
            if (primarySucc >= 0) {
                intList.add(primarySucc);
            }
            intList.setImmutable();
            successors = intList;
        }
        int primarySuccListIndex = successors.indexOf(primarySucc);
        while (extraBlockCount > 0) {
            insnSz--;
            Insn extraInsn = (Insn) insns.get(insnSz);
            boolean needsGoto = extraInsn.getOpcode().getBranchingness() == 1;
            insnList = new InsnList(needsGoto ? 2 : 1);
            IntList extraBlockSuccessors = successors;
            insnList.set(0, extraInsn);
            if (needsGoto) {
                insnList.set(1, new PlainInsn(Rops.GOTO, extraInsn.getPosition(), null, RegisterSpecList.EMPTY));
                extraBlockSuccessors = IntList.makeImmutable(primarySucc);
            }
            insnList.setImmutable();
            int label = getAvailableLabel();
            addBlock(new BasicBlock(label, insnList, extraBlockSuccessors, primarySucc), frame.getSubroutines());
            successors = successors.mutableCopy();
            successors.set(primarySuccListIndex, label);
            successors.setImmutable();
            primarySucc = label;
            extraBlockCount--;
        }
        if (insnSz == 0) {
            lastInsn = null;
        } else {
            lastInsn = (Insn) insns.get(insnSz - 1);
        }
        if (lastInsn == null || lastInsn.getOpcode().getBranchingness() == 1) {
            SourcePosition pos;
            if (lastInsn == null) {
                pos = SourcePosition.NO_INFO;
            } else {
                pos = lastInsn.getPosition();
            }
            insns.add(new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY));
            insnSz++;
        }
        insnList = new InsnList(insnSz);
        for (i = 0; i < insnSz; i++) {
            insnList.set(i, (Insn) insns.get(i));
        }
        insnList.setImmutable();
        addOrReplaceBlock(new BasicBlock(block.getLabel(), insnList, successors, primarySucc), frame.getSubroutines());
    }

    private void mergeAndWorkAsNecessary(int label, int pred, Subroutine calledSubroutine, Frame frame, int[] workSet) {
        Frame existing = this.startFrames[label];
        if (existing != null) {
            Frame merged;
            if (calledSubroutine != null) {
                merged = existing.mergeWithSubroutineCaller(frame, calledSubroutine.getStartBlock(), pred);
            } else {
                merged = existing.mergeWith(frame);
            }
            if (merged != existing) {
                this.startFrames[label] = merged;
                Bits.set(workSet, label);
                return;
            }
            return;
        }
        if (calledSubroutine != null) {
            this.startFrames[label] = frame.makeNewSubroutineStartFrame(label, pred);
        } else {
            this.startFrames[label] = frame;
        }
        Bits.set(workSet, label);
    }

    private void addSetupBlocks() {
        LocalVariableList localVariables = this.method.getLocalVariables();
        SourcePosition pos = this.method.makeSourcePosistion(0);
        StdTypeList params = this.method.getEffectiveDescriptor().getParameterTypes();
        int sz = params.size();
        InsnList insnList = new InsnList(sz + 1);
        int at = 0;
        for (int i = 0; i < sz; i++) {
            RegisterSpec result;
            Type one = params.get(i);
            LocalVariableList.Item local = localVariables.pcAndIndexToLocal(0, at);
            if (local == null) {
                result = RegisterSpec.make(at, one);
            } else {
                result = RegisterSpec.makeLocalOptional(at, one, local.getLocalItem());
            }
            insnList.set(i, new PlainCstInsn(Rops.opMoveParam(one), pos, result, RegisterSpecList.EMPTY, CstInteger.make(at)));
            at += one.getCategory();
        }
        insnList.set(sz, new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY));
        insnList.setImmutable();
        boolean synch = isSynchronized();
        int label = synch ? getSpecialLabel(-4) : 0;
        addBlock(new BasicBlock(getSpecialLabel(-1), insnList, IntList.makeImmutable(label), label), IntList.EMPTY);
        if (synch) {
            RegisterSpec synchReg = getSynchReg();
            if (isStatic()) {
                Insn throwingCstInsn = new ThrowingCstInsn(Rops.CONST_OBJECT, pos, RegisterSpecList.EMPTY, StdTypeList.EMPTY, this.method.getDefiningClass());
                insnList = new InsnList(1);
                insnList.set(0, throwingCstInsn);
            } else {
                insnList = new InsnList(2);
                insnList.set(0, new PlainCstInsn(Rops.MOVE_PARAM_OBJECT, pos, synchReg, RegisterSpecList.EMPTY, CstInteger.VALUE_0));
                insnList.set(1, new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY));
            }
            int label2 = getSpecialLabel(SYNCH_SETUP_2);
            insns.setImmutable();
            addBlock(new BasicBlock(label, insns, IntList.makeImmutable(label2), label2), IntList.EMPTY);
            insnList = new InsnList(isStatic() ? 2 : 1);
            if (isStatic()) {
                insnList.set(0, new PlainInsn(Rops.opMoveResultPseudo(synchReg), pos, synchReg, RegisterSpecList.EMPTY));
            }
            insnList.set(isStatic() ? 1 : 0, new ThrowingInsn(Rops.MONITOR_ENTER, pos, RegisterSpecList.make(synchReg), StdTypeList.EMPTY));
            insnList.setImmutable();
            addBlock(new BasicBlock(label2, insnList, IntList.makeImmutable(0), 0), IntList.EMPTY);
        }
    }

    private void addReturnBlock() {
        Rop returnOp = this.machine.getReturnOp();
        if (returnOp != null) {
            InsnList insns;
            RegisterSpecList sources;
            SourcePosition returnPos = this.machine.getReturnPosition();
            int label = getSpecialLabel(RETURN);
            if (isSynchronized()) {
                insns = new InsnList(1);
                insns.set(0, new ThrowingInsn(Rops.MONITOR_EXIT, returnPos, RegisterSpecList.make(getSynchReg()), StdTypeList.EMPTY));
                insns.setImmutable();
                int nextLabel = getSpecialLabel(SYNCH_RETURN);
                addBlock(new BasicBlock(label, insns, IntList.makeImmutable(nextLabel), nextLabel), IntList.EMPTY);
                label = nextLabel;
            }
            insns = new InsnList(1);
            TypeList sourceTypes = returnOp.getSources();
            if (sourceTypes.size() == 0) {
                sources = RegisterSpecList.EMPTY;
            } else {
                sources = RegisterSpecList.make(RegisterSpec.make(0, sourceTypes.getType(0)));
            }
            insns.set(0, new PlainInsn(returnOp, returnPos, null, sources));
            insns.setImmutable();
            addBlock(new BasicBlock(label, insns, IntList.EMPTY, -1), IntList.EMPTY);
        }
    }

    private void addSynchExceptionHandlerBlock() {
        if (this.synchNeedsExceptionHandler) {
            SourcePosition pos = this.method.makeSourcePosistion(0);
            RegisterSpec exReg = RegisterSpec.make(0, Type.THROWABLE);
            InsnList insns = new InsnList(2);
            insns.set(0, new PlainInsn(Rops.opMoveException(Type.THROWABLE), pos, exReg, RegisterSpecList.EMPTY));
            insns.set(1, new ThrowingInsn(Rops.MONITOR_EXIT, pos, RegisterSpecList.make(getSynchReg()), StdTypeList.EMPTY));
            insns.setImmutable();
            int label2 = getSpecialLabel(SYNCH_CATCH_2);
            addBlock(new BasicBlock(getSpecialLabel(SYNCH_CATCH_1), insns, IntList.makeImmutable(label2), label2), IntList.EMPTY);
            insns = new InsnList(1);
            insns.set(0, new ThrowingInsn(Rops.THROW, pos, RegisterSpecList.make(exReg), StdTypeList.EMPTY));
            insns.setImmutable();
            addBlock(new BasicBlock(label2, insns, IntList.EMPTY, -1), IntList.EMPTY);
        }
    }

    private void addExceptionSetupBlocks() {
        int len = this.catchInfos.length;
        for (int i = 0; i < len; i++) {
            CatchInfo catches = this.catchInfos[i];
            if (catches != null) {
                for (ExceptionHandlerSetup one : catches.getSetups()) {
                    SourcePosition pos = labelToBlock(i).getFirstInsn().getPosition();
                    InsnList il = new InsnList(2);
                    il.set(0, new PlainInsn(Rops.opMoveException(one.getCaughtType()), pos, RegisterSpec.make(this.maxLocals, one.getCaughtType()), RegisterSpecList.EMPTY));
                    il.set(1, new PlainInsn(Rops.GOTO, pos, null, RegisterSpecList.EMPTY));
                    il.setImmutable();
                    addBlock(new BasicBlock(one.getLabel(), il, IntList.makeImmutable(i), i), this.startFrames[i].getSubroutines());
                }
            }
        }
    }

    private boolean isSubroutineCaller(BasicBlock bb) {
        boolean z = true;
        IntList successors = bb.getSuccessors();
        if (successors.size() < 2) {
            return false;
        }
        int subLabel = successors.get(1);
        if (subLabel >= this.subroutines.length || this.subroutines[subLabel] == null) {
            z = false;
        }
        return z;
    }

    private void inlineSubroutines() {
        int i;
        final IntList reachableSubroutineCallerLabels = new IntList(4);
        forEachNonSubBlockDepthFirst(0, new Visitor() {
            public void visitBlock(BasicBlock b) {
                if (Ropper.this.isSubroutineCaller(b)) {
                    reachableSubroutineCallerLabels.add(b.getLabel());
                }
            }
        });
        int largestAllocedLabel = getAvailableLabel();
        ArrayList<IntList> labelToSubroutines = new ArrayList(largestAllocedLabel);
        for (i = 0; i < largestAllocedLabel; i++) {
            labelToSubroutines.add(null);
        }
        for (i = 0; i < this.result.size(); i++) {
            BasicBlock b = (BasicBlock) this.result.get(i);
            if (b != null) {
                labelToSubroutines.set(b.getLabel(), (IntList) this.resultSubroutines.get(i));
            }
        }
        int sz = reachableSubroutineCallerLabels.size();
        for (i = 0; i < sz; i++) {
            new SubroutineInliner(new LabelAllocator(getAvailableLabel()), labelToSubroutines).inlineSubroutineCalledFrom(labelToBlock(reachableSubroutineCallerLabels.get(i)));
        }
        deleteUnreachableBlocks();
    }

    private void deleteUnreachableBlocks() {
        final IntList reachableLabels = new IntList(this.result.size());
        this.resultSubroutines.clear();
        forEachNonSubBlockDepthFirst(getSpecialLabel(-1), new Visitor() {
            public void visitBlock(BasicBlock b) {
                reachableLabels.add(b.getLabel());
            }
        });
        reachableLabels.sort();
        for (int i = this.result.size() - 1; i >= 0; i--) {
            if (reachableLabels.indexOf(((BasicBlock) this.result.get(i)).getLabel()) < 0) {
                this.result.remove(i);
            }
        }
    }

    private Subroutine subroutineFromRetBlock(int label) {
        for (int i = this.subroutines.length - 1; i >= 0; i--) {
            if (this.subroutines[i] != null) {
                Subroutine subroutine = this.subroutines[i];
                if (subroutine.retBlocks.get(label)) {
                    return subroutine;
                }
            }
        }
        return null;
    }

    private InsnList filterMoveReturnAddressInsns(InsnList insns) {
        int i;
        int newSz = 0;
        int sz = insns.size();
        for (i = 0; i < sz; i++) {
            if (insns.get(i).getOpcode() != Rops.MOVE_RETURN_ADDRESS) {
                newSz++;
            }
        }
        if (newSz == sz) {
            return insns;
        }
        InsnList newInsns = new InsnList(newSz);
        i = 0;
        int newIndex = 0;
        while (i < sz) {
            int newIndex2;
            Insn insn = insns.get(i);
            if (insn.getOpcode() != Rops.MOVE_RETURN_ADDRESS) {
                newIndex2 = newIndex + 1;
                newInsns.set(newIndex, insn);
            } else {
                newIndex2 = newIndex;
            }
            i++;
            newIndex = newIndex2;
        }
        newInsns.setImmutable();
        return newInsns;
    }

    private void forEachNonSubBlockDepthFirst(int firstLabel, Visitor v) {
        forEachNonSubBlockDepthFirst0(labelToBlock(firstLabel), v, new BitSet(this.maxLabel));
    }

    private void forEachNonSubBlockDepthFirst0(BasicBlock next, Visitor v, BitSet visited) {
        v.visitBlock(next);
        visited.set(next.getLabel());
        IntList successors = next.getSuccessors();
        int sz = successors.size();
        int i = 0;
        while (i < sz) {
            int succ = successors.get(i);
            if (!visited.get(succ) && (!isSubroutineCaller(next) || i <= 0)) {
                int idx = labelToResultIndex(succ);
                if (idx >= 0) {
                    forEachNonSubBlockDepthFirst0((BasicBlock) this.result.get(idx), v, visited);
                }
            }
            i++;
        }
    }
}
