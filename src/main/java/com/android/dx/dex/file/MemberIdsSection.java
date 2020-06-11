package com.android.dx.dex.file;

import com.android.dex.DexIndexOverflowException;
import com.android.dx.rop.code.AccessFlags;
import java.util.Formatter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MemberIdsSection extends UniformItemSection {
    public MemberIdsSection(String name, DexFile file) {
        super(name, file, 4);
    }

    protected void orderItems() {
        int idx = 0;
        if (items().size() > AccessFlags.ACC_CONSTRUCTOR) {
            throw new DexIndexOverflowException(getTooManyMembersMessage());
        }
        for (MemberIdItem i : items()) {
            i.setIndex(idx);
            idx++;
        }
    }

    private String getTooManyMembersMessage() {
        Map<String, AtomicInteger> membersByPackage = new TreeMap();
        for (MemberIdItem member : items()) {
            String packageName = member.getDefiningClass().getPackageName();
            AtomicInteger count = (AtomicInteger) membersByPackage.get(packageName);
            if (count == null) {
                count = new AtomicInteger();
                membersByPackage.put(packageName, count);
            }
            count.incrementAndGet();
        }
        Formatter formatter = new Formatter();
        try {
            String memberType = this instanceof MethodIdsSection ? "method" : "field";
            formatter.format("Too many %1$s references to fit in one dex file: %2$d; max is %3$d.%nYou may try using multi-dex. If multi-dex is enabled then the list of classes for the main dex list is too large.%nReferences by package:", new Object[]{memberType, Integer.valueOf(items().size()), Integer.valueOf(AccessFlags.ACC_CONSTRUCTOR)});
            for (Entry<String, AtomicInteger> entry : membersByPackage.entrySet()) {
                formatter.format("%n%6d %s", new Object[]{Integer.valueOf(((AtomicInteger) entry.getValue()).get()), entry.getKey()});
            }
            String formatter2 = formatter.toString();
            return formatter2;
        } finally {
            formatter.close();
        }
    }
}
