package com.android.dx.command.dexer;

import com.android.dex.Dex;
import com.android.dex.DexException;
import com.android.dex.DexFormat;
import com.android.dex.util.FileUtils;
import com.android.dx.cf.code.SimException;
import com.android.dx.cf.direct.ClassPathOpener;
import com.android.dx.cf.direct.ClassPathOpener.Consumer;
import com.android.dx.cf.direct.ClassPathOpener.FileNameFilter;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.command.UsageException;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.file.EncodedMethod;
import com.android.dx.merge.CollisionPolicy;
import com.android.dx.merge.DexMerger;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.RegisterSpec;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Prototype;
import com.android.dx.rop.type.Type;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Main {
    static final /* synthetic */ boolean $assertionsDisabled = (!Main.class.desiredAssertionStatus() ? true : $assertionsDisabled);
    private static final Name CREATED_BY = new Name("Created-By");
    private static final String DEX_EXTENSION = ".dex";
    private static final String DEX_PREFIX = "classes";
    private static final String IN_RE_CORE_CLASSES = "Ill-advised or mistaken usage of a core class (java.* or javax.*)\nwhen not building a core library.\n\nThis is often due to inadvertently including a core library file\nin your application's project, when using an IDE (such as\nEclipse). If you are sure you're not intentionally defining a\ncore class, then this is the most likely explanation of what's\ngoing on.\n\nHowever, you might actually be trying to define a class in a core\nnamespace, the source of which you may have taken, for example,\nfrom a non-Android virtual machine project. This will most\nassuredly not work. At a minimum, it jeopardizes the\ncompatibility of your app with future versions of the platform.\nIt is also often of questionable legality.\n\nIf you really intend to build a core library -- which is only\nappropriate as part of creating a full virtual machine\ndistribution, as opposed to compiling an application -- then use\nthe \"--core-library\" option to suppress this error message.\n\nIf you go ahead and use \"--core-library\" but are in fact\nbuilding an application, then be forewarned that your application\nwill still fail to build or run, at some point. Please be\nprepared for angry customers who find, for example, that your\napplication ceases to function once they upgrade their operating\nsystem. You will be to blame for this problem.\n\nIf you are legitimately using some code that happens to be in a\ncore package, then the easiest safe alternative you have is to\nrepackage that code. That is, move the classes in question into\nyour own package namespace. This means that they will never be in\nconflict with core system classes. JarJar is a tool that may help\nyou in this endeavor. If you find that you cannot do this, then\nthat is an indication that the path you are on will ultimately\nlead to pain, suffering, grief, and lamentation.\n";
    private static final String[] JAVAX_CORE = new String[]{"accessibility", "crypto", "imageio", "management", "naming", "net", "print", "rmi", "security", "sip", "sound", "sql", "swing", "transaction", "xml"};
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    private static final int MAX_FIELD_ADDED_DURING_DEX_CREATION = 9;
    private static final int MAX_METHOD_ADDED_DURING_DEX_CREATION = 2;
    private List<Future<Boolean>> addToDexFutures = new ArrayList();
    private volatile boolean anyFilesProcessed;
    private Arguments args;
    private ExecutorService classDefItemConsumer;
    private ExecutorService classTranslatorPool;
    private Set<String> classesInMainDex = null;
    private final DxContext context;
    private ExecutorService dexOutPool;
    private List<byte[]> dexOutputArrays = new ArrayList();
    private List<Future<byte[]>> dexOutputFutures = new ArrayList();
    private Object dexRotationLock = new Object();
    private AtomicInteger errors = new AtomicInteger(0);
    private OutputStreamWriter humanOutWriter = null;
    private final List<byte[]> libraryDexBuffers = new ArrayList();
    private int maxFieldIdsInProcess = 0;
    private int maxMethodIdsInProcess = 0;
    private long minimumFileAge = 0;
    private DexFile outputDex;
    private TreeMap<String, byte[]> outputResources;

    public static class Arguments {
        private static final String INCREMENTAL_OPTION = "--incremental";
        private static final String INPUT_LIST_OPTION = "--input-list";
        private static final String MAIN_DEX_LIST_OPTION = "--main-dex-list";
        private static final String MINIMAL_MAIN_DEX_OPTION = "--minimal-main-dex";
        private static final String MULTI_DEX_OPTION = "--multi-dex";
        private static final String NUM_THREADS_OPTION = "--num-threads";
        public boolean allowAllInterfaceMethodInvokes;
        public CfOptions cfOptions;
        public final DxContext context;
        public boolean coreLibrary;
        public boolean debug;
        public DexOptions dexOptions;
        public String dontOptimizeListFile;
        public int dumpWidth;
        public boolean emptyOk;
        public String[] fileNames;
        public boolean forceJumbo;
        public String humanOutName;
        public boolean incremental;
        private List<String> inputList;
        public boolean jarOutput;
        public boolean keepClassesInJar;
        public boolean localInfo;
        public String mainDexListFile;
        public int maxNumberOfIdxPerDex;
        public String methodToDump;
        public int minSdkVersion;
        public boolean minimalMainDex;
        public boolean multiDex;
        public int numThreads;
        public boolean optimize;
        public String optimizeListFile;
        public String outName;
        private boolean outputIsDirectDex;
        private boolean outputIsDirectory;
        public int positionInfo;
        public boolean statistics;
        public boolean strictNameCheck;
        public boolean verbose;
        public boolean verboseDump;
        public boolean warnings;

        private static class ArgumentsParser {
            private final String[] arguments;
            private String current;
            private int index = 0;
            private String lastValue;

            public ArgumentsParser(String[] arguments) {
                this.arguments = arguments;
            }

            public String getCurrent() {
                return this.current;
            }

            public String getLastValue() {
                return this.lastValue;
            }

            public boolean getNext() {
                if (this.index >= this.arguments.length) {
                    return Main.$assertionsDisabled;
                }
                this.current = this.arguments[this.index];
                if (this.current.equals("--") || !this.current.startsWith("--")) {
                    return Main.$assertionsDisabled;
                }
                this.index++;
                return true;
            }

            private boolean getNextValue() {
                if (this.index >= this.arguments.length) {
                    return Main.$assertionsDisabled;
                }
                this.current = this.arguments[this.index];
                this.index++;
                return true;
            }

            public String[] getRemaining() {
                int n = this.arguments.length - this.index;
                String[] remaining = new String[n];
                if (n > 0) {
                    System.arraycopy(this.arguments, this.index, remaining, 0, n);
                }
                return remaining;
            }

            public boolean isArg(String prefix) {
                int n = prefix.length();
                if (n <= 0 || prefix.charAt(n - 1) != '=') {
                    return this.current.equals(prefix);
                }
                if (this.current.startsWith(prefix)) {
                    this.lastValue = this.current.substring(n);
                    return true;
                }
                prefix = prefix.substring(0, n - 1);
                if (!this.current.equals(prefix)) {
                    return Main.$assertionsDisabled;
                }
                if (getNextValue()) {
                    this.lastValue = this.current;
                    return true;
                }
                System.err.println("Missing value after parameter " + prefix);
                throw new UsageException();
            }
        }

        public Arguments(DxContext context) {
            this.debug = Main.$assertionsDisabled;
            this.warnings = true;
            this.verbose = Main.$assertionsDisabled;
            this.verboseDump = Main.$assertionsDisabled;
            this.coreLibrary = Main.$assertionsDisabled;
            this.methodToDump = null;
            this.dumpWidth = 0;
            this.outName = null;
            this.humanOutName = null;
            this.strictNameCheck = true;
            this.emptyOk = Main.$assertionsDisabled;
            this.jarOutput = Main.$assertionsDisabled;
            this.keepClassesInJar = Main.$assertionsDisabled;
            this.minSdkVersion = 13;
            this.positionInfo = 2;
            this.localInfo = true;
            this.incremental = Main.$assertionsDisabled;
            this.forceJumbo = Main.$assertionsDisabled;
            this.allowAllInterfaceMethodInvokes = Main.$assertionsDisabled;
            this.optimize = true;
            this.optimizeListFile = null;
            this.dontOptimizeListFile = null;
            this.numThreads = 1;
            this.multiDex = Main.$assertionsDisabled;
            this.mainDexListFile = null;
            this.minimalMainDex = Main.$assertionsDisabled;
            this.maxNumberOfIdxPerDex = AccessFlags.ACC_CONSTRUCTOR;
            this.inputList = null;
            this.outputIsDirectory = Main.$assertionsDisabled;
            this.outputIsDirectDex = Main.$assertionsDisabled;
            this.context = context;
        }

        public Arguments() {
            this(new DxContext());
        }

        private void parseFlags(ArgumentsParser parser) {
            while (parser.getNext()) {
                if (parser.isArg("--debug")) {
                    this.debug = true;
                } else if (parser.isArg("--no-warning")) {
                    this.warnings = Main.$assertionsDisabled;
                } else if (parser.isArg("--verbose")) {
                    this.verbose = true;
                } else if (parser.isArg("--verbose-dump")) {
                    this.verboseDump = true;
                } else if (parser.isArg("--no-files")) {
                    this.emptyOk = true;
                } else if (parser.isArg("--no-optimize")) {
                    this.optimize = Main.$assertionsDisabled;
                } else if (parser.isArg("--no-strict")) {
                    this.strictNameCheck = Main.$assertionsDisabled;
                } else if (parser.isArg("--core-library")) {
                    this.coreLibrary = true;
                } else if (parser.isArg("--statistics")) {
                    this.statistics = true;
                } else if (parser.isArg("--optimize-list=")) {
                    if (this.dontOptimizeListFile != null) {
                        this.context.err.println("--optimize-list and --no-optimize-list are incompatible.");
                        throw new UsageException();
                    } else {
                        this.optimize = true;
                        this.optimizeListFile = parser.getLastValue();
                    }
                } else if (parser.isArg("--no-optimize-list=")) {
                    if (this.dontOptimizeListFile != null) {
                        this.context.err.println("--optimize-list and --no-optimize-list are incompatible.");
                        throw new UsageException();
                    } else {
                        this.optimize = true;
                        this.dontOptimizeListFile = parser.getLastValue();
                    }
                } else if (parser.isArg("--keep-classes")) {
                    this.keepClassesInJar = true;
                } else if (parser.isArg("--output=")) {
                    this.outName = parser.getLastValue();
                    if (new File(this.outName).isDirectory()) {
                        this.jarOutput = Main.$assertionsDisabled;
                        this.outputIsDirectory = true;
                    } else if (FileUtils.hasArchiveSuffix(this.outName)) {
                        this.jarOutput = true;
                    } else if (this.outName.endsWith(Main.DEX_EXTENSION) || this.outName.equals("-")) {
                        this.jarOutput = Main.$assertionsDisabled;
                        this.outputIsDirectDex = true;
                    } else {
                        this.context.err.println("unknown output extension: " + this.outName);
                        throw new UsageException();
                    }
                } else if (parser.isArg("--dump-to=")) {
                    this.humanOutName = parser.getLastValue();
                } else if (parser.isArg("--dump-width=")) {
                    this.dumpWidth = Integer.parseInt(parser.getLastValue());
                } else if (parser.isArg("--dump-method=")) {
                    this.methodToDump = parser.getLastValue();
                    this.jarOutput = Main.$assertionsDisabled;
                } else if (parser.isArg("--positions=")) {
                    String pstr = parser.getLastValue().intern();
                    if (pstr == "none") {
                        this.positionInfo = 1;
                    } else if (pstr == "important") {
                        this.positionInfo = 3;
                    } else if (pstr == "lines") {
                        this.positionInfo = 2;
                    } else {
                        this.context.err.println("unknown positions option: " + pstr);
                        throw new UsageException();
                    }
                } else if (parser.isArg("--no-locals")) {
                    this.localInfo = Main.$assertionsDisabled;
                } else if (parser.isArg("--num-threads=")) {
                    this.numThreads = Integer.parseInt(parser.getLastValue());
                } else if (parser.isArg(INCREMENTAL_OPTION)) {
                    this.incremental = true;
                } else if (parser.isArg("--force-jumbo")) {
                    this.forceJumbo = true;
                } else if (parser.isArg(MULTI_DEX_OPTION)) {
                    this.multiDex = true;
                } else if (parser.isArg("--main-dex-list=")) {
                    this.mainDexListFile = parser.getLastValue();
                } else if (parser.isArg(MINIMAL_MAIN_DEX_OPTION)) {
                    this.minimalMainDex = true;
                } else if (parser.isArg("--set-max-idx-number=")) {
                    this.maxNumberOfIdxPerDex = Integer.parseInt(parser.getLastValue());
                } else if (parser.isArg("--input-list=")) {
                    File inputListFile = new File(parser.getLastValue());
                    try {
                        this.inputList = new ArrayList();
                        Main.readPathsFromFile(inputListFile.getAbsolutePath(), this.inputList);
                    } catch (IOException e) {
                        this.context.err.println("Unable to read input list file: " + inputListFile.getName());
                        throw new UsageException();
                    }
                } else if (parser.isArg("--min-sdk-version=")) {
                    int value;
                    String arg = parser.getLastValue();
                    try {
                        value = Integer.parseInt(arg);
                    } catch (NumberFormatException e2) {
                        value = -1;
                    }
                    if (value < 1) {
                        System.err.println("improper min-sdk-version option: " + arg);
                        throw new UsageException();
                    }
                    this.minSdkVersion = value;
                } else if (parser.isArg("--allow-all-interface-method-invokes")) {
                    this.allowAllInterfaceMethodInvokes = true;
                } else {
                    this.context.err.println("unknown option: " + parser.getCurrent());
                    throw new UsageException();
                }
            }
        }

        private void parse(String[] args) {
            ArgumentsParser parser = new ArgumentsParser(args);
            parseFlags(parser);
            this.fileNames = parser.getRemaining();
            if (!(this.inputList == null || this.inputList.isEmpty())) {
                this.inputList.addAll(Arrays.asList(this.fileNames));
                this.fileNames = (String[]) this.inputList.toArray(new String[this.inputList.size()]);
            }
            if (this.fileNames.length == 0) {
                if (!this.emptyOk) {
                    this.context.err.println("no input files specified");
                    throw new UsageException();
                }
            } else if (this.emptyOk) {
                this.context.out.println("ignoring input files");
            }
            if (this.humanOutName == null && this.methodToDump != null) {
                this.humanOutName = "-";
            }
            if (this.mainDexListFile != null && !this.multiDex) {
                this.context.err.println("--main-dex-list is only supported in combination with --multi-dex");
                throw new UsageException();
            } else if (this.minimalMainDex && (this.mainDexListFile == null || !this.multiDex)) {
                this.context.err.println("--minimal-main-dex is only supported in combination with --multi-dex and --main-dex-list");
                throw new UsageException();
            } else if (this.multiDex && this.incremental) {
                this.context.err.println("--incremental is not supported with --multi-dex");
                throw new UsageException();
            } else if (this.multiDex && this.outputIsDirectDex) {
                this.context.err.println("Unsupported output \"" + this.outName + "\". " + MULTI_DEX_OPTION + " supports only archive or directory output");
                throw new UsageException();
            } else {
                if (this.outputIsDirectory && !this.multiDex) {
                    this.outName = new File(this.outName, DexFormat.DEX_IN_JAR_NAME).getPath();
                }
                makeOptionsObjects();
            }
        }

        public void parseFlags(String[] flags) {
            parseFlags(new ArgumentsParser(flags));
        }

        public void makeOptionsObjects() {
            this.cfOptions = new CfOptions();
            this.cfOptions.positionInfo = this.positionInfo;
            this.cfOptions.localInfo = this.localInfo;
            this.cfOptions.strictNameCheck = this.strictNameCheck;
            this.cfOptions.optimize = this.optimize;
            this.cfOptions.optimizeListFile = this.optimizeListFile;
            this.cfOptions.dontOptimizeListFile = this.dontOptimizeListFile;
            this.cfOptions.statistics = this.statistics;
            if (this.warnings) {
                this.cfOptions.warn = this.context.err;
            } else {
                this.cfOptions.warn = this.context.noop;
            }
            this.dexOptions = new DexOptions(this.context.err);
            this.dexOptions.minSdkVersion = this.minSdkVersion;
            this.dexOptions.forceJumbo = this.forceJumbo;
            this.dexOptions.allowAllInterfaceMethodInvokes = this.allowAllInterfaceMethodInvokes;
        }
    }

    private class BestEffortMainDexListFilter implements FileNameFilter {
        Map<String, List<String>> map = new HashMap();

        public BestEffortMainDexListFilter() {
            for (String pathOfClass : Main.this.classesInMainDex) {
                String normalized = Main.fixPath(pathOfClass);
                String simple = getSimpleName(normalized);
                List<String> fullPath = (List) this.map.get(simple);
                if (fullPath == null) {
                    fullPath = new ArrayList(1);
                    this.map.put(simple, fullPath);
                }
                fullPath.add(normalized);
            }
        }

        public boolean accept(String path) {
            if (!path.endsWith(".class")) {
                return true;
            }
            String normalized = Main.fixPath(path);
            List<String> fullPaths = (List) this.map.get(getSimpleName(normalized));
            if (fullPaths != null) {
                for (String fullPath : fullPaths) {
                    if (normalized.endsWith(fullPath)) {
                        return true;
                    }
                }
            }
            return Main.$assertionsDisabled;
        }

        private String getSimpleName(String path) {
            int index = path.lastIndexOf(47);
            if (index >= 0) {
                return path.substring(index + 1);
            }
            return path;
        }
    }

    private class ClassDefItemConsumer implements Callable<Boolean> {
        Future<ClassDefItem> futureClazz;
        int maxFieldIdsInClass;
        int maxMethodIdsInClass;
        String name;

        private ClassDefItemConsumer(String name, Future<ClassDefItem> futureClazz, int maxMethodIdsInClass, int maxFieldIdsInClass) {
            this.name = name;
            this.futureClazz = futureClazz;
            this.maxMethodIdsInClass = maxMethodIdsInClass;
            this.maxFieldIdsInClass = maxFieldIdsInClass;
        }

        public Boolean call() throws Exception {
            try {
                ClassDefItem clazz = (ClassDefItem) this.futureClazz.get();
                if (clazz != null) {
                    Main.this.addClassToDex(clazz);
                    Main.this.updateStatus(true);
                }
                Boolean valueOf = Boolean.valueOf(true);
                if (Main.this.args.multiDex) {
                    synchronized (Main.this.dexRotationLock) {
                        Main.access$1920(Main.this, this.maxMethodIdsInClass);
                        Main.access$2020(Main.this, this.maxFieldIdsInClass);
                        Main.this.dexRotationLock.notifyAll();
                    }
                }
                return valueOf;
            } catch (Throwable ex) {
                Throwable t = ex.getCause();
                if (t instanceof Exception) {
                    t = (Exception) t;
                } else {
                    t = ex;
                }
                throw t;
            } catch (Throwable th) {
                if (Main.this.args.multiDex) {
                    synchronized (Main.this.dexRotationLock) {
                        Main.access$1920(Main.this, this.maxMethodIdsInClass);
                        Main.access$2020(Main.this, this.maxFieldIdsInClass);
                        Main.this.dexRotationLock.notifyAll();
                    }
                }
            }
        }
    }

    private class ClassParserTask implements Callable<DirectClassFile> {
        byte[] bytes;
        String name;

        private ClassParserTask(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        public DirectClassFile call() throws Exception {
            return Main.this.parseClass(this.name, this.bytes);
        }
    }

    private class ClassTranslatorTask implements Callable<ClassDefItem> {
        byte[] bytes;
        DirectClassFile classFile;
        String name;

        private ClassTranslatorTask(String name, byte[] bytes, DirectClassFile classFile) {
            this.name = name;
            this.bytes = bytes;
            this.classFile = classFile;
        }

        public ClassDefItem call() {
            return Main.this.translateClass(this.bytes, this.classFile);
        }
    }

    private class DexWriter implements Callable<byte[]> {
        private final DexFile dexFile;

        private DexWriter(DexFile dexFile) {
            this.dexFile = dexFile;
        }

        public byte[] call() throws IOException {
            return Main.this.writeDex(this.dexFile);
        }
    }

    private class DirectClassFileConsumer implements Callable<Boolean> {
        byte[] bytes;
        Future<DirectClassFile> dcff;
        String name;

        private DirectClassFileConsumer(String name, byte[] bytes, Future<DirectClassFile> dcff) {
            this.name = name;
            this.bytes = bytes;
            this.dcff = dcff;
        }

        public Boolean call() throws Exception {
            return call((DirectClassFile) this.dcff.get());
        }

        private Boolean call(DirectClassFile cf) {
            int maxMethodIdsInClass = 0;
            int maxFieldIdsInClass = 0;
            if (Main.this.args.multiDex) {
                int constantPoolSize = cf.getConstantPool().size();
                maxMethodIdsInClass = (cf.getMethods().size() + constantPoolSize) + 2;
                maxFieldIdsInClass = (cf.getFields().size() + constantPoolSize) + 9;
                synchronized (Main.this.dexRotationLock) {
                    int numMethodIds;
                    int numFieldIds;
                    synchronized (Main.this.outputDex) {
                        numMethodIds = Main.this.outputDex.getMethodIds().items().size();
                        numFieldIds = Main.this.outputDex.getFieldIds().items().size();
                    }
                    while (true) {
                        if ((numMethodIds + maxMethodIdsInClass) + Main.this.maxMethodIdsInProcess <= Main.this.args.maxNumberOfIdxPerDex && (numFieldIds + maxFieldIdsInClass) + Main.this.maxFieldIdsInProcess <= Main.this.args.maxNumberOfIdxPerDex) {
                            break;
                        }
                        if (Main.this.maxMethodIdsInProcess <= 0 && Main.this.maxFieldIdsInProcess <= 0) {
                            if (Main.this.outputDex.getClassDefs().items().size() <= 0) {
                                break;
                            }
                            Main.this.rotateDexFile();
                        } else {
                            try {
                                Main.this.dexRotationLock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        synchronized (Main.this.outputDex) {
                            numMethodIds = Main.this.outputDex.getMethodIds().items().size();
                            numFieldIds = Main.this.outputDex.getFieldIds().items().size();
                        }
                    }
                    Main.access$1912(Main.this, maxMethodIdsInClass);
                    Main.access$2012(Main.this, maxFieldIdsInClass);
                }
            }
            Main.this.addToDexFutures.add(Main.this.classDefItemConsumer.submit(new ClassDefItemConsumer(this.name, Main.this.classTranslatorPool.submit(new ClassTranslatorTask(this.name, this.bytes, cf)), maxMethodIdsInClass, maxFieldIdsInClass)));
            return Boolean.valueOf(true);
        }
    }

    private class FileBytesConsumer implements Consumer {
        private FileBytesConsumer() {
        }

        public boolean processFileBytes(String name, long lastModified, byte[] bytes) {
            return Main.this.processFileBytes(name, lastModified, bytes);
        }

        public void onException(Exception ex) {
            if (ex instanceof StopProcessing) {
                throw ((StopProcessing) ex);
            }
            if (ex instanceof SimException) {
                Main.this.context.err.println("\nEXCEPTION FROM SIMULATION:");
                Main.this.context.err.println(ex.getMessage() + "\n");
                Main.this.context.err.println(((SimException) ex).getContext());
            } else if (ex instanceof ParseException) {
                Main.this.context.err.println("\nPARSE ERROR:");
                ParseException parseException = (ParseException) ex;
                if (Main.this.args.debug) {
                    parseException.printStackTrace(Main.this.context.err);
                } else {
                    parseException.printContext(Main.this.context.err);
                }
            } else {
                Main.this.context.err.println("\nUNEXPECTED TOP-LEVEL EXCEPTION:");
                ex.printStackTrace(Main.this.context.err);
            }
            Main.this.errors.incrementAndGet();
        }

        public void onProcessArchiveStart(File file) {
            if (Main.this.args.verbose) {
                Main.this.context.out.println("processing archive " + file + "...");
            }
        }
    }

    private class MainDexListFilter implements FileNameFilter {
        private MainDexListFilter() {
        }

        public boolean accept(String fullPath) {
            if (!fullPath.endsWith(".class")) {
                return true;
            }
            return Main.this.classesInMainDex.contains(Main.fixPath(fullPath));
        }
    }

    private static class NotFilter implements FileNameFilter {
        private final FileNameFilter filter;

        private NotFilter(FileNameFilter filter) {
            this.filter = filter;
        }

        public boolean accept(String path) {
            return !this.filter.accept(path) ? true : Main.$assertionsDisabled;
        }
    }

    private static class RemoveModuleInfoFilter implements FileNameFilter {
        protected final FileNameFilter delegate;

        public RemoveModuleInfoFilter(FileNameFilter delegate) {
            this.delegate = delegate;
        }

        public boolean accept(String path) {
            return (!this.delegate.accept(path) || "module-info.class".equals(path)) ? Main.$assertionsDisabled : true;
        }
    }

    private static class StopProcessing extends RuntimeException {
        private StopProcessing() {
        }
    }

    static /* synthetic */ int access$1912(Main x0, int x1) {
        int i = x0.maxMethodIdsInProcess + x1;
        x0.maxMethodIdsInProcess = i;
        return i;
    }

    static /* synthetic */ int access$1920(Main x0, int x1) {
        int i = x0.maxMethodIdsInProcess - x1;
        x0.maxMethodIdsInProcess = i;
        return i;
    }

    static /* synthetic */ int access$2012(Main x0, int x1) {
        int i = x0.maxFieldIdsInProcess + x1;
        x0.maxFieldIdsInProcess = i;
        return i;
    }

    static /* synthetic */ int access$2020(Main x0, int x1) {
        int i = x0.maxFieldIdsInProcess - x1;
        x0.maxFieldIdsInProcess = i;
        return i;
    }

    public Main(DxContext context) {
        this.context = context;
    }

    public static void main(String[] argArray) throws IOException {
        DxContext context = new DxContext();
        Arguments arguments = new Arguments(context);
        arguments.parse(argArray);
        int result = new Main(context).runDx(arguments);
        if (result != 0) {
            System.exit(result);
        }
    }

    public static void clearInternTables() {
        Prototype.clearInternTable();
        RegisterSpec.clearInternTable();
        CstType.clearInternTable();
        Type.clearInternTable();
    }

    public static int run(Arguments arguments) throws IOException {
        return new Main(new DxContext()).runDx(arguments);
    }

    public int runDx(Arguments arguments) throws IOException {
        this.errors.set(0);
        this.libraryDexBuffers.clear();
        this.args = arguments;
        this.args.makeOptionsObjects();
        OutputStream humanOutRaw = null;
        if (this.args.humanOutName != null) {
            humanOutRaw = openOutput(this.args.humanOutName);
            this.humanOutWriter = new OutputStreamWriter(humanOutRaw);
        }
        try {
            int runMultiDex;
            if (this.args.multiDex) {
                runMultiDex = runMultiDex();
            } else {
                runMultiDex = runMonoDex();
                closeOutput(humanOutRaw);
            }
            return runMultiDex;
        } finally {
            closeOutput(humanOutRaw);
        }
    }

    private int runMonoDex() throws IOException {
        File incrementalOutFile = null;
        if (this.args.incremental) {
            if (this.args.outName == null) {
                this.context.err.println("error: no incremental output name specified");
                return -1;
            }
            incrementalOutFile = new File(this.args.outName);
            if (incrementalOutFile.exists()) {
                this.minimumFileAge = incrementalOutFile.lastModified();
            }
        }
        if (!processAllFiles()) {
            return 1;
        }
        if (this.args.incremental && !this.anyFilesProcessed) {
            return 0;
        }
        byte[] outArray = null;
        if (!(this.outputDex.isEmpty() && this.args.humanOutName == null)) {
            outArray = writeDex(this.outputDex);
            if (outArray == null) {
                return 2;
            }
        }
        if (this.args.incremental) {
            outArray = mergeIncremental(outArray, incrementalOutFile);
        }
        outArray = mergeLibraryDexBuffers(outArray);
        if (this.args.jarOutput) {
            this.outputDex = null;
            if (outArray != null) {
                this.outputResources.put(DexFormat.DEX_IN_JAR_NAME, outArray);
            }
            if (createJar(this.args.outName)) {
                return 0;
            }
            return 3;
        } else if (outArray == null || this.args.outName == null) {
            return 0;
        } else {
            OutputStream out = openOutput(this.args.outName);
            out.write(outArray);
            closeOutput(out);
            return 0;
        }
    }

    private int runMultiDex() throws IOException {
        if ($assertionsDisabled || !this.args.incremental) {
            if (this.args.mainDexListFile != null) {
                this.classesInMainDex = new HashSet();
                readPathsFromFile(this.args.mainDexListFile, this.classesInMainDex);
            }
            this.dexOutPool = Executors.newFixedThreadPool(this.args.numThreads);
            if (!processAllFiles()) {
                return 1;
            }
            if (this.libraryDexBuffers.isEmpty()) {
                if (this.outputDex != null) {
                    this.dexOutputFutures.add(this.dexOutPool.submit(new DexWriter(this.outputDex)));
                    this.outputDex = null;
                }
                try {
                    this.dexOutPool.shutdown();
                    if (this.dexOutPool.awaitTermination(600, TimeUnit.SECONDS)) {
                        for (Future<byte[]> f : this.dexOutputFutures) {
                            this.dexOutputArrays.add(f.get());
                        }
                        int i;
                        if (this.args.jarOutput) {
                            for (i = 0; i < this.dexOutputArrays.size(); i++) {
                                this.outputResources.put(getDexFileName(i), this.dexOutputArrays.get(i));
                            }
                            if (!createJar(this.args.outName)) {
                                return 3;
                            }
                        } else if (this.args.outName != null) {
                            File outDir = new File(this.args.outName);
                            if ($assertionsDisabled || outDir.isDirectory()) {
                                i = 0;
                                while (i < this.dexOutputArrays.size()) {
                                    OutputStream out = new FileOutputStream(new File(outDir, getDexFileName(i)));
                                    try {
                                        out.write((byte[]) this.dexOutputArrays.get(i));
                                        closeOutput(out);
                                        i++;
                                    } catch (Throwable th) {
                                        closeOutput(out);
                                        throw th;
                                    }
                                }
                            }
                            throw new AssertionError();
                        }
                        return 0;
                    }
                    throw new RuntimeException("Timed out waiting for dex writer threads.");
                } catch (InterruptedException e) {
                    this.dexOutPool.shutdownNow();
                    throw new RuntimeException("A dex writer thread has been interrupted.");
                } catch (Exception e2) {
                    this.dexOutPool.shutdownNow();
                    throw new RuntimeException("Unexpected exception in dex writer thread");
                }
            }
            throw new DexException("Library dex files are not supported in multi-dex mode");
        }
        throw new AssertionError();
    }

    private static String getDexFileName(int i) {
        if (i == 0) {
            return DexFormat.DEX_IN_JAR_NAME;
        }
        return DEX_PREFIX + (i + 1) + DEX_EXTENSION;
    }

    private static void readPathsFromFile(String fileName, Collection<String> paths) throws IOException {
        Throwable th;
        BufferedReader bfr = null;
        try {
            BufferedReader bfr2 = new BufferedReader(new FileReader(fileName));
            while (true) {
                try {
                    String line = bfr2.readLine();
                    if (line == null) {
                        break;
                    }
                    paths.add(fixPath(line));
                } catch (Throwable th2) {
                    th = th2;
                    bfr = bfr2;
                }
            }
            if (bfr2 != null) {
                bfr2.close();
            }
        } catch (Throwable th3) {
            th = th3;
            if (bfr != null) {
                bfr.close();
            }
            throw th;
        }
    }

    private byte[] mergeIncremental(byte[] update, File base) throws IOException {
        Dex dexA = null;
        Dex dexB = null;
        if (update != null) {
            dexA = new Dex(update);
        }
        if (base.exists()) {
            dexB = new Dex(base);
        }
        if (dexA == null && dexB == null) {
            return null;
        }
        Dex result;
        if (dexA == null) {
            result = dexB;
        } else if (dexB == null) {
            result = dexA;
        } else {
            result = new DexMerger(new Dex[]{dexA, dexB}, CollisionPolicy.KEEP_FIRST, this.context).merge();
        }
        OutputStream bytesOut = new ByteArrayOutputStream();
        result.writeTo(bytesOut);
        return bytesOut.toByteArray();
    }

    private byte[] mergeLibraryDexBuffers(byte[] outArray) throws IOException {
        ArrayList<Dex> dexes = new ArrayList();
        if (outArray != null) {
            dexes.add(new Dex(outArray));
        }
        for (byte[] libraryDex : this.libraryDexBuffers) {
            dexes.add(new Dex(libraryDex));
        }
        if (dexes.isEmpty()) {
            return null;
        }
        return new DexMerger((Dex[]) dexes.toArray(new Dex[dexes.size()]), CollisionPolicy.FAIL, this.context).merge().getBytes();
    }

    private boolean processAllFiles() {
        createDexFile();
        if (this.args.jarOutput) {
            this.outputResources = new TreeMap();
        }
        this.anyFilesProcessed = $assertionsDisabled;
        String[] fileNames = this.args.fileNames;
        Arrays.sort(fileNames);
        this.classTranslatorPool = new ThreadPoolExecutor(this.args.numThreads, this.args.numThreads, 0, TimeUnit.SECONDS, new ArrayBlockingQueue(this.args.numThreads * 2, true), new CallerRunsPolicy());
        this.classDefItemConsumer = Executors.newSingleThreadExecutor();
        try {
            FileNameFilter mainDexListFilter;
            if (this.args.mainDexListFile != null) {
                if (this.args.strictNameCheck) {
                    Main main = this;
                    mainDexListFilter = new MainDexListFilter();
                } else {
                    mainDexListFilter = new BestEffortMainDexListFilter();
                }
                for (String processOne : fileNames) {
                    processOne(processOne, mainPassFilter);
                }
                if (this.dexOutputFutures.size() > 0) {
                    throw new DexException("Too many classes in --main-dex-list, main dex capacity exceeded");
                }
                if (this.args.minimalMainDex) {
                    synchronized (this.dexRotationLock) {
                        while (true) {
                            if (this.maxMethodIdsInProcess <= 0 && this.maxFieldIdsInProcess <= 0) {
                                break;
                            }
                            try {
                                this.dexRotationLock.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    rotateDexFile();
                }
                mainDexListFilter = new RemoveModuleInfoFilter(new NotFilter(mainPassFilter));
                for (String processOne2 : fileNames) {
                    processOne(processOne2, mainDexListFilter);
                }
            } else {
                mainDexListFilter = new RemoveModuleInfoFilter(ClassPathOpener.acceptAll);
                for (String processOne22 : fileNames) {
                    processOne(processOne22, mainDexListFilter);
                }
            }
        } catch (StopProcessing e2) {
        }
        try {
            this.classTranslatorPool.shutdown();
            this.classTranslatorPool.awaitTermination(600, TimeUnit.SECONDS);
            this.classDefItemConsumer.shutdown();
            this.classDefItemConsumer.awaitTermination(600, TimeUnit.SECONDS);
            for (Future<Boolean> f : this.addToDexFutures) {
                f.get();
            }
            int errorNum = this.errors.get();
            if (errorNum != 0) {
                this.context.err.println(errorNum + " error" + (errorNum == 1 ? "" : "s") + "; aborting");
                return $assertionsDisabled;
            } else if (this.args.incremental && !this.anyFilesProcessed) {
                return true;
            } else {
                if (this.anyFilesProcessed || this.args.emptyOk) {
                    if (this.args.optimize && this.args.statistics) {
                        this.context.codeStatistics.dumpStatistics(this.context.out);
                    }
                    return true;
                }
                this.context.err.println("no classfiles specified");
                return $assertionsDisabled;
            }
        } catch (ExecutionException ex) {
            if (this.errors.incrementAndGet() >= 10) {
                throw new InterruptedException("Too many errors");
            } else if (this.args.debug) {
                this.context.err.println("Uncaught translation error:");
                ex.getCause().printStackTrace(this.context.err);
            } else {
                this.context.err.println("Uncaught translation error: " + ex.getCause());
            }
        } catch (Throwable ie) {
            this.classTranslatorPool.shutdownNow();
            this.classDefItemConsumer.shutdownNow();
            throw new RuntimeException("Translation has been interrupted", ie);
        } catch (Exception e3) {
            this.classTranslatorPool.shutdownNow();
            this.classDefItemConsumer.shutdownNow();
            e3.printStackTrace(this.context.out);
            throw new RuntimeException("Unexpected exception in translator thread.", e3);
        }
    }

    private void createDexFile() {
        this.outputDex = new DexFile(this.args.dexOptions);
        if (this.args.dumpWidth != 0) {
            this.outputDex.setDumpWidth(this.args.dumpWidth);
        }
    }

    private void rotateDexFile() {
        if (this.outputDex != null) {
            if (this.dexOutPool != null) {
                this.dexOutputFutures.add(this.dexOutPool.submit(new DexWriter(this.outputDex)));
            } else {
                this.dexOutputArrays.add(writeDex(this.outputDex));
            }
        }
        createDexFile();
    }

    private void processOne(String pathname, FileNameFilter filter) {
        if (new ClassPathOpener(pathname, true, filter, new FileBytesConsumer()).process()) {
            updateStatus(true);
        }
    }

    private void updateStatus(boolean res) {
        this.anyFilesProcessed |= res;
    }

    private boolean processFileBytes(String name, long lastModified, byte[] bytes) {
        boolean isClass = name.endsWith(".class");
        boolean isClassesDex = name.equals(DexFormat.DEX_IN_JAR_NAME);
        boolean keepResources = this.outputResources != null ? true : $assertionsDisabled;
        if (isClass || isClassesDex || keepResources) {
            if (this.args.verbose) {
                this.context.out.println("processing " + name + "...");
            }
            String fixedName = fixPath(name);
            if (isClass) {
                if (keepResources && this.args.keepClassesInJar) {
                    synchronized (this.outputResources) {
                        this.outputResources.put(fixedName, bytes);
                    }
                }
                if (lastModified < this.minimumFileAge) {
                    return true;
                }
                processClass(fixedName, bytes);
                return $assertionsDisabled;
            } else if (isClassesDex) {
                synchronized (this.libraryDexBuffers) {
                    this.libraryDexBuffers.add(bytes);
                }
                return true;
            } else {
                synchronized (this.outputResources) {
                    this.outputResources.put(fixedName, bytes);
                }
                return true;
            }
        } else if (!this.args.verbose) {
            return $assertionsDisabled;
        } else {
            this.context.out.println("ignored resource " + name);
            return $assertionsDisabled;
        }
    }

    private boolean processClass(String name, byte[] bytes) {
        if (!this.args.coreLibrary) {
            checkClassName(name);
        }
        try {
            new DirectClassFileConsumer(name, bytes, null).call(new ClassParserTask(name, bytes).call());
            return true;
        } catch (ParseException ex) {
            throw ex;
        } catch (Exception ex2) {
            throw new RuntimeException("Exception parsing classes", ex2);
        }
    }

    private DirectClassFile parseClass(String name, byte[] bytes) {
        DirectClassFile cf = new DirectClassFile(bytes, name, this.args.cfOptions.strictNameCheck);
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        cf.getMagic();
        return cf;
    }

    private ClassDefItem translateClass(byte[] bytes, DirectClassFile cf) {
        try {
            return CfTranslator.translate(this.context, cf, bytes, this.args.cfOptions, this.args.dexOptions, this.outputDex);
        } catch (ParseException ex) {
            this.context.err.println("\ntrouble processing:");
            if (this.args.debug) {
                ex.printStackTrace(this.context.err);
            } else {
                ex.printContext(this.context.err);
            }
            this.errors.incrementAndGet();
            return null;
        }
    }

    private boolean addClassToDex(ClassDefItem clazz) {
        synchronized (this.outputDex) {
            this.outputDex.add(clazz);
        }
        return true;
    }

    private void checkClassName(String name) {
        boolean bogus = $assertionsDisabled;
        if (name.startsWith("java/")) {
            bogus = true;
        } else if (name.startsWith("javax/")) {
            int slashAt = name.indexOf(47, 6);
            if (slashAt == -1) {
                bogus = true;
            } else {
                bogus = Arrays.binarySearch(JAVAX_CORE, name.substring(6, slashAt)) >= 0 ? true : $assertionsDisabled;
            }
        }
        if (bogus) {
            this.context.err.println("\ntrouble processing \"" + name + "\":\n\n" + IN_RE_CORE_CLASSES);
            this.errors.incrementAndGet();
            throw new StopProcessing();
        }
    }

    private byte[] writeDex(DexFile outputDex) {
        byte[] outArray = null;
        try {
            if (this.args.methodToDump != null) {
                outputDex.toDex(null, $assertionsDisabled);
                dumpMethod(outputDex, this.args.methodToDump, this.humanOutWriter);
            } else {
                outArray = outputDex.toDex(this.humanOutWriter, this.args.verboseDump);
            }
            if (this.args.statistics) {
                this.context.out.println(outputDex.getStatistics().toHuman());
            }
            if (this.humanOutWriter != null) {
                this.humanOutWriter.flush();
            }
            return outArray;
        } catch (Exception ex) {
            if (this.args.debug) {
                this.context.err.println("\ntrouble writing output:");
                ex.printStackTrace(this.context.err);
                return null;
            }
            this.context.err.println("\ntrouble writing output: " + ex.getMessage());
            return null;
        } catch (Throwable th) {
            if (this.humanOutWriter != null) {
                this.humanOutWriter.flush();
            }
        }
    }

    private boolean createJar(String fileName) {
        OutputStream out;
        JarOutputStream jarOut;
        try {
            Manifest manifest = makeManifest();
            out = openOutput(fileName);
            jarOut = new JarOutputStream(out, manifest);
            for (Entry<String, byte[]> e : this.outputResources.entrySet()) {
                String name = (String) e.getKey();
                byte[] contents = (byte[]) e.getValue();
                JarEntry entry = new JarEntry(name);
                int length = contents.length;
                if (this.args.verbose) {
                    this.context.out.println("writing " + name + "; size " + length + "...");
                }
                entry.setSize((long) length);
                jarOut.putNextEntry(entry);
                jarOut.write(contents);
                jarOut.closeEntry();
            }
            jarOut.finish();
            jarOut.flush();
            closeOutput(out);
            return true;
        } catch (Exception ex) {
            if (this.args.debug) {
                this.context.err.println("\ntrouble writing output:");
                ex.printStackTrace(this.context.err);
            } else {
                this.context.err.println("\ntrouble writing output: " + ex.getMessage());
            }
            return $assertionsDisabled;
        } catch (Throwable th) {
            jarOut.finish();
            jarOut.flush();
            closeOutput(out);
        }
    }

    private Manifest makeManifest() throws IOException {
        Manifest manifest;
        Attributes attribs;
        byte[] manifestBytes = (byte[]) this.outputResources.get(MANIFEST_NAME);
        if (manifestBytes == null) {
            manifest = new Manifest();
            attribs = manifest.getMainAttributes();
            attribs.put(Name.MANIFEST_VERSION, "1.0");
        } else {
            manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
            attribs = manifest.getMainAttributes();
            this.outputResources.remove(MANIFEST_NAME);
        }
        String createdBy = attribs.getValue(CREATED_BY);
        if (createdBy == null) {
            createdBy = "";
        } else {
            createdBy = createdBy + " + ";
        }
        attribs.put(CREATED_BY, createdBy + "dx 1.16");
        attribs.putValue("Dex-Location", DexFormat.DEX_IN_JAR_NAME);
        return manifest;
    }

    private OutputStream openOutput(String name) throws IOException {
        if (name.equals("-") || name.startsWith("-.")) {
            return this.context.out;
        }
        return new FileOutputStream(name);
    }

    private void closeOutput(OutputStream stream) throws IOException {
        if (stream != null) {
            stream.flush();
            if (stream != this.context.out) {
                stream.close();
            }
        }
    }

    private static String fixPath(String path) {
        if (File.separatorChar == '\\') {
            path = path.replace('\\', ClassPathElement.SEPARATOR_CHAR);
        }
        int index = path.lastIndexOf("/./");
        if (index != -1) {
            return path.substring(index + 3);
        }
        if (path.startsWith("./")) {
            return path.substring(2);
        }
        return path;
    }

    private void dumpMethod(DexFile dex, String fqName, OutputStreamWriter out) {
        boolean wildcard = fqName.endsWith("*");
        int lastDot = fqName.lastIndexOf(46);
        if (lastDot <= 0 || lastDot == fqName.length() - 1) {
            this.context.err.println("bogus fully-qualified method name: " + fqName);
            return;
        }
        String className = fqName.substring(0, lastDot).replace('.', ClassPathElement.SEPARATOR_CHAR);
        String methodName = fqName.substring(lastDot + 1);
        ClassDefItem clazz = dex.getClassOrNull(className);
        if (clazz == null) {
            this.context.err.println("no such class: " + className);
            return;
        }
        if (wildcard) {
            methodName = methodName.substring(0, methodName.length() - 1);
        }
        ArrayList<EncodedMethod> allMeths = clazz.getMethods();
        TreeMap<CstNat, EncodedMethod> meths = new TreeMap();
        Iterator it = allMeths.iterator();
        while (it.hasNext()) {
            EncodedMethod meth = (EncodedMethod) it.next();
            String methName = meth.getName().getString();
            if ((wildcard && methName.startsWith(methodName)) || (!wildcard && methName.equals(methodName))) {
                meths.put(meth.getRef().getNat(), meth);
            }
        }
        if (meths.size() == 0) {
            this.context.err.println("no such method: " + fqName);
            return;
        }
        PrintWriter pw = new PrintWriter(out);
        for (EncodedMethod meth2 : meths.values()) {
            meth2.debugPrint(pw, this.args.verboseDump);
            CstString sourceFile = clazz.getSourceFile();
            if (sourceFile != null) {
                pw.println("  source file: " + sourceFile.toQuoted());
            }
            Annotations methodAnnotations = clazz.getMethodAnnotations(meth2.getRef());
            AnnotationsList parameterAnnotations = clazz.getParameterAnnotations(meth2.getRef());
            if (methodAnnotations != null) {
                pw.println("  method annotations:");
                for (Annotation a : methodAnnotations.getAnnotations()) {
                    pw.println("    " + a);
                }
            }
            if (parameterAnnotations != null) {
                pw.println("  parameter annotations:");
                int sz = parameterAnnotations.size();
                for (int i = 0; i < sz; i++) {
                    pw.println("    parameter " + i);
                    for (Annotation a2 : parameterAnnotations.get(i).getAnnotations()) {
                        pw.println("      " + a2);
                    }
                }
            }
        }
        pw.flush();
    }
}
