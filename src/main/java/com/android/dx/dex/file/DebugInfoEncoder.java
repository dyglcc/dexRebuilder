package com.android.dx.dex.file;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.dex.code.LocalList;
import com.android.dx.dex.code.LocalList.Disposition;
import com.android.dx.dex.code.LocalList.Entry;
import com.android.dx.dex.code.PositionList;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.ByteArrayAnnotatedOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public final class DebugInfoEncoder {
    private static final boolean DEBUG = false;
    private int address = 0;
    private AnnotatedOutput annotateTo;
    private final int codeSize;
    private PrintWriter debugPrint;
    private final Prototype desc;
    private final DexFile file;
    private final boolean isStatic;
    private final Entry[] lastEntryForReg;
    private int line = 1;
    private final LocalList locals;
    private final ByteArrayAnnotatedOutput output;
    private final PositionList positions;
    private String prefix;
    private final int regSize;
    private boolean shouldConsume;

    public DebugInfoEncoder(PositionList positions, LocalList locals, DexFile file, int codeSize, int regSize, boolean isStatic, CstMethodRef ref) {
        this.positions = positions;
        this.locals = locals;
        this.file = file;
        this.desc = ref.getPrototype();
        this.isStatic = isStatic;
        this.codeSize = codeSize;
        this.regSize = regSize;
        this.output = new ByteArrayAnnotatedOutput();
        this.lastEntryForReg = new Entry[regSize];
    }

    private void annotate(int length, String message) {
        if (this.prefix != null) {
            message = this.prefix + message;
        }
        if (this.annotateTo != null) {
            AnnotatedOutput annotatedOutput = this.annotateTo;
            if (!this.shouldConsume) {
                length = 0;
            }
            annotatedOutput.annotate(length, message);
        }
        if (this.debugPrint != null) {
            this.debugPrint.println(message);
        }
    }

    public byte[] convert() {
        try {
            return convert0();
        } catch (IOException ex) {
            throw ExceptionWithContext.withContext(ex, "...while encoding debug info");
        }
    }

    public byte[] convertAndAnnotate(String prefix, PrintWriter debugPrint, AnnotatedOutput out, boolean consume) {
        this.prefix = prefix;
        this.debugPrint = debugPrint;
        this.annotateTo = out;
        this.shouldConsume = consume;
        return convert();
    }

    private byte[] convert0() throws IOException {
        ArrayList<PositionList.Entry> sortedPositions = buildSortedPositions();
        emitHeader(sortedPositions, extractMethodArguments());
        this.output.writeByte(7);
        if (!(this.annotateTo == null && this.debugPrint == null)) {
            annotate(1, String.format("%04x: prologue end", new Object[]{Integer.valueOf(this.address)}));
        }
        int positionsSz = sortedPositions.size();
        int localsSz = this.locals.size();
        int curPositionIdx = 0;
        int curLocalIdx = 0;
        while (true) {
            curLocalIdx = emitLocalsAtAddress(curLocalIdx);
            curPositionIdx = emitPositionsAtAddress(curPositionIdx, sortedPositions);
            int nextAddrL = Integer.MAX_VALUE;
            int nextAddrP = Integer.MAX_VALUE;
            if (curLocalIdx < localsSz) {
                nextAddrL = this.locals.get(curLocalIdx).getAddress();
            }
            if (curPositionIdx < positionsSz) {
                nextAddrP = ((PositionList.Entry) sortedPositions.get(curPositionIdx)).getAddress();
            }
            int next = Math.min(nextAddrP, nextAddrL);
            if (!(next == Integer.MAX_VALUE || (next == this.codeSize && nextAddrL == Integer.MAX_VALUE && nextAddrP == Integer.MAX_VALUE))) {
                if (next == nextAddrP) {
                    int curPositionIdx2 = curPositionIdx + 1;
                    emitPosition((PositionList.Entry) sortedPositions.get(curPositionIdx));
                    curPositionIdx = curPositionIdx2;
                } else {
                    emitAdvancePc(next - this.address);
                }
            }
        }
        emitEndSequence();
        return this.output.toByteArray();
    }

    private int emitLocalsAtAddress(int curLocalIdx) throws IOException {
        int sz = this.locals.size();
        int curLocalIdx2 = curLocalIdx;
        while (curLocalIdx2 < sz && this.locals.get(curLocalIdx2).getAddress() == this.address) {
            curLocalIdx = curLocalIdx2 + 1;
            Entry entry = this.locals.get(curLocalIdx2);
            int reg = entry.getRegister();
            Entry prevEntry = this.lastEntryForReg[reg];
            if (entry == prevEntry) {
                curLocalIdx2 = curLocalIdx;
            } else {
                this.lastEntryForReg[reg] = entry;
                if (entry.isStart()) {
                    if (prevEntry == null || !entry.matches(prevEntry)) {
                        emitLocalStart(entry);
                    } else if (prevEntry.isStart()) {
                        throw new RuntimeException("shouldn't happen");
                    } else {
                        emitLocalRestart(entry);
                    }
                } else if (entry.getDisposition() != Disposition.END_REPLACED) {
                    emitLocalEnd(entry);
                }
                curLocalIdx2 = curLocalIdx;
            }
        }
        return curLocalIdx2;
    }

    private int emitPositionsAtAddress(int curPositionIdx, ArrayList<PositionList.Entry> sortedPositions) throws IOException {
        int positionsSz = sortedPositions.size();
        int curPositionIdx2 = curPositionIdx;
        while (curPositionIdx2 < positionsSz && ((PositionList.Entry) sortedPositions.get(curPositionIdx2)).getAddress() == this.address) {
            curPositionIdx = curPositionIdx2 + 1;
            emitPosition((PositionList.Entry) sortedPositions.get(curPositionIdx2));
            curPositionIdx2 = curPositionIdx;
        }
        return curPositionIdx2;
    }

    private void emitHeader(ArrayList<PositionList.Entry> sortedPositions, ArrayList<Entry> methodArgs) throws IOException {
        Iterator it;
        Entry arg;
        boolean annotate = (this.annotateTo == null && this.debugPrint == null) ? false : true;
        int mark = this.output.getCursor();
        if (sortedPositions.size() > 0) {
            this.line = ((PositionList.Entry) sortedPositions.get(0)).getPosition().getLine();
        }
        this.output.writeUleb128(this.line);
        if (annotate) {
            annotate(this.output.getCursor() - mark, "line_start: " + this.line);
        }
        int curParam = getParamBase();
        StdTypeList paramTypes = this.desc.getParameterTypes();
        int szParamTypes = paramTypes.size();
        if (!this.isStatic) {
            it = methodArgs.iterator();
            while (it.hasNext()) {
                arg = (Entry) it.next();
                if (curParam == arg.getRegister()) {
                    this.lastEntryForReg[curParam] = arg;
                    break;
                }
            }
            curParam++;
        }
        mark = this.output.getCursor();
        this.output.writeUleb128(szParamTypes);
        if (annotate) {
            annotate(this.output.getCursor() - mark, String.format("parameters_size: %04x", new Object[]{Integer.valueOf(szParamTypes)}));
        }
        for (int i = 0; i < szParamTypes; i++) {
            Type pt = paramTypes.get(i);
            Entry found = null;
            mark = this.output.getCursor();
            it = methodArgs.iterator();
            while (it.hasNext()) {
                String parameterName;
                arg = (Entry) it.next();
                if (curParam == arg.getRegister()) {
                    found = arg;
                    if (arg.getSignature() != null) {
                        emitStringIndex(null);
                    } else {
                        emitStringIndex(arg.getName());
                    }
                    this.lastEntryForReg[curParam] = arg;
                    if (found == null) {
                        emitStringIndex(null);
                    }
                    if (!annotate) {
                        if (found == null && found.getSignature() == null) {
                            parameterName = found.getName().toHuman();
                        } else {
                            parameterName = "<unnamed>";
                        }
                        annotate(this.output.getCursor() - mark, "parameter " + parameterName + " " + RegisterSpec.PREFIX + curParam);
                    }
                    curParam += pt.getCategory();
                }
            }
            if (found == null) {
                emitStringIndex(null);
            }
            if (!annotate) {
                if (found == null) {
                }
                parameterName = "<unnamed>";
                annotate(this.output.getCursor() - mark, "parameter " + parameterName + " " + RegisterSpec.PREFIX + curParam);
            }
            curParam += pt.getCategory();
        }
        for (Entry arg2 : this.lastEntryForReg) {
            if (!(arg2 == null || arg2.getSignature() == null)) {
                emitLocalStartExtended(arg2);
            }
        }
    }

    private ArrayList<PositionList.Entry> buildSortedPositions() {
        int sz = this.positions == null ? 0 : this.positions.size();
        ArrayList<PositionList.Entry> result = new ArrayList(sz);
        for (int i = 0; i < sz; i++) {
            result.add(this.positions.get(i));
        }
        Collections.sort(result, new Comparator<PositionList.Entry>() {
            public int compare(PositionList.Entry a, PositionList.Entry b) {
                return a.getAddress() - b.getAddress();
            }

            public boolean equals(Object obj) {
                return obj == this;
            }
        });
        return result;
    }

    private int getParamBase() {
        return (this.regSize - this.desc.getParameterTypes().getWordCount()) - (this.isStatic ? 0 : 1);
    }

    private ArrayList<Entry> extractMethodArguments() {
        ArrayList<Entry> result = new ArrayList(this.desc.getParameterTypes().size());
        int argBase = getParamBase();
        BitSet seen = new BitSet(this.regSize - argBase);
        int sz = this.locals.size();
        for (int i = 0; i < sz; i++) {
            Entry e = this.locals.get(i);
            int reg = e.getRegister();
            if (reg >= argBase && !seen.get(reg - argBase)) {
                seen.set(reg - argBase);
                result.add(e);
            }
        }
        Collections.sort(result, new Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                return a.getRegister() - b.getRegister();
            }

            public boolean equals(Object obj) {
                return obj == this;
            }
        });
        return result;
    }

    private String entryAnnotationString(Entry e) {
        StringBuilder sb = new StringBuilder();
        sb.append(RegisterSpec.PREFIX);
        sb.append(e.getRegister());
        sb.append(' ');
        CstString name = e.getName();
        if (name == null) {
            sb.append("null");
        } else {
            sb.append(name.toHuman());
        }
        sb.append(' ');
        CstType type = e.getType();
        if (type == null) {
            sb.append("null");
        } else {
            sb.append(type.toHuman());
        }
        CstString signature = e.getSignature();
        if (signature != null) {
            sb.append(' ');
            sb.append(signature.toHuman());
        }
        return sb.toString();
    }

    private void emitLocalRestart(Entry entry) throws IOException {
        int mark = this.output.getCursor();
        this.output.writeByte(6);
        emitUnsignedLeb128(entry.getRegister());
        if (this.annotateTo != null || this.debugPrint != null) {
            annotate(this.output.getCursor() - mark, String.format("%04x: +local restart %s", new Object[]{Integer.valueOf(this.address), entryAnnotationString(entry)}));
        }
    }

    private void emitStringIndex(CstString string) throws IOException {
        if (string == null || this.file == null) {
            this.output.writeUleb128(0);
        } else {
            this.output.writeUleb128(this.file.getStringIds().indexOf(string) + 1);
        }
    }

    private void emitTypeIndex(CstType type) throws IOException {
        if (type == null || this.file == null) {
            this.output.writeUleb128(0);
        } else {
            this.output.writeUleb128(this.file.getTypeIds().indexOf(type) + 1);
        }
    }

    private void emitLocalStart(Entry entry) throws IOException {
        if (entry.getSignature() != null) {
            emitLocalStartExtended(entry);
            return;
        }
        int mark = this.output.getCursor();
        this.output.writeByte(3);
        emitUnsignedLeb128(entry.getRegister());
        emitStringIndex(entry.getName());
        emitTypeIndex(entry.getType());
        if (this.annotateTo != null || this.debugPrint != null) {
            annotate(this.output.getCursor() - mark, String.format("%04x: +local %s", new Object[]{Integer.valueOf(this.address), entryAnnotationString(entry)}));
        }
    }

    private void emitLocalStartExtended(Entry entry) throws IOException {
        int mark = this.output.getCursor();
        this.output.writeByte(4);
        emitUnsignedLeb128(entry.getRegister());
        emitStringIndex(entry.getName());
        emitTypeIndex(entry.getType());
        emitStringIndex(entry.getSignature());
        if (this.annotateTo != null || this.debugPrint != null) {
            annotate(this.output.getCursor() - mark, String.format("%04x: +localx %s", new Object[]{Integer.valueOf(this.address), entryAnnotationString(entry)}));
        }
    }

    private void emitLocalEnd(Entry entry) throws IOException {
        int mark = this.output.getCursor();
        this.output.writeByte(5);
        this.output.writeUleb128(entry.getRegister());
        if (this.annotateTo != null || this.debugPrint != null) {
            annotate(this.output.getCursor() - mark, String.format("%04x: -local %s", new Object[]{Integer.valueOf(this.address), entryAnnotationString(entry)}));
        }
    }

    private void emitPosition(PositionList.Entry entry) throws IOException {
        int newLine = entry.getPosition().getLine();
        int deltaLines = newLine - this.line;
        int deltaAddress = entry.getAddress() - this.address;
        if (deltaAddress < 0) {
            throw new RuntimeException("Position entries must be in ascending address order");
        }
        if (deltaLines < -4 || deltaLines > 10) {
            emitAdvanceLine(deltaLines);
            deltaLines = 0;
        }
        int opcode = computeOpcode(deltaLines, deltaAddress);
        if ((opcode & -256) > 0) {
            emitAdvancePc(deltaAddress);
            deltaAddress = 0;
            opcode = computeOpcode(deltaLines, 0);
            if ((opcode & -256) > 0) {
                emitAdvanceLine(deltaLines);
                deltaLines = 0;
                opcode = computeOpcode(0, 0);
            }
        }
        this.output.writeByte(opcode);
        this.line += deltaLines;
        this.address += deltaAddress;
        if (this.annotateTo != null || this.debugPrint != null) {
            annotate(1, String.format("%04x: line %d", new Object[]{Integer.valueOf(this.address), Integer.valueOf(this.line)}));
        }
    }

    private static int computeOpcode(int deltaLines, int deltaAddress) {
        if (deltaLines >= -4 && deltaLines <= 10) {
            return ((deltaLines + 4) + (deltaAddress * 15)) + 10;
        }
        throw new RuntimeException("Parameter out of range");
    }

    private void emitAdvanceLine(int deltaLines) throws IOException {
        int mark = this.output.getCursor();
        this.output.writeByte(2);
        this.output.writeSleb128(deltaLines);
        this.line += deltaLines;
        if (this.annotateTo != null || this.debugPrint != null) {
            annotate(this.output.getCursor() - mark, String.format("line = %d", new Object[]{Integer.valueOf(this.line)}));
        }
    }

    private void emitAdvancePc(int deltaAddress) throws IOException {
        int mark = this.output.getCursor();
        this.output.writeByte(1);
        this.output.writeUleb128(deltaAddress);
        this.address += deltaAddress;
        if (this.annotateTo != null || this.debugPrint != null) {
            annotate(this.output.getCursor() - mark, String.format("%04x: advance pc", new Object[]{Integer.valueOf(this.address)}));
        }
    }

    private void emitUnsignedLeb128(int n) throws IOException {
        if (n < 0) {
            throw new RuntimeException("Signed value where unsigned required: " + n);
        }
        this.output.writeUleb128(n);
    }

    private void emitEndSequence() {
        this.output.writeByte(0);
        if (this.annotateTo != null || this.debugPrint != null) {
            annotate(1, "end sequence");
        }
    }
}
