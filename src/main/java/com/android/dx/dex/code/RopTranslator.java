package com.android.dx.dex.code;

import com.android.dx.dex.DexOptions;
import com.android.dx.rop.code.BasicBlock;
import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.FillArrayDataInsn;
import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.Insn.BaseVisitor;
import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.code.InvokePolymorphicInsn;
import com.android.dx.rop.code.LocalVariableInfo;
import com.android.dx.rop.code.PlainCstInsn;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.code.SwitchInsn;
import com.android.dx.rop.code.ThrowingCstInsn;
import com.android.dx.rop.code.ThrowingInsn;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.util.Bits;
import com.android.dx.util.IntList;
import java.util.ArrayList;

public final class RopTranslator {
    private final BlockAddresses addresses;
    private final DexOptions dexOptions;
    private final LocalVariableInfo locals;
    private final RopMethod method;
    private int[] order = null;
    private final OutputCollector output;
    private final int paramSize;
    private final boolean paramsAreInOrder;
    private final int positionInfo;
    private final int regCount;
    private final TranslationVisitor translationVisitor;

    /* renamed from: com.android.dx.dex.code.RopTranslator$1 */
    class AnonymousClass1 extends BaseVisitor {
        final /* synthetic */ int val$initialRegCount;
        final /* synthetic */ int val$paramSize;
        final /* synthetic */ boolean[] val$paramsAreInOrder;

        AnonymousClass1(boolean[] zArr, int i, int i2) {
            this.val$paramsAreInOrder = zArr;
            this.val$initialRegCount = i;
            this.val$paramSize = i2;
        }

        public void visitPlainCstInsn(PlainCstInsn insn) {
            if (insn.getOpcode().getOpcode() == 3) {
                boolean z;
                int param = ((CstInteger) insn.getConstant()).getValue();
                boolean[] zArr = this.val$paramsAreInOrder;
                if (this.val$paramsAreInOrder[0] && (this.val$initialRegCount - this.val$paramSize) + param == insn.getResult().getReg()) {
                    z = true;
                } else {
                    z = false;
                }
                zArr[0] = z;
            }
        }
    }

    private class TranslationVisitor implements Visitor {
        private BasicBlock block;
        private CodeAddress lastAddress;
        private final OutputCollector output;

        public TranslationVisitor(OutputCollector output) {
            this.output = output;
        }

        public void setBlock(BasicBlock block, CodeAddress lastAddress) {
            this.block = block;
            this.lastAddress = lastAddress;
        }

        public void visitPlainInsn(PlainInsn insn) {
            Rop rop = insn.getOpcode();
            if (rop.getOpcode() != 54 && rop.getOpcode() != 56) {
                DalvInsn di;
                SourcePosition pos = insn.getPosition();
                Dop opcode = RopToDop.dopFor(insn);
                switch (rop.getBranchingness()) {
                    case 1:
                    case 2:
                    case 6:
                        di = new SimpleInsn(opcode, pos, RopTranslator.getRegs(insn));
                        break;
                    case 3:
                        return;
                    case 4:
                        di = new TargetInsn(opcode, pos, RopTranslator.getRegs(insn), RopTranslator.this.addresses.getStart(this.block.getSuccessors().get(1)));
                        break;
                    default:
                        throw new RuntimeException("shouldn't happen");
                }
                addOutput(di);
            }
        }

        public void visitPlainCstInsn(PlainCstInsn insn) {
            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            Rop rop = insn.getOpcode();
            int ropOpcode = rop.getOpcode();
            if (rop.getBranchingness() != 1) {
                throw new RuntimeException("shouldn't happen");
            } else if (ropOpcode != 3) {
                addOutput(new CstInsn(opcode, pos, RopTranslator.getRegs(insn), insn.getConstant()));
            } else if (!RopTranslator.this.paramsAreInOrder) {
                RegisterSpec dest = insn.getResult();
                addOutput(new SimpleInsn(opcode, pos, RegisterSpecList.make(dest, RegisterSpec.make((RopTranslator.this.regCount - RopTranslator.this.paramSize) + ((CstInteger) insn.getConstant()).getValue(), dest.getType()))));
            }
        }

        public void visitSwitchInsn(SwitchInsn insn) {
            SourcePosition pos = insn.getPosition();
            IntList cases = insn.getCases();
            IntList successors = this.block.getSuccessors();
            int casesSz = cases.size();
            int succSz = successors.size();
            int primarySuccessor = this.block.getPrimarySuccessor();
            if (casesSz == succSz - 1 && primarySuccessor == successors.get(casesSz)) {
                CodeAddress[] switchTargets = new CodeAddress[casesSz];
                for (int i = 0; i < casesSz; i++) {
                    switchTargets[i] = RopTranslator.this.addresses.getStart(successors.get(i));
                }
                CodeAddress dataAddress = new CodeAddress(pos);
                CodeAddress switchAddress = new CodeAddress(this.lastAddress.getPosition(), true);
                SwitchData dataInsn = new SwitchData(pos, switchAddress, cases, switchTargets);
                TargetInsn switchInsn = new TargetInsn(dataInsn.isPacked() ? Dops.PACKED_SWITCH : Dops.SPARSE_SWITCH, pos, RopTranslator.getRegs(insn), dataAddress);
                addOutput(switchAddress);
                addOutput(switchInsn);
                addOutputSuffix(new OddSpacer(pos));
                addOutputSuffix(dataAddress);
                addOutputSuffix(dataInsn);
                return;
            }
            throw new RuntimeException("shouldn't happen");
        }

        private RegisterSpec getNextMoveResultPseudo() {
            int label = this.block.getPrimarySuccessor();
            if (label < 0) {
                return null;
            }
            Insn insn = RopTranslator.this.method.getBlocks().labelToBlock(label).getInsns().get(0);
            if (insn.getOpcode().getOpcode() == 56) {
                return insn.getResult();
            }
            return null;
        }

        public void visitInvokePolymorphicInsn(InvokePolymorphicInsn insn) {
            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            Rop rop = insn.getOpcode();
            if (rop.getBranchingness() != 6) {
                throw new RuntimeException("Expected BRANCH_THROW got " + rop.getBranchingness());
            } else if (rop.isCallLike()) {
                addOutput(this.lastAddress);
                addOutput(new MultiCstInsn(opcode, pos, insn.getSources(), new Constant[]{insn.getPolymorphicMethod(), insn.getCallSiteProto()}));
            } else {
                throw new RuntimeException("Expected call-like operation");
            }
        }

        public void visitThrowingCstInsn(ThrowingCstInsn insn) {
            boolean z = true;
            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            Rop rop = insn.getOpcode();
            Constant cst = insn.getConstant();
            if (rop.getBranchingness() != 6) {
                throw new RuntimeException("Expected BRANCH_THROW got " + rop.getBranchingness());
            }
            addOutput(this.lastAddress);
            if (rop.isCallLike()) {
                addOutput(new CstInsn(opcode, pos, insn.getSources(), cst));
                return;
            }
            boolean hasResult;
            RegisterSpec realResult = getNextMoveResultPseudo();
            RegisterSpecList regs = RopTranslator.getRegs(insn, realResult);
            if (opcode.hasResult() || rop.getOpcode() == 43) {
                hasResult = true;
            } else {
                hasResult = false;
            }
            if (realResult == null) {
                z = false;
            }
            if (hasResult != z) {
                throw new RuntimeException("Insn with result/move-result-pseudo mismatch " + insn);
            }
            DalvInsn di;
            if (rop.getOpcode() != 41 || opcode.getOpcode() == 35) {
                di = new CstInsn(opcode, pos, regs, cst);
            } else {
                di = new SimpleInsn(opcode, pos, regs);
            }
            addOutput(di);
        }

        public void visitThrowingInsn(ThrowingInsn insn) {
            SourcePosition pos = insn.getPosition();
            Dop opcode = RopToDop.dopFor(insn);
            if (insn.getOpcode().getBranchingness() != 6) {
                throw new RuntimeException("shouldn't happen");
            }
            RegisterSpec realResult = getNextMoveResultPseudo();
            if (opcode.hasResult() != (realResult != null)) {
                throw new RuntimeException("Insn with result/move-result-pseudo mismatch" + insn);
            }
            addOutput(this.lastAddress);
            addOutput(new SimpleInsn(opcode, pos, RopTranslator.getRegs(insn, realResult)));
        }

        public void visitFillArrayDataInsn(FillArrayDataInsn insn) {
            SourcePosition pos = insn.getPosition();
            Constant cst = insn.getConstant();
            ArrayList<Constant> values = insn.getInitValues();
            if (insn.getOpcode().getBranchingness() != 1) {
                throw new RuntimeException("shouldn't happen");
            }
            CodeAddress dataAddress = new CodeAddress(pos);
            ArrayData dataInsn = new ArrayData(pos, this.lastAddress, values, cst);
            TargetInsn fillArrayDataInsn = new TargetInsn(Dops.FILL_ARRAY_DATA, pos, RopTranslator.getRegs(insn), dataAddress);
            addOutput(this.lastAddress);
            addOutput(fillArrayDataInsn);
            addOutputSuffix(new OddSpacer(pos));
            addOutputSuffix(dataAddress);
            addOutputSuffix(dataInsn);
        }

        protected void addOutput(DalvInsn insn) {
            this.output.add(insn);
        }

        protected void addOutputSuffix(DalvInsn insn) {
            this.output.addSuffix(insn);
        }
    }

    private class LocalVariableAwareTranslationVisitor extends TranslationVisitor {
        private final LocalVariableInfo locals;

        public LocalVariableAwareTranslationVisitor(OutputCollector output, LocalVariableInfo locals) {
            super(output);
            this.locals = locals;
        }

        public void visitPlainInsn(PlainInsn insn) {
            super.visitPlainInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        public void visitPlainCstInsn(PlainCstInsn insn) {
            super.visitPlainCstInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        public void visitSwitchInsn(SwitchInsn insn) {
            super.visitSwitchInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        public void visitThrowingCstInsn(ThrowingCstInsn insn) {
            super.visitThrowingCstInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        public void visitThrowingInsn(ThrowingInsn insn) {
            super.visitThrowingInsn(insn);
            addIntroductionIfNecessary(insn);
        }

        public void addIntroductionIfNecessary(Insn insn) {
            RegisterSpec spec = this.locals.getAssignment(insn);
            if (spec != null) {
                addOutput(new LocalStart(insn.getPosition(), spec));
            }
        }
    }

    public static DalvCode translate(RopMethod method, int positionInfo, LocalVariableInfo locals, int paramSize, DexOptions dexOptions) {
        return new RopTranslator(method, positionInfo, locals, paramSize, dexOptions).translateAndGetResult();
    }

    private RopTranslator(RopMethod method, int positionInfo, LocalVariableInfo locals, int paramSize, DexOptions dexOptions) {
        this.dexOptions = dexOptions;
        this.method = method;
        this.positionInfo = positionInfo;
        this.locals = locals;
        this.addresses = new BlockAddresses(method);
        this.paramSize = paramSize;
        this.paramsAreInOrder = calculateParamsAreInOrder(method, paramSize);
        BasicBlockList blocks = method.getBlocks();
        int bsz = blocks.size();
        int maxInsns = (bsz * 3) + blocks.getInstructionCount();
        if (locals != null) {
            maxInsns += locals.getAssignmentCount() + bsz;
        }
        this.regCount = (this.paramsAreInOrder ? 0 : this.paramSize) + blocks.getRegCount();
        this.output = new OutputCollector(dexOptions, maxInsns, bsz * 3, this.regCount, paramSize);
        if (locals != null) {
            this.translationVisitor = new LocalVariableAwareTranslationVisitor(this.output, locals);
        } else {
            this.translationVisitor = new TranslationVisitor(this.output);
        }
    }

    private static boolean calculateParamsAreInOrder(RopMethod method, int paramSize) {
        boolean[] paramsAreInOrder = new boolean[]{true};
        method.getBlocks().forEachInsn(new AnonymousClass1(paramsAreInOrder, method.getBlocks().getRegCount(), paramSize));
        return paramsAreInOrder[0];
    }

    private DalvCode translateAndGetResult() {
        pickOrder();
        outputInstructions();
        return new DalvCode(this.positionInfo, this.output.getFinisher(), new StdCatchBuilder(this.method, this.order, this.addresses));
    }

    private void outputInstructions() {
        BasicBlockList blocks = this.method.getBlocks();
        int[] order = this.order;
        int len = order.length;
        for (int i = 0; i < len; i++) {
            int nextI = i + 1;
            outputBlock(blocks.labelToBlock(order[i]), nextI == order.length ? -1 : order[nextI]);
        }
    }

    private void outputBlock(BasicBlock block, int nextLabel) {
        CodeAddress startAddress = this.addresses.getStart(block);
        this.output.add(startAddress);
        if (this.locals != null) {
            this.output.add(new LocalSnapshot(startAddress.getPosition(), this.locals.getStarts(block)));
        }
        this.translationVisitor.setBlock(block, this.addresses.getLast(block));
        block.getInsns().forEach(this.translationVisitor);
        this.output.add(this.addresses.getEnd(block));
        int succ = block.getPrimarySuccessor();
        Insn lastInsn = block.getLastInsn();
        if (succ >= 0 && succ != nextLabel) {
            if (lastInsn.getOpcode().getBranchingness() == 4 && block.getSecondarySuccessor() == nextLabel) {
                this.output.reverseBranch(1, this.addresses.getStart(succ));
                return;
            }
            this.output.add(new TargetInsn(Dops.GOTO, lastInsn.getPosition(), RegisterSpecList.EMPTY, this.addresses.getStart(succ)));
        }
    }

    private void pickOrder() {
        int i;
        BasicBlockList blocks = this.method.getBlocks();
        int sz = blocks.size();
        int maxLabel = blocks.getMaxLabel();
        int[] workSet = Bits.makeBitSet(maxLabel);
        int[] tracebackSet = Bits.makeBitSet(maxLabel);
        for (i = 0; i < sz; i++) {
            Bits.set(workSet, blocks.get(i).getLabel());
        }
        int[] order = new int[sz];
        int at = 0;
        int label = this.method.getFirstLabel();
        while (label != -1) {
            while (true) {
                IntList preds = this.method.labelToPredecessors(label);
                int psz = preds.size();
                i = 0;
                while (i < psz) {
                    int predLabel = preds.get(i);
                    if (Bits.get(tracebackSet, predLabel)) {
                        break;
                    } else if (Bits.get(workSet, predLabel) && blocks.labelToBlock(predLabel).getPrimarySuccessor() == label) {
                        label = predLabel;
                        Bits.set(tracebackSet, label);
                    } else {
                        i++;
                    }
                }
                break;
            }
            while (label != -1) {
                Bits.clear(workSet, label);
                Bits.clear(tracebackSet, label);
                order[at] = label;
                at++;
                BasicBlock one = blocks.labelToBlock(label);
                BasicBlock preferredBlock = blocks.preferredSuccessorOf(one);
                if (preferredBlock == null) {
                    break;
                }
                int preferred = preferredBlock.getLabel();
                int primary = one.getPrimarySuccessor();
                if (Bits.get(workSet, preferred)) {
                    label = preferred;
                } else if (primary == preferred || primary < 0 || !Bits.get(workSet, primary)) {
                    IntList successors = one.getSuccessors();
                    int ssz = successors.size();
                    label = -1;
                    for (i = 0; i < ssz; i++) {
                        int candidate = successors.get(i);
                        if (Bits.get(workSet, candidate)) {
                            label = candidate;
                            break;
                        }
                    }
                } else {
                    label = primary;
                }
            }
            label = Bits.findFirst(workSet, 0);
        }
        if (at != sz) {
            throw new RuntimeException("shouldn't happen");
        }
        this.order = order;
    }

    private static RegisterSpecList getRegs(Insn insn) {
        return getRegs(insn, insn.getResult());
    }

    private static RegisterSpecList getRegs(Insn insn, RegisterSpec resultReg) {
        RegisterSpecList regs = insn.getSources();
        if (insn.getOpcode().isCommutative() && regs.size() == 2 && resultReg.getReg() == regs.get(1).getReg()) {
            regs = RegisterSpecList.make(regs.get(1), regs.get(0));
        }
        return resultReg == null ? regs : regs.withFirst(resultReg);
    }
}
