package com.android.dx.dex.file;

import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.StdTypeList;
import com.android.dx.rop.type.Type;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;

public final class ProtoIdItem extends IndexedItem {
    private TypeListItem parameterTypes;
    private final Prototype prototype;
    private final CstString shortForm;

    public ProtoIdItem(Prototype prototype) {
        if (prototype == null) {
            throw new NullPointerException("prototype == null");
        }
        TypeListItem typeListItem;
        this.prototype = prototype;
        this.shortForm = makeShortForm(prototype);
        StdTypeList parameters = prototype.getParameterTypes();
        if (parameters.size() == 0) {
            typeListItem = null;
        } else {
            typeListItem = new TypeListItem(parameters);
        }
        this.parameterTypes = typeListItem;
    }

    private static CstString makeShortForm(Prototype prototype) {
        StdTypeList parameters = prototype.getParameterTypes();
        int size = parameters.size();
        StringBuilder sb = new StringBuilder(size + 1);
        sb.append(shortFormCharFor(prototype.getReturnType()));
        for (int i = 0; i < size; i++) {
            sb.append(shortFormCharFor(parameters.getType(i)));
        }
        return new CstString(sb.toString());
    }

    private static char shortFormCharFor(Type type) {
        char descriptorChar = type.getDescriptor().charAt(0);
        if (descriptorChar == '[') {
            return 'L';
        }
        return descriptorChar;
    }

    public ItemType itemType() {
        return ItemType.TYPE_PROTO_ID_ITEM;
    }

    public int writeSize() {
        return 12;
    }

    public void addContents(DexFile file) {
        StringIdsSection stringIds = file.getStringIds();
        TypeIdsSection typeIds = file.getTypeIds();
        MixedItemSection typeLists = file.getTypeLists();
        typeIds.intern(this.prototype.getReturnType());
        stringIds.intern(this.shortForm);
        if (this.parameterTypes != null) {
            this.parameterTypes = (TypeListItem) typeLists.intern(this.parameterTypes);
        }
    }

    public void writeTo(DexFile file, AnnotatedOutput out) {
        int shortyIdx = file.getStringIds().indexOf(this.shortForm);
        int returnIdx = file.getTypeIds().indexOf(this.prototype.getReturnType());
        int paramsOff = OffsettedItem.getAbsoluteOffsetOr0(this.parameterTypes);
        if (out.annotates()) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.prototype.getReturnType().toHuman());
            sb.append(" proto(");
            StdTypeList params = this.prototype.getParameterTypes();
            int size = params.size();
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(params.getType(i).toHuman());
            }
            sb.append(")");
            out.annotate(0, indexString() + ' ' + sb.toString());
            out.annotate(4, "  shorty_idx:      " + Hex.u4(shortyIdx) + " // " + this.shortForm.toQuoted());
            out.annotate(4, "  return_type_idx: " + Hex.u4(returnIdx) + " // " + this.prototype.getReturnType().toHuman());
            out.annotate(4, "  parameters_off:  " + Hex.u4(paramsOff));
        }
        out.writeInt(shortyIdx);
        out.writeInt(returnIdx);
        out.writeInt(paramsOff);
    }
}
