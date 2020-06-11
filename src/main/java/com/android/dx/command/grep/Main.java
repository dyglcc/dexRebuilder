package com.android.dx.command.grep;

import com.android.dex.Dex;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

public final class Main {
    public static void main(String[] args) throws IOException {
        int i = 0;
        if (new Grep(new Dex(new File(args[0])), Pattern.compile(args[1]), new PrintWriter(System.out)).grep() <= 0) {
            i = 1;
        }
        System.exit(i);
    }
}
