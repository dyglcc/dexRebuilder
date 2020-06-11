package com.android.dx.command.dexer;

import com.android.dx.dex.cf.CodeStatistics;
import com.android.dx.dex.cf.OptimizerOptions;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class DxContext {
    public final CodeStatistics codeStatistics;
    public final PrintStream err;
    final PrintStream noop;
    public final OptimizerOptions optimizerOptions;
    public final PrintStream out;

    public DxContext(OutputStream out, OutputStream err) {
        this.codeStatistics = new CodeStatistics();
        this.optimizerOptions = new OptimizerOptions();
        this.noop = new PrintStream(new OutputStream() {
            public void write(int b) throws IOException {
            }
        });
        this.out = new PrintStream(out);
        this.err = new PrintStream(err);
    }

    public DxContext() {
        this(System.out, System.err);
    }
}
