package com.android.dx.dex.file;

import com.android.dx.util.AnnotatedOutput;
import java.util.HashMap;
import java.util.TreeMap;

public final class Statistics {
    private final HashMap<String, Data> dataMap = new HashMap(50);

    private static class Data {
        private int count = 1;
        private int largestSize;
        private final String name;
        private int smallestSize;
        private int totalSize;

        public Data(Item item, String name) {
            int size = item.writeSize();
            this.name = name;
            this.totalSize = size;
            this.largestSize = size;
            this.smallestSize = size;
        }

        public void add(Item item) {
            int size = item.writeSize();
            this.count++;
            this.totalSize += size;
            if (size > this.largestSize) {
                this.largestSize = size;
            }
            if (size < this.smallestSize) {
                this.smallestSize = size;
            }
        }

        public void writeAnnotation(AnnotatedOutput out) {
            out.annotate(toHuman());
        }

        public String toHuman() {
            StringBuilder sb = new StringBuilder();
            sb.append("  " + this.name + ": " + this.count + " item" + (this.count == 1 ? "" : "s") + "; " + this.totalSize + " bytes total\n");
            if (this.smallestSize == this.largestSize) {
                sb.append("    " + this.smallestSize + " bytes/item\n");
            } else {
                sb.append("    " + this.smallestSize + ".." + this.largestSize + " bytes/item; average " + (this.totalSize / this.count) + "\n");
            }
            return sb.toString();
        }
    }

    public void add(Item item) {
        String typeName = item.typeName();
        Data data = (Data) this.dataMap.get(typeName);
        if (data == null) {
            this.dataMap.put(typeName, new Data(item, typeName));
        } else {
            data.add(item);
        }
    }

    public void addAll(Section list) {
        for (Item item : list.items()) {
            add(item);
        }
    }

    public final void writeAnnotation(AnnotatedOutput out) {
        if (this.dataMap.size() != 0) {
            out.annotate(0, "\nstatistics:\n");
            TreeMap<String, Data> sortedData = new TreeMap();
            for (Data data : this.dataMap.values()) {
                sortedData.put(data.name, data);
            }
            for (Data data2 : sortedData.values()) {
                data2.writeAnnotation(out);
            }
        }
    }

    public String toHuman() {
        StringBuilder sb = new StringBuilder();
        sb.append("Statistics:\n");
        TreeMap<String, Data> sortedData = new TreeMap();
        for (Data data : this.dataMap.values()) {
            sortedData.put(data.name, data);
        }
        for (Data data2 : sortedData.values()) {
            sb.append(data2.toHuman());
        }
        return sb.toString();
    }
}
