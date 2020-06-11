package com.android.dx.rop.code;

import com.android.dx.util.Hex;

public final class AccessFlags {
    public static final int ACC_ABSTRACT = 1024;
    public static final int ACC_ANNOTATION = 8192;
    public static final int ACC_BRIDGE = 64;
    public static final int ACC_CONSTRUCTOR = 65536;
    public static final int ACC_DECLARED_SYNCHRONIZED = 131072;
    public static final int ACC_ENUM = 16384;
    public static final int ACC_FINAL = 16;
    public static final int ACC_INTERFACE = 512;
    public static final int ACC_NATIVE = 256;
    public static final int ACC_PRIVATE = 2;
    public static final int ACC_PROTECTED = 4;
    public static final int ACC_PUBLIC = 1;
    public static final int ACC_STATIC = 8;
    public static final int ACC_STRICT = 2048;
    public static final int ACC_SUPER = 32;
    public static final int ACC_SYNCHRONIZED = 32;
    public static final int ACC_SYNTHETIC = 4096;
    public static final int ACC_TRANSIENT = 128;
    public static final int ACC_VARARGS = 128;
    public static final int ACC_VOLATILE = 64;
    public static final int CLASS_FLAGS = 30257;
    private static final int CONV_CLASS = 1;
    private static final int CONV_FIELD = 2;
    private static final int CONV_METHOD = 3;
    public static final int FIELD_FLAGS = 20703;
    public static final int INNER_CLASS_FLAGS = 30239;
    public static final int METHOD_FLAGS = 204287;

    private AccessFlags() {
    }

    public static String classString(int flags) {
        return humanHelper(flags, CLASS_FLAGS, 1);
    }

    public static String innerClassString(int flags) {
        return humanHelper(flags, INNER_CLASS_FLAGS, 1);
    }

    public static String fieldString(int flags) {
        return humanHelper(flags, FIELD_FLAGS, 2);
    }

    public static String methodString(int flags) {
        return humanHelper(flags, METHOD_FLAGS, 3);
    }

    public static boolean isPublic(int flags) {
        return (flags & 1) != 0;
    }

    public static boolean isProtected(int flags) {
        return (flags & 4) != 0;
    }

    public static boolean isPrivate(int flags) {
        return (flags & 2) != 0;
    }

    public static boolean isStatic(int flags) {
        return (flags & 8) != 0;
    }

    public static boolean isConstructor(int flags) {
        return (ACC_CONSTRUCTOR & flags) != 0;
    }

    public static boolean isInterface(int flags) {
        return (flags & 512) != 0;
    }

    public static boolean isSynchronized(int flags) {
        return (flags & 32) != 0;
    }

    public static boolean isAbstract(int flags) {
        return (flags & 1024) != 0;
    }

    public static boolean isNative(int flags) {
        return (flags & 256) != 0;
    }

    public static boolean isAnnotation(int flags) {
        return (flags & 8192) != 0;
    }

    public static boolean isDeclaredSynchronized(int flags) {
        return (ACC_DECLARED_SYNCHRONIZED & flags) != 0;
    }

    public static boolean isEnum(int flags) {
        return (flags & ACC_ENUM) != 0;
    }

    private static String humanHelper(int flags, int mask, int what) {
        StringBuilder sb = new StringBuilder(80);
        int extra = flags & (mask ^ -1);
        flags &= mask;
        if ((flags & 1) != 0) {
            sb.append("|public");
        }
        if ((flags & 2) != 0) {
            sb.append("|private");
        }
        if ((flags & 4) != 0) {
            sb.append("|protected");
        }
        if ((flags & 8) != 0) {
            sb.append("|static");
        }
        if ((flags & 16) != 0) {
            sb.append("|final");
        }
        if ((flags & 32) != 0) {
            if (what == 1) {
                sb.append("|super");
            } else {
                sb.append("|synchronized");
            }
        }
        if ((flags & 64) != 0) {
            if (what == 3) {
                sb.append("|bridge");
            } else {
                sb.append("|volatile");
            }
        }
        if ((flags & 128) != 0) {
            if (what == 3) {
                sb.append("|varargs");
            } else {
                sb.append("|transient");
            }
        }
        if ((flags & 256) != 0) {
            sb.append("|native");
        }
        if ((flags & 512) != 0) {
            sb.append("|interface");
        }
        if ((flags & 1024) != 0) {
            sb.append("|abstract");
        }
        if ((flags & 2048) != 0) {
            sb.append("|strictfp");
        }
        if ((flags & 4096) != 0) {
            sb.append("|synthetic");
        }
        if ((flags & 8192) != 0) {
            sb.append("|annotation");
        }
        if ((flags & ACC_ENUM) != 0) {
            sb.append("|enum");
        }
        if ((ACC_CONSTRUCTOR & flags) != 0) {
            sb.append("|constructor");
        }
        if ((ACC_DECLARED_SYNCHRONIZED & flags) != 0) {
            sb.append("|declared_synchronized");
        }
        if (extra != 0 || sb.length() == 0) {
            sb.append('|');
            sb.append(Hex.u2(extra));
        }
        return sb.substring(1);
    }
}
