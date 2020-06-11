package com.android.dx.command.dump;

import com.android.dx.cf.code.BasicBlocker;
import com.android.dx.cf.code.ByteBlock;
import com.android.dx.cf.code.ByteBlockList;
import com.android.dx.cf.code.ByteCatchList;
import com.android.dx.cf.code.ByteCatchList.Item;
import com.android.dx.cf.code.BytecodeArray;
import com.android.dx.cf.code.ConcreteMethod;
import com.android.dx.cf.code.Ropper;
import com.android.dx.cf.direct.CodeObserver;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.iface.Member;
import com.android.dx.cf.iface.Method;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.BasicBlock;
import com.android.dx.rop.code.BasicBlockList;
import com.android.dx.rop.code.DexTranslationAdvice;
import com.android.dx.rop.code.Insn;
import com.android.dx.rop.code.InsnList;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.rop.cst.CstType;
import com.android.dx.ssa.Optimizer;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;
import com.android.dx.util.IntList;
import java.io.PrintStream;

public class BlockDumper extends BaseDumper {
    protected DirectClassFile classFile = null;
    private boolean first = true;
    private final boolean optimize;
    private final boolean rop;
    protected boolean suppressDump = true;

    public static void dump(byte[] bytes, PrintStream out, String filePath, boolean rop, Args args) {
        new BlockDumper(bytes, out, filePath, rop, args).dump();
    }

    BlockDumper(byte[] bytes, PrintStream out, String filePath, boolean rop, Args args) {
        super(bytes, out, filePath, args);
        this.rop = rop;
        this.optimize = args.optimize;
    }

    public void dump() {
        ByteArray ba = new ByteArray(getBytes());
        this.classFile = new DirectClassFile(ba, getFilePath(), getStrictParse());
        this.classFile.setAttributeFactory(StdAttributeFactory.THE_ONE);
        this.classFile.getMagic();
        DirectClassFile liveCf = new DirectClassFile(ba, getFilePath(), getStrictParse());
        liveCf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        liveCf.setObserver(this);
        liveCf.getMagic();
    }

    public void changeIndent(int indentDelta) {
        if (!this.suppressDump) {
            super.changeIndent(indentDelta);
        }
    }

    public void parsed(ByteArray bytes, int offset, int len, String human) {
        if (!this.suppressDump) {
            super.parsed(bytes, offset, len, human);
        }
    }

    protected boolean shouldDumpMethod(String name) {
        return this.args.method == null || this.args.method.equals(name);
    }

    public void startParsingMember(ByteArray bytes, int offset, String name, String descriptor) {
        if (descriptor.indexOf(40) >= 0 && shouldDumpMethod(name)) {
            this.suppressDump = false;
            if (this.first) {
                this.first = false;
            } else {
                parsed(bytes, offset, 0, "\n");
            }
            parsed(bytes, offset, 0, "method " + name + " " + descriptor);
            this.suppressDump = true;
        }
    }

    public void endParsingMember(ByteArray bytes, int offset, String name, String descriptor, Member member) {
        if ((member instanceof Method) && shouldDumpMethod(name) && (member.getAccessFlags() & 1280) == 0) {
            ConcreteMethod meth = new ConcreteMethod((Method) member, this.classFile, true, true);
            if (this.rop) {
                ropDump(meth);
            } else {
                regularDump(meth);
            }
        }
    }

    private void regularDump(ConcreteMethod meth) {
        int end;
        BytecodeArray code = meth.getCode();
        ByteArray bytes = code.getBytes();
        ByteBlockList list = BasicBlocker.identifyBlocks(meth);
        int sz = list.size();
        CodeObserver codeObserver = new CodeObserver(bytes, this);
        this.suppressDump = false;
        int byteAt = 0;
        for (int i = 0; i < sz; i++) {
            ByteBlock bb = list.get(i);
            int start = bb.getStart();
            end = bb.getEnd();
            if (byteAt < start) {
                parsed(bytes, byteAt, start - byteAt, "dead code " + Hex.u2(byteAt) + ".." + Hex.u2(start));
            }
            parsed(bytes, start, 0, "block " + Hex.u2(bb.getLabel()) + ": " + Hex.u2(start) + ".." + Hex.u2(end));
            changeIndent(1);
            int j = start;
            while (j < end) {
                int len = code.parseInstruction(j, codeObserver);
                codeObserver.setPreviousOffset(j);
                j += len;
            }
            IntList successors = bb.getSuccessors();
            int ssz = successors.size();
            if (ssz == 0) {
                parsed(bytes, end, 0, "returns");
            } else {
                for (j = 0; j < ssz; j++) {
                    parsed(bytes, end, 0, "next " + Hex.u2(successors.get(j)));
                }
            }
            ByteCatchList catches = bb.getCatches();
            int csz = catches.size();
            for (j = 0; j < csz; j++) {
                String str;
                Item one = catches.get(j);
                CstType exceptionClass = one.getExceptionClass();
                StringBuilder append = new StringBuilder().append("catch ");
                if (exceptionClass == CstType.OBJECT) {
                    str = "<any>";
                } else {
                    str = exceptionClass.toHuman();
                }
                parsed(bytes, end, 0, append.append(str).append(" -> ").append(Hex.u2(one.getHandlerPc())).toString());
            }
            changeIndent(-1);
            byteAt = end;
        }
        end = bytes.size();
        if (byteAt < end) {
            parsed(bytes, byteAt, end - byteAt, "dead code " + Hex.u2(byteAt) + ".." + Hex.u2(end));
        }
        this.suppressDump = true;
    }

    private void ropDump(ConcreteMethod meth) {
        TranslationAdvice advice = DexTranslationAdvice.THE_ONE;
        ByteArray bytes = meth.getCode().getBytes();
        RopMethod rmeth = Ropper.convert(meth, advice, this.classFile.getMethods(), this.dexOptions);
        StringBuilder stringBuilder = new StringBuilder(2000);
        if (this.optimize) {
            boolean isStatic = AccessFlags.isStatic(meth.getAccessFlags());
            rmeth = Optimizer.optimize(rmeth, BaseDumper.computeParamWidth(meth, isStatic), isStatic, true, advice);
        }
        BasicBlockList blocks = rmeth.getBlocks();
        int[] order = blocks.getLabelsInOrder();
        stringBuilder.append("first " + Hex.u2(rmeth.getFirstLabel()) + "\n");
        for (int label : order) {
            int i;
            BasicBlock bb = blocks.get(blocks.indexOfLabel(label));
            stringBuilder.append("block ");
            stringBuilder.append(Hex.u2(label));
            stringBuilder.append("\n");
            IntList preds = rmeth.labelToPredecessors(label);
            int psz = preds.size();
            for (i = 0; i < psz; i++) {
                stringBuilder.append("  pred ");
                stringBuilder.append(Hex.u2(preds.get(i)));
                stringBuilder.append("\n");
            }
            InsnList il = bb.getInsns();
            int ilsz = il.size();
            for (i = 0; i < ilsz; i++) {
                Insn one = il.get(i);
                stringBuilder.append("  ");
                stringBuilder.append(il.get(i).toHuman());
                stringBuilder.append("\n");
            }
            IntList successors = bb.getSuccessors();
            int ssz = successors.size();
            if (ssz == 0) {
                stringBuilder.append("  returns\n");
            } else {
                int primary = bb.getPrimarySuccessor();
                for (i = 0; i < ssz; i++) {
                    int succ = successors.get(i);
                    stringBuilder.append("  next ");
                    stringBuilder.append(Hex.u2(succ));
                    if (ssz != 1 && succ == primary) {
                        stringBuilder.append(" *");
                    }
                    stringBuilder.append("\n");
                }
            }
        }
        this.suppressDump = false;
        parsed(bytes, 0, bytes.size(), stringBuilder.toString());
        this.suppressDump = true;
    }
}
