package com.android.dx.rop.code;

import com.android.dx.util.MutabilityControl;

public final class RegisterSpecSet extends MutabilityControl {
    public static final RegisterSpecSet EMPTY = new RegisterSpecSet(0);
    private int size;
    private final RegisterSpec[] specs;

    public RegisterSpecSet(int maxSize) {
        boolean z;
        if (maxSize != 0) {
            z = true;
        } else {
            z = false;
        }
        super(z);
        this.specs = new RegisterSpec[maxSize];
        this.size = 0;
    }

    public boolean equals(Object other) {
        if (!(other instanceof RegisterSpecSet)) {
            return false;
        }
        RegisterSpecSet otherSet = (RegisterSpecSet) other;
        RegisterSpec[] otherSpecs = otherSet.specs;
        int len = this.specs.length;
        if (len != otherSpecs.length || size() != otherSet.size()) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            RegisterSpec s1 = this.specs[i];
            RegisterSpec s2 = otherSpecs[i];
            if (s1 != s2) {
                if (s1 == null) {
                    return false;
                }
                if (!s1.equals(s2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        int hash = 0;
        for (RegisterSpec spec : this.specs) {
            hash = (hash * 31) + (spec == null ? 0 : spec.hashCode());
        }
        return hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(len * 25);
        sb.append('{');
        boolean any = false;
        for (RegisterSpec spec : this.specs) {
            if (spec != null) {
                if (any) {
                    sb.append(", ");
                } else {
                    any = true;
                }
                sb.append(spec);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    public int getMaxSize() {
        return this.specs.length;
    }

    public int size() {
        int result = this.size;
        if (result < 0) {
            result = 0;
            for (RegisterSpec registerSpec : this.specs) {
                if (registerSpec != null) {
                    result++;
                }
            }
            this.size = result;
        }
        return result;
    }

    public RegisterSpec get(int reg) {
        try {
            return this.specs[reg];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("bogus reg");
        }
    }

    public RegisterSpec get(RegisterSpec spec) {
        return get(spec.getReg());
    }

    public RegisterSpec findMatchingLocal(RegisterSpec spec) {
        for (RegisterSpec s : this.specs) {
            if (s != null && spec.matchesVariable(s)) {
                return s;
            }
        }
        return null;
    }

    public RegisterSpec localItemToSpec(LocalItem local) {
        for (RegisterSpec spec : this.specs) {
            if (spec != null && local.equals(spec.getLocalItem())) {
                return spec;
            }
        }
        return null;
    }

    public void remove(RegisterSpec toRemove) {
        try {
            this.specs[toRemove.getReg()] = null;
            this.size = -1;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("bogus reg");
        }
    }

    public void put(RegisterSpec spec) {
        throwIfImmutable();
        if (spec == null) {
            throw new NullPointerException("spec == null");
        }
        this.size = -1;
        try {
            int reg = spec.getReg();
            this.specs[reg] = spec;
            if (reg > 0) {
                int prevReg = reg - 1;
                RegisterSpec prevSpec = this.specs[prevReg];
                if (prevSpec != null && prevSpec.getCategory() == 2) {
                    this.specs[prevReg] = null;
                }
            }
            if (spec.getCategory() == 2) {
                this.specs[reg + 1] = null;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("spec.getReg() out of range");
        }
    }

    public void putAll(RegisterSpecSet set) {
        int max = set.getMaxSize();
        for (int i = 0; i < max; i++) {
            RegisterSpec spec = set.get(i);
            if (spec != null) {
                put(spec);
            }
        }
    }

    public void intersect(RegisterSpecSet other, boolean localPrimary) {
        int i;
        throwIfImmutable();
        RegisterSpec[] otherSpecs = other.specs;
        int thisLen = this.specs.length;
        int len = Math.min(thisLen, otherSpecs.length);
        this.size = -1;
        for (i = 0; i < len; i++) {
            RegisterSpec spec = this.specs[i];
            if (spec != null) {
                RegisterSpec intersection = spec.intersect(otherSpecs[i], localPrimary);
                if (intersection != spec) {
                    this.specs[i] = intersection;
                }
            }
        }
        for (i = len; i < thisLen; i++) {
            this.specs[i] = null;
        }
    }

    public RegisterSpecSet withOffset(int delta) {
        RegisterSpecSet result = new RegisterSpecSet(len + delta);
        for (RegisterSpec spec : this.specs) {
            if (spec != null) {
                result.put(spec.withOffset(delta));
            }
        }
        result.size = this.size;
        if (isImmutable()) {
            result.setImmutable();
        }
        return result;
    }

    public RegisterSpecSet mutableCopy() {
        RegisterSpecSet copy = new RegisterSpecSet(len);
        for (RegisterSpec spec : this.specs) {
            if (spec != null) {
                copy.put(spec);
            }
        }
        copy.size = this.size;
        return copy;
    }
}
