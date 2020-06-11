package com.android.dx.rop.cst;

import com.android.dx.io.Opcodes;
import com.android.dx.rop.type.Type;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;

public final class CstString extends TypedConstant {
    public static final CstString EMPTY_STRING = new CstString("");
    private final ByteArray bytes;
    private final String string;

    public static byte[] stringToUtf8Bytes(String string) {
        int len = string.length();
        byte[] bytes = new byte[(len * 3)];
        int outAt = 0;
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if (c != '\u0000' && c < '') {
                bytes[outAt] = (byte) c;
                outAt++;
            } else if (c < 'ࠀ') {
                bytes[outAt] = (byte) (((c >> 6) & 31) | 192);
                bytes[outAt + 1] = (byte) ((c & 63) | 128);
                outAt += 2;
            } else {
                bytes[outAt] = (byte) (((c >> 12) & 15) | Opcodes.SHL_INT_LIT8);
                bytes[outAt + 1] = (byte) (((c >> 6) & 63) | 128);
                bytes[outAt + 2] = (byte) ((c & 63) | 128);
                outAt += 3;
            }
        }
        byte[] result = new byte[outAt];
        System.arraycopy(bytes, 0, result, 0, outAt);
        return result;
    }

    public static String utf8BytesToString(ByteArray bytes) {
        int length = bytes.size();
        char[] chars = new char[length];
        int outAt = 0;
        int at = 0;
        while (length > 0) {
            char out;
            int v0 = bytes.getUnsignedByte(at);
            int v1;
            int value;
            switch (v0 >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    length--;
                    if (v0 != 0) {
                        out = (char) v0;
                        at++;
                        break;
                    }
                    return throwBadUtf8(v0, at);
                case 12:
                case 13:
                    length -= 2;
                    if (length < 0) {
                        return throwBadUtf8(v0, at);
                    }
                    v1 = bytes.getUnsignedByte(at + 1);
                    if ((v1 & 192) != 128) {
                        return throwBadUtf8(v1, at + 1);
                    }
                    value = ((v0 & 31) << 6) | (v1 & 63);
                    if (value == 0 || value >= 128) {
                        out = (char) value;
                        at += 2;
                        break;
                    }
                    return throwBadUtf8(v1, at + 1);
                case 14:
                    length -= 3;
                    if (length >= 0) {
                        v1 = bytes.getUnsignedByte(at + 1);
                        if ((v1 & 192) == 128) {
                            int v2 = bytes.getUnsignedByte(at + 2);
                            if ((v1 & 192) == 128) {
                                value = (((v0 & 15) << 12) | ((v1 & 63) << 6)) | (v2 & 63);
                                if (value >= 2048) {
                                    out = (char) value;
                                    at += 3;
                                    break;
                                }
                                return throwBadUtf8(v2, at + 2);
                            }
                            return throwBadUtf8(v2, at + 2);
                        }
                        return throwBadUtf8(v1, at + 1);
                    }
                    return throwBadUtf8(v0, at);
                default:
                    return throwBadUtf8(v0, at);
            }
            chars[outAt] = out;
            outAt++;
        }
        return new String(chars, 0, outAt);
    }

    private static String throwBadUtf8(int value, int offset) {
        throw new IllegalArgumentException("bad utf-8 byte " + Hex.u1(value) + " at offset " + Hex.u4(offset));
    }

    public CstString(String string) {
        if (string == null) {
            throw new NullPointerException("string == null");
        }
        this.string = string.intern();
        this.bytes = new ByteArray(stringToUtf8Bytes(string));
    }

    public CstString(ByteArray bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }
        this.bytes = bytes;
        this.string = utf8BytesToString(bytes).intern();
    }

    public boolean equals(Object other) {
        if (other instanceof CstString) {
            return this.string.equals(((CstString) other).string);
        }
        return false;
    }

    public int hashCode() {
        return this.string.hashCode();
    }

    protected int compareTo0(Constant other) {
        return this.string.compareTo(((CstString) other).string);
    }

    public String toString() {
        return "string{\"" + toHuman() + "\"}";
    }

    public String typeName() {
        return "utf8";
    }

    public boolean isCategory2() {
        return false;
    }

    public String toHuman() {
        int len = this.string.length();
        StringBuilder sb = new StringBuilder((len * 3) / 2);
        for (int i = 0; i < len; i++) {
            char c = this.string.charAt(i);
            if (c >= ' ' && c < '') {
                if (c == '\'' || c == '\"' || c == '\\') {
                    sb.append('\\');
                }
                sb.append(c);
            } else if (c <= '') {
                switch (c) {
                    case '\t':
                        sb.append("\\t");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    default:
                        char nextChar;
                        boolean displayZero;
                        if (i < len - 1) {
                            nextChar = this.string.charAt(i + 1);
                        } else {
                            nextChar = '\u0000';
                        }
                        if (nextChar < '0' || nextChar > '7') {
                            displayZero = false;
                        } else {
                            displayZero = true;
                        }
                        sb.append('\\');
                        for (int shift = 6; shift >= 0; shift -= 3) {
                            char outChar = (char) (((c >> shift) & 7) + 48);
                            if (outChar != '0' || displayZero) {
                                sb.append(outChar);
                                displayZero = true;
                            }
                        }
                        if (!displayZero) {
                            sb.append('0');
                            break;
                        }
                        break;
                        break;
                }
            } else {
                sb.append("\\u");
                sb.append(Character.forDigit(c >> 12, 16));
                sb.append(Character.forDigit((c >> 8) & 15, 16));
                sb.append(Character.forDigit((c >> 4) & 15, 16));
                sb.append(Character.forDigit(c & 15, 16));
            }
        }
        return sb.toString();
    }

    public String toQuoted() {
        return '\"' + toHuman() + '\"';
    }

    public String toQuoted(int maxLength) {
        String ellipses;
        String string = toHuman();
        if (string.length() <= maxLength - 2) {
            ellipses = "";
        } else {
            string = string.substring(0, maxLength - 5);
            ellipses = "...";
        }
        return '\"' + string + ellipses + '\"';
    }

    public String getString() {
        return this.string;
    }

    public ByteArray getBytes() {
        return this.bytes;
    }

    public int getUtf8Size() {
        return this.bytes.size();
    }

    public int getUtf16Size() {
        return this.string.length();
    }

    public Type getType() {
        return Type.STRING;
    }
}
