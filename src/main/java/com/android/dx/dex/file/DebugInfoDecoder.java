package com.android.dx.dex.file;

import com.android.dex.Leb128;
import com.android.dex.util.ByteArrayByteInput;
import com.android.dex.util.ByteInput;
import com.android.dex.util.ExceptionWithContext;
import com.android.dx.dex.code.DalvCode;
import com.android.dx.dex.code.DalvInsnList;
import com.android.dx.dex.code.LocalList;
import com.android.dx.dex.code.LocalList.Disposition;
import com.android.dx.dex.code.PositionList;
import com.android.dx.dex.code.PositionList.Entry;
import com.android.dx.io.Opcodes;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DebugInfoDecoder {
    private int address = 0;
    private final int codesize;
    private final Prototype desc;
    private final byte[] encoded;
    private final DexFile file;
    private final boolean isStatic;
    private final LocalEntry[] lastEntryForReg;
    private int line = 1;
    private final ArrayList<LocalEntry> locals;
    private final ArrayList<PositionEntry> positions;
    private final int regSize;
    private final int thisStringIdx;

    private static class LocalEntry {
        public int address;
        public boolean isStart;
        public int nameIndex;
        public int reg;
        public int signatureIndex;
        public int typeIndex;

        public LocalEntry(int address, boolean isStart, int reg, int nameIndex, int typeIndex, int signatureIndex) {
            this.address = address;
            this.isStart = isStart;
            this.reg = reg;
            this.nameIndex = nameIndex;
            this.typeIndex = typeIndex;
            this.signatureIndex = signatureIndex;
        }

        public String toString() {
            String str = "[%x %s v%d %04x %04x %04x]";
            Object[] objArr = new Object[6];
            objArr[0] = Integer.valueOf(this.address);
            objArr[1] = this.isStart ? "start" : "end";
            objArr[2] = Integer.valueOf(this.reg);
            objArr[3] = Integer.valueOf(this.nameIndex);
            objArr[4] = Integer.valueOf(this.typeIndex);
            objArr[5] = Integer.valueOf(this.signatureIndex);
            return String.format(str, objArr);
        }
    }

    private static class PositionEntry {
        public int address;
        public int line;

        public PositionEntry(int address, int line) {
            this.address = address;
            this.line = line;
        }
    }

    DebugInfoDecoder(byte[] encoded, int codesize, int regSize, boolean isStatic, CstMethodRef ref, DexFile file) {
        if (encoded == null) {
            throw new NullPointerException("encoded == null");
        }
        this.encoded = encoded;
        this.isStatic = isStatic;
        this.desc = ref.getPrototype();
        this.file = file;
        this.regSize = regSize;
        this.positions = new ArrayList();
        this.locals = new ArrayList();
        this.codesize = codesize;
        this.lastEntryForReg = new LocalEntry[regSize];
        int idx = -1;
        try {
            idx = file.getStringIds().indexOf(new CstString("this"));
        } catch (IllegalArgumentException e) {
        }
        this.thisStringIdx = idx;
    }

    public List<PositionEntry> getPositionList() {
        return this.positions;
    }

    public List<LocalEntry> getLocals() {
        return this.locals;
    }

    public void decode() {
        try {
            decode0();
        } catch (Exception ex) {
            throw ExceptionWithContext.withContext(ex, "...while decoding debug info");
        }
    }

    private int readStringIndex(ByteInput bs) throws IOException {
        return Leb128.readUnsignedLeb128(bs) - 1;
    }

    private int getParamBase() {
        return (this.regSize - this.desc.getParameterTypes().getWordCount()) - (this.isStatic ? 0 : 1);
    }

    private void decode0() throws IOException {
        ByteInput byteArrayByteInput = new ByteArrayByteInput(this.encoded);
        this.line = Leb128.readUnsignedLeb128(byteArrayByteInput);
        int szParams = Leb128.readUnsignedLeb128(byteArrayByteInput);
        StdTypeList params = this.desc.getParameterTypes();
        int curReg = getParamBase();
        if (szParams == params.size()) {
            LocalEntry le;
            if (!this.isStatic) {
                LocalEntry thisEntry = new LocalEntry(0, true, curReg, this.thisStringIdx, 0, 0);
                this.locals.add(thisEntry);
                this.lastEntryForReg[curReg] = thisEntry;
                curReg++;
            }
            for (int i = 0; i < szParams; i++) {
                Type paramType = params.getType(i);
                int nameIdx = readStringIndex(byteArrayByteInput);
                if (nameIdx == -1) {
                    le = new LocalEntry(0, true, curReg, -1, 0, 0);
                } else {
                    le = new LocalEntry(0, true, curReg, nameIdx, 0, 0);
                }
                this.locals.add(le);
                this.lastEntryForReg[curReg] = le;
                curReg += paramType.getCategory();
            }
            while (true) {
                int opcode = byteArrayByteInput.readByte() & Opcodes.CONST_METHOD_TYPE;
                int reg;
                LocalEntry prevle;
                LocalEntry localEntry;
                switch (opcode) {
                    case 0:
                        return;
                    case 1:
                        this.address += Leb128.readUnsignedLeb128(byteArrayByteInput);
                        break;
                    case 2:
                        this.line += Leb128.readSignedLeb128(byteArrayByteInput);
                        break;
                    case 3:
                        reg = Leb128.readUnsignedLeb128(byteArrayByteInput);
                        le = new LocalEntry(this.address, true, reg, readStringIndex(byteArrayByteInput), readStringIndex(byteArrayByteInput), 0);
                        this.locals.add(le);
                        this.lastEntryForReg[reg] = le;
                        break;
                    case 4:
                        reg = Leb128.readUnsignedLeb128(byteArrayByteInput);
                        le = new LocalEntry(this.address, true, reg, readStringIndex(byteArrayByteInput), readStringIndex(byteArrayByteInput), readStringIndex(byteArrayByteInput));
                        this.locals.add(le);
                        this.lastEntryForReg[reg] = le;
                        break;
                    case 5:
                        reg = Leb128.readUnsignedLeb128(byteArrayByteInput);
                        prevle = this.lastEntryForReg[reg];
                        if (prevle.isStart) {
                            try {
                                localEntry = new LocalEntry(this.address, false, reg, prevle.nameIndex, prevle.typeIndex, prevle.signatureIndex);
                                this.locals.add(localEntry);
                                this.lastEntryForReg[reg] = localEntry;
                                break;
                            } catch (NullPointerException e) {
                                throw new RuntimeException("Encountered END_LOCAL on new v" + reg);
                            }
                        }
                        throw new RuntimeException("nonsensical END_LOCAL on dead register v" + reg);
                    case 6:
                        reg = Leb128.readUnsignedLeb128(byteArrayByteInput);
                        prevle = this.lastEntryForReg[reg];
                        if (prevle.isStart) {
                            throw new RuntimeException("nonsensical RESTART_LOCAL on live register v" + reg);
                        }
                        try {
                            localEntry = new LocalEntry(this.address, true, reg, prevle.nameIndex, prevle.typeIndex, 0);
                            this.locals.add(localEntry);
                            this.lastEntryForReg[reg] = localEntry;
                            break;
                        } catch (NullPointerException e2) {
                            throw new RuntimeException("Encountered RESTART_LOCAL on new v" + reg);
                        }
                    case 7:
                    case 8:
                    case 9:
                        break;
                    default:
                        if (opcode >= 10) {
                            int adjopcode = opcode - 10;
                            this.address += adjopcode / 15;
                            this.line += (adjopcode % 15) - 4;
                            this.positions.add(new PositionEntry(this.address, this.line));
                            break;
                        }
                        throw new RuntimeException("Invalid extended opcode encountered " + opcode);
                }
            }
        }
        throw new RuntimeException("Mismatch between parameters_size and prototype");
    }

    public static void validateEncode(byte[] info, DexFile file, CstMethodRef ref, DalvCode code, boolean isStatic) {
        PositionList pl = code.getPositions();
        LocalList ll = code.getLocals();
        DalvInsnList insns = code.getInsns();
        try {
            validateEncode0(info, insns.codeSize(), insns.getRegistersSize(), isStatic, ref, file, pl, ll);
        } catch (RuntimeException ex) {
            System.err.println("instructions:");
            insns.debugPrint(System.err, "  ", true);
            System.err.println("local list:");
            ll.debugPrint(System.err, "  ");
            throw ExceptionWithContext.withContext(ex, "while processing " + ref.toHuman());
        }
    }

    private static void validateEncode0(byte[] info, int codeSize, int countRegisters, boolean isStatic, CstMethodRef ref, DexFile file, PositionList pl, LocalList ll) {
        DebugInfoDecoder decoder = new DebugInfoDecoder(info, codeSize, countRegisters, isStatic, ref, file);
        decoder.decode();
        List<PositionEntry> decodedEntries = decoder.getPositionList();
        if (decodedEntries.size() != pl.size()) {
            throw new RuntimeException("Decoded positions table not same size was " + decodedEntries.size() + " expected " + pl.size());
        }
        int i;
        for (PositionEntry entry : decodedEntries) {
            boolean found = false;
            for (i = pl.size() - 1; i >= 0; i--) {
                Entry ple = pl.get(i);
                if (entry.line == ple.getPosition().getLine() && entry.address == ple.getAddress()) {
                    found = true;
                    continue;
                    break;
                }
            }
            if (!found) {
                throw new RuntimeException("Could not match position entry: " + entry.address + ", " + entry.line);
            }
        }
        List<LocalEntry> decodedLocals = decoder.getLocals();
        int thisStringIdx = decoder.thisStringIdx;
        int decodedSz = decodedLocals.size();
        int paramBase = decoder.getParamBase();
        for (i = 0; i < decodedSz; i++) {
            LocalEntry entry2 = (LocalEntry) decodedLocals.get(i);
            int idx = entry2.nameIndex;
            if (idx < 0 || idx == thisStringIdx) {
                int j = i + 1;
                while (j < decodedSz) {
                    LocalEntry e2 = (LocalEntry) decodedLocals.get(j);
                    if (e2.address == 0) {
                        if (entry2.reg == e2.reg && e2.isStart) {
                            decodedLocals.set(i, e2);
                            decodedLocals.remove(j);
                            decodedSz--;
                            break;
                        }
                        j++;
                    } else {
                        break;
                    }
                }
            }
        }
        int origSz = ll.size();
        int decodeAt = 0;
        boolean problem = false;
        for (i = 0; i < origSz; i++) {
            LocalList.Entry origEntry = ll.get(i);
            if (origEntry.getDisposition() != Disposition.END_REPLACED) {
                LocalEntry decodedEntry;
                do {
                    decodedEntry = (LocalEntry) decodedLocals.get(decodeAt);
                    if (decodedEntry.nameIndex >= 0) {
                        break;
                    }
                    decodeAt++;
                } while (decodeAt < decodedSz);
                int decodedAddress = decodedEntry.address;
                if (decodedEntry.reg == origEntry.getRegister()) {
                    if (decodedEntry.isStart == origEntry.isStart()) {
                        if (decodedAddress != origEntry.getAddress() && (decodedAddress != 0 || decodedEntry.reg < paramBase)) {
                            System.err.println("local address mismatch at orig " + i + " / decoded " + decodeAt);
                            problem = true;
                            break;
                        }
                        decodeAt++;
                    } else {
                        System.err.println("local start/end mismatch at orig " + i + " / decoded " + decodeAt);
                        problem = true;
                        break;
                    }
                }
                System.err.println("local register mismatch at orig " + i + " / decoded " + decodeAt);
                problem = true;
                break;
            }
        }
        if (problem) {
            System.err.println("decoded locals:");
            for (LocalEntry e : decodedLocals) {
                System.err.println("  " + e);
            }
            throw new RuntimeException("local table problem");
        }
    }
}
