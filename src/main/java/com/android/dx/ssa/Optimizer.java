package com.android.dx.ssa;

import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.ssa.back.LivenessAnalyzer;
import com.android.dx.ssa.back.SsaToRop;
import java.util.EnumSet;

public class Optimizer {
    private static TranslationAdvice advice;
    private static boolean preserveLocals = true;

    public enum OptionalStep {
        MOVE_PARAM_COMBINER,
        SCCP,
        LITERAL_UPGRADE,
        CONST_COLLECTOR,
        ESCAPE_ANALYSIS
    }

    public static boolean getPreserveLocals() {
        return preserveLocals;
    }

    public static TranslationAdvice getAdvice() {
        return advice;
    }

    public static RopMethod optimize(RopMethod rmeth, int paramWidth, boolean isStatic, boolean inPreserveLocals, TranslationAdvice inAdvice) {
        return optimize(rmeth, paramWidth, isStatic, inPreserveLocals, inAdvice, EnumSet.allOf(OptionalStep.class));
    }

    public static RopMethod optimize(RopMethod rmeth, int paramWidth, boolean isStatic, boolean inPreserveLocals, TranslationAdvice inAdvice, EnumSet<OptionalStep> steps) {
        preserveLocals = inPreserveLocals;
        advice = inAdvice;
        SsaMethod ssaMeth = SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
        runSsaFormSteps(ssaMeth, steps);
        RopMethod resultMeth = SsaToRop.convertToRopMethod(ssaMeth, false);
        if (resultMeth.getBlocks().getRegCount() > advice.getMaxOptimalRegisterCount()) {
            return optimizeMinimizeRegisters(rmeth, paramWidth, isStatic, steps);
        }
        return resultMeth;
    }

    private static RopMethod optimizeMinimizeRegisters(RopMethod rmeth, int paramWidth, boolean isStatic, EnumSet<OptionalStep> steps) {
        SsaMethod ssaMeth = SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
        EnumSet<OptionalStep> newSteps = steps.clone();
        newSteps.remove(OptionalStep.CONST_COLLECTOR);
        runSsaFormSteps(ssaMeth, newSteps);
        return SsaToRop.convertToRopMethod(ssaMeth, true);
    }

    private static void runSsaFormSteps(SsaMethod ssaMeth, EnumSet<OptionalStep> steps) {
        boolean needsDeadCodeRemover = true;
        if (steps.contains(OptionalStep.MOVE_PARAM_COMBINER)) {
            MoveParamCombiner.process(ssaMeth);
        }
        if (steps.contains(OptionalStep.SCCP)) {
            SCCP.process(ssaMeth);
            DeadCodeRemover.process(ssaMeth);
            needsDeadCodeRemover = false;
        }
        if (steps.contains(OptionalStep.LITERAL_UPGRADE)) {
            LiteralOpUpgrader.process(ssaMeth);
            DeadCodeRemover.process(ssaMeth);
            needsDeadCodeRemover = false;
        }
        steps.remove(OptionalStep.ESCAPE_ANALYSIS);
        if (steps.contains(OptionalStep.ESCAPE_ANALYSIS)) {
            EscapeAnalysis.process(ssaMeth);
            DeadCodeRemover.process(ssaMeth);
            needsDeadCodeRemover = false;
        }
        if (steps.contains(OptionalStep.CONST_COLLECTOR)) {
            ConstCollector.process(ssaMeth);
            DeadCodeRemover.process(ssaMeth);
            needsDeadCodeRemover = false;
        }
        if (needsDeadCodeRemover) {
            DeadCodeRemover.process(ssaMeth);
        }
        PhiTypeResolver.process(ssaMeth);
    }

    public static SsaMethod debugEdgeSplit(RopMethod rmeth, int paramWidth, boolean isStatic, boolean inPreserveLocals, TranslationAdvice inAdvice) {
        preserveLocals = inPreserveLocals;
        advice = inAdvice;
        return SsaConverter.testEdgeSplit(rmeth, paramWidth, isStatic);
    }

    public static SsaMethod debugPhiPlacement(RopMethod rmeth, int paramWidth, boolean isStatic, boolean inPreserveLocals, TranslationAdvice inAdvice) {
        preserveLocals = inPreserveLocals;
        advice = inAdvice;
        return SsaConverter.testPhiPlacement(rmeth, paramWidth, isStatic);
    }

    public static SsaMethod debugRenaming(RopMethod rmeth, int paramWidth, boolean isStatic, boolean inPreserveLocals, TranslationAdvice inAdvice) {
        preserveLocals = inPreserveLocals;
        advice = inAdvice;
        return SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
    }

    public static SsaMethod debugDeadCodeRemover(RopMethod rmeth, int paramWidth, boolean isStatic, boolean inPreserveLocals, TranslationAdvice inAdvice) {
        preserveLocals = inPreserveLocals;
        advice = inAdvice;
        SsaMethod ssaMeth = SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
        DeadCodeRemover.process(ssaMeth);
        return ssaMeth;
    }

    public static SsaMethod debugNoRegisterAllocation(RopMethod rmeth, int paramWidth, boolean isStatic, boolean inPreserveLocals, TranslationAdvice inAdvice, EnumSet<OptionalStep> steps) {
        preserveLocals = inPreserveLocals;
        advice = inAdvice;
        SsaMethod ssaMeth = SsaConverter.convertToSsaMethod(rmeth, paramWidth, isStatic);
        runSsaFormSteps(ssaMeth, steps);
        LivenessAnalyzer.constructInterferenceGraph(ssaMeth);
        return ssaMeth;
    }
}
