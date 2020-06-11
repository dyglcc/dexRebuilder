package com.android.dx.dex.code;

import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstString;
import com.android.dx.util.Hex;

public final class CstInsn extends FixedSizeInsn {
    private int classIndex;
    private final Constant constant;
    private int index;

    public CstInsn(Dop opcode, SourcePosition position, RegisterSpecList registers, Constant constant) {
        super(opcode, position, registers);
        if (constant == null) {
            throw new NullPointerException("constant == null");
        }
        this.constant = constant;
        this.index = -1;
        this.classIndex = -1;
    }

    public DalvInsn withOpcode(Dop opcode) {
        CstInsn result = new CstInsn(opcode, getPosition(), getRegisters(), this.constant);
        if (this.index >= 0) {
            result.setIndex(this.index);
        }
        if (this.classIndex >= 0) {
            result.setClassIndex(this.classIndex);
        }
        return result;
    }

    public DalvInsn withRegisters(RegisterSpecList registers) {
        CstInsn result = new CstInsn(getOpcode(), getPosition(), registers, this.constant);
        if (this.index >= 0) {
            result.setIndex(this.index);
        }
        if (this.classIndex >= 0) {
            result.setClassIndex(this.classIndex);
        }
        return result;
    }

    public Constant getConstant() {
        return this.constant;
    }

    public int getIndex() {
        if (this.index >= 0) {
            return this.index;
        }
        throw new IllegalStateException("index not yet set for " + this.constant);
    }

    public boolean hasIndex() {
        return this.index >= 0;
    }

    public void setIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        } else if (this.index >= 0) {
            throw new IllegalStateException("index already set");
        } else {
            this.index = index;
        }
    }

    public int getClassIndex() {
        if (this.classIndex >= 0) {
            return this.classIndex;
        }
        throw new IllegalStateException("class index not yet set");
    }

    public boolean hasClassIndex() {
        return this.classIndex >= 0;
    }

    public void setClassIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        } else if (this.classIndex >= 0) {
            throw new IllegalStateException("class index already set");
        } else {
            this.classIndex = index;
        }
    }

    protected String argString() {
        return this.constant.toHuman();
    }

    public String cstString() {
        if (this.constant instanceof CstString) {
            return ((CstString) this.constant).toQuoted();
        }
        return this.constant.toHuman();
    }

    public String cstComment() {
        if (!hasIndex()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(20);
        sb.append(getConstant().typeName());
        sb.append('@');
        if (this.index < AccessFlags.ACC_CONSTRUCTOR) {
            sb.append(Hex.u2(this.index));
        } else {
            sb.append(Hex.u4(this.index));
        }
        return sb.toString();
    }
}
