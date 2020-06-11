package com.android.dx.util;

import com.android.dx.io.Opcodes;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class ByteArray {
    private final byte[] bytes;
    private final int size;
    private final int start;

    public interface GetCursor {
        int getCursor();
    }

    public static class MyDataInputStream extends DataInputStream {
        private final MyInputStream wrapped;

        public MyDataInputStream(MyInputStream wrapped) {
            super(wrapped);
            this.wrapped = wrapped;
        }
    }

    public class MyInputStream extends InputStream {
        private int cursor = 0;
        private int mark = 0;

        public int read() throws IOException {
            if (this.cursor >= ByteArray.this.size) {
                return -1;
            }
            int result = ByteArray.this.getUnsignedByte0(this.cursor);
            this.cursor++;
            return result;
        }

        public int read(byte[] arr, int offset, int length) {
            if (offset + length > arr.length) {
                length = arr.length - offset;
            }
            int maxLength = ByteArray.this.size - this.cursor;
            if (length > maxLength) {
                length = maxLength;
            }
            System.arraycopy(ByteArray.this.bytes, this.cursor + ByteArray.this.start, arr, offset, length);
            this.cursor += length;
            return length;
        }

        public int available() {
            return ByteArray.this.size - this.cursor;
        }

        public void mark(int reserve) {
            this.mark = this.cursor;
        }

        public void reset() {
            this.cursor = this.mark;
        }

        public boolean markSupported() {
            return true;
        }
    }

    public ByteArray(byte[] bytes, int start, int end) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        } else if (start < 0) {
            throw new IllegalArgumentException("start < 0");
        } else if (end < start) {
            throw new IllegalArgumentException("end < start");
        } else if (end > bytes.length) {
            throw new IllegalArgumentException("end > bytes.length");
        } else {
            this.bytes = bytes;
            this.start = start;
            this.size = end - start;
        }
    }

    public ByteArray(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public int size() {
        return this.size;
    }

    public ByteArray slice(int start, int end) {
        checkOffsets(start, end);
        return new ByteArray(Arrays.copyOfRange(this.bytes, start, end));
    }

    public int underlyingOffset(int offset) {
        return this.start + offset;
    }

    public int getByte(int off) {
        checkOffsets(off, off + 1);
        return getByte0(off);
    }

    public int getShort(int off) {
        checkOffsets(off, off + 2);
        return (getByte0(off) << 8) | getUnsignedByte0(off + 1);
    }

    public int getInt(int off) {
        checkOffsets(off, off + 4);
        return (((getByte0(off) << 24) | (getUnsignedByte0(off + 1) << 16)) | (getUnsignedByte0(off + 2) << 8)) | getUnsignedByte0(off + 3);
    }

    public long getLong(int off) {
        checkOffsets(off, off + 8);
        return (((long) ((((getByte0(off + 4) << 24) | (getUnsignedByte0(off + 5) << 16)) | (getUnsignedByte0(off + 6) << 8)) | getUnsignedByte0(off + 7))) & 4294967295L) | (((long) ((((getByte0(off) << 24) | (getUnsignedByte0(off + 1) << 16)) | (getUnsignedByte0(off + 2) << 8)) | getUnsignedByte0(off + 3))) << 32);
    }

    public int getUnsignedByte(int off) {
        checkOffsets(off, off + 1);
        return getUnsignedByte0(off);
    }

    public int getUnsignedShort(int off) {
        checkOffsets(off, off + 2);
        return (getUnsignedByte0(off) << 8) | getUnsignedByte0(off + 1);
    }

    public void getBytes(byte[] out, int offset) {
        if (out.length - offset < this.size) {
            throw new IndexOutOfBoundsException("(out.length - offset) < size()");
        }
        System.arraycopy(this.bytes, this.start, out, offset, this.size);
    }

    private void checkOffsets(int s, int e) {
        if (s < 0 || e < s || e > this.size) {
            throw new IllegalArgumentException("bad range: " + s + ".." + e + "; actual size " + this.size);
        }
    }

    private int getByte0(int off) {
        return this.bytes[this.start + off];
    }

    private int getUnsignedByte0(int off) {
        return this.bytes[this.start + off] & Opcodes.CONST_METHOD_TYPE;
    }

    public MyDataInputStream makeDataInputStream() {
        return new MyDataInputStream(makeInputStream());
    }

    public MyInputStream makeInputStream() {
        return new MyInputStream();
    }
}
