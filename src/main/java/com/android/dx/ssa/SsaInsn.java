package com.android.dx.ssa;

import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.LocalItem;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.util.ToHuman;

public abstract class SsaInsn implements ToHuman, Cloneable {
    private final SsaBasicBlock block;
    private RegisterSpec result;

    public interface Visitor {
        void visitMoveInsn(NormalSsaInsn normalSsaInsn);

        void visitNonMoveInsn(NormalSsaInsn normalSsaInsn);

        void visitPhiInsn(PhiInsn phiInsn);
    }

    public abstract void accept(Visitor visitor);

    public abstract boolean canThrow();

    public abstract Rop getOpcode();

    public abstract Insn getOriginalRopInsn();

    public abstract RegisterSpecList getSources();

    public abstract boolean hasSideEffect();

    public abstract boolean isPhiOrMove();

    public abstract void mapSourceRegisters(RegisterMapper registerMapper);

    public abstract Insn toRopInsn();

    protected SsaInsn(RegisterSpec result, SsaBasicBlock block) {
        if (block == null) {
            throw new NullPointerException("block == null");
        }
        this.block = block;
        this.result = result;
    }

    public static SsaInsn makeFromRop(Insn insn, SsaBasicBlock block) {
        return new NormalSsaInsn(insn, block);
    }

    public SsaInsn clone() {
        try {
            return (SsaInsn) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("unexpected", ex);
        }
    }

    public RegisterSpec getResult() {
        return this.result;
    }

    protected void setResult(RegisterSpec result) {
        if (result == null) {
            throw new NullPointerException("result == null");
        }
        this.result = result;
    }

    public SsaBasicBlock getBlock() {
        return this.block;
    }

    public boolean isResultReg(int reg) {
        return this.result != null && this.result.getReg() == reg;
    }

    public void changeResultReg(int reg) {
        if (this.result != null) {
            this.result = this.result.withReg(reg);
        }
    }

    public final void setResultLocal(LocalItem local) {
        if (local == this.result.getLocalItem()) {
            return;
        }
        if (local == null || !local.equals(this.result.getLocalItem())) {
            this.result = RegisterSpec.makeLocalOptional(this.result.getReg(), this.result.getType(), local);
        }
    }

    public final void mapRegisters(RegisterMapper mapper) {
        RegisterSpec oldResult = this.result;
        this.result = mapper.map(this.result);
        this.block.getParent().updateOneDefinition(this, oldResult);
        mapSourceRegisters(mapper);
    }

    public RegisterSpec getLocalAssignment() {
        if (this.result == null || this.result.getLocalItem() == null) {
            return null;
        }
        return this.result;
    }

    public boolean isRegASource(int reg) {
        return getSources().specForRegister(reg) != null;
    }

    public boolean isNormalMoveInsn() {
        return false;
    }

    public boolean isMoveException() {
        return false;
    }
}
