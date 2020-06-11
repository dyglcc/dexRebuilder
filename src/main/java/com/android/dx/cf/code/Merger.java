package com.android.dx.cf.code;

import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeBearer;
import com.android.dx.util.Hex;

public final class Merger {
    private Merger() {
    }

    public static OneLocalsArray mergeLocals(OneLocalsArray locals1, OneLocalsArray locals2) {
        if (locals1 == locals2) {
            return locals1;
        }
        int sz = locals1.getMaxLocals();
        OneLocalsArray result = null;
        if (locals2.getMaxLocals() != sz) {
            throw new SimException("mismatched maxLocals values");
        }
        for (int i = 0; i < sz; i++) {
            TypeBearer tb1 = locals1.getOrNull(i);
            TypeBearer resultType = mergeType(tb1, locals2.getOrNull(i));
            if (resultType != tb1) {
                if (result == null) {
                    result = locals1.copy();
                }
                if (resultType == null) {
                    result.invalidate(i);
                } else {
                    result.set(i, resultType);
                }
            }
        }
        if (result == null) {
            return locals1;
        }
        result.setImmutable();
        return result;
    }

    public static ExecutionStack mergeStack(ExecutionStack stack1, ExecutionStack stack2) {
        if (stack1 == stack2) {
            return stack1;
        }
        int sz = stack1.size();
        ExecutionStack result = null;
        if (stack2.size() != sz) {
            throw new SimException("mismatched stack depths");
        }
        for (int i = 0; i < sz; i++) {
            TypeBearer tb1 = stack1.peek(i);
            TypeBearer tb2 = stack2.peek(i);
            TypeBearer resultType = mergeType(tb1, tb2);
            if (resultType != tb1) {
                if (result == null) {
                    result = stack1.copy();
                }
                if (resultType == null) {
                    try {
                        throw new SimException("incompatible: " + tb1 + ", " + tb2);
                    } catch (SimException ex) {
                        ex.addContext("...while merging stack[" + Hex.u2(i) + "]");
                        throw ex;
                    }
                }
                result.change(i, resultType);
            }
        }
        if (result == null) {
            return stack1;
        }
        result.setImmutable();
        return result;
    }

    public static TypeBearer mergeType(TypeBearer ft1, TypeBearer ft2) {
        if (ft1 == null || ft1.equals(ft2)) {
            return ft1;
        }
        if (ft2 == null) {
            return null;
        }
        TypeBearer type1 = ft1.getType();
        TypeBearer type2 = ft2.getType();
        if (type1 == type2) {
            return type1;
        }
        if (!type1.isReference() || !type2.isReference()) {
            return (type1.isIntlike() && type2.isIntlike()) ? Type.INT : null;
        } else {
            if (type1 == Type.KNOWN_NULL) {
                return type2;
            }
            if (type2 == Type.KNOWN_NULL) {
                return type1;
            }
            if (!type1.isArray() || !type2.isArray()) {
                return Type.OBJECT;
            }
            TypeBearer componentUnion = mergeType(type1.getComponentType(), type2.getComponentType());
            if (componentUnion == null) {
                return Type.OBJECT;
            }
            return ((Type) componentUnion).getArrayType();
        }
    }

    public static boolean isPossiblyAssignableFrom(TypeBearer supertypeBearer, TypeBearer subtypeBearer) {
        boolean z = false;
        Type supertype = supertypeBearer.getType();
        Type subtype = subtypeBearer.getType();
        if (supertype.equals(subtype)) {
            return true;
        }
        int superBt = supertype.getBasicType();
        int subBt = subtype.getBasicType();
        if (superBt == 10) {
            supertype = Type.OBJECT;
            superBt = 9;
        }
        if (subBt == 10) {
            subtype = Type.OBJECT;
            subBt = 9;
        }
        if (superBt == 9 && subBt == 9) {
            if (supertype == Type.KNOWN_NULL) {
                return false;
            }
            if (subtype == Type.KNOWN_NULL || supertype == Type.OBJECT) {
                return true;
            }
            if (supertype.isArray()) {
                if (!subtype.isArray()) {
                    return false;
                }
                do {
                    supertype = supertype.getComponentType();
                    subtype = subtype.getComponentType();
                    if (!supertype.isArray()) {
                        break;
                    }
                } while (subtype.isArray());
                return isPossiblyAssignableFrom(supertype, subtype);
            } else if (!subtype.isArray()) {
                return true;
            } else {
                if (supertype == Type.SERIALIZABLE || supertype == Type.CLONEABLE) {
                    z = true;
                }
                return z;
            }
        } else if (supertype.isIntlike() && subtype.isIntlike()) {
            return true;
        } else {
            return false;
        }
    }
}
