package com.android.dx.cf.code;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.util.IntList;

public final class Frame {
    private final LocalsArray locals;
    private final ExecutionStack stack;
    private final IntList subroutines;

    private Frame(LocalsArray locals, ExecutionStack stack) {
        this(locals, stack, IntList.EMPTY);
    }

    private Frame(LocalsArray locals, ExecutionStack stack, IntList subroutines) {
        if (locals == null) {
            throw new NullPointerException("locals == null");
        } else if (stack == null) {
            throw new NullPointerException("stack == null");
        } else {
            subroutines.throwIfMutable();
            this.locals = locals;
            this.stack = stack;
            this.subroutines = subroutines;
        }
    }

    public Frame(int maxLocals, int maxStack) {
        this(new OneLocalsArray(maxLocals), new ExecutionStack(maxStack));
    }

    public Frame copy() {
        return new Frame(this.locals.copy(), this.stack.copy(), this.subroutines);
    }

    public void setImmutable() {
        this.locals.setImmutable();
        this.stack.setImmutable();
    }

    public void makeInitialized(Type type) {
        this.locals.makeInitialized(type);
        this.stack.makeInitialized(type);
    }

    public LocalsArray getLocals() {
        return this.locals;
    }

    public ExecutionStack getStack() {
        return this.stack;
    }

    public IntList getSubroutines() {
        return this.subroutines;
    }

    public void initializeWithParameters(StdTypeList params) {
        int at = 0;
        int sz = params.size();
        for (int i = 0; i < sz; i++) {
            Type one = params.get(i);
            this.locals.set(at, one);
            at += one.getCategory();
        }
    }

    public Frame subFrameForLabel(int startLabel, int subLabel) {
        LocalsArray subLocals = null;
        if (this.locals instanceof LocalsArraySet) {
            subLocals = ((LocalsArraySet) this.locals).subArrayForLabel(subLabel);
        }
        try {
            IntList newSubroutines = this.subroutines.mutableCopy();
            if (newSubroutines.pop() != startLabel) {
                throw new RuntimeException("returning from invalid subroutine");
            }
            newSubroutines.setImmutable();
            if (subLocals == null) {
                return null;
            }
            return new Frame(subLocals, this.stack, newSubroutines);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("returning from invalid subroutine");
        } catch (NullPointerException e2) {
            throw new NullPointerException("can't return from non-subroutine");
        }
    }

    public Frame mergeWith(Frame other) {
        LocalsArray resultLocals = getLocals().merge(other.getLocals());
        ExecutionStack resultStack = getStack().merge(other.getStack());
        IntList resultSubroutines = mergeSubroutineLists(other.subroutines);
        resultLocals = adjustLocalsForSubroutines(resultLocals, resultSubroutines);
        if (resultLocals == getLocals() && resultStack == getStack() && this.subroutines == resultSubroutines) {
            return this;
        }
        this(resultLocals, resultStack, resultSubroutines);
        return this;
    }

    private IntList mergeSubroutineLists(IntList otherSubroutines) {
        if (this.subroutines.equals(otherSubroutines)) {
            return this.subroutines;
        }
        IntList resultSubroutines = new IntList();
        int szSubroutines = this.subroutines.size();
        int szOthers = otherSubroutines.size();
        int i = 0;
        while (i < szSubroutines && i < szOthers && this.subroutines.get(i) == otherSubroutines.get(i)) {
            resultSubroutines.add(i);
            i++;
        }
        resultSubroutines.setImmutable();
        return resultSubroutines;
    }

    private static LocalsArray adjustLocalsForSubroutines(LocalsArray locals, IntList subroutines) {
        if (!(locals instanceof LocalsArraySet)) {
            return locals;
        }
        LocalsArray laSet = (LocalsArraySet) locals;
        return subroutines.size() == 0 ? laSet.getPrimary() : laSet;
    }

    public Frame mergeWithSubroutineCaller(Frame other, int subLabel, int predLabel) {
        LocalsArray resultLocals = getLocals().mergeWithSubroutineCaller(other.getLocals(), predLabel);
        ExecutionStack resultStack = getStack().merge(other.getStack());
        IntList newOtherSubroutines = other.subroutines.mutableCopy();
        newOtherSubroutines.add(subLabel);
        newOtherSubroutines.setImmutable();
        if (resultLocals == getLocals() && resultStack == getStack() && this.subroutines.equals(newOtherSubroutines)) {
            return this;
        }
        IntList resultSubroutines;
        if (this.subroutines.equals(newOtherSubroutines)) {
            resultSubroutines = this.subroutines;
        } else {
            IntList nonResultSubroutines;
            if (this.subroutines.size() > newOtherSubroutines.size()) {
                resultSubroutines = this.subroutines;
                nonResultSubroutines = newOtherSubroutines;
            } else {
                resultSubroutines = newOtherSubroutines;
                nonResultSubroutines = this.subroutines;
            }
            int szResult = resultSubroutines.size();
            int szNonResult = nonResultSubroutines.size();
            for (int i = szNonResult - 1; i >= 0; i--) {
                if (nonResultSubroutines.get(i) != resultSubroutines.get((szResult - szNonResult) + i)) {
                    throw new RuntimeException("Incompatible merged subroutines");
                }
            }
        }
        this(resultLocals, resultStack, resultSubroutines);
        return this;
    }

    public Frame makeNewSubroutineStartFrame(int subLabel, int callerLabel) {
        this.subroutines.mutableCopy().add(subLabel);
        return new Frame(this.locals.getPrimary(), this.stack, IntList.makeImmutable(subLabel)).mergeWithSubroutineCaller(this, subLabel, callerLabel);
    }

    public Frame makeExceptionHandlerStartFrame(CstType exceptionClass) {
        ExecutionStack newStack = getStack().copy();
        newStack.clear();
        newStack.push(exceptionClass);
        return new Frame(getLocals(), newStack, this.subroutines);
    }

    public void annotate(ExceptionWithContext ex) {
        this.locals.annotate(ex);
        this.stack.annotate(ex);
    }
}
