package com.android.dx.merge;

import com.android.dex.Annotation;
import com.android.dex.CallSiteId;
import com.android.dex.ClassData;
import com.android.dex.ClassData.Field;
import com.android.dex.ClassData.Method;
import com.android.dex.ClassDef;
import com.android.dex.Code;
import com.android.dex.Code.CatchHandler;
import com.android.dex.Code.Try;
import com.android.dex.Dex;
import com.android.dex.Dex.Section;
import com.android.dex.DexException;
import com.android.dex.DexIndexOverflowException;
import com.android.dex.FieldId;
import com.android.dex.MethodHandle;
import com.android.dex.MethodId;
import com.android.dex.ProtoId;
import com.android.dex.TableOfContents;
import com.android.dex.TypeList;
import com.android.dx.command.dexer.DxContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

public final class DexMerger {
    private static final byte DBG_ADVANCE_LINE = (byte) 2;
    private static final byte DBG_ADVANCE_PC = (byte) 1;
    private static final byte DBG_END_LOCAL = (byte) 5;
    private static final byte DBG_END_SEQUENCE = (byte) 0;
    private static final byte DBG_RESTART_LOCAL = (byte) 6;
    private static final byte DBG_SET_EPILOGUE_BEGIN = (byte) 8;
    private static final byte DBG_SET_FILE = (byte) 9;
    private static final byte DBG_SET_PROLOGUE_END = (byte) 7;
    private static final byte DBG_START_LOCAL = (byte) 3;
    private static final byte DBG_START_LOCAL_EXTENDED = (byte) 4;
    private final Section annotationOut;
    private final Section annotationSetOut;
    private final Section annotationSetRefListOut;
    private final Section annotationsDirectoryOut;
    private final Section classDataOut;
    private final Section codeOut;
    private final CollisionPolicy collisionPolicy;
    private int compactWasteThreshold;
    private final TableOfContents contentsOut;
    private final DxContext context;
    private final Section debugInfoOut;
    private final Dex dexOut;
    private final Dex[] dexes;
    private final Section encodedArrayOut;
    private final Section headerOut;
    private final Section idsDefsOut;
    private final IndexMap[] indexMaps;
    private final InstructionTransformer instructionTransformer;
    private final Section mapListOut;
    private final Section stringDataOut;
    private final Section typeListOut;
    private final WriterSizes writerSizes;

    abstract class IdMerger<T extends Comparable<T>> {
        private final Section out;

        class UnsortedValue implements Comparable<UnsortedValue> {
            final int index;
            final IndexMap indexMap;
            final int offset;
            final Dex source;
            final T value;

            UnsortedValue(Dex source, IndexMap indexMap, T value, int index, int offset) {
                this.source = source;
                this.indexMap = indexMap;
                this.value = value;
                this.index = index;
                this.offset = offset;
            }

            public int compareTo(UnsortedValue unsortedValue) {
                return this.value.compareTo(unsortedValue.value);
            }
        }

        abstract TableOfContents.Section getSection(TableOfContents tableOfContents);

        abstract T read(Section section, IndexMap indexMap, int i);

        abstract void updateIndex(int i, IndexMap indexMap, int i2, int i3);

        abstract void write(T t);

        protected IdMerger(Section out) {
            this.out = out;
        }

        public final void mergeSorted() {
            TableOfContents.Section[] sections = new TableOfContents.Section[DexMerger.this.dexes.length];
            Section[] dexSections = new Section[DexMerger.this.dexes.length];
            int[] offsets = new int[DexMerger.this.dexes.length];
            int[] indexes = new int[DexMerger.this.dexes.length];
            TreeMap<T, List<Integer>> values = new TreeMap();
            for (int i = 0; i < DexMerger.this.dexes.length; i++) {
                sections[i] = getSection(DexMerger.this.dexes[i].getTableOfContents());
                dexSections[i] = sections[i].exists() ? DexMerger.this.dexes[i].open(sections[i].off) : null;
                offsets[i] = readIntoMap(dexSections[i], sections[i], DexMerger.this.indexMaps[i], indexes[i], values, i);
            }
            if (values.isEmpty()) {
                getSection(DexMerger.this.contentsOut).off = 0;
                getSection(DexMerger.this.contentsOut).size = 0;
                return;
            }
            getSection(DexMerger.this.contentsOut).off = this.out.getPosition();
            int outCount = 0;
            while (!values.isEmpty()) {
                Entry<T, List<Integer>> first = values.pollFirstEntry();
                for (Integer dex : (List) first.getValue()) {
                    int i2 = offsets[dex.intValue()];
                    IndexMap indexMap = DexMerger.this.indexMaps[dex.intValue()];
                    int intValue = dex.intValue();
                    int i3 = indexes[intValue];
                    indexes[intValue] = i3 + 1;
                    updateIndex(i2, indexMap, i3, outCount);
                    offsets[dex.intValue()] = readIntoMap(dexSections[dex.intValue()], sections[dex.intValue()], DexMerger.this.indexMaps[dex.intValue()], indexes[dex.intValue()], values, dex.intValue());
                }
                write((Comparable) first.getKey());
                outCount++;
            }
            getSection(DexMerger.this.contentsOut).size = outCount;
        }

        private int readIntoMap(Section in, TableOfContents.Section section, IndexMap indexMap, int index, TreeMap<T, List<Integer>> values, int dex) {
            int offset = in != null ? in.getPosition() : -1;
            if (index < section.size) {
                T v = read(in, indexMap, index);
                List<Integer> l = (List) values.get(v);
                if (l == null) {
                    l = new ArrayList();
                    values.put(v, l);
                }
                l.add(Integer.valueOf(dex));
            }
            return offset;
        }

        public final void mergeUnsorted() {
            int i;
            getSection(DexMerger.this.contentsOut).off = this.out.getPosition();
            List<UnsortedValue> all = new ArrayList();
            for (i = 0; i < DexMerger.this.dexes.length; i++) {
                all.addAll(readUnsortedValues(DexMerger.this.dexes[i], DexMerger.this.indexMaps[i]));
            }
            if (all.isEmpty()) {
                getSection(DexMerger.this.contentsOut).off = 0;
                getSection(DexMerger.this.contentsOut).size = 0;
                return;
            }
            Collections.sort(all);
            int outCount = 0;
            i = 0;
            while (i < all.size()) {
                int i2 = i + 1;
                UnsortedValue e1 = (UnsortedValue) all.get(i);
                updateIndex(e1.offset, e1.indexMap, e1.index, outCount - 1);
                i = i2;
                while (i < all.size() && e1.compareTo((UnsortedValue) all.get(i)) == 0) {
                    i2 = i + 1;
                    UnsortedValue e2 = (UnsortedValue) all.get(i);
                    updateIndex(e2.offset, e2.indexMap, e2.index, outCount - 1);
                    i = i2;
                }
                write(e1.value);
                outCount++;
            }
            getSection(DexMerger.this.contentsOut).size = outCount;
        }

        private List<UnsortedValue> readUnsortedValues(Dex source, IndexMap indexMap) {
            TableOfContents.Section section = getSection(source.getTableOfContents());
            if (!section.exists()) {
                return Collections.emptyList();
            }
            List<UnsortedValue> result = new ArrayList();
            Section in = source.open(section.off);
            for (int i = 0; i < section.size; i++) {
                Dex dex = source;
                IndexMap indexMap2 = indexMap;
                result.add(new UnsortedValue(dex, indexMap2, read(in, indexMap, 0), i, in.getPosition()));
            }
            return result;
        }
    }

    private static class WriterSizes {
        private int annotation;
        private int annotationsDirectory;
        private int annotationsSet;
        private int annotationsSetRefList;
        private int classData;
        private int code;
        private int debugInfo;
        private int encodedArray;
        private int header = 112;
        private int idsDefs;
        private int mapList;
        private int stringData;
        private int typeList;

        public WriterSizes(Dex[] dexes) {
            for (Dex tableOfContents : dexes) {
                plus(tableOfContents.getTableOfContents(), false);
            }
            fourByteAlign();
        }

        public WriterSizes(DexMerger dexMerger) {
            this.header = dexMerger.headerOut.used();
            this.idsDefs = dexMerger.idsDefsOut.used();
            this.mapList = dexMerger.mapListOut.used();
            this.typeList = dexMerger.typeListOut.used();
            this.classData = dexMerger.classDataOut.used();
            this.code = dexMerger.codeOut.used();
            this.stringData = dexMerger.stringDataOut.used();
            this.debugInfo = dexMerger.debugInfoOut.used();
            this.encodedArray = dexMerger.encodedArrayOut.used();
            this.annotationsDirectory = dexMerger.annotationsDirectoryOut.used();
            this.annotationsSet = dexMerger.annotationSetOut.used();
            this.annotationsSetRefList = dexMerger.annotationSetRefListOut.used();
            this.annotation = dexMerger.annotationOut.used();
            fourByteAlign();
        }

        private void plus(TableOfContents contents, boolean exact) {
            this.idsDefs += (((((contents.stringIds.size * 4) + (contents.typeIds.size * 4)) + (contents.protoIds.size * 12)) + (contents.fieldIds.size * 8)) + (contents.methodIds.size * 8)) + (contents.classDefs.size * 32);
            this.mapList = (contents.sections.length * 12) + 4;
            this.typeList += fourByteAlign(contents.typeLists.byteCount);
            this.stringData += contents.stringDatas.byteCount;
            this.annotationsDirectory += contents.annotationsDirectories.byteCount;
            this.annotationsSet += contents.annotationSets.byteCount;
            this.annotationsSetRefList += contents.annotationSetRefLists.byteCount;
            if (exact) {
                this.code += contents.codes.byteCount;
                this.classData += contents.classDatas.byteCount;
                this.encodedArray += contents.encodedArrays.byteCount;
                this.annotation += contents.annotations.byteCount;
                this.debugInfo += contents.debugInfos.byteCount;
                return;
            }
            this.code += (int) Math.ceil(((double) contents.codes.byteCount) * 1.25d);
            this.classData += (int) Math.ceil(((double) contents.classDatas.byteCount) * 1.67d);
            this.encodedArray += contents.encodedArrays.byteCount * 2;
            this.annotation += (int) Math.ceil((double) (contents.annotations.byteCount * 2));
            this.debugInfo += (contents.debugInfos.byteCount * 2) + 8;
        }

        private void fourByteAlign() {
            this.header = fourByteAlign(this.header);
            this.idsDefs = fourByteAlign(this.idsDefs);
            this.mapList = fourByteAlign(this.mapList);
            this.typeList = fourByteAlign(this.typeList);
            this.classData = fourByteAlign(this.classData);
            this.code = fourByteAlign(this.code);
            this.stringData = fourByteAlign(this.stringData);
            this.debugInfo = fourByteAlign(this.debugInfo);
            this.encodedArray = fourByteAlign(this.encodedArray);
            this.annotationsDirectory = fourByteAlign(this.annotationsDirectory);
            this.annotationsSet = fourByteAlign(this.annotationsSet);
            this.annotationsSetRefList = fourByteAlign(this.annotationsSetRefList);
            this.annotation = fourByteAlign(this.annotation);
        }

        private static int fourByteAlign(int position) {
            return (position + 3) & -4;
        }

        public int size() {
            return (((((((((((this.header + this.idsDefs) + this.mapList) + this.typeList) + this.classData) + this.code) + this.stringData) + this.debugInfo) + this.encodedArray) + this.annotationsDirectory) + this.annotationsSet) + this.annotationsSetRefList) + this.annotation;
        }
    }

    public DexMerger(Dex[] dexes, CollisionPolicy collisionPolicy, DxContext context) throws IOException {
        this(dexes, collisionPolicy, context, new WriterSizes(dexes));
    }

    private DexMerger(Dex[] dexes, CollisionPolicy collisionPolicy, DxContext context, WriterSizes writerSizes) throws IOException {
        this.compactWasteThreshold = 1048576;
        this.dexes = dexes;
        this.collisionPolicy = collisionPolicy;
        this.context = context;
        this.writerSizes = writerSizes;
        this.dexOut = new Dex(writerSizes.size());
        this.indexMaps = new IndexMap[dexes.length];
        for (int i = 0; i < dexes.length; i++) {
            this.indexMaps[i] = new IndexMap(this.dexOut, dexes[i].getTableOfContents());
        }
        this.instructionTransformer = new InstructionTransformer();
        this.headerOut = this.dexOut.appendSection(writerSizes.header, "header");
        this.idsDefsOut = this.dexOut.appendSection(writerSizes.idsDefs, "ids defs");
        this.contentsOut = this.dexOut.getTableOfContents();
        this.contentsOut.dataOff = this.dexOut.getNextSectionStart();
        this.contentsOut.mapList.off = this.dexOut.getNextSectionStart();
        this.contentsOut.mapList.size = 1;
        this.mapListOut = this.dexOut.appendSection(writerSizes.mapList, "map list");
        this.contentsOut.typeLists.off = this.dexOut.getNextSectionStart();
        this.typeListOut = this.dexOut.appendSection(writerSizes.typeList, "type list");
        this.contentsOut.annotationSetRefLists.off = this.dexOut.getNextSectionStart();
        this.annotationSetRefListOut = this.dexOut.appendSection(writerSizes.annotationsSetRefList, "annotation set ref list");
        this.contentsOut.annotationSets.off = this.dexOut.getNextSectionStart();
        this.annotationSetOut = this.dexOut.appendSection(writerSizes.annotationsSet, "annotation sets");
        this.contentsOut.classDatas.off = this.dexOut.getNextSectionStart();
        this.classDataOut = this.dexOut.appendSection(writerSizes.classData, "class data");
        this.contentsOut.codes.off = this.dexOut.getNextSectionStart();
        this.codeOut = this.dexOut.appendSection(writerSizes.code, "code");
        this.contentsOut.stringDatas.off = this.dexOut.getNextSectionStart();
        this.stringDataOut = this.dexOut.appendSection(writerSizes.stringData, "string data");
        this.contentsOut.debugInfos.off = this.dexOut.getNextSectionStart();
        this.debugInfoOut = this.dexOut.appendSection(writerSizes.debugInfo, "debug info");
        this.contentsOut.annotations.off = this.dexOut.getNextSectionStart();
        this.annotationOut = this.dexOut.appendSection(writerSizes.annotation, "annotation");
        this.contentsOut.encodedArrays.off = this.dexOut.getNextSectionStart();
        this.encodedArrayOut = this.dexOut.appendSection(writerSizes.encodedArray, "encoded array");
        this.contentsOut.annotationsDirectories.off = this.dexOut.getNextSectionStart();
        this.annotationsDirectoryOut = this.dexOut.appendSection(writerSizes.annotationsDirectory, "annotations directory");
        this.contentsOut.dataSize = this.dexOut.getNextSectionStart() - this.contentsOut.dataOff;
    }

    public void setCompactWasteThreshold(int compactWasteThreshold) {
        this.compactWasteThreshold = compactWasteThreshold;
    }

    private Dex mergeDexes() throws IOException {
        mergeStringIds();
        mergeTypeIds();
        mergeTypeLists();
        mergeProtoIds();
        mergeFieldIds();
        mergeMethodIds();
        mergeMethodHandles();
        mergeAnnotations();
        unionAnnotationSetsAndDirectories();
        mergeCallSiteIds();
        mergeClassDefs();
        Arrays.sort(this.contentsOut.sections);
        this.contentsOut.header.off = 0;
        this.contentsOut.header.size = 1;
        this.contentsOut.fileSize = this.dexOut.getLength();
        this.contentsOut.computeSizesFromOffsets();
        this.contentsOut.writeHeader(this.headerOut, mergeApiLevels());
        this.contentsOut.writeMap(this.mapListOut);
        this.dexOut.writeHashes();
        return this.dexOut;
    }

    public Dex merge() throws IOException {
        if (this.dexes.length == 1) {
            return this.dexes[0];
        }
        if (this.dexes.length == 0) {
            return null;
        }
        long start = System.nanoTime();
        Dex result = mergeDexes();
        WriterSizes compactedSizes = new WriterSizes(this);
        if (this.writerSizes.size() - compactedSizes.size() > this.compactWasteThreshold) {
            result = new DexMerger(new Dex[]{this.dexOut, new Dex(0)}, CollisionPolicy.FAIL, this.context, compactedSizes).mergeDexes();
            this.context.out.printf("Result compacted from %.1fKiB to %.1fKiB to save %.1fKiB%n", new Object[]{Float.valueOf(((float) this.dexOut.getLength()) / 1024.0f), Float.valueOf(((float) result.getLength()) / 1024.0f), Float.valueOf(((float) wastedByteCount) / 1024.0f)});
        }
        long elapsed = System.nanoTime() - start;
        for (int i = 0; i < this.dexes.length; i++) {
            this.context.out.printf("Merged dex #%d (%d defs/%.1fKiB)%n", new Object[]{Integer.valueOf(i + 1), Integer.valueOf(this.dexes[i].getTableOfContents().classDefs.size), Float.valueOf(((float) this.dexes[i].getLength()) / 1024.0f)});
        }
        this.context.out.printf("Result is %d defs/%.1fKiB. Took %.1fs%n", new Object[]{Integer.valueOf(result.getTableOfContents().classDefs.size), Float.valueOf(((float) result.getLength()) / 1024.0f), Float.valueOf(((float) elapsed) / 1.0E9f)});
        return result;
    }

    private int mergeApiLevels() {
        int maxApi = -1;
        for (Dex tableOfContents : this.dexes) {
            int dexMinApi = tableOfContents.getTableOfContents().apiLevel;
            if (maxApi < dexMinApi) {
                maxApi = dexMinApi;
            }
        }
        return maxApi;
    }

    private void mergeStringIds() {
        new IdMerger<String>(this.idsDefsOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.stringIds;
            }

            String read(Section in, IndexMap indexMap, int index) {
                return in.readString();
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.stringIds[oldIndex] = newIndex;
            }

            void write(String value) {
                TableOfContents.Section section = DexMerger.this.contentsOut.stringDatas;
                section.size++;
                DexMerger.this.idsDefsOut.writeInt(DexMerger.this.stringDataOut.getPosition());
                DexMerger.this.stringDataOut.writeStringData(value);
            }
        }.mergeSorted();
    }

    private void mergeTypeIds() {
        new IdMerger<Integer>(this.idsDefsOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.typeIds;
            }

            Integer read(Section in, IndexMap indexMap, int index) {
                return Integer.valueOf(indexMap.adjustString(in.readInt()));
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                if (newIndex < 0 || newIndex > 65535) {
                    throw new DexIndexOverflowException("type ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.typeIds[oldIndex] = (short) newIndex;
            }

            void write(Integer value) {
                DexMerger.this.idsDefsOut.writeInt(value.intValue());
            }
        }.mergeSorted();
    }

    private void mergeTypeLists() {
        new IdMerger<TypeList>(this.typeListOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.typeLists;
            }

            TypeList read(Section in, IndexMap indexMap, int index) {
                return indexMap.adjustTypeList(in.readTypeList());
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.putTypeListOffset(offset, DexMerger.this.typeListOut.getPosition());
            }

            void write(TypeList value) {
                DexMerger.this.typeListOut.writeTypeList(value);
            }
        }.mergeUnsorted();
    }

    private void mergeProtoIds() {
        new IdMerger<ProtoId>(this.idsDefsOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.protoIds;
            }

            ProtoId read(Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readProtoId());
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                if (newIndex < 0 || newIndex > 65535) {
                    throw new DexIndexOverflowException("proto ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.protoIds[oldIndex] = (short) newIndex;
            }

            void write(ProtoId value) {
                value.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeSorted();
    }

    private void mergeCallSiteIds() {
        new IdMerger<CallSiteId>(this.idsDefsOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.callSiteIds;
            }

            CallSiteId read(Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readCallSiteId());
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.callSiteIds[oldIndex] = newIndex;
            }

            void write(CallSiteId value) {
                value.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeSorted();
    }

    private void mergeMethodHandles() {
        new IdMerger<MethodHandle>(this.idsDefsOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.methodHandles;
            }

            MethodHandle read(Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readMethodHandle());
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.methodHandleIds.put(Integer.valueOf(oldIndex), Integer.valueOf(indexMap.methodHandleIds.size()));
            }

            void write(MethodHandle value) {
                value.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeUnsorted();
    }

    private void mergeFieldIds() {
        new IdMerger<FieldId>(this.idsDefsOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.fieldIds;
            }

            FieldId read(Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readFieldId());
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                if (newIndex < 0 || newIndex > 65535) {
                    throw new DexIndexOverflowException("field ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.fieldIds[oldIndex] = (short) newIndex;
            }

            void write(FieldId value) {
                value.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeSorted();
    }

    private void mergeMethodIds() {
        new IdMerger<MethodId>(this.idsDefsOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.methodIds;
            }

            MethodId read(Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readMethodId());
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                if (newIndex < 0 || newIndex > 65535) {
                    throw new DexIndexOverflowException("method ID not in [0, 0xffff]: " + newIndex);
                }
                indexMap.methodIds[oldIndex] = (short) newIndex;
            }

            void write(MethodId methodId) {
                methodId.writeTo(DexMerger.this.idsDefsOut);
            }
        }.mergeSorted();
    }

    private void mergeAnnotations() {
        new IdMerger<Annotation>(this.annotationOut) {
            TableOfContents.Section getSection(TableOfContents tableOfContents) {
                return tableOfContents.annotations;
            }

            Annotation read(Section in, IndexMap indexMap, int index) {
                return indexMap.adjust(in.readAnnotation());
            }

            void updateIndex(int offset, IndexMap indexMap, int oldIndex, int newIndex) {
                indexMap.putAnnotationOffset(offset, DexMerger.this.annotationOut.getPosition());
            }

            void write(Annotation value) {
                value.writeTo(DexMerger.this.annotationOut);
            }
        }.mergeUnsorted();
    }

    private void mergeClassDefs() {
        SortableType[] types = getSortedTypes();
        this.contentsOut.classDefs.off = this.idsDefsOut.getPosition();
        this.contentsOut.classDefs.size = types.length;
        for (SortableType type : types) {
            transformClassDef(type.getDex(), type.getClassDef(), type.getIndexMap());
        }
    }

    private SortableType[] getSortedTypes() {
        SortableType[] sortableTypes = new SortableType[this.contentsOut.typeIds.size];
        for (int i = 0; i < this.dexes.length; i++) {
            readSortableTypes(sortableTypes, this.dexes[i], this.indexMaps[i]);
        }
        boolean allDone;
        do {
            allDone = true;
            for (SortableType sortableType : sortableTypes) {
                if (!(sortableType == null || sortableType.isDepthAssigned())) {
                    allDone &= sortableType.tryAssignDepth(sortableTypes);
                }
            }
        } while (!allDone);
        Arrays.sort(sortableTypes, SortableType.NULLS_LAST_ORDER);
        int firstNull = Arrays.asList(sortableTypes).indexOf(null);
        if (firstNull != -1) {
            return (SortableType[]) Arrays.copyOfRange(sortableTypes, 0, firstNull);
        }
        return sortableTypes;
    }

    private void readSortableTypes(SortableType[] sortableTypes, Dex buffer, IndexMap indexMap) {
        for (ClassDef classDef : buffer.classDefs()) {
            SortableType sortableType = indexMap.adjust(new SortableType(buffer, indexMap, classDef));
            int t = sortableType.getTypeIndex();
            if (sortableTypes[t] == null) {
                sortableTypes[t] = sortableType;
            } else if (this.collisionPolicy != CollisionPolicy.KEEP_FIRST) {
                throw new DexException("Multiple dex files define " + ((String) buffer.typeNames().get(classDef.getTypeIndex())));
            }
        }
    }

    private void unionAnnotationSetsAndDirectories() {
        int i;
        for (i = 0; i < this.dexes.length; i++) {
            transformAnnotationSets(this.dexes[i], this.indexMaps[i]);
        }
        for (i = 0; i < this.dexes.length; i++) {
            transformAnnotationSetRefLists(this.dexes[i], this.indexMaps[i]);
        }
        for (i = 0; i < this.dexes.length; i++) {
            transformAnnotationDirectories(this.dexes[i], this.indexMaps[i]);
        }
        for (i = 0; i < this.dexes.length; i++) {
            transformStaticValues(this.dexes[i], this.indexMaps[i]);
        }
    }

    private void transformAnnotationSets(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationSets;
        if (section.exists()) {
            Section setIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationSet(indexMap, setIn);
            }
        }
    }

    private void transformAnnotationSetRefLists(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationSetRefLists;
        if (section.exists()) {
            Section setIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationSetRefList(indexMap, setIn);
            }
        }
    }

    private void transformAnnotationDirectories(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().annotationsDirectories;
        if (section.exists()) {
            Section directoryIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformAnnotationDirectory(directoryIn, indexMap);
            }
        }
    }

    private void transformStaticValues(Dex in, IndexMap indexMap) {
        TableOfContents.Section section = in.getTableOfContents().encodedArrays;
        if (section.exists()) {
            Section staticValuesIn = in.open(section.off);
            for (int i = 0; i < section.size; i++) {
                transformStaticValues(staticValuesIn, indexMap);
            }
        }
    }

    private void transformClassDef(Dex in, ClassDef classDef, IndexMap indexMap) {
        this.idsDefsOut.assertFourByteAligned();
        this.idsDefsOut.writeInt(classDef.getTypeIndex());
        this.idsDefsOut.writeInt(classDef.getAccessFlags());
        this.idsDefsOut.writeInt(classDef.getSupertypeIndex());
        this.idsDefsOut.writeInt(classDef.getInterfacesOffset());
        this.idsDefsOut.writeInt(indexMap.adjustString(classDef.getSourceFileIndex()));
        this.idsDefsOut.writeInt(indexMap.adjustAnnotationDirectory(classDef.getAnnotationsOffset()));
        if (classDef.getClassDataOffset() == 0) {
            this.idsDefsOut.writeInt(0);
        } else {
            this.idsDefsOut.writeInt(this.classDataOut.getPosition());
            transformClassData(in, in.readClassData(classDef), indexMap);
        }
        this.idsDefsOut.writeInt(indexMap.adjustEncodedArray(classDef.getStaticValuesOffset()));
    }

    private void transformAnnotationDirectory(Section directoryIn, IndexMap indexMap) {
        int i;
        TableOfContents.Section section = this.contentsOut.annotationsDirectories;
        section.size++;
        this.annotationsDirectoryOut.assertFourByteAligned();
        indexMap.putAnnotationDirectoryOffset(directoryIn.getPosition(), this.annotationsDirectoryOut.getPosition());
        this.annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSet(directoryIn.readInt()));
        int fieldsSize = directoryIn.readInt();
        this.annotationsDirectoryOut.writeInt(fieldsSize);
        int methodsSize = directoryIn.readInt();
        this.annotationsDirectoryOut.writeInt(methodsSize);
        int parameterListSize = directoryIn.readInt();
        this.annotationsDirectoryOut.writeInt(parameterListSize);
        for (i = 0; i < fieldsSize; i++) {
            this.annotationsDirectoryOut.writeInt(indexMap.adjustField(directoryIn.readInt()));
            this.annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSet(directoryIn.readInt()));
        }
        for (i = 0; i < methodsSize; i++) {
            this.annotationsDirectoryOut.writeInt(indexMap.adjustMethod(directoryIn.readInt()));
            this.annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSet(directoryIn.readInt()));
        }
        for (i = 0; i < parameterListSize; i++) {
            this.annotationsDirectoryOut.writeInt(indexMap.adjustMethod(directoryIn.readInt()));
            this.annotationsDirectoryOut.writeInt(indexMap.adjustAnnotationSetRefList(directoryIn.readInt()));
        }
    }

    private void transformAnnotationSet(IndexMap indexMap, Section setIn) {
        TableOfContents.Section section = this.contentsOut.annotationSets;
        section.size++;
        this.annotationSetOut.assertFourByteAligned();
        indexMap.putAnnotationSetOffset(setIn.getPosition(), this.annotationSetOut.getPosition());
        int size = setIn.readInt();
        this.annotationSetOut.writeInt(size);
        for (int j = 0; j < size; j++) {
            this.annotationSetOut.writeInt(indexMap.adjustAnnotation(setIn.readInt()));
        }
    }

    private void transformAnnotationSetRefList(IndexMap indexMap, Section refListIn) {
        TableOfContents.Section section = this.contentsOut.annotationSetRefLists;
        section.size++;
        this.annotationSetRefListOut.assertFourByteAligned();
        indexMap.putAnnotationSetRefListOffset(refListIn.getPosition(), this.annotationSetRefListOut.getPosition());
        int parameterCount = refListIn.readInt();
        this.annotationSetRefListOut.writeInt(parameterCount);
        for (int p = 0; p < parameterCount; p++) {
            this.annotationSetRefListOut.writeInt(indexMap.adjustAnnotationSet(refListIn.readInt()));
        }
    }

    private void transformClassData(Dex in, ClassData classData, IndexMap indexMap) {
        TableOfContents.Section section = this.contentsOut.classDatas;
        section.size++;
        Field[] staticFields = classData.getStaticFields();
        Field[] instanceFields = classData.getInstanceFields();
        Method[] directMethods = classData.getDirectMethods();
        Method[] virtualMethods = classData.getVirtualMethods();
        this.classDataOut.writeUleb128(staticFields.length);
        this.classDataOut.writeUleb128(instanceFields.length);
        this.classDataOut.writeUleb128(directMethods.length);
        this.classDataOut.writeUleb128(virtualMethods.length);
        transformFields(indexMap, staticFields);
        transformFields(indexMap, instanceFields);
        transformMethods(in, indexMap, directMethods);
        transformMethods(in, indexMap, virtualMethods);
    }

    private void transformFields(IndexMap indexMap, Field[] fields) {
        int lastOutFieldIndex = 0;
        for (Field field : fields) {
            int outFieldIndex = indexMap.adjustField(field.getFieldIndex());
            this.classDataOut.writeUleb128(outFieldIndex - lastOutFieldIndex);
            lastOutFieldIndex = outFieldIndex;
            this.classDataOut.writeUleb128(field.getAccessFlags());
        }
    }

    private void transformMethods(Dex in, IndexMap indexMap, Method[] methods) {
        int lastOutMethodIndex = 0;
        for (Method method : methods) {
            int outMethodIndex = indexMap.adjustMethod(method.getMethodIndex());
            this.classDataOut.writeUleb128(outMethodIndex - lastOutMethodIndex);
            lastOutMethodIndex = outMethodIndex;
            this.classDataOut.writeUleb128(method.getAccessFlags());
            if (method.getCodeOffset() == 0) {
                this.classDataOut.writeUleb128(0);
            } else {
                this.codeOut.alignToFourBytesWithZeroFill();
                this.classDataOut.writeUleb128(this.codeOut.getPosition());
                transformCode(in, in.readCode(method), indexMap);
            }
        }
    }

    private void transformCode(Dex in, Code code, IndexMap indexMap) {
        TableOfContents.Section section = this.contentsOut.codes;
        section.size++;
        this.codeOut.assertFourByteAligned();
        this.codeOut.writeUnsignedShort(code.getRegistersSize());
        this.codeOut.writeUnsignedShort(code.getInsSize());
        this.codeOut.writeUnsignedShort(code.getOutsSize());
        Try[] tries = code.getTries();
        CatchHandler[] catchHandlers = code.getCatchHandlers();
        this.codeOut.writeUnsignedShort(tries.length);
        int debugInfoOffset = code.getDebugInfoOffset();
        if (debugInfoOffset != 0) {
            this.codeOut.writeInt(this.debugInfoOut.getPosition());
            transformDebugInfoItem(in.open(debugInfoOffset), indexMap);
        } else {
            this.codeOut.writeInt(0);
        }
        short[] newInstructions = this.instructionTransformer.transform(indexMap, code.getInstructions());
        this.codeOut.writeInt(newInstructions.length);
        this.codeOut.write(newInstructions);
        if (tries.length > 0) {
            if (newInstructions.length % 2 == 1) {
                this.codeOut.writeShort((short) 0);
            }
            Section triesSection = this.dexOut.open(this.codeOut.getPosition());
            this.codeOut.skip(tries.length * 8);
            transformTries(triesSection, tries, transformCatchHandlers(indexMap, catchHandlers));
        }
    }

    private int[] transformCatchHandlers(IndexMap indexMap, CatchHandler[] catchHandlers) {
        int baseOffset = this.codeOut.getPosition();
        this.codeOut.writeUleb128(catchHandlers.length);
        int[] offsets = new int[catchHandlers.length];
        for (int i = 0; i < catchHandlers.length; i++) {
            offsets[i] = this.codeOut.getPosition() - baseOffset;
            transformEncodedCatchHandler(catchHandlers[i], indexMap);
        }
        return offsets;
    }

    private void transformTries(Section out, Try[] tries, int[] catchHandlerOffsets) {
        for (Try tryItem : tries) {
            out.writeInt(tryItem.getStartAddress());
            out.writeUnsignedShort(tryItem.getInstructionCount());
            out.writeUnsignedShort(catchHandlerOffsets[tryItem.getCatchHandlerIndex()]);
        }
    }

    private void transformDebugInfoItem(Section in, IndexMap indexMap) {
        TableOfContents.Section section = this.contentsOut.debugInfos;
        section.size++;
        this.debugInfoOut.writeUleb128(in.readUleb128());
        int parametersSize = in.readUleb128();
        this.debugInfoOut.writeUleb128(parametersSize);
        for (int p = 0; p < parametersSize; p++) {
            this.debugInfoOut.writeUleb128p1(indexMap.adjustString(in.readUleb128p1()));
        }
        while (true) {
            int opcode = in.readByte();
            this.debugInfoOut.writeByte(opcode);
            switch (opcode) {
                case 0:
                    return;
                case 1:
                    this.debugInfoOut.writeUleb128(in.readUleb128());
                    break;
                case 2:
                    this.debugInfoOut.writeSleb128(in.readSleb128());
                    break;
                case 3:
                case 4:
                    this.debugInfoOut.writeUleb128(in.readUleb128());
                    this.debugInfoOut.writeUleb128p1(indexMap.adjustString(in.readUleb128p1()));
                    this.debugInfoOut.writeUleb128p1(indexMap.adjustType(in.readUleb128p1()));
                    if (opcode != 4) {
                        break;
                    }
                    this.debugInfoOut.writeUleb128p1(indexMap.adjustString(in.readUleb128p1()));
                    break;
                case 5:
                case 6:
                    this.debugInfoOut.writeUleb128(in.readUleb128());
                    break;
                case 9:
                    this.debugInfoOut.writeUleb128p1(indexMap.adjustString(in.readUleb128p1()));
                    break;
                default:
                    break;
            }
        }
    }

    private void transformEncodedCatchHandler(CatchHandler catchHandler, IndexMap indexMap) {
        int catchAllAddress = catchHandler.getCatchAllAddress();
        int[] typeIndexes = catchHandler.getTypeIndexes();
        int[] addresses = catchHandler.getAddresses();
        if (catchAllAddress != -1) {
            this.codeOut.writeSleb128(-typeIndexes.length);
        } else {
            this.codeOut.writeSleb128(typeIndexes.length);
        }
        for (int i = 0; i < typeIndexes.length; i++) {
            this.codeOut.writeUleb128(indexMap.adjustType(typeIndexes[i]));
            this.codeOut.writeUleb128(addresses[i]);
        }
        if (catchAllAddress != -1) {
            this.codeOut.writeUleb128(catchAllAddress);
        }
    }

    private void transformStaticValues(Section in, IndexMap indexMap) {
        TableOfContents.Section section = this.contentsOut.encodedArrays;
        section.size++;
        indexMap.putEncodedArrayValueOffset(in.getPosition(), this.encodedArrayOut.getPosition());
        indexMap.adjustEncodedArray(in.readEncodedArray()).writeTo(this.encodedArrayOut);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            printUsage();
            return;
        }
        Dex[] dexes = new Dex[(args.length - 1)];
        for (int i = 1; i < args.length; i++) {
            dexes[i - 1] = new Dex(new File(args[i]));
        }
        new DexMerger(dexes, CollisionPolicy.KEEP_FIRST, new DxContext()).merge().writeTo(new File(args[0]));
    }

    private static void printUsage() {
        System.out.println("Usage: DexMerger <out.dex> <a.dex> <b.dex> ...");
        System.out.println();
        System.out.println("If a class is defined in several dex, the class found in the first dex will be used.");
    }
}
