package com.android.dx.dex.code;

import com.android.dx.rop.code.SourcePosition;
import com.android.dx.util.FixedSizeList;

public final class PositionList extends FixedSizeList {
    public static final PositionList EMPTY = new PositionList(0);
    public static final int IMPORTANT = 3;
    public static final int LINES = 2;
    public static final int NONE = 1;

    public static class Entry {
        private final int address;
        private final SourcePosition position;

        public Entry(int address, SourcePosition position) {
            if (address < 0) {
                throw new IllegalArgumentException("address < 0");
            } else if (position == null) {
                throw new NullPointerException("position == null");
            } else {
                this.address = address;
                this.position = position;
            }
        }

        public int getAddress() {
            return this.address;
        }

        public SourcePosition getPosition() {
            return this.position;
        }
    }

    public static PositionList make(DalvInsnList insns, int howMuch) {
        switch (howMuch) {
            case 1:
                return EMPTY;
            case 2:
            case 3:
                int i;
                SourcePosition noInfo = SourcePosition.NO_INFO;
                SourcePosition cur = noInfo;
                int sz = insns.size();
                Entry[] arr = new Entry[sz];
                boolean lastWasTarget = false;
                int at = 0;
                for (i = 0; i < sz; i++) {
                    DalvInsn insn = insns.get(i);
                    if (insn instanceof CodeAddress) {
                        lastWasTarget = true;
                    } else {
                        SourcePosition pos = insn.getPosition();
                        if (!(pos.equals(noInfo) || pos.sameLine(cur) || (howMuch == 3 && !lastWasTarget))) {
                            cur = pos;
                            arr[at] = new Entry(insn.getAddress(), pos);
                            at++;
                            lastWasTarget = false;
                        }
                    }
                }
                PositionList result = new PositionList(at);
                for (i = 0; i < at; i++) {
                    result.set(i, arr[i]);
                }
                result.setImmutable();
                return result;
            default:
                throw new IllegalArgumentException("bogus howMuch");
        }
    }

    public PositionList(int size) {
        super(size);
    }

    public Entry get(int n) {
        return (Entry) get0(n);
    }

    public void set(int n, Entry entry) {
        set0(n, entry);
    }
}
