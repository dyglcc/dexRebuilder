package com.android.dx.ssa;

import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.LocalItem;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.Rop;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.util.Hex;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class PhiInsn extends SsaInsn {
    private final ArrayList<Operand> operands = new ArrayList();
    private final int ropResultReg;
    private RegisterSpecList sources;

    private static class Operand {
        public final int blockIndex;
        public RegisterSpec regSpec;
        public final int ropLabel;

        public Operand(RegisterSpec regSpec, int blockIndex, int ropLabel) {
            this.regSpec = regSpec;
            this.blockIndex = blockIndex;
            this.ropLabel = ropLabel;
        }
    }

    public interface Visitor {
        void visitPhiInsn(PhiInsn phiInsn);
    }

    public PhiInsn(RegisterSpec resultReg, SsaBasicBlock block) {
        super(resultReg, block);
        this.ropResultReg = resultReg.getReg();
    }

    public PhiInsn(int resultReg, SsaBasicBlock block) {
        super(RegisterSpec.make(resultReg, Type.VOID), block);
        this.ropResultReg = resultReg;
    }

    public PhiInsn clone() {
        throw new UnsupportedOperationException("can't clone phi");
    }

    public void updateSourcesToDefinitions(SsaMethod ssaMeth) {
        Iterator it = this.operands.iterator();
        while (it.hasNext()) {
            Operand o = (Operand) it.next();
            o.regSpec = o.regSpec.withType(ssaMeth.getDefinitionForRegister(o.regSpec.getReg()).getResult().getType());
        }
        this.sources = null;
    }

    public void changeResultType(TypeBearer type, LocalItem local) {
        setResult(RegisterSpec.makeLocalOptional(getResult().getReg(), type, local));
    }

    public int getRopResultReg() {
        return this.ropResultReg;
    }

    public void addPhiOperand(RegisterSpec registerSpec, SsaBasicBlock predBlock) {
        this.operands.add(new Operand(registerSpec, predBlock.getIndex(), predBlock.getRopLabel()));
        this.sources = null;
    }

    public void removePhiRegister(RegisterSpec registerSpec) {
        ArrayList<Operand> operandsToRemove = new ArrayList();
        Iterator it = this.operands.iterator();
        while (it.hasNext()) {
            Operand o = (Operand) it.next();
            if (o.regSpec.getReg() == registerSpec.getReg()) {
                operandsToRemove.add(o);
            }
        }
        this.operands.removeAll(operandsToRemove);
        this.sources = null;
    }

    public int predBlockIndexForSourcesIndex(int sourcesIndex) {
        return ((Operand) this.operands.get(sourcesIndex)).blockIndex;
    }

    public Rop getOpcode() {
        return null;
    }

    public Insn getOriginalRopInsn() {
        return null;
    }

    public boolean canThrow() {
        return false;
    }

    public RegisterSpecList getSources() {
        if (this.sources != null) {
            return this.sources;
        }
        if (this.operands.size() == 0) {
            return RegisterSpecList.EMPTY;
        }
        int szSources = this.operands.size();
        this.sources = new RegisterSpecList(szSources);
        for (int i = 0; i < szSources; i++) {
            this.sources.set(i, ((Operand) this.operands.get(i)).regSpec);
        }
        this.sources.setImmutable();
        return this.sources;
    }

    public boolean isRegASource(int reg) {
        Iterator it = this.operands.iterator();
        while (it.hasNext()) {
            if (((Operand) it.next()).regSpec.getReg() == reg) {
                return true;
            }
        }
        return false;
    }

    public boolean areAllOperandsEqual() {
        if (this.operands.size() == 0) {
            return true;
        }
        int firstReg = ((Operand) this.operands.get(0)).regSpec.getReg();
        Iterator it = this.operands.iterator();
        while (it.hasNext()) {
            if (firstReg != ((Operand) it.next()).regSpec.getReg()) {
                return false;
            }
        }
        return true;
    }

    public final void mapSourceRegisters(RegisterMapper mapper) {
        Iterator it = this.operands.iterator();
        while (it.hasNext()) {
            Operand o = (Operand) it.next();
            RegisterSpec old = o.regSpec;
            o.regSpec = mapper.map(old);
            if (old != o.regSpec) {
                getBlock().getParent().onSourceChanged(this, old, o.regSpec);
            }
        }
        this.sources = null;
    }

    public Insn toRopInsn() {
        throw new IllegalArgumentException("Cannot convert phi insns to rop form");
    }

    public List<SsaBasicBlock> predBlocksForReg(int reg, SsaMethod ssaMeth) {
        ArrayList<SsaBasicBlock> ret = new ArrayList();
        Iterator it = this.operands.iterator();
        while (it.hasNext()) {
            Operand o = (Operand) it.next();
            if (o.regSpec.getReg() == reg) {
                ret.add(ssaMeth.getBlocks().get(o.blockIndex));
            }
        }
        return ret;
    }

    public boolean isPhiOrMove() {
        return true;
    }

    public boolean hasSideEffect() {
        return Optimizer.getPreserveLocals() && getLocalAssignment() != null;
    }

    public void accept(com.android.dx.ssa.SsaInsn.Visitor v) {
        v.visitPhiInsn(this);
    }

    public String toHuman() {
        return toHumanWithInline(null);
    }

    protected final String toHumanWithInline(String extra) {
        StringBuilder sb = new StringBuilder(80);
        sb.append(SourcePosition.NO_INFO);
        sb.append(": phi");
        if (extra != null) {
            sb.append("(");
            sb.append(extra);
            sb.append(")");
        }
        RegisterSpec result = getResult();
        if (result == null) {
            sb.append(" .");
        } else {
            sb.append(" ");
            sb.append(result.toHuman());
        }
        sb.append(" <-");
        int sz = getSources().size();
        if (sz == 0) {
            sb.append(" .");
        } else {
            for (int i = 0; i < sz; i++) {
                sb.append(" ");
                sb.append(this.sources.get(i).toHuman() + "[b=" + Hex.u2(((Operand) this.operands.get(i)).ropLabel) + "]");
            }
        }
        return sb.toString();
    }
}
