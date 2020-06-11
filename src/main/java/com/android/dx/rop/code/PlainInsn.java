package com.android.dx.rop.code;

import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.rop.type.TypeList;

public final class PlainInsn extends Insn {
    public PlainInsn(Rop opcode, SourcePosition position, RegisterSpec result, RegisterSpecList sources) {
        super(opcode, position, result, sources);
        switch (opcode.getBranchingness()) {
            case 5:
            case 6:
                throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
            default:
                if (result != null && opcode.getBranchingness() != 1) {
                    throw new IllegalArgumentException("can't mix branchingness with result");
                }
                return;
        }
    }

    public PlainInsn(Rop opcode, SourcePosition position, RegisterSpec result, RegisterSpec source) {
        this(opcode, position, result, RegisterSpecList.make(source));
    }

    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }

    public void accept(Visitor visitor) {
        visitor.visitPlainInsn(this);
    }

    public Insn withAddedCatch(Type type) {
        throw new UnsupportedOperationException("unsupported");
    }

    public Insn withRegisterOffset(int delta) {
        return new PlainInsn(getOpcode(), getPosition(), getResult().withOffset(delta), getSources().withOffset(delta));
    }

    public Insn withSourceLiteral() {
        RegisterSpecList sources = getSources();
        int szSources = sources.size();
        if (szSources == 0) {
            return this;
        }
        TypeBearer lastType = sources.get(szSources - 1).getTypeBearer();
        Constant cst;
        RegisterSpecList newSources;
        if (lastType.isConstant()) {
            cst = (Constant) lastType;
            newSources = sources.withoutLast();
            try {
                int opcode = getOpcode().getOpcode();
                if (opcode == 15 && (cst instanceof CstInteger)) {
                    opcode = 14;
                    cst = CstInteger.make(-((CstInteger) cst).getValue());
                }
                return new PlainCstInsn(Rops.ropFor(opcode, getResult(), newSources, cst), getPosition(), getResult(), newSources, cst);
            } catch (IllegalArgumentException e) {
                return this;
            }
        }
        TypeBearer firstType = sources.get(0).getTypeBearer();
        if (szSources != 2 || !firstType.isConstant()) {
            return this;
        }
        cst = (Constant) firstType;
        newSources = sources.withoutFirst();
        return new PlainCstInsn(Rops.ropFor(getOpcode().getOpcode(), getResult(), newSources, cst), getPosition(), getResult(), newSources, cst);
    }

    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new PlainInsn(getOpcode(), getPosition(), result, sources);
    }
}
