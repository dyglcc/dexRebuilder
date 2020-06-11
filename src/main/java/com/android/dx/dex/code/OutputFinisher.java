package com.android.dx.dex.code;

import com.android.dex.DexException;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.code.DalvCode.AssignIndicesCallback;
import com.android.dx.rop.code.LocalItem;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.RegisterSpecSet;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstMemberRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.ssa.BasicRegisterMapper;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;

public final class OutputFinisher {
    private final DexOptions dexOptions;
    private boolean hasAnyLocalInfo = false;
    private boolean hasAnyPositionInfo = false;
    private ArrayList<DalvInsn> insns;
    private final int paramSize;
    private int reservedCount = -1;
    private int reservedParameterCount;
    private final int unreservedRegCount;

    public OutputFinisher(DexOptions dexOptions, int initialCapacity, int regCount, int paramSize) {
        this.dexOptions = dexOptions;
        this.unreservedRegCount = regCount;
        this.insns = new ArrayList(initialCapacity);
        this.paramSize = paramSize;
    }

    public boolean hasAnyPositionInfo() {
        return this.hasAnyPositionInfo;
    }

    public boolean hasAnyLocalInfo() {
        return this.hasAnyLocalInfo;
    }

    private static boolean hasLocalInfo(DalvInsn insn) {
        if (insn instanceof LocalSnapshot) {
            RegisterSpecSet specs = ((LocalSnapshot) insn).getLocals();
            int size = specs.size();
            for (int i = 0; i < size; i++) {
                if (hasLocalInfo(specs.get(i))) {
                    return true;
                }
            }
        } else if ((insn instanceof LocalStart) && hasLocalInfo(((LocalStart) insn).getLocal())) {
            return true;
        }
        return false;
    }

    private static boolean hasLocalInfo(RegisterSpec spec) {
        return (spec == null || spec.getLocalItem().getName() == null) ? false : true;
    }

    public HashSet<Constant> getAllConstants() {
        HashSet result = new HashSet(20);
        Iterator it = this.insns.iterator();
        while (it.hasNext()) {
            addConstants(result, (DalvInsn) it.next());
        }
        return result;
    }

    private static void addConstants(HashSet<Constant> result, DalvInsn insn) {
        if (insn instanceof CstInsn) {
            result.add(((CstInsn) insn).getConstant());
        } else if (insn instanceof MultiCstInsn) {
            MultiCstInsn m = (MultiCstInsn) insn;
            for (i = 0; i < m.getNumberOfConstants(); i++) {
                result.add(m.getConstant(i));
            }
        } else if (insn instanceof LocalSnapshot) {
            RegisterSpecSet specs = ((LocalSnapshot) insn).getLocals();
            int size = specs.size();
            for (i = 0; i < size; i++) {
                addConstants((HashSet) result, specs.get(i));
            }
        } else if (insn instanceof LocalStart) {
            addConstants((HashSet) result, ((LocalStart) insn).getLocal());
        }
    }

    private static void addConstants(HashSet<Constant> result, RegisterSpec spec) {
        if (spec != null) {
            LocalItem local = spec.getLocalItem();
            CstString name = local.getName();
            CstString signature = local.getSignature();
            Type type = spec.getType();
            if (type != Type.KNOWN_NULL) {
                result.add(CstType.intern(type));
            } else {
                result.add(CstType.intern(Type.OBJECT));
            }
            if (name != null) {
                result.add(name);
            }
            if (signature != null) {
                result.add(signature);
            }
        }
    }

    public void add(DalvInsn insn) {
        this.insns.add(insn);
        updateInfo(insn);
    }

    public void insert(int at, DalvInsn insn) {
        this.insns.add(at, insn);
        updateInfo(insn);
    }

    private void updateInfo(DalvInsn insn) {
        if (!this.hasAnyPositionInfo && insn.getPosition().getLine() >= 0) {
            this.hasAnyPositionInfo = true;
        }
        if (!this.hasAnyLocalInfo && hasLocalInfo(insn)) {
            this.hasAnyLocalInfo = true;
        }
    }

    public void reverseBranch(int which, CodeAddress newTarget) {
        int index = (this.insns.size() - which) - 1;
        try {
            this.insns.set(index, ((TargetInsn) this.insns.get(index)).withNewTargetAndReversed(newTarget));
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("too few instructions");
        } catch (ClassCastException e2) {
            throw new IllegalArgumentException("non-reversible instruction");
        }
    }

    public void assignIndices(AssignIndicesCallback callback) {
        Iterator it = this.insns.iterator();
        while (it.hasNext()) {
            DalvInsn insn = (DalvInsn) it.next();
            if (insn instanceof CstInsn) {
                assignIndices((CstInsn) insn, callback);
            } else if (insn instanceof MultiCstInsn) {
                assignIndices((MultiCstInsn) insn, callback);
            }
        }
    }

    private static void assignIndices(CstInsn insn, AssignIndicesCallback callback) {
        Constant cst = insn.getConstant();
        int index = callback.getIndex(cst);
        if (index >= 0) {
            insn.setIndex(index);
        }
        if (cst instanceof CstMemberRef) {
            index = callback.getIndex(((CstMemberRef) cst).getDefiningClass());
            if (index >= 0) {
                insn.setClassIndex(index);
            }
        }
    }

    private static void assignIndices(MultiCstInsn insn, AssignIndicesCallback callback) {
        for (int i = 0; i < insn.getNumberOfConstants(); i++) {
            Constant cst = insn.getConstant(i);
            insn.setIndex(i, callback.getIndex(cst));
            if (cst instanceof CstMemberRef) {
                insn.setClassIndex(callback.getIndex(((CstMemberRef) cst).getDefiningClass()));
            }
        }
    }

    public DalvInsnList finishProcessingAndGetList() {
        if (this.reservedCount >= 0) {
            throw new UnsupportedOperationException("already processed");
        }
        Dop[] opcodes = makeOpcodesArray();
        reserveRegisters(opcodes);
        if (this.dexOptions.ALIGN_64BIT_REGS_IN_OUTPUT_FINISHER) {
            align64bits(opcodes);
        }
        massageInstructions(opcodes);
        assignAddressesAndFixBranches();
        return DalvInsnList.makeImmutable(this.insns, (this.reservedCount + this.unreservedRegCount) + this.reservedParameterCount);
    }

    private Dop[] makeOpcodesArray() {
        int size = this.insns.size();
        Dop[] result = new Dop[size];
        for (int i = 0; i < size; i++) {
            result[i] = ((DalvInsn) this.insns.get(i)).getOpcode();
        }
        return result;
    }

    private boolean reserveRegisters(Dop[] opcodes) {
        boolean reservedCountExpanded = false;
        int oldReservedCount = this.reservedCount < 0 ? 0 : this.reservedCount;
        while (true) {
            int newReservedCount = calculateReservedCount(opcodes);
            if (oldReservedCount >= newReservedCount) {
                this.reservedCount = oldReservedCount;
                return reservedCountExpanded;
            }
            reservedCountExpanded = true;
            int reservedDifference = newReservedCount - oldReservedCount;
            int size = this.insns.size();
            for (int i = 0; i < size; i++) {
                DalvInsn insn = (DalvInsn) this.insns.get(i);
                if (!(insn instanceof CodeAddress)) {
                    this.insns.set(i, insn.withRegisterOffset(reservedDifference));
                }
            }
            oldReservedCount = newReservedCount;
        }
    }

    private int calculateReservedCount(Dop[] opcodes) {
        int size = this.insns.size();
        int newReservedCount = this.reservedCount;
        for (int i = 0; i < size; i++) {
            DalvInsn insn = (DalvInsn) this.insns.get(i);
            Dop originalOpcode = opcodes[i];
            Dop newOpcode = findOpcodeForInsn(insn, originalOpcode);
            if (newOpcode == null) {
                int reserve = insn.getMinimumRegisterRequirement(findExpandedOpcodeForInsn(insn).getFormat().compatibleRegs(insn));
                if (reserve > newReservedCount) {
                    newReservedCount = reserve;
                }
            } else if (originalOpcode == newOpcode) {
            }
            opcodes[i] = newOpcode;
        }
        return newReservedCount;
    }

    private Dop findOpcodeForInsn(DalvInsn insn, Dop guess) {
        while (guess != null && (!guess.getFormat().isCompatible(insn) || (this.dexOptions.forceJumbo && guess.getOpcode() == 26))) {
            guess = Dops.getNextOrNull(guess, this.dexOptions);
        }
        return guess;
    }

    private Dop findExpandedOpcodeForInsn(DalvInsn insn) {
        Dop result = findOpcodeForInsn(insn.getLowRegVersion(), insn.getOpcode());
        if (result != null) {
            return result;
        }
        throw new DexException("No expanded opcode for " + insn);
    }

    private void massageInstructions(Dop[] opcodes) {
        if (this.reservedCount == 0) {
            int size = this.insns.size();
            for (int i = 0; i < size; i++) {
                DalvInsn insn = (DalvInsn) this.insns.get(i);
                Dop originalOpcode = insn.getOpcode();
                Dop currentOpcode = opcodes[i];
                if (originalOpcode != currentOpcode) {
                    this.insns.set(i, insn.withOpcode(currentOpcode));
                }
            }
            return;
        }
        this.insns = performExpansion(opcodes);
    }

    private ArrayList<DalvInsn> performExpansion(Dop[] opcodes) {
        int size = this.insns.size();
        ArrayList<DalvInsn> result = new ArrayList(size * 2);
        ArrayList<CodeAddress> closelyBoundAddresses = new ArrayList();
        for (int i = 0; i < size; i++) {
            DalvInsn prefix;
            DalvInsn suffix;
            DalvInsn insn = (DalvInsn) this.insns.get(i);
            Dop originalOpcode = insn.getOpcode();
            Dop currentOpcode = opcodes[i];
            if (currentOpcode != null) {
                prefix = null;
                suffix = null;
            } else {
                currentOpcode = findExpandedOpcodeForInsn(insn);
                BitSet compatRegs = currentOpcode.getFormat().compatibleRegs(insn);
                prefix = insn.expandedPrefix(compatRegs);
                suffix = insn.expandedSuffix(compatRegs);
                insn = insn.expandedVersion(compatRegs);
            }
            if ((insn instanceof CodeAddress) && ((CodeAddress) insn).getBindsClosely()) {
                closelyBoundAddresses.add((CodeAddress) insn);
            } else {
                if (prefix != null) {
                    result.add(prefix);
                }
                if (!(insn instanceof ZeroSizeInsn) && closelyBoundAddresses.size() > 0) {
                    Iterator it = closelyBoundAddresses.iterator();
                    while (it.hasNext()) {
                        result.add((CodeAddress) it.next());
                    }
                    closelyBoundAddresses.clear();
                }
                if (currentOpcode != originalOpcode) {
                    insn = insn.withOpcode(currentOpcode);
                }
                result.add(insn);
                if (suffix != null) {
                    result.add(suffix);
                }
            }
        }
        return result;
    }

    private void assignAddressesAndFixBranches() {
        do {
            assignAddresses();
        } while (fixBranches());
    }

    private void assignAddresses() {
        int address = 0;
        int size = this.insns.size();
        for (int i = 0; i < size; i++) {
            DalvInsn insn = (DalvInsn) this.insns.get(i);
            insn.setAddress(address);
            address += insn.codeSize();
        }
    }

    private boolean fixBranches() {
        int size = this.insns.size();
        boolean anyFixed = false;
        int i = 0;
        while (i < size) {
            DalvInsn insn = (DalvInsn) this.insns.get(i);
            if (insn instanceof TargetInsn) {
                Dop opcode = insn.getOpcode();
                TargetInsn target = (TargetInsn) insn;
                if (opcode.getFormat().branchFits(target)) {
                    continue;
                } else {
                    if (opcode.getFamily() == 40) {
                        opcode = findOpcodeForInsn(insn, opcode);
                        if (opcode == null) {
                            throw new UnsupportedOperationException("method too long");
                        }
                        this.insns.set(i, insn.withOpcode(opcode));
                    } else {
                        try {
                            CodeAddress newTarget = (CodeAddress) this.insns.get(i + 1);
                            this.insns.set(i, new TargetInsn(Dops.GOTO, target.getPosition(), RegisterSpecList.EMPTY, target.getTarget()));
                            this.insns.add(i, target.withNewTargetAndReversed(newTarget));
                            size++;
                            i++;
                        } catch (IndexOutOfBoundsException e) {
                            throw new IllegalStateException("unpaired TargetInsn (dangling)");
                        } catch (ClassCastException e2) {
                            throw new IllegalStateException("unpaired TargetInsn");
                        }
                    }
                    anyFixed = true;
                }
            }
            i++;
        }
        return anyFixed;
    }

    private void align64bits(Dop[] opcodes) {
        do {
            int notAligned64bitRegAccess = 0;
            int aligned64bitRegAccess = 0;
            int notAligned64bitParamAccess = 0;
            int aligned64bitParamAccess = 0;
            int firstParameter = ((this.unreservedRegCount + this.reservedCount) + this.reservedParameterCount) - this.paramSize;
            Iterator it = this.insns.iterator();
            while (it.hasNext()) {
                RegisterSpecList regs = ((DalvInsn) it.next()).getRegisters();
                for (int usedRegIdx = 0; usedRegIdx < regs.size(); usedRegIdx++) {
                    RegisterSpec reg = regs.get(usedRegIdx);
                    if (reg.isCategory2()) {
                        boolean isParameter = reg.getReg() >= firstParameter;
                        if (reg.isEvenRegister()) {
                            if (isParameter) {
                                aligned64bitParamAccess++;
                            } else {
                                aligned64bitRegAccess++;
                            }
                        } else if (isParameter) {
                            notAligned64bitParamAccess++;
                        } else {
                            notAligned64bitRegAccess++;
                        }
                    }
                }
            }
            if (notAligned64bitParamAccess > aligned64bitParamAccess && notAligned64bitRegAccess > aligned64bitRegAccess) {
                addReservedRegisters(1);
            } else if (notAligned64bitParamAccess > aligned64bitParamAccess) {
                addReservedParameters(1);
            } else if (notAligned64bitRegAccess > aligned64bitRegAccess) {
                addReservedRegisters(1);
                if (this.paramSize != 0 && aligned64bitParamAccess > notAligned64bitParamAccess) {
                    addReservedParameters(1);
                }
            } else {
                return;
            }
        } while (reserveRegisters(opcodes));
    }

    private void addReservedParameters(int delta) {
        shiftParameters(delta);
        this.reservedParameterCount += delta;
    }

    private void addReservedRegisters(int delta) {
        shiftAllRegisters(delta);
        this.reservedCount += delta;
    }

    private void shiftAllRegisters(int delta) {
        int insnSize = this.insns.size();
        for (int i = 0; i < insnSize; i++) {
            DalvInsn insn = (DalvInsn) this.insns.get(i);
            if (!(insn instanceof CodeAddress)) {
                this.insns.set(i, insn.withRegisterOffset(delta));
            }
        }
    }

    private void shiftParameters(int delta) {
        int i;
        int insnSize = this.insns.size();
        int lastParameter = (this.unreservedRegCount + this.reservedCount) + this.reservedParameterCount;
        int firstParameter = lastParameter - this.paramSize;
        BasicRegisterMapper mapper = new BasicRegisterMapper(lastParameter);
        for (i = 0; i < lastParameter; i++) {
            if (i >= firstParameter) {
                mapper.addMapping(i, i + delta, 1);
            } else {
                mapper.addMapping(i, i, 1);
            }
        }
        for (i = 0; i < insnSize; i++) {
            DalvInsn insn = (DalvInsn) this.insns.get(i);
            if (!(insn instanceof CodeAddress)) {
                this.insns.set(i, insn.withMapper(mapper));
            }
        }
    }
}
