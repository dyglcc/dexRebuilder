package com.android.dex;

import com.android.dex.Dex.Section;
import com.android.dex.util.ByteArrayByteInput;
import com.android.dex.util.ByteInput;
import com.android.dx.io.Opcodes;

public final class EncodedValue implements Comparable<EncodedValue> {
    private final byte[] data;

    public EncodedValue(byte[] data) {
        this.data = data;
    }

    public ByteInput asByteInput() {
        return new ByteArrayByteInput(this.data);
    }

    public byte[] getBytes() {
        return this.data;
    }

    public void writeTo(Section out) {
        out.write(this.data);
    }

    public int compareTo(EncodedValue other) {
        int size = Math.min(this.data.length, other.data.length);
        for (int i = 0; i < size; i++) {
            if (this.data[i] != other.data[i]) {
                return (this.data[i] & Opcodes.CONST_METHOD_TYPE) - (other.data[i] & Opcodes.CONST_METHOD_TYPE);
            }
        }
        return this.data.length - other.data.length;
    }

    public String toString() {
        return Integer.toHexString(this.data[0] & Opcodes.CONST_METHOD_TYPE) + "...(" + this.data.length + ")";
    }
}
