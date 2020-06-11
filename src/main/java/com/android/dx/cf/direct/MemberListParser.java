package com.android.dx.cf.direct;

import com.android.dx.cf.iface.AttributeList;
import com.android.dx.cf.iface.Member;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.cf.iface.ParseObserver;
import com.android.dx.cf.iface.StdAttributeList;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;

abstract class MemberListParser {
    private final AttributeFactory attributeFactory;
    private final DirectClassFile cf;
    private final CstType definer;
    private int endOffset;
    private ParseObserver observer;
    private final int offset;

    protected abstract int getAttributeContext();

    protected abstract String humanAccessFlags(int i);

    protected abstract String humanName();

    protected abstract Member set(int i, int i2, CstNat cstNat, AttributeList attributeList);

    public MemberListParser(DirectClassFile cf, CstType definer, int offset, AttributeFactory attributeFactory) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        } else if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        } else if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        } else {
            this.cf = cf;
            this.definer = definer;
            this.offset = offset;
            this.attributeFactory = attributeFactory;
            this.endOffset = -1;
        }
    }

    public int getEndOffset() {
        parseIfNecessary();
        return this.endOffset;
    }

    public final void setObserver(ParseObserver observer) {
        this.observer = observer;
    }

    protected final void parseIfNecessary() {
        if (this.endOffset < 0) {
            parse();
        }
    }

    protected final int getCount() {
        return this.cf.getBytes().getUnsignedShort(this.offset);
    }

    protected final CstType getDefiner() {
        return this.definer;
    }

    private void parse() {
        int attributeContext = getAttributeContext();
        int count = getCount();
        int at = this.offset + 2;
        ByteArray bytes = this.cf.getBytes();
        ConstantPool pool = this.cf.getConstantPool();
        if (this.observer != null) {
            this.observer.parsed(bytes, this.offset, 2, humanName() + "s_count: " + Hex.u2(count));
        }
        int i = 0;
        while (i < count) {
            try {
                int accessFlags = bytes.getUnsignedShort(at);
                int nameIdx = bytes.getUnsignedShort(at + 2);
                CstString name = (CstString) pool.get(nameIdx);
                CstString desc = (CstString) pool.get(bytes.getUnsignedShort(at + 4));
                if (this.observer != null) {
                    this.observer.startParsingMember(bytes, at, name.getString(), desc.getString());
                    this.observer.parsed(bytes, at, 0, "\n" + humanName() + "s[" + i + "]:\n");
                    this.observer.changeIndent(1);
                    this.observer.parsed(bytes, at, 2, "access_flags: " + humanAccessFlags(accessFlags));
                    this.observer.parsed(bytes, at + 2, 2, "name: " + name.toHuman());
                    this.observer.parsed(bytes, at + 4, 2, "descriptor: " + desc.toHuman());
                }
                AttributeListParser attributeListParser = new AttributeListParser(this.cf, attributeContext, at + 6, this.attributeFactory);
                attributeListParser.setObserver(this.observer);
                at = attributeListParser.getEndOffset();
                StdAttributeList attributes = attributeListParser.getList();
                attributes.setImmutable();
                Member member = set(i, accessFlags, new CstNat(name, desc), attributes);
                if (this.observer != null) {
                    this.observer.changeIndent(-1);
                    this.observer.parsed(bytes, at, 0, "end " + humanName() + "s[" + i + "]\n");
                    this.observer.endParsingMember(bytes, at, name.getString(), desc.getString(), member);
                }
                i++;
            } catch (ParseException ex) {
                ex.addContext("...while parsing " + humanName() + "s[" + i + "]");
                throw ex;
            } catch (Throwable ex2) {
                ParseException parseException = new ParseException(ex2);
                parseException.addContext("...while parsing " + humanName() + "s[" + i + "]");
                throw parseException;
            }
        }
        this.endOffset = at;
    }
}
