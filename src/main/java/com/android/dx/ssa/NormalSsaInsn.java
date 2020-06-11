package com.android.dx.ssa;

import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.ssa.SsaInsn.Visitor;

public final class NormalSsaInsn extends SsaInsn implements Cloneable {
    private Insn insn;

    NormalSsaInsn(Insn insn, SsaBasicBlock block) {
        super(insn.getResult(), block);
        this.insn = insn;
    }

    public final void mapSourceRegisters(RegisterMapper mapper) {
        RegisterSpecList oldSources = this.insn.getSources();
        RegisterSpecList newSources = mapper.map(oldSources);
        if (newSources != oldSources) {
            this.insn = this.insn.withNewRegisters(getResult(), newSources);
            getBlock().getParent().onSourcesChanged(this, oldSources);
        }
    }

    public final void changeOneSource(int index, RegisterSpec newSpec) {
        RegisterSpecList origSources = this.insn.getSources();
        int sz = origSources.size();
        RegisterSpecList newSources = new RegisterSpecList(sz);
        int i = 0;
        while (i < sz) {
            newSources.set(i, i == index ? newSpec : origSources.get(i));
            i++;
        }
        newSources.setImmutable();
        RegisterSpec origSpec = origSources.get(index);
        if (origSpec.getReg() != newSpec.getReg()) {
            getBlock().getParent().onSourceChanged(this, origSpec, newSpec);
        }
        this.insn = this.insn.withNewRegisters(getResult(), newSources);
    }

    public final void setNewSources(RegisterSpecList newSources) {
        if (this.insn.getSources().size() != newSources.size()) {
            throw new RuntimeException("Sources counts don't match");
        }
        this.insn = this.insn.withNewRegisters(getResult(), newSources);
    }

    public NormalSsaInsn clone() {
        return (NormalSsaInsn) super.clone();
    }

    public RegisterSpecList getSources() {
        return this.insn.getSources();
    }

    public String toHuman() {
        return toRopInsn().toHuman();
    }

    public Insn toRopInsn() {
        return this.insn.withNewRegisters(getResult(), this.insn.getSources());
    }

    public Rop getOpcode() {
        return this.insn.getOpcode();
    }

    public Insn getOriginalRopInsn() {
        return this.insn;
    }

    public RegisterSpec getLocalAssignment() {
        RegisterSpec assignment;
        if (this.insn.getOpcode().getOpcode() == 54) {
            assignment = this.insn.getSources().get(0);
        } else {
            assignment = getResult();
        }
        if (assignment == null) {
            return null;
        }
        if (assignment.getLocalItem() == null) {
            return null;
        }
        return assignment;
    }

    public void upgradeToLiteral() {
        RegisterSpecList oldSources = this.insn.getSources();
        this.insn = this.insn.withSourceLiteral();
        getBlock().getParent().onSourcesChanged(this, oldSources);
    }

    public boolean isNormalMoveInsn() {
        return this.insn.getOpcode().getOpcode() == 2;
    }

    public boolean isMoveException() {
        return this.insn.getOpcode().getOpcode() == 4;
    }

    public boolean canThrow() {
        return this.insn.canThrow();
    }

    public void accept(Visitor v) {
        if (isNormalMoveInsn()) {
            v.visitMoveInsn(this);
        } else {
            v.visitNonMoveInsn(this);
        }
    }

    public boolean isPhiOrMove() {
        return isNormalMoveInsn();
    }

    public boolean hasSideEffect() {
        Rop opcode = getOpcode();
        if (opcode.getBranchingness() != 1) {
            return true;
        }
        boolean hasLocalSideEffect = Optimizer.getPreserveLocals() && getLocalAssignment() != null;
        switch (opcode.getOpcode()) {
            case 2:
            case 5:
            case 55:
                return hasLocalSideEffect;
            default:
                return true;
        }
    }
}
