package com.android.dx.cf.attrib;

import com.android.dx.cf.code.LocalVariableList;

public final class AttLocalVariableTypeTable extends BaseLocalVariables {
    public static final String ATTRIBUTE_NAME = "LocalVariableTypeTable";

    public AttLocalVariableTypeTable(LocalVariableList localVariables) {
        super(ATTRIBUTE_NAME, localVariables);
    }
}
