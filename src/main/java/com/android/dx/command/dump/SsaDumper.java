package com.android.dx.command.dump;

import com.android.dx.cf.code.ConcreteMethod;
import com.android.dx.cf.code.Ropper;
import com.android.dx.cf.iface.Member;
import com.android.dx.cf.iface.Method;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.DexTranslationAdvice;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.ssa.Optimizer;
import com.android.dx.ssa.Optimizer.OptionalStep;
import com.android.dx.ssa.SsaBasicBlock;
import com.android.dx.ssa.SsaInsn;
import com.android.dx.ssa.SsaMethod;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;
import com.android.dx.util.IntList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;

public class SsaDumper extends BlockDumper {
    public static void dump(byte[] bytes, PrintStream out, String filePath, Args args) {
        new SsaDumper(bytes, out, filePath, args).dump();
    }

    private SsaDumper(byte[] bytes, PrintStream out, String filePath, Args args) {
        super(bytes, out, filePath, true, args);
    }

    public void endParsingMember(ByteArray bytes, int offset, String name, String descriptor, Member member) {
        if ((member instanceof Method) && shouldDumpMethod(name) && (member.getAccessFlags() & 1280) == 0) {
            ConcreteMethod meth = new ConcreteMethod((Method) member, this.classFile, true, true);
            TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
            RopMethod rmeth = Ropper.convert(meth, advice, this.classFile.getMethods(), this.dexOptions);
            SsaMethod ssaMeth = null;
            boolean isStatic = AccessFlags.isStatic(meth.getAccessFlags());
            int paramWidth = BaseDumper.computeParamWidth(meth, isStatic);
            if (this.args.ssaStep == null) {
                ssaMeth = Optimizer.debugNoRegisterAllocation(rmeth, paramWidth, isStatic, true, advice, EnumSet.allOf(OptionalStep.class));
            } else if ("edge-split".equals(this.args.ssaStep)) {
                ssaMeth = Optimizer.debugEdgeSplit(rmeth, paramWidth, isStatic, true, advice);
            } else if ("phi-placement".equals(this.args.ssaStep)) {
                ssaMeth = Optimizer.debugPhiPlacement(rmeth, paramWidth, isStatic, true, advice);
            } else if ("renaming".equals(this.args.ssaStep)) {
                ssaMeth = Optimizer.debugRenaming(rmeth, paramWidth, isStatic, true, advice);
            } else if ("dead-code".equals(this.args.ssaStep)) {
                ssaMeth = Optimizer.debugDeadCodeRemover(rmeth, paramWidth, isStatic, true, advice);
            }
            StringBuilder stringBuilder = new StringBuilder(2000);
            stringBuilder.append("first ");
            stringBuilder = stringBuilder;
            stringBuilder.append(Hex.u2(ssaMeth.blockIndexToRopLabel(ssaMeth.getEntryBlockIndex())));
            stringBuilder.append('\n');
            ArrayList<SsaBasicBlock> sortedBlocks = (ArrayList) ssaMeth.getBlocks().clone();
            Collections.sort(sortedBlocks, SsaBasicBlock.LABEL_COMPARATOR);
            Iterator it = sortedBlocks.iterator();
            while (it.hasNext()) {
                int i;
                SsaBasicBlock block = (SsaBasicBlock) it.next();
                stringBuilder.append("block ").append(Hex.u2(block.getRopLabel())).append('\n');
                BitSet preds = block.getPredecessors();
                for (i = preds.nextSetBit(0); i >= 0; i = preds.nextSetBit(i + 1)) {
                    stringBuilder.append("  pred ");
                    stringBuilder.append(Hex.u2(ssaMeth.blockIndexToRopLabel(i)));
                    stringBuilder.append('\n');
                }
                stringBuilder.append("  live in:" + block.getLiveInRegs());
                stringBuilder.append("\n");
                Iterator it2 = block.getInsns().iterator();
                while (it2.hasNext()) {
                    SsaInsn insn = (SsaInsn) it2.next();
                    stringBuilder.append("  ");
                    stringBuilder.append(insn.toHuman());
                    stringBuilder.append('\n');
                }
                if (block.getSuccessors().cardinality() == 0) {
                    stringBuilder.append("  returns\n");
                } else {
                    int primary = block.getPrimarySuccessorRopLabel();
                    IntList succLabelList = block.getRopLabelSuccessorList();
                    int szSuccLabels = succLabelList.size();
                    i = 0;
                    while (i < szSuccLabels) {
                        stringBuilder.append("  next ");
                        stringBuilder.append(Hex.u2(succLabelList.get(i)));
                        if (szSuccLabels != 1 && primary == succLabelList.get(i)) {
                            stringBuilder.append(" *");
                        }
                        stringBuilder.append('\n');
                        i++;
                    }
                }
                stringBuilder.append("  live out:" + block.getLiveOutRegs());
                stringBuilder.append("\n");
            }
            this.suppressDump = false;
            parsed(bytes, 0, bytes.size(), stringBuilder.toString());
            this.suppressDump = true;
        }
    }
}
