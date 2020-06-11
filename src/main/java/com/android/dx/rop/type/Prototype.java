package com.android.dx.rop.type;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Prototype implements Comparable<Prototype> {
    private static final ConcurrentMap<String, Prototype> internTable = new ConcurrentHashMap(10000, 0.75f);
    private final String descriptor;
    private StdTypeList parameterFrameTypes;
    private final StdTypeList parameterTypes;
    private final Type returnType;

    public static Prototype intern(String descriptor) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        }
        Prototype result = (Prototype) internTable.get(descriptor);
        if (result != null) {
            return result;
        }
        return putIntern(fromDescriptor(descriptor));
    }

    public static Prototype fromDescriptor(String descriptor) {
        Prototype result = (Prototype) internTable.get(descriptor);
        if (result != null) {
            return result;
        }
        Type[] params = makeParameterArray(descriptor);
        int paramCount = 0;
        int at = 1;
        while (true) {
            int startAt = at;
            char c = descriptor.charAt(at);
            if (c == ')') {
                break;
            }
            while (c == '[') {
                at++;
                c = descriptor.charAt(at);
            }
            if (c == 'L') {
                int endAt = descriptor.indexOf(59, at);
                if (endAt == -1) {
                    throw new IllegalArgumentException("bad descriptor");
                }
                at = endAt + 1;
            } else {
                at++;
            }
            params[paramCount] = Type.intern(descriptor.substring(startAt, at));
            paramCount++;
        }
        Type returnType = Type.internReturnType(descriptor.substring(at + 1));
        StdTypeList parameterTypes = new StdTypeList(paramCount);
        for (int i = 0; i < paramCount; i++) {
            parameterTypes.set(i, params[i]);
        }
        return new Prototype(descriptor, returnType, parameterTypes);
    }

    public static void clearInternTable() {
        internTable.clear();
    }

    private static Type[] makeParameterArray(String descriptor) {
        int length = descriptor.length();
        if (descriptor.charAt(0) != '(') {
            throw new IllegalArgumentException("bad descriptor");
        }
        int closeAt = 0;
        int maxParams = 0;
        for (int i = 1; i < length; i++) {
            char c = descriptor.charAt(i);
            if (c == ')') {
                closeAt = i;
                break;
            }
            if (c >= 'A' && c <= 'Z') {
                maxParams++;
            }
        }
        if (closeAt == 0 || closeAt == length - 1) {
            throw new IllegalArgumentException("bad descriptor");
        } else if (descriptor.indexOf(41, closeAt + 1) == -1) {
            return new Type[maxParams];
        } else {
            throw new IllegalArgumentException("bad descriptor");
        }
    }

    public static Prototype intern(String descriptor, Type definer, boolean isStatic, boolean isInit) {
        Prototype base = intern(descriptor);
        if (isStatic) {
            return base;
        }
        if (isInit) {
            definer = definer.asUninitialized(Integer.MAX_VALUE);
        }
        return base.withFirstParameter(definer);
    }

    public static Prototype internInts(Type returnType, int count) {
        StringBuilder sb = new StringBuilder(100);
        sb.append('(');
        for (int i = 0; i < count; i++) {
            sb.append('I');
        }
        sb.append(')');
        sb.append(returnType.getDescriptor());
        return intern(sb.toString());
    }

    private Prototype(String descriptor, Type returnType, StdTypeList parameterTypes) {
        if (descriptor == null) {
            throw new NullPointerException("descriptor == null");
        } else if (returnType == null) {
            throw new NullPointerException("returnType == null");
        } else if (parameterTypes == null) {
            throw new NullPointerException("parameterTypes == null");
        } else {
            this.descriptor = descriptor;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
            this.parameterFrameTypes = null;
        }
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Prototype) {
            return this.descriptor.equals(((Prototype) other).descriptor);
        }
        return false;
    }

    public int hashCode() {
        return this.descriptor.hashCode();
    }

    public int compareTo(Prototype other) {
        if (this == other) {
            return 0;
        }
        int result = this.returnType.compareTo(other.returnType);
        if (result != 0) {
            return result;
        }
        int thisSize = this.parameterTypes.size();
        int otherSize = other.parameterTypes.size();
        int size = Math.min(thisSize, otherSize);
        for (int i = 0; i < size; i++) {
            result = this.parameterTypes.get(i).compareTo(other.parameterTypes.get(i));
            if (result != 0) {
                return result;
            }
        }
        if (thisSize < otherSize) {
            return -1;
        }
        return thisSize > otherSize ? 1 : 0;
    }

    public String toString() {
        return this.descriptor;
    }

    public String getDescriptor() {
        return this.descriptor;
    }

    public Type getReturnType() {
        return this.returnType;
    }

    public StdTypeList getParameterTypes() {
        return this.parameterTypes;
    }

    public StdTypeList getParameterFrameTypes() {
        if (this.parameterFrameTypes == null) {
            int sz = this.parameterTypes.size();
            StdTypeList list = new StdTypeList(sz);
            boolean any = false;
            for (int i = 0; i < sz; i++) {
                Type one = this.parameterTypes.get(i);
                if (one.isIntlike()) {
                    any = true;
                    one = Type.INT;
                }
                list.set(i, one);
            }
            if (!any) {
                list = this.parameterTypes;
            }
            this.parameterFrameTypes = list;
        }
        return this.parameterFrameTypes;
    }

    public Prototype withFirstParameter(Type param) {
        String newDesc = "(" + param.getDescriptor() + this.descriptor.substring(1);
        StdTypeList newParams = this.parameterTypes.withFirst(param);
        newParams.setImmutable();
        return putIntern(new Prototype(newDesc, this.returnType, newParams));
    }

    private static Prototype putIntern(Prototype desc) {
        Prototype result = (Prototype) internTable.putIfAbsent(desc.getDescriptor(), desc);
        return result != null ? result : desc;
    }
}
