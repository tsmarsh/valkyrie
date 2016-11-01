package com.tailoredshapes.valkyrie.util;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.util.Date;

import static com.tailoredshapes.underbar.Die.rethrow;

/**
 * Created by tmarsh on 11/1/16.
 */
public class IO {
    public static ByteArrayInputStream stringInputStream(String s){
        return new ByteArrayInputStream(s.getBytes());
    }

    public static ByteArrayInputStream stringInputStream(String s, String encoding){
        return rethrow(() -> new ByteArrayInputStream(s.getBytes(encoding)));
    }

    public static void close(Closeable stream){
        rethrow(stream::close);
    }

    public static Date lastModifiedDate(File file){
        long l = file.lastModified() / 1000;
        return new Date(l * 1000);
    }
}
