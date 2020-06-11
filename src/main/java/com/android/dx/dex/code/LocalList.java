package com.android.dx.dex.code;

import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.code.RegisterSpecSet;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.util.FixedSizeList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public final class LocalList extends FixedSizeList {
    private static final boolean DEBUG = false;
    public static final LocalList EMPTY = new LocalList(0);

    public enum Disposition {
        START,
        END_SIMPLY,
        END_REPLACED,
        END_MOVED,
        END_CLOBBERED_BY_PREV,
        END_CLOBBERED_BY_NEXT
    }

    public static class Entry implements Comparable<Entry> {
        private final int address;
        private final Disposition disposition;
        private final RegisterSpec spec;
        private final CstType type;

        public Entry(int address, Disposition disposition, RegisterSpec spec) {
            if (address < 0) {
                throw new IllegalArgumentException("address < 0");
            } else if (disposition == null) {
                throw new NullPointerException("disposition == null");
            } else {
                try {
                    if (spec.getLocalItem() == null) {
                        throw new NullPointerException("spec.getLocalItem() == null");
                    }
                    this.address = address;
                    this.disposition = disposition;
                    this.spec = spec;
                    this.type = CstType.intern(spec.getType());
                } catch (NullPointerException e) {
                    throw new NullPointerException("spec == null");
                }
            }
        }

        public String toString() {
            return Integer.toHexString(this.address) + " " + this.disposition + " " + this.spec;
        }

        public boolean equals(Object other) {
            if ((other instanceof Entry) && compareTo((Entry) other) == 0) {
                return true;
            }
            return false;
        }

        public int compareTo(Entry other) {
            if (this.address < other.address) {
                return -1;
            }
            if (this.address > other.address) {
                return 1;
            }
            boolean thisIsStart = isStart();
            if (thisIsStart == other.isStart()) {
                return this.spec.compareTo(other.spec);
            }
            if (thisIsStart) {
                return 1;
            }
            return -1;
        }

        public int getAddress() {
            return this.address;
        }

        public Disposition getDisposition() {
            return this.disposition;
        }

        public boolean isStart() {
            return this.disposition == Disposition.START;
        }

        public CstString getName() {
            return this.spec.getLocalItem().getName();
        }

        public CstString getSignature() {
            return this.spec.getLocalItem().getSignature();
        }

        public CstType getType() {
            return this.type;
        }

        public int getRegister() {
            return this.spec.getReg();
        }

        public RegisterSpec getRegisterSpec() {
            return this.spec;
        }

        public boolean matches(RegisterSpec otherSpec) {
            return this.spec.equalsUsingSimpleType(otherSpec);
        }

        public boolean matches(Entry other) {
            return matches(other.spec);
        }

        public Entry withDisposition(Disposition disposition) {
            return disposition == this.disposition ? this : new Entry(this.address, disposition, this.spec);
        }
    }

    public static class MakeState {
        private int[] endIndices = null;
        private final int lastAddress = 0;
        private int nullResultCount = 0;
        private RegisterSpecSet regs = null;
        private final ArrayList<Entry> result;

        public MakeState(int initialSize) {
            this.result = new ArrayList(initialSize);
        }

        private void aboutToProcess(int address, int reg) {
            boolean first;
            if (this.endIndices == null) {
                first = true;
            } else {
                first = false;
            }
            if (address == this.lastAddress && !first) {
                return;
            }
            if (address < this.lastAddress) {
                throw new RuntimeException("shouldn't happen");
            } else if (first || reg >= this.endIndices.length) {
                int newSz = reg + 1;
                RegisterSpecSet newRegs = new RegisterSpecSet(newSz);
                int[] newEnds = new int[newSz];
                Arrays.fill(newEnds, -1);
                if (!first) {
                    newRegs.putAll(this.regs);
                    System.arraycopy(this.endIndices, 0, newEnds, 0, this.endIndices.length);
                }
                this.regs = newRegs;
                this.endIndices = newEnds;
            }
        }

        public void snapshot(int address, RegisterSpecSet specs) {
            int sz = specs.getMaxSize();
            aboutToProcess(address, sz - 1);
            for (int i = 0; i < sz; i++) {
                RegisterSpec oldSpec = this.regs.get(i);
                RegisterSpec newSpec = filterSpec(specs.get(i));
                if (oldSpec == null) {
                    if (newSpec != null) {
                        startLocal(address, newSpec);
                    }
                } else if (newSpec == null) {
                    endLocal(address, oldSpec);
                } else if (!newSpec.equalsUsingSimpleType(oldSpec)) {
                    endLocal(address, oldSpec);
                    startLocal(address, newSpec);
                }
            }
        }

        public void startLocal(int address, RegisterSpec startedLocal) {
            int regNum = startedLocal.getReg();
            startedLocal = filterSpec(startedLocal);
            aboutToProcess(address, regNum);
            RegisterSpec existingLocal = this.regs.get(regNum);
            if (!startedLocal.equalsUsingSimpleType(existingLocal)) {
                RegisterSpec movedLocal = this.regs.findMatchingLocal(startedLocal);
                if (movedLocal != null) {
                    addOrUpdateEnd(address, Disposition.END_MOVED, movedLocal);
                }
                int endAt = this.endIndices[regNum];
                if (existingLocal != null) {
                    add(address, Disposition.END_REPLACED, existingLocal);
                } else if (endAt >= 0) {
                    Entry endEntry = (Entry) this.result.get(endAt);
                    if (endEntry.getAddress() == address) {
                        if (endEntry.matches(startedLocal)) {
                            this.result.set(endAt, null);
                            this.nullResultCount++;
                            this.regs.put(startedLocal);
                            this.endIndices[regNum] = -1;
                            return;
                        }
                        this.result.set(endAt, endEntry.withDisposition(Disposition.END_REPLACED));
                    }
                }
                if (regNum > 0) {
                    RegisterSpec justBelow = this.regs.get(regNum - 1);
                    if (justBelow != null && justBelow.isCategory2()) {
                        addOrUpdateEnd(address, Disposition.END_CLOBBERED_BY_NEXT, justBelow);
                    }
                }
                if (startedLocal.isCategory2()) {
                    RegisterSpec justAbove = this.regs.get(regNum + 1);
                    if (justAbove != null) {
                        addOrUpdateEnd(address, Disposition.END_CLOBBERED_BY_PREV, justAbove);
                    }
                }
                add(address, Disposition.START, startedLocal);
            }
        }

        public void endLocal(int address, RegisterSpec endedLocal) {
            endLocal(address, endedLocal, Disposition.END_SIMPLY);
        }

        public void endLocal(int address, RegisterSpec endedLocal, Disposition disposition) {
            int regNum = endedLocal.getReg();
            endedLocal = filterSpec(endedLocal);
            aboutToProcess(address, regNum);
            if (this.endIndices[regNum] < 0 && !checkForEmptyRange(address, endedLocal)) {
                add(address, disposition, endedLocal);
            }
        }

        private boolean checkForEmptyRange(int address, RegisterSpec endedLocal) {
            Entry entry;
            int at = this.result.size() - 1;
            while (at >= 0) {
                entry = (Entry) this.result.get(at);
                if (entry != null) {
                    if (entry.getAddress() != address) {
                        return false;
                    }
                    if (entry.matches(endedLocal)) {
                        break;
                    }
                }
                at--;
            }
            this.regs.remove(endedLocal);
            this.result.set(at, null);
            this.nullResultCount++;
            int regNum = endedLocal.getReg();
            boolean found = false;
            entry = null;
            at--;
            while (at >= 0) {
                entry = (Entry) this.result.get(at);
                if (entry != null && entry.getRegisterSpec().getReg() == regNum) {
                    found = true;
                    break;
                }
                at--;
            }
            if (found) {
                this.endIndices[regNum] = at;
                if (entry.getAddress() == address) {
                    this.result.set(at, entry.withDisposition(Disposition.END_SIMPLY));
                }
            }
            return true;
        }

        private static RegisterSpec filterSpec(RegisterSpec orig) {
            if (orig == null || orig.getType() != Type.KNOWN_NULL) {
                return orig;
            }
            return orig.withType(Type.OBJECT);
        }

        private void add(int address, Disposition disposition, RegisterSpec spec) {
            int regNum = spec.getReg();
            this.result.add(new Entry(address, disposition, spec));
            if (disposition == Disposition.START) {
                this.regs.put(spec);
                this.endIndices[regNum] = -1;
                return;
            }
            this.regs.remove(spec);
            this.endIndices[regNum] = this.result.size() - 1;
        }

        private void addOrUpdateEnd(int address, Disposition disposition, RegisterSpec spec) {
            if (disposition == Disposition.START) {
                throw new RuntimeException("shouldn't happen");
            }
            int endAt = this.endIndices[spec.getReg()];
            if (endAt >= 0) {
                Entry endEntry = (Entry) this.result.get(endAt);
                if (endEntry.getAddress() == address && endEntry.getRegisterSpec().equals(spec)) {
                    this.result.set(endAt, endEntry.withDisposition(disposition));
                    this.regs.remove(spec);
                    return;
                }
            }
            endLocal(address, spec, disposition);
        }

        public LocalList finish() {
            aboutToProcess(Integer.MAX_VALUE, 0);
            int resultSz = this.result.size();
            int finalSz = resultSz - this.nullResultCount;
            if (finalSz == 0) {
                return LocalList.EMPTY;
            }
            Entry[] resultArr = new Entry[finalSz];
            if (resultSz == finalSz) {
                this.result.toArray(resultArr);
            } else {
                int at = 0;
                Iterator it = this.result.iterator();
                while (it.hasNext()) {
                    Entry e = (Entry) it.next();
                    if (e != null) {
                        int at2 = at + 1;
                        resultArr[at] = e;
                        at = at2;
                    }
                }
            }
            Arrays.sort(resultArr);
            LocalList resultList = new LocalList(finalSz);
            for (int i = 0; i < finalSz; i++) {
                resultList.set(i, resultArr[i]);
            }
            resultList.setImmutable();
            return resultList;
        }
    }

    public LocalList(int size) {
        super(size);
    }

    public Entry get(int n) {
        return (Entry) get0(n);
    }

    public void set(int n, Entry entry) {
        set0(n, entry);
    }

    public void debugPrint(PrintStream out, String prefix) {
        int sz = size();
        for (int i = 0; i < sz; i++) {
            out.print(prefix);
            out.println(get(i));
        }
    }

    public static LocalList make(DalvInsnList insns) {
        int sz = insns.size();
        MakeState state = new MakeState(sz);
        for (int i = 0; i < sz; i++) {
            DalvInsn insn = insns.get(i);
            if (insn instanceof LocalSnapshot) {
                state.snapshot(insn.getAddress(), ((LocalSnapshot) insn).getLocals());
            } else if (insn instanceof LocalStart) {
                state.startLocal(insn.getAddress(), ((LocalStart) insn).getLocal());
            }
        }
        return state.finish();
    }

    private static void debugVerify(LocalList locals) {
        try {
            debugVerify0(locals);
        } catch (RuntimeException ex) {
            int sz = locals.size();
            for (int i = 0; i < sz; i++) {
                System.err.println(locals.get(i));
            }
            throw ex;
        }
    }

    private static void debugVerify0(LocalList locals) {
        int sz = locals.size();
        Entry[] active = new Entry[AccessFlags.ACC_CONSTRUCTOR];
        for (int i = 0; i < sz; i++) {
            Entry e = locals.get(i);
            int reg = e.getRegister();
            if (e.isStart()) {
                Entry already = active[reg];
                if (already == null || !e.matches(already)) {
                    active[reg] = e;
                } else {
                    throw new RuntimeException("redundant start at " + Integer.toHexString(e.getAddress()) + ": got " + e + "; had " + already);
                }
            } else if (active[reg] == null) {
                throw new RuntimeException("redundant end at " + Integer.toHexString(e.getAddress()));
            } else {
                int addr = e.getAddress();
                boolean foundStart = false;
                for (int j = i + 1; j < sz; j++) {
                    Entry test = locals.get(j);
                    if (test.getAddress() != addr) {
                        break;
                    }
                    if (test.getRegisterSpec().getReg() == reg) {
                        if (!test.isStart()) {
                            throw new RuntimeException("redundant end at " + Integer.toHexString(addr));
                        } else if (e.getDisposition() != Disposition.END_REPLACED) {
                            throw new RuntimeException("improperly marked end at " + Integer.toHexString(addr));
                        } else {
                            foundStart = true;
                        }
                    }
                }
                if (foundStart || e.getDisposition() != Disposition.END_REPLACED) {
                    active[reg] = null;
                } else {
                    throw new RuntimeException("improper end replacement claim at " + Integer.toHexString(addr));
                }
            }
        }
    }
}
