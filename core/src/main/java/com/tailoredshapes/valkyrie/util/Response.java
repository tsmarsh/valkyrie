package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stringmap.StringMap;
import com.tailoredshapes.underbar.Strings;
import com.tailoredshapes.underbar.UnderBar;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;

import static com.tailoredshapes.stringmap.StringMap.smap;
import static com.tailoredshapes.underbar.Die.rethrow;
import static com.tailoredshapes.underbar.UnderBar.*;
import static com.tailoredshapes.valkyrie.util.IO.lastModifiedDate;
import static com.tailoredshapes.valkyrie.util.Parsing.reCharset;
import static com.tailoredshapes.valkyrie.util.Time.formatDate;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;

/**
 * Created by tmarsh on 10/25/16.
 */
public class Response {
    public enum RedirectStatusCode {
        MOVED_PERMANENTLY(301),
        FOUND(302),
        SEE_OTHER(303),
        TEMPORARY_REDIRECT(307),
        PERMANENT_REDIRECT(308);

        int code;

        RedirectStatusCode(int statusCode) {
            this.code = statusCode;
        }
    }

    public static StringMap redirect(String url) {
        return redirect(url, RedirectStatusCode.FOUND);
    }

    public static StringMap redirect(String url, RedirectStatusCode status) {
        return smap(
                "status", status.code,
                "headers", smap("Location", url),
                "body", ""
        );
    }

    public static StringMap created(String url, String body) {
        return smap(
                "status", 201,
                "headers", smap("Location", url),
                "body", body
        );
    }

    public static StringMap created(String url) {
        return created(url, null);
    }

    public static StringMap notFound(String body) {
        return smap(
                "status", 404,
                "headers", smap(),
                "body", body
        );
    }

    public static StringMap response(String body) {
        return smap(
                "status", 200,
                "headers", smap(),
                "body", body
        );
    }

    public static StringMap status(StringMap response, RedirectStatusCode status) {
        return response.put("status", status);
    }

    public static <T> StringMap header(StringMap response, String key, T value) {
        return response.smap("headers").put(key, value);
    }

    private static boolean isSafePath(String root, String path) {
        return rethrow(() -> new File(root, path)
                .getCanonicalPath()
                .startsWith(
                        new File(root)
                                .getCanonicalPath()));
    }

    public static Optional<File> findFileNamed(File dir, String filename) {
        File path = new File(dir, filename);
        return path.isFile() ? optional(path) : optional();
    }

    public static boolean isDirectoryTraversal(String path) {
        return set(path.split("/|\\\\")).contains("..");
    }

    public static Optional<File> findFileStartingWith(File dir, String prefix) {
        return optionally_(Optional.ofNullable(dir.listFiles()), (files) ->
                        stream(list(files))
                                .filter(
                                        (file) ->
                                                file.getName()
                                                        .toLowerCase()
                                                        .startsWith(prefix))
                                .findFirst()
                , UnderBar::optional);
    }

    public static Optional<File> findIndexFile(File dir) {
        Optional<Supplier<Optional<File>>> optionalSupplier = takeWhile(list(
                lazy(() -> findFileNamed(dir, "index.html")),
                lazy(() -> findFileNamed(dir, "index.htm")),
                lazy(() -> findFileStartingWith(dir, "index."))), (x) -> x.get().isPresent());

        return optionalSupplier.isPresent() ? optionalSupplier.get().get() : optional();
    }

    public static File safelyFindFile(String path, StringMap opts) {
        String root = opts.string("root");
        if (Strings.hasContent(root)) {
            if ((isSafePath(root, path)) || (opts.bool("allowSymlinks?") && !isDirectoryTraversal(path))) {
                return new File(root, path);
            } else {
                return null;
            }
        } else {
            return new File(path);
        }
    }

    public static File findFile(String path, StringMap opts) {
        return maybeNull(safelyFindFile(path, opts), f -> {
            if (f.isDirectory()) {
                if (opts.bool("indexFiles?", true)) {
                    return optionally_(findIndexFile(f), (o) -> o, () -> null);
                }
            } else if (f.exists()) {
                return f;
            }
            return null;
        }, () -> null);
    }

    public static StringMap fileData(File file) {
        return smap(
                "content", file,
                "content-length", file.length(),
                "last-modified", lastModifiedDate(file));
    }

    public static StringMap contentLength(StringMap resp, int len) {
        return header(resp, "Content-Length", len);
    }

    public static StringMap lastModified(StringMap resp, Date lastModified) {
        return header(resp, "Last-Modified", formatDate(lastModified));
    }

    public static Optional<StringMap> fileResponse(String filePath) {
        return fileResponse(filePath, smap());
    }

    public static Optional<StringMap> fileResponse(String filePath, StringMap opts) {
        File file = findFile(filePath, opts);
        if (file != null) {
            StringMap data = fileData(file);
            return optional(lastModified(
                    contentLength(
                            response(
                                    data.string("content")),
                            data.integer("content-length")),
                    data.date("last-modified").toDate()));
        }
        return optional();
    }

    public static File urlAsFile(URL u) {
        return new File(decode(u
                .getFile()
                .replace('/', File.separatorChar)
                .replace('+', rethrow(
                        () -> encode("+", "UTF-8")).charAt(0))), "UTF-8");
    }

    public static StringMap contentType(StringMap resp, String contentType) {
        return header(resp, "Content-Type", contentType);
    }


    public static String getHeader(StringMap resp, String headerName) {
        return resp.smap("headers").string(headerName);
    }

    public static StringMap updateHeader(StringMap resp, String headerName, Function<String, String> upD) {
        StringMap headers = resp.smap("headers");
        return headers.put(headerName, upD.apply(headers.string("headerName")));
    }

    public static StringMap charset(StringMap resp, String charset) {
        return updateHeader(resp, "Content-Type",
                (ct) -> maybeNull(ct, c -> c, () -> "text/plain")
                        .replace(";\\s*charset=[^;]*", "") + "; charset=" + charset);
    }

    public static Optional<String> getCharset(StringMap resp) {
        String contentType = getHeader(resp, "Content-Type");
        Matcher hasFoundCharset = reCharset.matcher(contentType);
        if (hasFoundCharset.matches()) {
            return optional(hasFoundCharset.group(1));
        }
        return optional();
    }

    public static StringMap setCookie(StringMap resp, String name, String value) {
        return setCookie(resp, name, value, smap());
    }

    public static StringMap setCookie(StringMap resp, String name, String value, StringMap opts) {
        return resp.smap("cookies").put(name, opts.put("value", value));
    }

    public static boolean isReponse(StringMap resp) {
        return resp.intish("status").isPresent() &&
                resp.smapish("headers").isPresent();
    }

    public static Optional<StringMap> resourceData(URL url) {
        switch (url.getProtocol().toLowerCase()) {
            case "url":
                return fileResourceData(url);
            case "jar":
                return jarResourceData();
            default:
                return smap(
                        "content", "",
                        "content-length", null, "last-modified", null);
        }
    }

    public static Optional<StringMap> fileResourceData(URL url) {
        File file = urlAsFile(url);
        if (file.exists()) {
            if (!file.isDirectory()) {
                return optional(fileData(file));
            }
        }
        return optional();
    }

    public static Optional<StringMap> jarResourceData(URL url) {
        URLConnection conn = rethrow(() -> url.openConnection());

        if (conn instanceof JarURLConnection) {
            return isJARDirectory(
                    (JarURLConnection) conn) ?
                    optional(
                            smap(
                                    "content", rethrow(() -> conn.getInputStream()),
                                    "content-length", connectionContentLength(conn),
                                    "last-modified", connectionLastModified(conn))) :
                    optional(smap());
        }
        return optional(smap());
    }

    public static String addEndingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    public static boolean isJARDirectory(JarURLConnection conn) {
        JarFile jarFile = rethrow(conn::getJarFile);
        String entryName = conn.getEntryName();
        ZipEntry entry = jarFile.getEntry(addEndingSlash(entryName));
        return entry != null && entry.isDirectory();
    }

    public static Optional<Integer> connectionContentLength(URLConnection conn) {
        int contentLength = conn.getContentLength();
        return 0 <= contentLength ? optional(contentLength) : optional();
    }

    public static Optional<Long> connectionLastModified(URLConnection conn) {
        long lastModified = conn.getLastModified();
        return lastModified != 0 ? optional(lastModified) : optional();
    }


    public static Optional<StringMap> urlResponse(URL url){
        Optional<StringMap> odata = resourceData(url);
        if(odata.isPresent()){
            StringMap data = odata.get();
            String content = optionally_(data.stringMaybe("content"), f->f, ()-> "");
            Integer contentLength = optionally_(data.integerMaybe("content-length"), f -> f, () -> 0);
            Number lastModified = optionally_(data.longMaybe("last-modified"), f -> f, () -> 0);
            return optional(
                    lastModified(
                            contentLength(
                                    response(content),
                                    contentLength),
                            new Date(lastModified.longValue())));

        }
            return odata;
    }

    public static Optional<StringMap> resourceResponse(String path, String root, ClassLoader loader){
        String base = (root + "/" + path)
                        .replace("//", "/")
                        .replaceAll("^/", "");
        URL url = loader != null ? loader.getResource(base) :
                Object.class.getResource(base);
        return urlResponse(url);

    }
}


