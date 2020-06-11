package com.android.dx.io.instructions;

import java.util.HashMap;

public final class AddressMap {
    private final HashMap<Integer, Integer> map = new HashMap();

    public int get(int keyAddress) {
        Integer value = (Integer) this.map.get(Integer.valueOf(keyAddress));
        return value == null ? -1 : value.intValue();
    }

    public void put(int keyAddress, int valueAddress) {
        this.map.put(Integer.valueOf(keyAddress), Integer.valueOf(valueAddress));
    }
}
