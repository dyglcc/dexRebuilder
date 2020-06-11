package com.android.dx.cf.code;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.util.Hex;
import com.android.dx.util.MutabilityControl;

public final class ExecutionStack extends MutabilityControl {
    private final boolean[] local;
    private final TypeBearer[] stack;
    private int stackPtr;

    public ExecutionStack(int maxStack) {
        boolean z;
        if (maxStack != 0) {
            z = true;
        } else {
            z = false;
        }
        super(z);
        this.stack = new TypeBearer[maxStack];
        this.local = new boolean[maxStack];
        this.stackPtr = 0;
    }

    public ExecutionStack copy() {
        ExecutionStack result = new ExecutionStack(this.stack.length);
        System.arraycopy(this.stack, 0, result.stack, 0, this.stack.length);
        System.arraycopy(this.local, 0, result.local, 0, this.local.length);
        result.stackPtr = this.stackPtr;
        return result;
    }

    public void annotate(ExceptionWithContext ex) {
        int limit = this.stackPtr - 1;
        int i = 0;
        while (i <= limit) {
            ex.addContext("stack[" + (i == limit ? "top0" : Hex.u2(limit - i)) + "]: " + stackElementString(this.stack[i]));
            i++;
        }
    }

    public void makeInitialized(Type type) {
        if (this.stackPtr != 0) {
            throwIfImmutable();
            Type initializedType = type.getInitializedType();
            for (int i = 0; i < this.stackPtr; i++) {
                if (this.stack[i] == type) {
                    this.stack[i] = initializedType;
                }
            }
        }
    }

    public int getMaxStack() {
        return this.stack.length;
    }

    public int size() {
        return this.stackPtr;
    }

    public void clear() {
        throwIfImmutable();
        for (int i = 0; i < this.stackPtr; i++) {
            this.stack[i] = null;
            this.local[i] = false;
        }
        this.stackPtr = 0;
    }

    public void push(TypeBearer type) {
        throwIfImmutable();
        try {
            type = type.getFrameType();
            int category = type.getType().getCategory();
            if (this.stackPtr + category > this.stack.length) {
                throwSimException("overflow");
                return;
            }
            if (category == 2) {
                this.stack[this.stackPtr] = null;
                this.stackPtr++;
            }
            this.stack[this.stackPtr] = type;
            this.stackPtr++;
        } catch (NullPointerException e) {
            throw new NullPointerException("type == null");
        }
    }

    public void setLocal() {
        throwIfImmutable();
        this.local[this.stackPtr] = true;
    }

    public TypeBearer peek(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        } else if (n >= this.stackPtr) {
            return throwSimException("underflow");
        } else {
            return this.stack[(this.stackPtr - n) - 1];
        }
    }

    public boolean peekLocal(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n < 0");
        } else if (n < this.stackPtr) {
            return this.local[(this.stackPtr - n) - 1];
        } else {
            throw new SimException("stack: underflow");
        }
    }

    public Type peekType(int n) {
        return peek(n).getType();
    }

    public TypeBearer pop() {
        throwIfImmutable();
        TypeBearer result = peek(0);
        this.stack[this.stackPtr - 1] = null;
        this.local[this.stackPtr - 1] = false;
        this.stackPtr -= result.getType().getCategory();
        return result;
    }

    public void change(int n, TypeBearer type) {
        throwIfImmutable();
        try {
            type = type.getFrameType();
            int idx = (this.stackPtr - n) - 1;
            TypeBearer orig = this.stack[idx];
            if (orig == null || orig.getType().getCategory() != type.getType().getCategory()) {
                throwSimException("incompatible substitution: " + stackElementString(orig) + " -> " + stackElementString(type));
            }
            this.stack[idx] = type;
        } catch (NullPointerException e) {
            throw new NullPointerException("type == null");
        }
    }

    public ExecutionStack merge(ExecutionStack other) {
        try {
            return Merger.mergeStack(this, other);
        } catch (SimException ex) {
            ex.addContext("underlay stack:");
            annotate(ex);
            ex.addContext("overlay stack:");
            other.annotate(ex);
            throw ex;
        }
    }

    private static String stackElementString(TypeBearer type) {
        if (type == null) {
            return "<invalid>";
        }
        return type.toString();
    }

    private static TypeBearer throwSimException(String msg) {
        throw new SimException("stack: " + msg);
    }
}
