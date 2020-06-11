package com.android.dex;

import com.android.dex.util.ByteInput;
import com.android.dx.io.Opcodes;
import java.io.UTFDataFormatException;

public final class Mutf8 {
    private Mutf8() {
    }

    public static String decode(ByteInput in, char[] out) throws UTFDataFormatException {
        int s = 0;
        while (true) {
            char a = (char) (in.readByte() & Opcodes.CONST_METHOD_TYPE);
            if (a == '\u0000') {
                return new String(out, 0, s);
            }
            out[s] = a;
            if (a < '') {
                s++;
            } else if ((a & Opcodes.SHL_INT_LIT8) == 192) {
                b = in.readByte() & Opcodes.CONST_METHOD_TYPE;
                if ((b & 192) != 128) {
                    throw new UTFDataFormatException("bad second byte");
                }
                s = s + 1;
                out[s] = (char) (((a & 31) << 6) | (b & 63));
                s = s;
            } else if ((a & 240) == Opcodes.SHL_INT_LIT8) {
                b = in.readByte() & Opcodes.CONST_METHOD_TYPE;
                int c = in.readByte() & Opcodes.CONST_METHOD_TYPE;
                if ((b & 192) == 128 && (c & 192) == 128) {
                    s = s + 1;
                    out[s] = (char) ((((a & 15) << 12) | ((b & 63) << 6)) | (c & 63));
                    s = s;
                }
            } else {
                throw new UTFDataFormatException("bad byte");
            }
        }
        throw new UTFDataFormatException("bad second or third byte");
    }

    private static long countBytes(String s, boolean shortLength) throws UTFDataFormatException {
        long result = 0;
        int length = s.length();
        int i = 0;
        while (i < length) {
            char ch = s.charAt(i);
            if (ch != '\u0000' && ch <= '') {
                result++;
            } else if (ch <= '߿') {
                result += 2;
            } else {
                result += 3;
            }
            if (!shortLength || result <= 65535) {
                i++;
            } else {
                throw new UTFDataFormatException("String more than 65535 UTF bytes long");
            }
        }
        return result;
    }

    public static void encode(byte[] dst, int offset, String s) {
        int length = s.length();
        int i = 0;
        int offset2 = offset;
        while (i < length) {
            char ch = s.charAt(i);
            if (ch != '\u0000' && ch <= '') {
                offset = offset2 + 1;
                dst[offset2] = (byte) ch;
            } else if (ch <= '߿') {
                offset = offset2 + 1;
                dst[offset2] = (byte) (((ch >> 6) & 31) | 192);
                offset2 = offset + 1;
                dst[offset] = (byte) ((ch & 63) | 128);
                offset = offset2;
            } else {
                offset = offset2 + 1;
                dst[offset2] = (byte) (((ch >> 12) & 15) | Opcodes.SHL_INT_LIT8);
                offset2 = offset + 1;
                dst[offset] = (byte) (((ch >> 6) & 63) | 128);
                offset = offset2 + 1;
                dst[offset2] = (byte) ((ch & 63) | 128);
            }
            i++;
            offset2 = offset;
        }
    }

    public static byte[] encode(String s) throws UTFDataFormatException {
        byte[] result = new byte[((int) countBytes(s, true))];
        encode(result, 0, s);
        return result;
    }
}
