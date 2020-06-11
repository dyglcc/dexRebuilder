package com.android.dx.command.dump;

import com.android.dx.cf.code.ConcreteMethod;
import com.android.dx.cf.iface.Member;
import com.android.dx.cf.iface.ParseObserver;
import com.android.dx.dex.DexOptions;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;
import com.android.dx.util.IndentingWriter;
import com.android.dx.util.TwoColumnOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;

public abstract class BaseDumper implements ParseObserver {
    protected Args args;
    private final byte[] bytes;
    protected final DexOptions dexOptions;
    private final String filePath;
    private final int hexCols;
    private int indent;
    private final PrintStream out;
    private final boolean rawBytes;
    private int readBytes;
    private String separator;
    private final boolean strictParse;
    private final int width;

    public BaseDumper(byte[] bytes, PrintStream out, String filePath, Args args) {
        this.bytes = bytes;
        this.rawBytes = args.rawBytes;
        this.out = out;
        this.width = args.width <= 0 ? 79 : args.width;
        this.filePath = filePath;
        this.strictParse = args.strictParse;
        this.indent = 0;
        this.separator = this.rawBytes ? "|" : "";
        this.readBytes = 0;
        this.args = args;
        this.dexOptions = new DexOptions();
        int hexCols = (((this.width - 5) / 15) + 1) & -2;
        if (hexCols < 6) {
            hexCols = 6;
        } else if (hexCols > 10) {
            hexCols = 10;
        }
        this.hexCols = hexCols;
    }

    static int computeParamWidth(ConcreteMethod meth, boolean isStatic) {
        return meth.getEffectiveDescriptor().getParameterTypes().getWordCount();
    }

    public void changeIndent(int indentDelta) {
        this.indent += indentDelta;
        this.separator = this.rawBytes ? "|" : "";
        for (int i = 0; i < this.indent; i++) {
            this.separator += "  ";
        }
    }

    public void parsed(ByteArray bytes, int offset, int len, String human) {
        print(twoColumns(getRawBytes() ? hexDump(bytes.underlyingOffset(offset), len) : "", human));
        this.readBytes += len;
    }

    public void startParsingMember(ByteArray bytes, int offset, String name, String descriptor) {
    }

    public void endParsingMember(ByteArray bytes, int offset, String name, String descriptor, Member member) {
    }

    protected final int getReadBytes() {
        return this.readBytes;
    }

    protected final byte[] getBytes() {
        return this.bytes;
    }

    protected final String getFilePath() {
        return this.filePath;
    }

    protected final boolean getStrictParse() {
        return this.strictParse;
    }

    protected final void print(String s) {
        this.out.print(s);
    }

    protected final void println(String s) {
        this.out.println(s);
    }

    protected final boolean getRawBytes() {
        return this.rawBytes;
    }

    protected final int getWidth1() {
        if (this.rawBytes) {
            return ((this.hexCols * 2) + 5) + (this.hexCols / 2);
        }
        return 0;
    }

    protected final int getWidth2() {
        return (this.width - (this.rawBytes ? getWidth1() + 1 : 0)) - (this.indent * 2);
    }

    protected final String hexDump(int offset, int len) {
        return Hex.dump(this.bytes, offset, len, offset, this.hexCols, 4);
    }

    protected final String twoColumns(String s1, String s2) {
        int w1 = getWidth1();
        int w2 = getWidth2();
        if (w1 != 0) {
            return TwoColumnOutput.toString(s1, w1, this.separator, s2, w2);
        }
        try {
            int len2 = s2.length();
            StringWriter sw = new StringWriter(len2 * 2);
            IndentingWriter iw = new IndentingWriter(sw, w2, this.separator);
            iw.write(s2);
            if (len2 == 0 || s2.charAt(len2 - 1) != '\n') {
                iw.write(10);
            }
            iw.flush();
            return sw.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
