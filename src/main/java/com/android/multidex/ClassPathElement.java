package com.android.multidex;

import java.io.IOException;
import java.io.InputStream;

interface ClassPathElement {
    public static final char SEPARATOR_CHAR = '/';

    void close() throws IOException;

    Iterable<String> list();

    InputStream open(String str) throws IOException;
}
