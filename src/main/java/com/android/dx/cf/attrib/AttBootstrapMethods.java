package com.android.dx.cf.attrib;

import com.android.dx.cf.code.BootstrapMethodsList;

public class AttBootstrapMethods extends BaseAttribute {
    private static final int ATTRIBUTE_HEADER_BYTES = 8;
    public static final String ATTRIBUTE_NAME = "BootstrapMethods";
    private static final int BOOTSTRAP_ARGUMENT_BYTES = 2;
    private static final int BOOTSTRAP_METHOD_BYTES = 4;
    private final BootstrapMethodsList bootstrapMethods;
    private final int byteLength;

    public AttBootstrapMethods(BootstrapMethodsList bootstrapMethods) {
        super(ATTRIBUTE_NAME);
        this.bootstrapMethods = bootstrapMethods;
        int bytes = (bootstrapMethods.size() * 4) + 8;
        for (int i = 0; i < bootstrapMethods.size(); i++) {
            bytes += bootstrapMethods.get(i).getBootstrapMethodArguments().size() * 2;
        }
        this.byteLength = bytes;
    }

    public int byteLength() {
        return this.byteLength;
    }

    public BootstrapMethodsList getBootstrapMethods() {
        return this.bootstrapMethods;
    }
}
