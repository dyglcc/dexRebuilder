package com.android.dx.dex.code;

import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.RegisterSpecList;
import com.android.dx.rop.code.SourcePosition;
import com.android.dx.rop.cst.Constant;
import com.android.dx.util.Hex;

public final class MultiCstInsn extends FixedSizeInsn {
    private static final int NOT_SET = -1;
    private int classIndex;
    private final Constant[] constants;
    private final int[] index;

    public MultiCstInsn(Dop opcode, SourcePosition position, RegisterSpecList registers, Constant[] constants) {
        super(opcode, position, registers);
        if (constants == null) {
            throw new NullPointerException("constants == null");
        }
        this.constants = constants;
        this.index = new int[constants.length];
        for (int i = 0; i < this.index.length; i++) {
            if (constants[i] == null) {
                throw new NullPointerException("constants[i] == null");
            }
            this.index[i] = -1;
        }
        this.classIndex = -1;
    }

    private MultiCstInsn(Dop opcode, SourcePosition position, RegisterSpecList registers, Constant[] constants, int[] index, int classIndex) {
        super(opcode, position, registers);
        this.constants = constants;
        this.index = index;
        this.classIndex = classIndex;
    }

    public DalvInsn withOpcode(Dop opcode) {
        return new MultiCstInsn(opcode, getPosition(), getRegisters(), this.constants, this.index, this.classIndex);
    }

    public DalvInsn withRegisters(RegisterSpecList registers) {
        return new MultiCstInsn(getOpcode(), getPosition(), registers, this.constants, this.index, this.classIndex);
    }

    public int getNumberOfConstants() {
        return this.constants.length;
    }

    public Constant getConstant(int position) {
        return this.constants[position];
    }

    public int getIndex(int position) {
        if (hasIndex(position)) {
            return this.index[position];
        }
        throw new IllegalStateException("index not yet set for constant " + position + " value = " + this.constants[position]);
    }

    public boolean hasIndex(int position) {
        return this.index[position] != -1;
    }

    public void setIndex(int position, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        } else if (hasIndex(position)) {
            throw new IllegalStateException("index already set");
        } else {
            this.index[position] = index;
        }
    }

    public int getClassIndex() {
        if (hasClassIndex()) {
            return this.classIndex;
        }
        throw new IllegalStateException("class index not yet set");
    }

    public boolean hasClassIndex() {
        return this.classIndex != -1;
    }

    public void setClassIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index < 0");
        } else if (hasClassIndex()) {
            throw new IllegalStateException("class index already set");
        } else {
            this.classIndex = index;
        }
    }

    protected String argString() {
        StringBuilder sb = new StringBuilder();
        for (Constant toHuman : this.constants) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(toHuman.toHuman());
        }
        return sb.toString();
    }

    public String cstString() {
        return argString();
    }

    public String cstComment() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.constants.length; i++) {
            if (!hasIndex(i)) {
                return "";
            }
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getConstant(i).typeName());
            sb.append('@');
            int currentIndex = getIndex(i);
            if (currentIndex < AccessFlags.ACC_CONSTRUCTOR) {
                sb.append(Hex.u2(currentIndex));
            } else {
                sb.append(Hex.u4(currentIndex));
            }
        }
        return sb.toString();
    }
}
