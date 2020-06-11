package com.android.multidex;

import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

class Path {
    static final /* synthetic */ boolean $assertionsDisabled = (!Path.class.desiredAssertionStatus());
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(40960);
    private final String definition;
    List<ClassPathElement> elements = new ArrayList();
    private final byte[] readBuffer = new byte[20480];

    static ClassPathElement getClassPathElement(File file) throws ZipException, IOException {
        if (file.isDirectory()) {
            return new FolderPathElement(file);
        }
        if (file.isFile()) {
            return new ArchivePathElement(new ZipFile(file));
        }
        if (file.exists()) {
            throw new IOException("\"" + file.getPath() + "\" is not a directory neither a zip file");
        }
        throw new FileNotFoundException("File \"" + file.getPath() + "\" not found");
    }

    Path(String definition) throws IOException {
        this.definition = definition;
        String[] split = definition.split(Pattern.quote(File.pathSeparator));
        int length = split.length;
        int i = 0;
        while (i < length) {
            try {
                addElement(getClassPathElement(new File(split[i])));
                i++;
            } catch (IOException e) {
                throw new IOException("Wrong classpath: " + e.getMessage(), e);
            }
        }
    }

    private static byte[] readStream(InputStream in, ByteArrayOutputStream baos, byte[] readBuffer) throws IOException {
        while (true) {
            try {
                int amt = in.read(readBuffer);
                if (amt < 0) {
                    break;
                }
                baos.write(readBuffer, 0, amt);
            } finally {
                in.close();
            }
        }
        return baos.toByteArray();
    }

    public String toString() {
        return this.definition;
    }

    Iterable<ClassPathElement> getElements() {
        return this.elements;
    }

    private void addElement(ClassPathElement element) {
        if ($assertionsDisabled || element != null) {
            this.elements.add(element);
            return;
        }
        throw new AssertionError();
    }

    synchronized DirectClassFile getClass(String path) throws FileNotFoundException {
        DirectClassFile classFile;
        Throwable th;
        try {
            DirectClassFile classFile2 = null;
            for (ClassPathElement element : this.elements) {
                try {
                    try {
                        InputStream in = element.open(path);
                        try {
                            byte[] bytes = readStream(in, this.baos, this.readBuffer);
                            this.baos.reset();
                            classFile = new DirectClassFile(bytes, path, false);
                            try {
                                classFile.setAttributeFactory(StdAttributeFactory.THE_ONE);
                            } catch (Throwable th2) {
                                th = th2;
                                in.close();
                                throw th;
                            }
                            try {
                                in.close();
                                break;
                            } catch (IOException e) {
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            classFile = classFile2;
                            in.close();
                            throw th;
                        }
                    } catch (IOException e2) {
                        classFile = classFile2;
                        classFile2 = classFile;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    classFile = classFile2;
                }
            }
            classFile = classFile2;
            if (classFile != null) {
                return classFile;
            }
            throw new FileNotFoundException("File \"" + path + "\" not found");
        } catch (Throwable th5) {
            th = th5;
            throw th;
        }
    }
}
