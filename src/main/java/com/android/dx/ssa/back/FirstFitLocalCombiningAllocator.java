package com.android.dx.ssa.back;

import com.android.dx.rop.code.CstInsn;
import com.android.dx.rop.code.LocalItem;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.ssa.InterferenceRegisterMapper;
import com.android.dx.ssa.NormalSsaInsn;
import com.android.dx.ssa.Optimizer;
import com.android.dx.ssa.PhiInsn;
import com.android.dx.ssa.RegisterMapper;
import com.android.dx.ssa.SsaBasicBlock;
import com.android.dx.ssa.SsaInsn;
import com.android.dx.ssa.SsaInsn.Visitor;
import com.android.dx.ssa.SsaMethod;
import com.android.dx.util.IntIterator;
import com.android.dx.util.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class FirstFitLocalCombiningAllocator extends RegisterAllocator {
    private static final boolean DEBUG = false;
    private final ArrayList<NormalSsaInsn> invokeRangeInsns;
    private final Map<LocalItem, ArrayList<RegisterSpec>> localVariables;
    private final InterferenceRegisterMapper mapper;
    private final boolean minimizeRegisters;
    private final ArrayList<NormalSsaInsn> moveResultPseudoInsns;
    private final int paramRangeEnd;
    private final ArrayList<PhiInsn> phiInsns;
    private final BitSet reservedRopRegs = new BitSet(this.paramRangeEnd * 2);
    private final BitSet ssaRegsMapped;
    private final BitSet usedRopRegs;

    private enum Alignment {
        EVEN {
            int nextClearBit(BitSet bitSet, int startIdx) {
                int bitNumber = bitSet.nextClearBit(startIdx);
                while (!FirstFitLocalCombiningAllocator.isEven(bitNumber)) {
                    bitNumber = bitSet.nextClearBit(bitNumber + 1);
                }
                return bitNumber;
            }
        },
        ODD {
            int nextClearBit(BitSet bitSet, int startIdx) {
                int bitNumber = bitSet.nextClearBit(startIdx);
                while (FirstFitLocalCombiningAllocator.isEven(bitNumber)) {
                    bitNumber = bitSet.nextClearBit(bitNumber + 1);
                }
                return bitNumber;
            }
        },
        UNSPECIFIED {
            int nextClearBit(BitSet bitSet, int startIdx) {
                return bitSet.nextClearBit(startIdx);
            }
        };

        abstract int nextClearBit(BitSet bitSet, int i);
    }

    private static class Multiset {
        private final int[] count;
        private final int[] reg;
        private int size = 0;

        public Multiset(int maxSize) {
            this.reg = new int[maxSize];
            this.count = new int[maxSize];
        }

        public void add(int element) {
            for (int i = 0; i < this.size; i++) {
                if (this.reg[i] == element) {
                    int[] iArr = this.count;
                    iArr[i] = iArr[i] + 1;
                    return;
                }
            }
            this.reg[this.size] = element;
            this.count[this.size] = 1;
            this.size++;
        }

        public int getAndRemoveHighestCount() {
            int maxIndex = -1;
            int maxReg = -1;
            int maxCount = 0;
            for (int i = 0; i < this.size; i++) {
                if (maxCount < this.count[i]) {
                    maxIndex = i;
                    maxReg = this.reg[i];
                    maxCount = this.count[i];
                }
            }
            this.count[maxIndex] = 0;
            return maxReg;
        }

        public int getSize() {
            return this.size;
        }
    }

    public FirstFitLocalCombiningAllocator(SsaMethod ssaMeth, InterferenceGraph interference, boolean minimizeRegisters) {
        super(ssaMeth, interference);
        this.ssaRegsMapped = new BitSet(ssaMeth.getRegCount());
        this.mapper = new InterferenceRegisterMapper(interference, ssaMeth.getRegCount());
        this.minimizeRegisters = minimizeRegisters;
        this.paramRangeEnd = ssaMeth.getParamWidth();
        this.reservedRopRegs.set(0, this.paramRangeEnd);
        this.usedRopRegs = new BitSet(this.paramRangeEnd * 2);
        this.localVariables = new TreeMap();
        this.moveResultPseudoInsns = new ArrayList();
        this.invokeRangeInsns = new ArrayList();
        this.phiInsns = new ArrayList();
    }

    public boolean wantsParamsMovedHigh() {
        return true;
    }

    public RegisterMapper allocateRegisters() {
        analyzeInstructions();
        handleLocalAssociatedParams();
        handleUnassociatedParameters();
        handleInvokeRangeInsns();
        handleLocalAssociatedOther();
        handleCheckCastResults();
        handlePhiInsns();
        handleNormalUnassociated();
        return this.mapper;
    }

    private void printLocalVars() {
        System.out.println("Printing local vars");
        for (Entry<LocalItem, ArrayList<RegisterSpec>> e : this.localVariables.entrySet()) {
            StringBuilder regs = new StringBuilder();
            regs.append('{');
            regs.append(' ');
            Iterator it = ((ArrayList) e.getValue()).iterator();
            while (it.hasNext()) {
                RegisterSpec reg = (RegisterSpec) it.next();
                regs.append('v');
                regs.append(reg.getReg());
                regs.append(' ');
            }
            regs.append('}');
            System.out.printf("Local: %s Registers: %s\n", new Object[]{e.getKey(), regs});
        }
    }

    private void handleLocalAssociatedParams() {
        for (ArrayList<RegisterSpec> ssaRegs : this.localVariables.values()) {
            int sz = ssaRegs.size();
            int paramIndex = -1;
            int paramCategory = 0;
            for (int i = 0; i < sz; i++) {
                RegisterSpec ssaSpec = (RegisterSpec) ssaRegs.get(i);
                paramIndex = getParameterIndexForReg(ssaSpec.getReg());
                if (paramIndex >= 0) {
                    paramCategory = ssaSpec.getCategory();
                    addMapping(ssaSpec, paramIndex);
                    break;
                }
            }
            if (paramIndex >= 0) {
                tryMapRegs(ssaRegs, paramIndex, paramCategory, true);
            }
        }
    }

    private int getParameterIndexForReg(int ssaReg) {
        SsaInsn defInsn = this.ssaMeth.getDefinitionForRegister(ssaReg);
        if (defInsn == null) {
            return -1;
        }
        Rop opcode = defInsn.getOpcode();
        if (opcode == null || opcode.getOpcode() != 3) {
            return -1;
        }
        return ((CstInteger) ((CstInsn) defInsn.getOriginalRopInsn()).getConstant()).getValue();
    }

    private void handleLocalAssociatedOther() {
        for (ArrayList<RegisterSpec> specs : this.localVariables.values()) {
            int ropReg = this.paramRangeEnd;
            boolean done = false;
            do {
                int maxCategory = 1;
                int sz = specs.size();
                for (int i = 0; i < sz; i++) {
                    RegisterSpec ssaSpec = (RegisterSpec) specs.get(i);
                    int category = ssaSpec.getCategory();
                    if (!this.ssaRegsMapped.get(ssaSpec.getReg()) && category > maxCategory) {
                        maxCategory = category;
                    }
                }
                ropReg = findRopRegForLocal(ropReg, maxCategory);
                if (canMapRegs(specs, ropReg)) {
                    done = tryMapRegs(specs, ropReg, maxCategory, true);
                }
                ropReg++;
            } while (!done);
        }
    }

    private boolean tryMapRegs(ArrayList<RegisterSpec> specs, int ropReg, int maxAllowedCategory, boolean markReserved) {
        boolean remaining = false;
        Iterator it = specs.iterator();
        while (it.hasNext()) {
            RegisterSpec spec = (RegisterSpec) it.next();
            if (!this.ssaRegsMapped.get(spec.getReg())) {
                boolean succeeded = tryMapReg(spec, ropReg, maxAllowedCategory);
                if (!succeeded || remaining) {
                    remaining = true;
                } else {
                    remaining = false;
                }
                if (succeeded && markReserved) {
                    markReserved(ropReg, spec.getCategory());
                }
            }
        }
        if (remaining) {
            return false;
        }
        return true;
    }

    private boolean tryMapReg(RegisterSpec ssaSpec, int ropReg, int maxAllowedCategory) {
        if (ssaSpec.getCategory() > maxAllowedCategory || this.ssaRegsMapped.get(ssaSpec.getReg()) || !canMapReg(ssaSpec, ropReg)) {
            return false;
        }
        addMapping(ssaSpec, ropReg);
        return true;
    }

    private void markReserved(int ropReg, int category) {
        this.reservedRopRegs.set(ropReg, ropReg + category, true);
    }

    private boolean rangeContainsReserved(int ropRangeStart, int width) {
        for (int i = ropRangeStart; i < ropRangeStart + width; i++) {
            if (this.reservedRopRegs.get(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean isThisPointerReg(int startReg) {
        return startReg == 0 && !this.ssaMeth.isStatic();
    }

    private Alignment getAlignment(int regCategory) {
        Alignment alignment = Alignment.UNSPECIFIED;
        if (regCategory != 2) {
            return alignment;
        }
        if (isEven(this.paramRangeEnd)) {
            return Alignment.EVEN;
        }
        return Alignment.ODD;
    }

    private int findNextUnreservedRopReg(int startReg, int regCategory) {
        return findNextUnreservedRopReg(startReg, regCategory, getAlignment(regCategory));
    }

    private int findNextUnreservedRopReg(int startReg, int width, Alignment alignment) {
        int reg = alignment.nextClearBit(this.reservedRopRegs, startReg);
        while (true) {
            int i = 1;
            while (i < width && !this.reservedRopRegs.get(reg + i)) {
                i++;
            }
            if (i == width) {
                return reg;
            }
            reg = alignment.nextClearBit(this.reservedRopRegs, reg + i);
        }
    }

    private int findRopRegForLocal(int startReg, int category) {
        Alignment alignment = getAlignment(category);
        int reg = alignment.nextClearBit(this.usedRopRegs, startReg);
        while (true) {
            int i = 1;
            while (i < category && !this.usedRopRegs.get(reg + i)) {
                i++;
            }
            if (i == category) {
                return reg;
            }
            reg = alignment.nextClearBit(this.usedRopRegs, reg + i);
        }
    }

    private void handleUnassociatedParameters() {
        int szSsaRegs = this.ssaMeth.getRegCount();
        for (int ssaReg = 0; ssaReg < szSsaRegs; ssaReg++) {
            if (!this.ssaRegsMapped.get(ssaReg)) {
                int paramIndex = getParameterIndexForReg(ssaReg);
                RegisterSpec ssaSpec = getDefinitionSpecForSsaReg(ssaReg);
                if (paramIndex >= 0) {
                    addMapping(ssaSpec, paramIndex);
                }
            }
        }
    }

    private void handleInvokeRangeInsns() {
        Iterator it = this.invokeRangeInsns.iterator();
        while (it.hasNext()) {
            adjustAndMapSourceRangeRange((NormalSsaInsn) it.next());
        }
    }

    private void handleCheckCastResults() {
        Iterator it = this.moveResultPseudoInsns.iterator();
        while (it.hasNext()) {
            NormalSsaInsn insn = (NormalSsaInsn) it.next();
            RegisterSpec moveRegSpec = insn.getResult();
            int moveReg = moveRegSpec.getReg();
            BitSet predBlocks = insn.getBlock().getPredecessors();
            if (predBlocks.cardinality() == 1) {
                ArrayList<SsaInsn> insnList = ((SsaBasicBlock) this.ssaMeth.getBlocks().get(predBlocks.nextSetBit(0))).getInsns();
                SsaInsn checkCastInsn = (SsaInsn) insnList.get(insnList.size() - 1);
                if (checkCastInsn.getOpcode().getOpcode() == 43) {
                    RegisterSpec checkRegSpec = checkCastInsn.getSources().get(0);
                    int checkReg = checkRegSpec.getReg();
                    int category = checkRegSpec.getCategory();
                    boolean moveMapped = this.ssaRegsMapped.get(moveReg);
                    boolean checkMapped = this.ssaRegsMapped.get(checkReg);
                    if (((!checkMapped ? 1 : 0) & moveMapped) != 0) {
                        checkMapped = tryMapReg(checkRegSpec, this.mapper.oldToNew(moveReg), category);
                    }
                    if (((!moveMapped ? 1 : 0) & checkMapped) != 0) {
                        moveMapped = tryMapReg(moveRegSpec, this.mapper.oldToNew(checkReg), category);
                    }
                    if (!(moveMapped && checkMapped)) {
                        int ropReg = findNextUnreservedRopReg(this.paramRangeEnd, category);
                        ArrayList<RegisterSpec> ssaRegs = new ArrayList(2);
                        ssaRegs.add(moveRegSpec);
                        ssaRegs.add(checkRegSpec);
                        while (!tryMapRegs(ssaRegs, ropReg, category, false)) {
                            ropReg = findNextUnreservedRopReg(ropReg + 1, category);
                        }
                    }
                    boolean hasExceptionHandlers = checkCastInsn.getOriginalRopInsn().getCatches().size() != 0;
                    int moveRopReg = this.mapper.oldToNew(moveReg);
                    if (!(moveRopReg == this.mapper.oldToNew(checkReg) || hasExceptionHandlers)) {
                        ((NormalSsaInsn) checkCastInsn).changeOneSource(0, insertMoveBefore(checkCastInsn, checkRegSpec));
                        addMapping(checkCastInsn.getSources().get(0), moveRopReg);
                    }
                }
            }
        }
    }

    private void handlePhiInsns() {
        Iterator it = this.phiInsns.iterator();
        while (it.hasNext()) {
            processPhiInsn((PhiInsn) it.next());
        }
    }

    private void handleNormalUnassociated() {
        int szSsaRegs = this.ssaMeth.getRegCount();
        for (int ssaReg = 0; ssaReg < szSsaRegs; ssaReg++) {
            if (!this.ssaRegsMapped.get(ssaReg)) {
                RegisterSpec ssaSpec = getDefinitionSpecForSsaReg(ssaReg);
                if (ssaSpec != null) {
                    int category = ssaSpec.getCategory();
                    int ropReg = findNextUnreservedRopReg(this.paramRangeEnd, category);
                    while (!canMapReg(ssaSpec, ropReg)) {
                        ropReg = findNextUnreservedRopReg(ropReg + 1, category);
                    }
                    addMapping(ssaSpec, ropReg);
                }
            }
        }
    }

    private boolean canMapRegs(ArrayList<RegisterSpec> specs, int ropReg) {
        Iterator it = specs.iterator();
        while (it.hasNext()) {
            RegisterSpec spec = (RegisterSpec) it.next();
            if (!this.ssaRegsMapped.get(spec.getReg()) && !canMapReg(spec, ropReg)) {
                return false;
            }
        }
        return true;
    }

    private boolean canMapReg(RegisterSpec ssaSpec, int ropReg) {
        return (spansParamRange(ropReg, ssaSpec.getCategory()) || this.mapper.interferes(ssaSpec, ropReg)) ? false : true;
    }

    private boolean spansParamRange(int ssaReg, int category) {
        return ssaReg < this.paramRangeEnd && ssaReg + category > this.paramRangeEnd;
    }

    private void analyzeInstructions() {
        this.ssaMeth.forEachInsn(new Visitor() {
            public void visitMoveInsn(NormalSsaInsn insn) {
                processInsn(insn);
            }

            public void visitPhiInsn(PhiInsn insn) {
                processInsn(insn);
            }

            public void visitNonMoveInsn(NormalSsaInsn insn) {
                processInsn(insn);
            }

            private void processInsn(SsaInsn insn) {
                RegisterSpec assignment = insn.getLocalAssignment();
                if (assignment != null) {
                    LocalItem local = assignment.getLocalItem();
                    ArrayList<RegisterSpec> regList = (ArrayList) FirstFitLocalCombiningAllocator.this.localVariables.get(local);
                    if (regList == null) {
                        regList = new ArrayList();
                        FirstFitLocalCombiningAllocator.this.localVariables.put(local, regList);
                    }
                    regList.add(assignment);
                }
                if (insn instanceof NormalSsaInsn) {
                    if (insn.getOpcode().getOpcode() == 56) {
                        FirstFitLocalCombiningAllocator.this.moveResultPseudoInsns.add((NormalSsaInsn) insn);
                    } else if (Optimizer.getAdvice().requiresSourcesInOrder(insn.getOriginalRopInsn().getOpcode(), insn.getSources())) {
                        FirstFitLocalCombiningAllocator.this.invokeRangeInsns.add((NormalSsaInsn) insn);
                    }
                } else if (insn instanceof PhiInsn) {
                    FirstFitLocalCombiningAllocator.this.phiInsns.add((PhiInsn) insn);
                }
            }
        });
    }

    private void addMapping(RegisterSpec ssaSpec, int ropReg) {
        int ssaReg = ssaSpec.getReg();
        if (this.ssaRegsMapped.get(ssaReg) || !canMapReg(ssaSpec, ropReg)) {
            throw new RuntimeException("attempt to add invalid register mapping");
        }
        int category = ssaSpec.getCategory();
        this.mapper.addMapping(ssaSpec.getReg(), ropReg, category);
        this.ssaRegsMapped.set(ssaReg);
        this.usedRopRegs.set(ropReg, ropReg + category);
    }

    private void adjustAndMapSourceRangeRange(NormalSsaInsn insn) {
        int newRegStart = findRangeAndAdjust(insn);
        RegisterSpecList sources = insn.getSources();
        int szSources = sources.size();
        int nextRopReg = newRegStart;
        for (int i = 0; i < szSources; i++) {
            RegisterSpec source = sources.get(i);
            int sourceReg = source.getReg();
            int category = source.getCategory();
            int curRopReg = nextRopReg;
            nextRopReg += category;
            if (!this.ssaRegsMapped.get(sourceReg)) {
                LocalItem localItem = getLocalItemForReg(sourceReg);
                addMapping(source, curRopReg);
                if (localItem != null) {
                    markReserved(curRopReg, category);
                    ArrayList<RegisterSpec> similarRegisters = (ArrayList) this.localVariables.get(localItem);
                    int szSimilar = similarRegisters.size();
                    for (int j = 0; j < szSimilar; j++) {
                        RegisterSpec similarSpec = (RegisterSpec) similarRegisters.get(j);
                        if (-1 == sources.indexOfRegister(similarSpec.getReg())) {
                            tryMapReg(similarSpec, curRopReg, category);
                        }
                    }
                }
            }
        }
    }

    private int findRangeAndAdjust(NormalSsaInsn insn) {
        int i;
        RegisterSpecList sources = insn.getSources();
        int szSources = sources.size();
        int[] categoriesForIndex = new int[szSources];
        int rangeLength = 0;
        for (i = 0; i < szSources; i++) {
            categoriesForIndex[i] = sources.get(i).getCategory();
            rangeLength += categoriesForIndex[i];
        }
        int maxScore = Integer.MIN_VALUE;
        int resultRangeStart = -1;
        BitSet resultMovesRequired = null;
        int rangeStartOffset = 0;
        for (i = 0; i < szSources; i++) {
            int ssaCenterReg = sources.get(i).getReg();
            if (i != 0) {
                rangeStartOffset -= categoriesForIndex[i - 1];
            }
            if (this.ssaRegsMapped.get(ssaCenterReg)) {
                int rangeStart = this.mapper.oldToNew(ssaCenterReg) + rangeStartOffset;
                if (rangeStart >= 0 && !spansParamRange(rangeStart, rangeLength)) {
                    BitSet curMovesRequired = new BitSet(szSources);
                    int fitWidth = fitPlanForRange(rangeStart, insn, categoriesForIndex, curMovesRequired);
                    if (fitWidth >= 0) {
                        int score = fitWidth - curMovesRequired.cardinality();
                        if (score > maxScore) {
                            maxScore = score;
                            resultRangeStart = rangeStart;
                            resultMovesRequired = curMovesRequired;
                        }
                        if (fitWidth == rangeLength) {
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        if (resultRangeStart == -1) {
            resultMovesRequired = new BitSet(szSources);
            resultRangeStart = findAnyFittingRange(insn, rangeLength, categoriesForIndex, resultMovesRequired);
        }
        for (i = resultMovesRequired.nextSetBit(0); i >= 0; i = resultMovesRequired.nextSetBit(i + 1)) {
            insn.changeOneSource(i, insertMoveBefore(insn, sources.get(i)));
        }
        return resultRangeStart;
    }

    private int findAnyFittingRange(NormalSsaInsn insn, int rangeLength, int[] categoriesForIndex, BitSet outMovesRequired) {
        Alignment alignment = Alignment.UNSPECIFIED;
        int regNumber = 0;
        int p64bitsAligned = 0;
        int p64bitsNotAligned = 0;
        for (int category : categoriesForIndex) {
            if (category == 2) {
                if (isEven(regNumber)) {
                    p64bitsAligned++;
                } else {
                    p64bitsNotAligned++;
                }
                regNumber += 2;
            } else {
                regNumber++;
            }
        }
        if (p64bitsNotAligned > p64bitsAligned) {
            if (isEven(this.paramRangeEnd)) {
                alignment = Alignment.ODD;
            } else {
                alignment = Alignment.EVEN;
            }
        } else if (p64bitsAligned > 0) {
            if (isEven(this.paramRangeEnd)) {
                alignment = Alignment.EVEN;
            } else {
                alignment = Alignment.ODD;
            }
        }
        int rangeStart = this.paramRangeEnd;
        while (true) {
            rangeStart = findNextUnreservedRopReg(rangeStart, rangeLength, alignment);
            if (fitPlanForRange(rangeStart, insn, categoriesForIndex, outMovesRequired) >= 0) {
                return rangeStart;
            }
            rangeStart++;
            outMovesRequired.clear();
        }
    }

    private int fitPlanForRange(int ropReg, NormalSsaInsn insn, int[] categoriesForIndex, BitSet outMovesRequired) {
        RegisterSpecList sources = insn.getSources();
        int szSources = sources.size();
        int fitWidth = 0;
        RegisterSpecList liveOutSpecs = ssaSetToSpecs(insn.getBlock().getLiveOutRegs());
        BitSet seen = new BitSet(this.ssaMeth.getRegCount());
        for (int i = 0; i < szSources; i++) {
            RegisterSpec ssaSpec = sources.get(i);
            int ssaReg = ssaSpec.getReg();
            int category = categoriesForIndex[i];
            if (i != 0) {
                ropReg += categoriesForIndex[i - 1];
            }
            if (this.ssaRegsMapped.get(ssaReg) && this.mapper.oldToNew(ssaReg) == ropReg) {
                fitWidth += category;
            } else if (rangeContainsReserved(ropReg, category)) {
                return -1;
            } else {
                if (!this.ssaRegsMapped.get(ssaReg) && canMapReg(ssaSpec, ropReg) && !seen.get(ssaReg)) {
                    fitWidth += category;
                } else if (this.mapper.areAnyPinned(liveOutSpecs, ropReg, category) || this.mapper.areAnyPinned(sources, ropReg, category)) {
                    return -1;
                } else {
                    outMovesRequired.set(i);
                }
            }
            seen.set(ssaReg);
        }
        return fitWidth;
    }

    RegisterSpecList ssaSetToSpecs(IntSet ssaSet) {
        RegisterSpecList result = new RegisterSpecList(ssaSet.elements());
        IntIterator iter = ssaSet.iterator();
        int i = 0;
        while (iter.hasNext()) {
            int i2 = i + 1;
            result.set(i, getDefinitionSpecForSsaReg(iter.next()));
            i = i2;
        }
        return result;
    }

    private LocalItem getLocalItemForReg(int ssaReg) {
        for (Entry<LocalItem, ArrayList<RegisterSpec>> entry : this.localVariables.entrySet()) {
            Iterator it = ((ArrayList) entry.getValue()).iterator();
            while (it.hasNext()) {
                if (((RegisterSpec) it.next()).getReg() == ssaReg) {
                    return (LocalItem) entry.getKey();
                }
            }
        }
        return null;
    }

    private void processPhiInsn(PhiInsn insn) {
        int i;
        RegisterSpec result = insn.getResult();
        int resultReg = result.getReg();
        int category = result.getCategory();
        RegisterSpecList sources = insn.getSources();
        int sourcesSize = sources.size();
        ArrayList<RegisterSpec> ssaRegs = new ArrayList();
        Multiset mapSet = new Multiset(sourcesSize + 1);
        if (this.ssaRegsMapped.get(resultReg)) {
            mapSet.add(this.mapper.oldToNew(resultReg));
        } else {
            ssaRegs.add(result);
        }
        for (i = 0; i < sourcesSize; i++) {
            RegisterSpec sourceDef = this.ssaMeth.getDefinitionForRegister(sources.get(i).getReg()).getResult();
            int sourceReg = sourceDef.getReg();
            if (this.ssaRegsMapped.get(sourceReg)) {
                mapSet.add(this.mapper.oldToNew(sourceReg));
            } else {
                ssaRegs.add(sourceDef);
            }
        }
        for (i = 0; i < mapSet.getSize(); i++) {
            tryMapRegs(ssaRegs, mapSet.getAndRemoveHighestCount(), category, false);
        }
        int mapReg = findNextUnreservedRopReg(this.paramRangeEnd, category);
        while (!tryMapRegs(ssaRegs, mapReg, category, false)) {
            mapReg = findNextUnreservedRopReg(mapReg + 1, category);
        }
    }

    private static boolean isEven(int regNumger) {
        return (regNumger & 1) == 0;
    }
}
