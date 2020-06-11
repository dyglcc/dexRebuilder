package com.android.multidex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

class FolderPathElement implements ClassPathElement {
    private final File baseFolder;

    public FolderPathElement(File baseFolder) {
        this.baseFolder = baseFolder;
    }

    public InputStream open(String path) throws FileNotFoundException {
        return new FileInputStream(new File(this.baseFolder, path.replace(ClassPathElement.SEPARATOR_CHAR, File.separatorChar)));
    }

    public void close() {
    }

    public Iterable<String> list() {
        ArrayList<String> result = new ArrayList();
        collect(this.baseFolder, "", result);
        return result;
    }

    private void collect(File folder, String prefix, ArrayList<String> result) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                collect(file, prefix + ClassPathElement.SEPARATOR_CHAR + file.getName(), result);
            } else {
                result.add(prefix + ClassPathElement.SEPARATOR_CHAR + file.getName());
            }
        }
    }
}
