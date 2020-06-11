package com.android.dx.ssa;

import com.android.dx.rop.code.Exceptions;
import com.android.dx.rop.code.FillArrayDataInsn;
import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.PlainCstInsn;
import com.android.dx.rop.code.PlainInsn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.code.Rops;
import com.android.dx.rop.code.ThrowingCstInsn;
import com.android.dx.rop.code.ThrowingInsn;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstLiteralBits;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.TypedConstant;
import com.android.dx.rop.cst.Zeroes;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.ssa.SsaBasicBlock.Visitor;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class EscapeAnalysis {
    private final ArrayList<EscapeSet> latticeValues = new ArrayList();
    private final int regCount;
    private final SsaMethod ssaMeth;

    static class EscapeSet {
        ArrayList<EscapeSet> childSets = new ArrayList();
        EscapeState escape;
        ArrayList<EscapeSet> parentSets = new ArrayList();
        BitSet regSet;
        boolean replaceableArray = false;

        EscapeSet(int reg, int size, EscapeState escState) {
            this.regSet = new BitSet(size);
            this.regSet.set(reg);
            this.escape = escState;
        }
    }

    public enum EscapeState {
        TOP,
        NONE,
        METHOD,
        INTER,
        GLOBAL
    }

    private EscapeAnalysis(SsaMethod ssaMeth) {
        this.ssaMeth = ssaMeth;
        this.regCount = ssaMeth.getRegCount();
    }

    private int findSetIndex(RegisterSpec reg) {
        int i = 0;
        while (i < this.latticeValues.size() && !((EscapeSet) this.latticeValues.get(i)).regSet.get(reg.getReg())) {
            i++;
        }
        return i;
    }

    private SsaInsn getInsnForMove(SsaInsn moveInsn) {
        ArrayList<SsaInsn> predInsns = ((SsaBasicBlock) this.ssaMeth.getBlocks().get(moveInsn.getBlock().getPredecessors().nextSetBit(0))).getInsns();
        return (SsaInsn) predInsns.get(predInsns.size() - 1);
    }

    private SsaInsn getMoveForInsn(SsaInsn insn) {
        return (SsaInsn) ((SsaBasicBlock) this.ssaMeth.getBlocks().get(insn.getBlock().getSuccessors().nextSetBit(0))).getInsns().get(0);
    }

    private void addEdge(EscapeSet parentSet, EscapeSet childSet) {
        if (!childSet.parentSets.contains(parentSet)) {
            childSet.parentSets.add(parentSet);
        }
        if (!parentSet.childSets.contains(childSet)) {
            parentSet.childSets.add(childSet);
        }
    }

    private void replaceNode(EscapeSet newNode, EscapeSet oldNode) {
        Iterator it = oldNode.parentSets.iterator();
        while (it.hasNext()) {
            EscapeSet e = (EscapeSet) it.next();
            e.childSets.remove(oldNode);
            e.childSets.add(newNode);
            newNode.parentSets.add(e);
        }
        it = oldNode.childSets.iterator();
        while (it.hasNext()) {
            e = (EscapeSet) it.next();
            e.parentSets.remove(oldNode);
            e.parentSets.add(newNode);
            newNode.childSets.add(e);
        }
    }

    public static void process(SsaMethod ssaMethod) {
        new EscapeAnalysis(ssaMethod).run();
    }

    private void processInsn(SsaInsn insn) {
        int op = insn.getOpcode().getOpcode();
        RegisterSpec result = insn.getResult();
        if (op == 56 && result.getTypeBearer().getBasicType() == 9) {
            processRegister(result, processMoveResultPseudoInsn(insn));
        } else if (op == 3 && result.getTypeBearer().getBasicType() == 9) {
            escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
            this.latticeValues.add(escSet);
            processRegister(result, escSet);
        } else if (op == 55 && result.getTypeBearer().getBasicType() == 9) {
            escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
            this.latticeValues.add(escSet);
            processRegister(result, escSet);
        }
    }

    private EscapeSet processMoveResultPseudoInsn(SsaInsn insn) {
        EscapeSet escSet;
        RegisterSpec result = insn.getResult();
        SsaInsn prevSsaInsn = getInsnForMove(insn);
        switch (prevSsaInsn.getOpcode().getOpcode()) {
            case 5:
            case 40:
                escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
                break;
            case 38:
            case 43:
            case 45:
                RegisterSpec prevSource = prevSsaInsn.getSources().get(0);
                int setIndex = findSetIndex(prevSource);
                if (setIndex == this.latticeValues.size()) {
                    if (prevSource.getType() != Type.KNOWN_NULL) {
                        escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.GLOBAL);
                        break;
                    }
                    escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
                    break;
                }
                escSet = (EscapeSet) this.latticeValues.get(setIndex);
                escSet.regSet.set(result.getReg());
                return escSet;
            case 41:
            case 42:
                if (!prevSsaInsn.getSources().get(0).getTypeBearer().isConstant()) {
                    escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.GLOBAL);
                    break;
                }
                escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.NONE);
                escSet.replaceableArray = true;
                break;
            case 46:
                escSet = new EscapeSet(result.getReg(), this.regCount, EscapeState.GLOBAL);
                break;
            default:
                return null;
        }
        this.latticeValues.add(escSet);
        return escSet;
    }

    private void processRegister(RegisterSpec result, EscapeSet escSet) {
        ArrayList<RegisterSpec> regWorklist = new ArrayList();
        regWorklist.add(result);
        while (!regWorklist.isEmpty()) {
            RegisterSpec def = (RegisterSpec) regWorklist.remove(regWorklist.size() - 1);
            for (SsaInsn use : this.ssaMeth.getUseListForRegister(def.getReg())) {
                if (use.getOpcode() == null) {
                    processPhiUse(use, escSet, regWorklist);
                } else {
                    processUse(def, use, escSet, regWorklist);
                }
            }
        }
    }

    private void processPhiUse(SsaInsn use, EscapeSet escSet, ArrayList<RegisterSpec> regWorklist) {
        int setIndex = findSetIndex(use.getResult());
        if (setIndex != this.latticeValues.size()) {
            EscapeSet mergeSet = (EscapeSet) this.latticeValues.get(setIndex);
            if (mergeSet != escSet) {
                escSet.replaceableArray = false;
                escSet.regSet.or(mergeSet.regSet);
                if (escSet.escape.compareTo(mergeSet.escape) < 0) {
                    escSet.escape = mergeSet.escape;
                }
                replaceNode(escSet, mergeSet);
                this.latticeValues.remove(setIndex);
                return;
            }
            return;
        }
        escSet.regSet.set(use.getResult().getReg());
        regWorklist.add(use.getResult());
    }

    private void processUse(RegisterSpec def, SsaInsn use, EscapeSet escSet, ArrayList<RegisterSpec> regWorklist) {
        switch (use.getOpcode().getOpcode()) {
            case 2:
                escSet.regSet.set(use.getResult().getReg());
                regWorklist.add(use.getResult());
                return;
            case 7:
            case 8:
            case 43:
                if (escSet.escape.compareTo(EscapeState.METHOD) < 0) {
                    escSet.escape = EscapeState.METHOD;
                    return;
                }
                return;
            case 33:
            case 35:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
                escSet.escape = EscapeState.INTER;
                return;
            case 38:
                if (!use.getSources().get(1).getTypeBearer().isConstant()) {
                    escSet.replaceableArray = false;
                    return;
                }
                return;
            case 39:
                if (!use.getSources().get(2).getTypeBearer().isConstant()) {
                    escSet.replaceableArray = false;
                    break;
                }
                break;
            case 47:
                break;
            case 48:
                escSet.escape = EscapeState.GLOBAL;
                return;
            default:
                return;
        }
        if (use.getSources().get(0).getTypeBearer().getBasicType() == 9) {
            escSet.replaceableArray = false;
            RegisterSpecList sources = use.getSources();
            int setIndex;
            if (sources.get(0).getReg() == def.getReg()) {
                setIndex = findSetIndex(sources.get(1));
                if (setIndex != this.latticeValues.size()) {
                    EscapeSet parentSet = (EscapeSet) this.latticeValues.get(setIndex);
                    addEdge(parentSet, escSet);
                    if (escSet.escape.compareTo(parentSet.escape) < 0) {
                        escSet.escape = parentSet.escape;
                        return;
                    }
                    return;
                }
                return;
            }
            setIndex = findSetIndex(sources.get(0));
            if (setIndex != this.latticeValues.size()) {
                EscapeSet childSet = (EscapeSet) this.latticeValues.get(setIndex);
                addEdge(escSet, childSet);
                if (childSet.escape.compareTo(escSet.escape) < 0) {
                    childSet.escape = escSet.escape;
                }
            }
        }
    }

    private void scalarReplacement() {
        Iterator it = this.latticeValues.iterator();
        while (it.hasNext()) {
            EscapeSet escSet = (EscapeSet) it.next();
            if (escSet.replaceableArray && escSet.escape == EscapeState.NONE) {
                int e = escSet.regSet.nextSetBit(0);
                SsaInsn def = this.ssaMeth.getDefinitionForRegister(e);
                SsaInsn prev = getInsnForMove(def);
                int length = ((CstLiteralBits) prev.getSources().get(0).getTypeBearer()).getIntBits();
                ArrayList<RegisterSpec> newRegs = new ArrayList(length);
                HashSet<SsaInsn> deletedInsns = new HashSet();
                replaceDef(def, prev, length, newRegs);
                deletedInsns.add(prev);
                deletedInsns.add(def);
                for (SsaInsn use : this.ssaMeth.getUseListForRegister(e)) {
                    replaceUse(use, prev, newRegs, deletedInsns);
                    deletedInsns.add(use);
                }
                this.ssaMeth.deleteInsns(deletedInsns);
                this.ssaMeth.onInsnsChanged();
                SsaConverter.updateSsaMethod(this.ssaMeth, this.regCount);
                movePropagate();
            }
        }
    }

    private void replaceDef(SsaInsn def, SsaInsn prev, int length, ArrayList<RegisterSpec> newRegs) {
        Type resultType = def.getResult().getType();
        for (int i = 0; i < length; i++) {
            Constant newZero = Zeroes.zeroFor(resultType.getComponentType());
            RegisterSpec newReg = RegisterSpec.make(this.ssaMeth.makeNewSsaReg(), (TypedConstant) newZero);
            newRegs.add(newReg);
            insertPlainInsnBefore(def, RegisterSpecList.EMPTY, newReg, 5, newZero);
        }
    }

    private void replaceUse(SsaInsn use, SsaInsn prev, ArrayList<RegisterSpec> newRegs, HashSet<SsaInsn> deletedInsns) {
        int length = newRegs.size();
        SsaInsn next;
        RegisterSpecList sources;
        int index;
        RegisterSpec source;
        switch (use.getOpcode().getOpcode()) {
            case 34:
                TypeBearer lengthReg = prev.getSources().get(0).getTypeBearer();
                next = getMoveForInsn(use);
                insertPlainInsnBefore(next, RegisterSpecList.EMPTY, next.getResult(), 5, (Constant) lengthReg);
                deletedInsns.add(next);
                return;
            case 38:
                next = getMoveForInsn(use);
                sources = use.getSources();
                index = ((CstLiteralBits) sources.get(1).getTypeBearer()).getIntBits();
                if (index < length) {
                    source = (RegisterSpec) newRegs.get(index);
                    insertPlainInsnBefore(next, RegisterSpecList.make(source), source.withReg(next.getResult().getReg()), 2, null);
                } else {
                    insertExceptionThrow(next, sources.get(1), deletedInsns);
                    deletedInsns.add(next.getBlock().getInsns().get(2));
                }
                deletedInsns.add(next);
                return;
            case 39:
                sources = use.getSources();
                index = ((CstLiteralBits) sources.get(2).getTypeBearer()).getIntBits();
                if (index < length) {
                    source = sources.get(0);
                    RegisterSpec result = source.withReg(((RegisterSpec) newRegs.get(index)).getReg());
                    insertPlainInsnBefore(use, RegisterSpecList.make(source), result, 2, null);
                    newRegs.set(index, result.withSimpleType());
                    return;
                }
                insertExceptionThrow(use, sources.get(2), deletedInsns);
                return;
            case 57:
                ArrayList<Constant> constList = ((FillArrayDataInsn) use.getOriginalRopInsn()).getInitValues();
                for (int i = 0; i < length; i++) {
                    RegisterSpec newFill = RegisterSpec.make(((RegisterSpec) newRegs.get(i)).getReg(), (TypeBearer) constList.get(i));
                    insertPlainInsnBefore(use, RegisterSpecList.EMPTY, newFill, 5, (Constant) constList.get(i));
                    newRegs.set(i, newFill);
                }
                return;
            default:
                return;
        }
    }

    private void movePropagate() {
        for (int i = 0; i < this.ssaMeth.getRegCount(); i++) {
            SsaInsn insn = this.ssaMeth.getDefinitionForRegister(i);
            if (!(insn == null || insn.getOpcode() == null || insn.getOpcode().getOpcode() != 2)) {
                ArrayList<SsaInsn>[] useList = this.ssaMeth.getUseListCopy();
                final RegisterSpec source = insn.getSources().get(0);
                final RegisterSpec result = insn.getResult();
                if (source.getReg() >= this.regCount || result.getReg() >= this.regCount) {
                    RegisterMapper mapper = new RegisterMapper() {
                        public int getNewRegisterCount() {
                            return EscapeAnalysis.this.ssaMeth.getRegCount();
                        }

                        public RegisterSpec map(RegisterSpec registerSpec) {
                            if (registerSpec.getReg() == result.getReg()) {
                                return source;
                            }
                            return registerSpec;
                        }
                    };
                    Iterator it = useList[result.getReg()].iterator();
                    while (it.hasNext()) {
                        ((SsaInsn) it.next()).mapSourceRegisters(mapper);
                    }
                }
            }
        }
    }

    private void run() {
        this.ssaMeth.forEachBlockDepthFirstDom(new Visitor() {
            public void visitBlock(SsaBasicBlock block, SsaBasicBlock unused) {
                block.forEachInsn(new SsaInsn.Visitor() {
                    public void visitMoveInsn(NormalSsaInsn insn) {
                    }

                    public void visitPhiInsn(PhiInsn insn) {
                    }

                    public void visitNonMoveInsn(NormalSsaInsn insn) {
                        EscapeAnalysis.this.processInsn(insn);
                    }
                });
            }
        });
        Iterator it = this.latticeValues.iterator();
        while (it.hasNext()) {
            EscapeSet e = (EscapeSet) it.next();
            if (e.escape != EscapeState.NONE) {
                Iterator it2 = e.childSets.iterator();
                while (it2.hasNext()) {
                    EscapeSet field = (EscapeSet) it2.next();
                    if (e.escape.compareTo(field.escape) > 0) {
                        field.escape = e.escape;
                    }
                }
            }
        }
        scalarReplacement();
    }

    private void insertExceptionThrow(SsaInsn insn, RegisterSpec index, HashSet<SsaInsn> deletedInsns) {
        CstType exception = new CstType(Exceptions.TYPE_ArrayIndexOutOfBoundsException);
        insertThrowingInsnBefore(insn, RegisterSpecList.EMPTY, null, 40, exception);
        SsaBasicBlock currBlock = insn.getBlock();
        SsaBasicBlock newBlock = currBlock.insertNewSuccessor(currBlock.getPrimarySuccessor());
        SsaInsn newInsn = (SsaInsn) newBlock.getInsns().get(0);
        RegisterSpec newReg = RegisterSpec.make(this.ssaMeth.makeNewSsaReg(), exception);
        insertPlainInsnBefore(newInsn, RegisterSpecList.EMPTY, newReg, 56, null);
        SsaBasicBlock newBlock2 = newBlock.insertNewSuccessor(newBlock.getPrimarySuccessor());
        SsaInsn newInsn2 = (SsaInsn) newBlock2.getInsns().get(0);
        insertThrowingInsnBefore(newInsn2, RegisterSpecList.make(newReg, index), null, 52, new CstMethodRef(exception, new CstNat(new CstString("<init>"), new CstString("(I)V"))));
        deletedInsns.add(newInsn2);
        SsaBasicBlock newBlock3 = newBlock2.insertNewSuccessor(newBlock2.getPrimarySuccessor());
        SsaInsn newInsn3 = (SsaInsn) newBlock3.getInsns().get(0);
        insertThrowingInsnBefore(newInsn3, RegisterSpecList.make(newReg), null, 35, null);
        newBlock3.replaceSuccessor(newBlock3.getPrimarySuccessorIndex(), this.ssaMeth.getExitBlock().getIndex());
        deletedInsns.add(newInsn3);
    }

    private void insertPlainInsnBefore(SsaInsn insn, RegisterSpecList newSources, RegisterSpec newResult, int newOpcode, Constant cst) {
        Rop newRop;
        Insn newRopInsn;
        Insn originalRopInsn = insn.getOriginalRopInsn();
        if (newOpcode == 56) {
            newRop = Rops.opMoveResultPseudo(newResult.getType());
        } else {
            newRop = Rops.ropFor(newOpcode, newResult, newSources, cst);
        }
        if (cst == null) {
            newRopInsn = new PlainInsn(newRop, originalRopInsn.getPosition(), newResult, newSources);
        } else {
            newRopInsn = new PlainCstInsn(newRop, originalRopInsn.getPosition(), newResult, newSources, cst);
        }
        NormalSsaInsn newInsn = new NormalSsaInsn(newRopInsn, insn.getBlock());
        List<SsaInsn> insns = insn.getBlock().getInsns();
        insns.add(insns.lastIndexOf(insn), newInsn);
        this.ssaMeth.onInsnAdded(newInsn);
    }

    private void insertThrowingInsnBefore(SsaInsn insn, RegisterSpecList newSources, RegisterSpec newResult, int newOpcode, Constant cst) {
        Insn newRopInsn;
        Insn origRopInsn = insn.getOriginalRopInsn();
        Rop newRop = Rops.ropFor(newOpcode, newResult, newSources, cst);
        if (cst == null) {
            newRopInsn = new ThrowingInsn(newRop, origRopInsn.getPosition(), newSources, StdTypeList.EMPTY);
        } else {
            newRopInsn = new ThrowingCstInsn(newRop, origRopInsn.getPosition(), newSources, StdTypeList.EMPTY, cst);
        }
        NormalSsaInsn newInsn = new NormalSsaInsn(newRopInsn, insn.getBlock());
        List<SsaInsn> insns = insn.getBlock().getInsns();
        insns.add(insns.lastIndexOf(insn), newInsn);
        this.ssaMeth.onInsnAdded(newInsn);
    }
}
