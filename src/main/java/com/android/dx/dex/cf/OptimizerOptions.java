package com.android.dx.dex.cf;

import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.ssa.Optimizer;
import com.android.dx.ssa.Optimizer.OptionalStep;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;

public class OptimizerOptions {
    private HashSet<String> dontOptimizeList;
    private HashSet<String> optimizeList;
    private boolean optimizeListsLoaded;

    public void loadOptimizeLists(String optimizeListFile, String dontOptimizeListFile) {
        if (!this.optimizeListsLoaded) {
            if (optimizeListFile == null || dontOptimizeListFile == null) {
                if (optimizeListFile != null) {
                    this.optimizeList = loadStringsFromFile(optimizeListFile);
                }
                if (dontOptimizeListFile != null) {
                    this.dontOptimizeList = loadStringsFromFile(dontOptimizeListFile);
                }
                this.optimizeListsLoaded = true;
                return;
            }
            throw new RuntimeException("optimize and don't optimize lists  are mutually exclusive.");
        }
    }

    private static HashSet<String> loadStringsFromFile(String filename) {
        HashSet<String> result = new HashSet();
        try {
            FileReader fr = new FileReader(filename);
            BufferedReader bfr = new BufferedReader(fr);
            while (true) {
                String line = bfr.readLine();
                if (line != null) {
                    result.add(line);
                } else {
                    fr.close();
                    return result;
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error with optimize list: " + filename, ex);
        }
    }

    public void compareOptimizerStep(RopMethod nonOptRmeth, int paramSize, boolean isStatic, CfOptions args, TranslationAdvice advice, RopMethod rmeth) {
        EnumSet<OptionalStep> steps = EnumSet.allOf(OptionalStep.class);
        steps.remove(OptionalStep.CONST_COLLECTOR);
        RopMethod skipRopMethod = Optimizer.optimize(nonOptRmeth, paramSize, isStatic, args.localInfo, advice, steps);
        int normalInsns = rmeth.getBlocks().getEffectiveInstructionCount();
        int skipInsns = skipRopMethod.getBlocks().getEffectiveInstructionCount();
        System.err.printf("optimize step regs:(%d/%d/%.2f%%) insns:(%d/%d/%.2f%%)\n", new Object[]{Integer.valueOf(rmeth.getBlocks().getRegCount()), Integer.valueOf(skipRopMethod.getBlocks().getRegCount()), Double.valueOf(100.0d * ((double) (((float) (skipRopMethod.getBlocks().getRegCount() - rmeth.getBlocks().getRegCount())) / ((float) skipRopMethod.getBlocks().getRegCount())))), Integer.valueOf(normalInsns), Integer.valueOf(skipInsns), Double.valueOf(100.0d * ((double) (((float) (skipInsns - normalInsns)) / ((float) skipInsns))))});
    }

    public boolean shouldOptimize(String canonicalMethodName) {
        if (this.optimizeList != null) {
            return this.optimizeList.contains(canonicalMethodName);
        }
        if (this.dontOptimizeList == null || !this.dontOptimizeList.contains(canonicalMethodName)) {
            return true;
        }
        return false;
    }
}
