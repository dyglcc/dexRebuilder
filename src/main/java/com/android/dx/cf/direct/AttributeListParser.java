package com.android.dx.cf.direct;

import com.android.dx.cf.iface.Attribute;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.cf.iface.ParseObserver;
import com.android.dx.cf.iface.StdAttributeList;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;

final class AttributeListParser {
    private final AttributeFactory attributeFactory;
    private final DirectClassFile cf;
    private final int context;
    private int endOffset;
    private final StdAttributeList list;
    private ParseObserver observer;
    private final int offset;

    public AttributeListParser(DirectClassFile cf, int context, int offset, AttributeFactory attributeFactory) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        } else if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        } else {
            int size = cf.getBytes().getUnsignedShort(offset);
            this.cf = cf;
            this.context = context;
            this.offset = offset;
            this.attributeFactory = attributeFactory;
            this.list = new StdAttributeList(size);
            this.endOffset = -1;
        }
    }

    public void setObserver(ParseObserver observer) {
        this.observer = observer;
    }

    public int getEndOffset() {
        parseIfNecessary();
        return this.endOffset;
    }

    public StdAttributeList getList() {
        parseIfNecessary();
        return this.list;
    }

    private void parseIfNecessary() {
        if (this.endOffset < 0) {
            parse();
        }
    }

    private void parse() {
        int sz = this.list.size();
        int at = this.offset + 2;
        ByteArray bytes = this.cf.getBytes();
        if (this.observer != null) {
            this.observer.parsed(bytes, this.offset, 2, "attributes_count: " + Hex.u2(sz));
        }
        int i = 0;
        while (i < sz) {
            try {
                if (this.observer != null) {
                    this.observer.parsed(bytes, at, 0, "\nattributes[" + i + "]:\n");
                    this.observer.changeIndent(1);
                }
                Attribute attrib = this.attributeFactory.parse(this.cf, this.context, at, this.observer);
                at += attrib.byteLength();
                this.list.set(i, attrib);
                if (this.observer != null) {
                    this.observer.changeIndent(-1);
                    this.observer.parsed(bytes, at, 0, "end attributes[" + i + "]\n");
                }
                i++;
            } catch (ParseException ex) {
                ex.addContext("...while parsing attributes[" + i + "]");
                throw ex;
            } catch (Throwable ex2) {
                ParseException pe = new ParseException(ex2);
                pe.addContext("...while parsing attributes[" + i + "]");
                throw pe;
            }
        }
        this.endOffset = at;
    }
}
