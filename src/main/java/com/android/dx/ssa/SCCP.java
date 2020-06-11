package com.android.dx.ssa;

import com.android.dx.rop.code.CstInsn;
import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.TypedConstant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

public class SCCP {
    private static final int CONSTANT = 1;
    private static final int TOP = 0;
    private static final int VARYING = 2;
    private final ArrayList<SsaInsn> branchWorklist;
    private final ArrayList<SsaBasicBlock> cfgPhiWorklist = new ArrayList();
    private final ArrayList<SsaBasicBlock> cfgWorklist = new ArrayList();
    private final BitSet executableBlocks;
    private final Constant[] latticeConstants = new Constant[this.regCount];
    private final int[] latticeValues = new int[this.regCount];
    private final int regCount;
    private final SsaMethod ssaMeth;
    private final ArrayList<SsaInsn> ssaWorklist;
    private final ArrayList<SsaInsn> varyingWorklist;

    private SCCP(SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
        this.regCount = ssaMeth.getRegCount();
        this.executableBlocks = new BitSet(ssaMeth.getBlocks().size());
        this.ssaWorklist = new ArrayList();
        this.varyingWorklist = new ArrayList();
        this.branchWorklist = new ArrayList();
        for (int i = 0; i < this.regCount; i++) {
            this.latticeValues[i] = 0;
            this.latticeConstants[i] = null;
        }
    }

    public static void process(SsaMethod ssaMethod) {
        new SCCP(ssaMethod).run();
    }

    private void addBlockToWorklist(SsaBasicBlock ssaBlock) {
        if (this.executableBlocks.get(ssaBlock.getIndex())) {
            this.cfgPhiWorklist.add(ssaBlock);
            return;
        }
        this.cfgWorklist.add(ssaBlock);
        this.executableBlocks.set(ssaBlock.getIndex());
    }

    private void addUsersToWorklist(int reg, int latticeValue) {
        if (latticeValue == 2) {
            for (SsaInsn insn : this.ssaMeth.getUseListForRegister(reg)) {
                this.varyingWorklist.add(insn);
            }
            return;
        }
        for (SsaInsn insn2 : this.ssaMeth.getUseListForRegister(reg)) {
            this.ssaWorklist.add(insn2);
        }
    }

    private boolean setLatticeValueTo(int reg, int value, Constant cst) {
        if (value != 1) {
            if (this.latticeValues[reg] == value) {
                return false;
            }
            this.latticeValues[reg] = value;
            return true;
        } else if (this.latticeValues[reg] == value && this.latticeConstants[reg].equals(cst)) {
            return false;
        } else {
            this.latticeValues[reg] = value;
            this.latticeConstants[reg] = cst;
            return true;
        }
    }

    private void simulatePhi(PhiInsn insn) {
        int phiResultReg = insn.getResult().getReg();
        if (this.latticeValues[phiResultReg] != 2) {
            RegisterSpecList sources = insn.getSources();
            int phiResultValue = 0;
            Constant phiConstant = null;
            int sourceSize = sources.size();
            for (int i = 0; i < sourceSize; i++) {
                int predBlockIndex = insn.predBlockIndexForSourcesIndex(i);
                int sourceReg = sources.get(i).getReg();
                int sourceRegValue = this.latticeValues[sourceReg];
                if (this.executableBlocks.get(predBlockIndex)) {
                    if (sourceRegValue != 1) {
                        phiResultValue = sourceRegValue;
                        break;
                    } else if (phiConstant == null) {
                        phiConstant = this.latticeConstants[sourceReg];
                        phiResultValue = 1;
                    } else if (!this.latticeConstants[sourceReg].equals(phiConstant)) {
                        phiResultValue = 2;
                        break;
                    }
                }
            }
            if (setLatticeValueTo(phiResultReg, phiResultValue, phiConstant)) {
                addUsersToWorklist(phiResultReg, phiResultValue);
            }
        }
    }

    private void simulateBlock(SsaBasicBlock block) {
        Iterator it = block.getInsns().iterator();
        while (it.hasNext()) {
            SsaInsn insn = (SsaInsn) it.next();
            if (insn instanceof PhiInsn) {
                simulatePhi((PhiInsn) insn);
            } else {
                simulateStmt(insn);
            }
        }
    }

    private void simulatePhiBlock(SsaBasicBlock block) {
        Iterator it = block.getInsns().iterator();
        while (it.hasNext()) {
            SsaInsn insn = (SsaInsn) it.next();
            if (insn instanceof PhiInsn) {
                simulatePhi((PhiInsn) insn);
            } else {
                return;
            }
        }
    }

    private static String latticeValName(int latticeVal) {
        switch (latticeVal) {
            case 0:
                return "TOP";
            case 1:
                return "CONSTANT";
            case 2:
                return "VARYING";
            default:
                return "UNKNOWN";
        }
    }

    private void simulateBranch(SsaInsn insn) {
        Rop opcode = insn.getOpcode();
        RegisterSpecList sources = insn.getSources();
        boolean constantBranch = false;
        boolean constantSuccessor = false;
        if (opcode.getBranchingness() == 4) {
            Constant cA = null;
            Constant cB = null;
            RegisterSpec specA = sources.get(0);
            int regA = specA.getReg();
            if (!this.ssaMeth.isRegALocal(specA) && this.latticeValues[regA] == 1) {
                cA = this.latticeConstants[regA];
            }
            if (sources.size() == 2) {
                RegisterSpec specB = sources.get(1);
                int regB = specB.getReg();
                if (!this.ssaMeth.isRegALocal(specB) && this.latticeValues[regB] == 1) {
                    cB = this.latticeConstants[regB];
                }
            }
            int vA;
            if (cA == null || sources.size() != 1) {
                if (!(cA == null || cB == null)) {
                    switch (((TypedConstant) cA).getBasicType()) {
                        case 6:
                            constantBranch = true;
                            vA = ((CstInteger) cA).getValue();
                            int vB = ((CstInteger) cB).getValue();
                            switch (opcode.getOpcode()) {
                                case 7:
                                    constantSuccessor = vA == vB;
                                    break;
                                case 8:
                                    constantSuccessor = vA != vB;
                                    break;
                                case 9:
                                    constantSuccessor = vA < vB;
                                    break;
                                case 10:
                                    constantSuccessor = vA >= vB;
                                    break;
                                case 11:
                                    constantSuccessor = vA <= vB;
                                    break;
                                case 12:
                                    constantSuccessor = vA > vB;
                                    break;
                                default:
                                    throw new RuntimeException("Unexpected op");
                            }
                        default:
                            break;
                    }
                }
            }
            switch (((TypedConstant) cA).getBasicType()) {
                case 6:
                    constantBranch = true;
                    vA = ((CstInteger) cA).getValue();
                    switch (opcode.getOpcode()) {
                        case 7:
                            constantSuccessor = vA == 0;
                            break;
                        case 8:
                            constantSuccessor = vA != 0;
                            break;
                        case 9:
                            constantSuccessor = vA < 0;
                            break;
                        case 10:
                            constantSuccessor = vA >= 0;
                            break;
                        case 11:
                            constantSuccessor = vA <= 0;
                            break;
                        case 12:
                            constantSuccessor = vA > 0;
                            break;
                        default:
                            throw new RuntimeException("Unexpected op");
                    }
            }
        }
        SsaBasicBlock block = insn.getBlock();
        if (constantBranch) {
            int successorBlock;
            if (constantSuccessor) {
                successorBlock = block.getSuccessorList().get(1);
            } else {
                successorBlock = block.getSuccessorList().get(0);
            }
            addBlockToWorklist((SsaBasicBlock) this.ssaMeth.getBlocks().get(successorBlock));
            this.branchWorklist.add(insn);
            return;
        }
        for (int i = 0; i < block.getSuccessorList().size(); i++) {
            addBlockToWorklist((SsaBasicBlock) this.ssaMeth.getBlocks().get(block.getSuccessorList().get(i)));
        }
    }

    private Constant simulateMath(SsaInsn insn, int resultType) {
        Constant cA;
        Constant cB;
        Insn ropInsn = insn.getOriginalRopInsn();
        int opcode = insn.getOpcode().getOpcode();
        RegisterSpecList sources = insn.getSources();
        int regA = sources.get(0).getReg();
        if (this.latticeValues[regA] != 1) {
            cA = null;
        } else {
            cA = this.latticeConstants[regA];
        }
        if (sources.size() == 1) {
            cB = ((CstInsn) ropInsn).getConstant();
        } else {
            int regB = sources.get(1).getReg();
            if (this.latticeValues[regB] != 1) {
                cB = null;
            } else {
                cB = this.latticeConstants[regB];
            }
        }
        if (cA == null || cB == null) {
            return null;
        }
        switch (resultType) {
            case 6:
                int vR;
                boolean skip = false;
                int vA = ((CstInteger) cA).getValue();
                int vB = ((CstInteger) cB).getValue();
                switch (opcode) {
                    case 14:
                        vR = vA + vB;
                        break;
                    case 15:
                        if (sources.size() != 1) {
                            vR = vA - vB;
                            break;
                        }
                        vR = vB - vA;
                        break;
                    case 16:
                        vR = vA * vB;
                        break;
                    case 17:
                        if (vB != 0) {
                            vR = vA / vB;
                            break;
                        }
                        skip = true;
                        vR = 0;
                        break;
                    case 18:
                        if (vB != 0) {
                            vR = vA % vB;
                            break;
                        }
                        skip = true;
                        vR = 0;
                        break;
                    case 20:
                        vR = vA & vB;
                        break;
                    case 21:
                        vR = vA | vB;
                        break;
                    case 22:
                        vR = vA ^ vB;
                        break;
                    case 23:
                        vR = vA << vB;
                        break;
                    case 24:
                        vR = vA >> vB;
                        break;
                    case 25:
                        vR = vA >>> vB;
                        break;
                    default:
                        throw new RuntimeException("Unexpected op");
                }
                if (skip) {
                    return null;
                }
                return CstInteger.make(vR);
            default:
                return null;
        }
    }

    private void simulateStmt(SsaInsn insn) {
        Insn ropInsn = insn.getOriginalRopInsn();
        if (ropInsn.getOpcode().getBranchingness() != 1 || ropInsn.getOpcode().isCallLike()) {
            simulateBranch(insn);
        }
        int opcode = insn.getOpcode().getOpcode();
        RegisterSpec result = insn.getResult();
        if (result == null) {
            if (opcode == 17 || opcode == 18) {
                result = ((SsaInsn) insn.getBlock().getPrimarySuccessor().getInsns().get(0)).getResult();
            } else {
                return;
            }
        }
        int resultReg = result.getReg();
        int resultValue = 2;
        Constant resultConstant = null;
        switch (opcode) {
            case 2:
                if (insn.getSources().size() == 1) {
                    int sourceReg = insn.getSources().get(0).getReg();
                    resultValue = this.latticeValues[sourceReg];
                    resultConstant = this.latticeConstants[sourceReg];
                    break;
                }
                break;
            case 5:
                resultValue = 1;
                resultConstant = ((CstInsn) ropInsn).getConstant();
                break;
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
                resultConstant = simulateMath(insn, result.getBasicType());
                if (resultConstant != null) {
                    resultValue = 1;
                    break;
                }
                break;
            case 56:
                if (this.latticeValues[resultReg] == 1) {
                    resultValue = this.latticeValues[resultReg];
                    resultConstant = this.latticeConstants[resultReg];
                    break;
                }
                break;
        }
        if (setLatticeValueTo(resultReg, resultValue, resultConstant)) {
            addUsersToWorklist(resultReg, resultValue);
        }
    }

    private void run() {
        addBlockToWorklist(this.ssaMeth.getEntryBlock());
        while (true) {
            if (this.cfgWorklist.isEmpty() && this.cfgPhiWorklist.isEmpty() && this.ssaWorklist.isEmpty() && this.varyingWorklist.isEmpty()) {
                replaceConstants();
                replaceBranches();
                return;
            }
            while (!this.cfgWorklist.isEmpty()) {
                simulateBlock((SsaBasicBlock) this.cfgWorklist.remove(this.cfgWorklist.size() - 1));
            }
            while (!this.cfgPhiWorklist.isEmpty()) {
                simulatePhiBlock((SsaBasicBlock) this.cfgPhiWorklist.remove(this.cfgPhiWorklist.size() - 1));
            }
            while (!this.varyingWorklist.isEmpty()) {
                SsaInsn insn = (SsaInsn) this.varyingWorklist.remove(this.varyingWorklist.size() - 1);
                if (this.executableBlocks.get(insn.getBlock().getIndex())) {
                    if (insn instanceof PhiInsn) {
                        simulatePhi((PhiInsn) insn);
                    } else {
                        simulateStmt(insn);
                    }
                }
            }
            while (!this.ssaWorklist.isEmpty()) {
                insn = (SsaInsn) this.ssaWorklist.remove(this.ssaWorklist.size() - 1);
                if (this.executableBlocks.get(insn.getBlock().getIndex())) {
                    if (insn instanceof PhiInsn) {
                        simulatePhi((PhiInsn) insn);
                    } else {
                        simulateStmt(insn);
                    }
                }
            }
        }
    }

    private void replaceConstants() {
        int reg = 0;
        while (reg < this.regCount) {
            if (this.latticeValues[reg] == 1 && (this.latticeConstants[reg] instanceof TypedConstant)) {
                SsaInsn defn = this.ssaMeth.getDefinitionForRegister(reg);
                if (!defn.getResult().getTypeBearer().isConstant()) {
                    defn.setResult(defn.getResult().withType((TypedConstant) this.latticeConstants[reg]));
                    for (SsaInsn insn : this.ssaMeth.getUseListForRegister(reg)) {
                        if (!insn.isPhiOrMove()) {
                            NormalSsaInsn nInsn = (NormalSsaInsn) insn;
                            RegisterSpecList sources = insn.getSources();
                            int index = sources.indexOfRegister(reg);
                            nInsn.changeOneSource(index, sources.get(index).withType((TypedConstant) this.latticeConstants[reg]));
                        }
                    }
                }
            }
            reg++;
        }
    }

    private void replaceBranches() {
        Iterator it = this.branchWorklist.iterator();
        while (it.hasNext()) {
            SsaInsn insn = (SsaInsn) it.next();
            int oldSuccessor = -1;
            SsaBasicBlock block = insn.getBlock();
            int successorSize = block.getSuccessorList().size();
            for (int i = 0; i < successorSize; i++) {
                int successorBlock = block.getSuccessorList().get(i);
                if (!this.executableBlocks.get(successorBlock)) {
                    oldSuccessor = successorBlock;
                }
            }
            if (successorSize == 2 && oldSuccessor != -1) {
                block.replaceLastInsn(new PlainInsn(Rops.GOTO, insn.getOriginalRopInsn().getPosition(), null, RegisterSpecList.EMPTY));
                block.removeSuccessor(oldSuccessor);
            }
        }
    }
}
