package com.android.dx.dex.file;

import com.android.dex.DexFormat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;

public final class HeaderItem extends IndexedItem {
    public ItemType itemType() {
        return ItemType.TYPE_HEADER_ITEM;
    }

    public int writeSize() {
        return 112;
    }

    public void addContents(DexFile file) {
    }

    public void writeTo(DexFile file, AnnotatedOutput out) {
        int mapOff = file.getMap().getFileOffset();
        Section firstDataSection = file.getFirstDataSection();
        Section lastDataSection = file.getLastDataSection();
        int dataOff = firstDataSection.getFileOffset();
        int dataSize = (lastDataSection.getFileOffset() + lastDataSection.writeSize()) - dataOff;
        String magic = file.getDexOptions().getMagic();
        if (out.annotates()) {
            out.annotate(8, "magic: " + new CstString(magic).toQuoted());
            out.annotate(4, "checksum");
            out.annotate(20, "signature");
            out.annotate(4, "file_size:       " + Hex.u4(file.getFileSize()));
            out.annotate(4, "header_size:     " + Hex.u4(112));
            out.annotate(4, "endian_tag:      " + Hex.u4(DexFormat.ENDIAN_TAG));
            out.annotate(4, "link_size:       0");
            out.annotate(4, "link_off:        0");
            out.annotate(4, "map_off:         " + Hex.u4(mapOff));
        }
        for (int i = 0; i < 8; i++) {
            out.writeByte(magic.charAt(i));
        }
        out.writeZeroes(24);
        out.writeInt(file.getFileSize());
        out.writeInt(112);
        out.writeInt(DexFormat.ENDIAN_TAG);
        out.writeZeroes(8);
        out.writeInt(mapOff);
        file.getStringIds().writeHeaderPart(out);
        file.getTypeIds().writeHeaderPart(out);
        file.getProtoIds().writeHeaderPart(out);
        file.getFieldIds().writeHeaderPart(out);
        file.getMethodIds().writeHeaderPart(out);
        file.getClassDefs().writeHeaderPart(out);
        if (out.annotates()) {
            out.annotate(4, "data_size:       " + Hex.u4(dataSize));
            out.annotate(4, "data_off:        " + Hex.u4(dataOff));
        }
        out.writeInt(dataSize);
        out.writeInt(dataOff);
    }
}