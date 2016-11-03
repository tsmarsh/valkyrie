package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.underbar.UnderBar;
import com.tailoredshapes.underbar.UnderString;

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

import static com.tailoredshapes.stash.Stash.stash;
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

    public static Stash redirect(String url) {
        return redirect(url, RedirectStatusCode.FOUND);
    }

    public static Stash redirect(String url, RedirectStatusCode status) {
        return Stash.stash(
                "status", status.code,
                "headers", Stash.stash("Location", url),
                "body", ""
        );
    }

    public static Stash created(String url, String body) {
        return Stash.stash(
                "status", 201,
                "headers", Stash.stash("Location", url),
                "body", body
        );
    }

    public static Stash created(String url) {
        return created(url, null);
    }

    public static Stash notFound(String body) {
        return Stash.stash(
                "status", 404,
                "headers", Stash.stash(),
                "body", body
        );
    }

    public static Stash response(String body) {
        return Stash.stash(
                "status", 200,
                "headers", Stash.stash(),
                "body", body
        );
    }

    public static Stash status(Stash response, RedirectStatusCode status) {
        return response.update("status", status);
    }

    public static <T> Stash header(Stash response, String key, T value) {
        return response.get("headers", Stash.class).update(key, value);
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
        return optionally(Optional.ofNullable(dir.listFiles()), (files) ->
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

    public static File safelyFindFile(String path, Stash opts) {
        String root = opts.get("root");
        if (UnderString.hasContent(root)) {
            if ((isSafePath(root, path)) || (opts.bool("allowSymlinks?") && !isDirectoryTraversal(path))) {
                return new File(root, path);
            } else {
                return null;
            }
        } else {
            return new File(path);
        }
    }

    public static File findFile(String path, Stash opts) {
        return maybeNull(safelyFindFile(path, opts), f -> {
            if (f.isDirectory()) {
                if (opts.get("indexFiles?", true)) {
                    return optionally(findIndexFile(f), (o) -> o, () -> null);
                }
            } else if (f.exists()) {
                return f;
            }
            return null;
        }, () -> null);
    }

    public static Stash fileData(File file) {
        return Stash.stash(
                "content", file,
                "content-length", file.length(),
                "last-modified", lastModifiedDate(file));
    }

    public static Stash contentLength(Stash resp, int len) {
        return header(resp, "Content-Length", len);
    }

    public static Stash lastModified(Stash resp, Date lastModified) {
        return header(resp, "Last-Modified", formatDate(lastModified));
    }

    public static Optional<Stash> fileResponse(String filePath) {
        return fileResponse(filePath, Stash.stash());
    }

    public static Optional<Stash> fileResponse(String filePath, Stash opts) {
        File file = findFile(filePath, opts);
        if (file != null) {
            Stash data = fileData(file);
            return optional(lastModified(
                    contentLength(
                            response(
                                    data.get("content")),
                            data.i("content-length")),
                    data.get("last-modified")));
        }
        return optional();
    }

    public static File urlAsFile(URL u) {
        return new File(
                rethrow(
                        () -> decode(
                                u
                                        .getFile()
                                        .replace('/', File.separatorChar)
                                        .replace('+', rethrow(
                                                () -> encode("+", "UTF-8")).charAt(0)), "UTF-8")), "UTF-8");
    }

    public static Stash contentType(Stash resp, String contentType) {
        return header(resp, "Content-Type", contentType);
    }


    public static String getHeader(Stash resp, String headerName) {
        return ((Stash) resp.get("headers")).get(headerName);
    }

    public static Stash updateHeader(Stash resp, String headerName, Function<String, String> upD) {
        Stash headers = resp.get("headers");
        return headers.update(headerName, upD.apply(headers.get("headerName")));
    }

    public static Stash charset(Stash resp, String charset) {
        return updateHeader(resp, "Content-Type",
                (ct) -> maybeNull(ct, c -> c, () -> "text/plain")
                        .replace(";\\s*charset=[^;]*", "") + "; charset=" + charset);
    }

    public static Optional<String> getCharset(Stash resp) {
        String contentType = getHeader(resp, "Content-Type");
        Matcher hasFoundCharset = reCharset.matcher(contentType);
        if (hasFoundCharset.matches()) {
            return optional(hasFoundCharset.group(1));
        }
        return optional();
    }

    public static Stash setCookie(Stash resp, String name, String value) {
        return setCookie(resp, name, value, Stash.stash());
    }

    public static Stash setCookie(Stash resp, String name, String value, Stash opts) {
        return resp.get("cookies", Stash.class).update(name, opts.update("value", value));
    }

    public static boolean isReponse(Stash resp) {
        return resp.maybe("status").isPresent() &&
                resp.maybe("headers").isPresent();
    }

    public static Optional<Stash> resourceData(URL url) {
        switch (url.getProtocol().toLowerCase()) {
            case "url":
                return fileResourceData(url);
            case "jar":
                return jarResourceData(url);
            default:
                return optional(stash(
                        "content", "",
                        "content-length", null, "last-modified", null));
        }
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


    public static Optional<Stash> urlResponse(URL url) {
        Optional<Stash> odata = resourceData(url);
        if (odata.isPresent()) {
            Stash data = odata.get();
            String content = optionally(data.<String>optional("content"), f -> f, () -> "");
            Integer contentLength = optionally(data.<Integer>optional("content-length"), f -> f, () -> 0);
            Number lastModified = optionally(data.<Number>optional("last-modified"), f -> f, () -> 0);
            return optional(
                    lastModified(
                            contentLength(
                                    response(content),
                                    contentLength),
                            new Date(lastModified.longValue())));

        }
        return odata;
    }

    public static Optional<Stash> resourceResponse(String path, String root, ClassLoader loader) {
        String base = (root + "/" + path)
                .replace("//", "/")
                .replaceAll("^/", "");
        URL url = loader != null ? loader.getResource(base) :
                Object.class.getResource(base);
        return urlResponse(url);

    }
}


