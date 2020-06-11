package com.android.dx.rop.code;

import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.ToHuman;

public abstract class Insn implements ToHuman {
    private final Rop opcode;
    private final SourcePosition position;
    private final RegisterSpec result;
    private final RegisterSpecList sources;

    public interface Visitor {
        void visitFillArrayDataInsn(FillArrayDataInsn fillArrayDataInsn);

        void visitInvokePolymorphicInsn(InvokePolymorphicInsn invokePolymorphicInsn);

        void visitPlainCstInsn(PlainCstInsn plainCstInsn);

        void visitPlainInsn(PlainInsn plainInsn);

        void visitSwitchInsn(SwitchInsn switchInsn);

        void visitThrowingCstInsn(ThrowingCstInsn throwingCstInsn);

        void visitThrowingInsn(ThrowingInsn throwingInsn);
    }

    public static class BaseVisitor implements Visitor {
        public void visitPlainInsn(PlainInsn insn) {
        }

        public void visitPlainCstInsn(PlainCstInsn insn) {
        }

        public void visitSwitchInsn(SwitchInsn insn) {
        }

        public void visitThrowingCstInsn(ThrowingCstInsn insn) {
        }

        public void visitThrowingInsn(ThrowingInsn insn) {
        }

        public void visitFillArrayDataInsn(FillArrayDataInsn insn) {
        }

        public void visitInvokePolymorphicInsn(InvokePolymorphicInsn insn) {
        }
    }

    public abstract void accept(Visitor visitor);

    public abstract TypeList getCatches();

    public abstract Insn withAddedCatch(Type type);

    public abstract Insn withNewRegisters(RegisterSpec registerSpec, RegisterSpecList registerSpecList);

    public abstract Insn withRegisterOffset(int i);

    public Insn(Rop opcode, SourcePosition position, RegisterSpec result, RegisterSpecList sources) {
        if (opcode == null) {
            throw new NullPointerException("opcode == null");
        } else if (position == null) {
            throw new NullPointerException("position == null");
        } else if (sources == null) {
            throw new NullPointerException("sources == null");
        } else {
            this.opcode = opcode;
            this.position = position;
            this.result = result;
            this.sources = sources;
        }
    }

    public final boolean equals(Object other) {
        return this == other;
    }

    public final int hashCode() {
        return System.identityHashCode(this);
    }

    public String toString() {
        return toStringWithInline(getInlineString());
    }

    public String toHuman() {
        return toHumanWithInline(getInlineString());
    }

    public String getInlineString() {
        return null;
    }

    public final Rop getOpcode() {
        return this.opcode;
    }

    public final SourcePosition getPosition() {
        return this.position;
    }

    public final RegisterSpec getResult() {
        return this.result;
    }

    public final RegisterSpec getLocalAssignment() {
        RegisterSpec assignment;
        if (this.opcode.getOpcode() == 54) {
            assignment = this.sources.get(0);
        } else {
            assignment = this.result;
        }
        if (assignment == null) {
            return null;
        }
        if (assignment.getLocalItem() == null) {
            return null;
        }
        return assignment;
    }

    public final RegisterSpecList getSources() {
        return this.sources;
    }

    public final boolean canThrow() {
        return this.opcode.canThrow();
    }

    public Insn withSourceLiteral() {
        return this;
    }

    public Insn copy() {
        return withRegisterOffset(0);
    }

    private static boolean equalsHandleNulls(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public boolean contentEquals(Insn b) {
        if (this.opcode == b.getOpcode() && this.position.equals(b.getPosition()) && getClass() == b.getClass() && equalsHandleNulls(this.result, b.getResult()) && equalsHandleNulls(this.sources, b.getSources()) && StdTypeList.equalContents(getCatches(), b.getCatches())) {
            return true;
        }
        return false;
    }

    protected final String toStringWithInline(String extra) {
        StringBuilder sb = new StringBuilder(80);
        sb.append("Insn{");
        sb.append(this.position);
        sb.append(' ');
        sb.append(this.opcode);
        if (extra != null) {
            sb.append(' ');
            sb.append(extra);
        }
        sb.append(" :: ");
        if (this.result != null) {
            sb.append(this.result);
            sb.append(" <- ");
        }
        sb.append(this.sources);
        sb.append('}');
        return sb.toString();
    }

    protected final String toHumanWithInline(String extra) {
        StringBuilder sb = new StringBuilder(80);
        sb.append(this.position);
        sb.append(": ");
        sb.append(this.opcode.getNickname());
        if (extra != null) {
            sb.append("(");
            sb.append(extra);
            sb.append(")");
        }
        if (this.result == null) {
            sb.append(" .");
        } else {
            sb.append(" ");
            sb.append(this.result.toHuman());
        }
        sb.append(" <-");
        int sz = this.sources.size();
        if (sz == 0) {
            sb.append(" .");
        } else {
            for (int i = 0; i < sz; i++) {
                sb.append(" ");
                sb.append(this.sources.get(i).toHuman());
            }
        }
        return sb.toString();
    }
}
