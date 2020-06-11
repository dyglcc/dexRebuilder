package com.android.dx.command.dump;

import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.util.ByteArray;
import java.io.PrintStream;

public final class ClassDumper extends BaseDumper {
    public static void dump(byte[] bytes, PrintStream out, String filePath, Args args) {
        new ClassDumper(bytes, out, filePath, args).dump();
    }

    private ClassDumper(byte[] bytes, PrintStream out, String filePath, Args args) {
        super(bytes, out, filePath, args);
    }

    public void dump() {
        byte[] bytes = getBytes();
        ByteArray ba = new ByteArray(bytes);
        DirectClassFile cf = new DirectClassFile(ba, getFilePath(), getStrictParse());
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        cf.setObserver(this);
        cf.getMagic();
        int readBytes = getReadBytes();
        if (readBytes != bytes.length) {
            parsed(ba, readBytes, bytes.length - readBytes, "<extra data at end of file>");
        }
    }
}
