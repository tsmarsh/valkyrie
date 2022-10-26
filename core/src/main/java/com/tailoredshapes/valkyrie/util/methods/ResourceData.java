package com.tailoredshapes.valkyrie.util.methods;

import com.tailoredshapes.stash.Stash;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.ocho.Die.rethrow;
import static com.tailoredshapes.underbar.ocho.UnderBar.optional;
import static com.tailoredshapes.valkyrie.util.Response.*;


public abstract class ResourceData {

    private static final Map<String, Function<URL, Optional<Stash>>> impls = new HashMap<>();

    static {
        extend("url", ResourceData::fileResourceData);
        extend("file", ResourceData::fileResourceData);
        extend("jar", ResourceData::jarResourceData);
    }


    public static Optional<Stash> fileResourceData(URL url) {
        File file = urlAsFile(url);
        if (file.exists()) {
            if (!file.isDirectory()) {
                return optional(fileData(file));
            }
        }
        return optional();
    }

    public static Optional<Stash> jarResourceData(URL url) {
        URLConnection conn = rethrow(() -> url.openConnection());

        if (conn instanceof JarURLConnection) {
            return isJARDirectory(
                    (JarURLConnection) conn) ?
                    optional(
                            Stash.stash(
                                    "content", rethrow(() -> conn.getInputStream()),
                                    "content-length", connectionContentLength(conn),
                                    "last-modified", connectionLastModified(conn))) :
                    optional(Stash.stash());
        }
        return optional(Stash.stash());
    }

    public static <T> void extend(String protocol, Function<URL, Optional<Stash>> impl) {
        synchronized (impls) {
            impls.put(protocol, impl);
        }
    }

    public static Optional<Stash> resourceData(URL url) {
        Function<URL, Optional<Stash>> content = impls.getOrDefault(url.getProtocol(), (x) -> optional(stash(
                "content", "",
                "content-length", null, "last-modified", null)));
        return content.apply(url);
    }
}
