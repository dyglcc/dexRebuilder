package com.android.dx.rop.code;

import com.android.dx.rop.code.Insn.Visitor;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import java.util.ArrayList;

public final class FillArrayDataInsn extends Insn {
    private final Constant arrayType;
    private final ArrayList<Constant> initValues;

    public FillArrayDataInsn(Rop opcode, SourcePosition position, RegisterSpecList sources, ArrayList<Constant> initValues, Constant cst) {
        super(opcode, position, null, sources);
        if (opcode.getBranchingness() != 1) {
            throw new IllegalArgumentException("opcode with invalid branchingness: " + opcode.getBranchingness());
        }
        this.initValues = initValues;
        this.arrayType = cst;
    }

    public TypeList getCatches() {
        return StdTypeList.EMPTY;
    }

    public ArrayList<Constant> getInitValues() {
        return this.initValues;
    }

    public Constant getConstant() {
        return this.arrayType;
    }

    public void accept(Visitor visitor) {
        visitor.visitFillArrayDataInsn(this);
    }

    public Insn withAddedCatch(Type type) {
        throw new UnsupportedOperationException("unsupported");
    }

    public Insn withRegisterOffset(int delta) {
        return new FillArrayDataInsn(getOpcode(), getPosition(), getSources().withOffset(delta), this.initValues, this.arrayType);
    }

    public Insn withNewRegisters(RegisterSpec result, RegisterSpecList sources) {
        return new FillArrayDataInsn(getOpcode(), getPosition(), sources, this.initValues, this.arrayType);
    }
}
