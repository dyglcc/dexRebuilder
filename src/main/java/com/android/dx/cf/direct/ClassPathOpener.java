package com.android.dx.cf.direct;

import com.android.dex.util.FileUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassPathOpener {
    public static final FileNameFilter acceptAll = new FileNameFilter() {
        public boolean accept(String path) {
            return true;
        }
    };
    private final Consumer consumer;
    private FileNameFilter filter;
    private final String pathname;
    private final boolean sort;

    public interface FileNameFilter {
        boolean accept(String str);
    }

    public interface Consumer {
        void onException(Exception exception);

        void onProcessArchiveStart(File file);

        boolean processFileBytes(String str, long j, byte[] bArr);
    }

    public ClassPathOpener(String pathname, boolean sort, Consumer consumer) {
        this(pathname, sort, acceptAll, consumer);
    }

    public ClassPathOpener(String pathname, boolean sort, FileNameFilter filter, Consumer consumer) {
        this.pathname = pathname;
        this.sort = sort;
        this.consumer = consumer;
        this.filter = filter;
    }

    public boolean process() {
        return processOne(new File(this.pathname), true);
    }

    private boolean processOne(File file, boolean topLevel) {
        try {
            if (file.isDirectory()) {
                return processDirectory(file, topLevel);
            }
            String path = file.getPath();
            if (path.endsWith(".zip") || path.endsWith(".jar") || path.endsWith(".apk")) {
                return processArchive(file);
            }
            if (!this.filter.accept(path)) {
                return false;
            }
            return this.consumer.processFileBytes(path, file.lastModified(), FileUtils.readFile(file));
        } catch (Exception ex) {
            this.consumer.onException(ex);
            return false;
        }
    }

    private static int compareClassNames(String a, String b) {
        return a.replace('$', '0').replace("package-info", "").compareTo(b.replace('$', '0').replace("package-info", ""));
    }

    private boolean processDirectory(File dir, boolean topLevel) {
        if (topLevel) {
            dir = new File(dir, ".");
        }
        File[] files = dir.listFiles();
        boolean any = false;
        if (this.sort) {
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File a, File b) {
                    return ClassPathOpener.compareClassNames(a.getName(), b.getName());
                }
            });
        }
        for (File processOne : files) {
            any |= processOne(processOne, false);
        }
        return any;
    }

    private boolean processArchive(File file) throws IOException {
        ZipFile zip = new ZipFile(file);
        ArrayList<? extends ZipEntry> entriesList = Collections.list(zip.entries());
        if (this.sort) {
            Collections.sort(entriesList, new Comparator<ZipEntry>() {
                public int compare(ZipEntry a, ZipEntry b) {
                    return ClassPathOpener.compareClassNames(a.getName(), b.getName());
                }
            });
        }
        this.consumer.onProcessArchiveStart(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(40000);
        byte[] buf = new byte[20000];
        boolean any = false;
        Iterator it = entriesList.iterator();
        while (it.hasNext()) {
            ZipEntry one = (ZipEntry) it.next();
            boolean isDirectory = one.isDirectory();
            String path = one.getName();
            if (this.filter.accept(path)) {
                byte[] bytes;
                if (isDirectory) {
                    bytes = new byte[0];
                } else {
                    InputStream in = zip.getInputStream(one);
                    baos.reset();
                    while (true) {
                        int read = in.read(buf);
                        if (read == -1) {
                            break;
                        }
                        baos.write(buf, 0, read);
                    }
                    in.close();
                    bytes = baos.toByteArray();
                }
                any |= this.consumer.processFileBytes(path, one.getTime(), bytes);
            }
        }
        zip.close();
        return any;
    }
}
