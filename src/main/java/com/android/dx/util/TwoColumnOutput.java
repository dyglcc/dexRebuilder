package com.android.dx.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

public final class TwoColumnOutput {
    private final StringBuffer leftBuf;
    private final IndentingWriter leftColumn;
    private final int leftWidth;
    private final Writer out;
    private final StringBuffer rightBuf;
    private final IndentingWriter rightColumn;

    public static String toString(String s1, int width1, String spacer, String s2, int width2) {
        Writer sw = new StringWriter((s1.length() + s2.length()) * 3);
        TwoColumnOutput twoOut = new TwoColumnOutput(sw, width1, width2, spacer);
        try {
            twoOut.getLeft().write(s1);
            twoOut.getRight().write(s2);
            twoOut.flush();
            return sw.toString();
        } catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }
    }

    public TwoColumnOutput(Writer out, int leftWidth, int rightWidth, String spacer) {
        if (out == null) {
            throw new NullPointerException("out == null");
        } else if (leftWidth < 1) {
            throw new IllegalArgumentException("leftWidth < 1");
        } else if (rightWidth < 1) {
            throw new IllegalArgumentException("rightWidth < 1");
        } else if (spacer == null) {
            throw new NullPointerException("spacer == null");
        } else {
            StringWriter leftWriter = new StringWriter(1000);
            StringWriter rightWriter = new StringWriter(1000);
            this.out = out;
            this.leftWidth = leftWidth;
            this.leftBuf = leftWriter.getBuffer();
            this.rightBuf = rightWriter.getBuffer();
            this.leftColumn = new IndentingWriter(leftWriter, leftWidth);
            this.rightColumn = new IndentingWriter(rightWriter, rightWidth, spacer);
        }
    }

    public TwoColumnOutput(OutputStream out, int leftWidth, int rightWidth, String spacer) {
        this(new OutputStreamWriter(out), leftWidth, rightWidth, spacer);
    }

    public Writer getLeft() {
        return this.leftColumn;
    }

    public Writer getRight() {
        return this.rightColumn;
    }

    public void flush() {
        try {
            appendNewlineIfNecessary(this.leftBuf, this.leftColumn);
            appendNewlineIfNecessary(this.rightBuf, this.rightColumn);
            outputFullLines();
            flushLeft();
            flushRight();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void outputFullLines() throws IOException {
        while (true) {
            int leftLen = this.leftBuf.indexOf("\n");
            if (leftLen >= 0) {
                int rightLen = this.rightBuf.indexOf("\n");
                if (rightLen >= 0) {
                    if (leftLen != 0) {
                        this.out.write(this.leftBuf.substring(0, leftLen));
                    }
                    if (rightLen != 0) {
                        writeSpaces(this.out, this.leftWidth - leftLen);
                        this.out.write(this.rightBuf.substring(0, rightLen));
                    }
                    this.out.write(10);
                    this.leftBuf.delete(0, leftLen + 1);
                    this.rightBuf.delete(0, rightLen + 1);
                } else {
                    return;
                }
            }
            return;
        }
    }

    private void flushLeft() throws IOException {
        appendNewlineIfNecessary(this.leftBuf, this.leftColumn);
        while (this.leftBuf.length() != 0) {
            this.rightColumn.write(10);
            outputFullLines();
        }
    }

    private void flushRight() throws IOException {
        appendNewlineIfNecessary(this.rightBuf, this.rightColumn);
        while (this.rightBuf.length() != 0) {
            this.leftColumn.write(10);
            outputFullLines();
        }
    }

    private static void appendNewlineIfNecessary(StringBuffer buf, Writer out) throws IOException {
        int len = buf.length();
        if (len != 0 && buf.charAt(len - 1) != '\n') {
            out.write(10);
        }
    }

    private static void writeSpaces(Writer out, int amt) throws IOException {
        while (amt > 0) {
            out.write(32);
            amt--;
        }
    }
}
