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
import static com.tailoredshapes.underbar.IO.lastModifiedDate;
import static com.tailoredshapes.underbar.IO.slurp;
import static com.tailoredshapes.underbar.UnderBar.*;
import static com.tailoredshapes.valkyrie.util.Parsing.reCharset;
import static com.tailoredshapes.valkyrie.util.Time.formatDate;
import static java.net.URLDecoder.decode;
import static java.net.URLEncoder.encode;

/**
 * Created by tmarsh on 10/25/16.
 */
public interface Response {
    enum StatusCode {
        OK(200),
        CREATED(201),
        MOVED_PERMANENTLY(301),
        FOUND(302),
        SEE_OTHER(303),
        TEMPORARY_REDIRECT(307),
        PERMANENT_REDIRECT(308),

        NOT_FOUND(404);
        int code;

        StatusCode(int statusCode) {
            this.code = statusCode;
        }
    }

    static Stash redirect(String url) {
        return redirect(url, StatusCode.FOUND);
    }

    static Stash redirect(String url, StatusCode status) {
        return Stash.stash(
                "status", status.code,
                "headers", Stash.stash("Location", url),
                "body", ""
        );
    }

    static Stash created(String url, String body) {
        return Stash.stash(
                "status", StatusCode.CREATED.code,
                "headers", Stash.stash("Location", url),
                "body", body
        );
    }

    static Stash created(String url) {
        return created(url, null);
    }

    static Stash notFound(String body) {
        return Stash.stash(
                "status", StatusCode.NOT_FOUND.code,
                "headers", Stash.stash(),
                "body", body
        );
    }

    static Stash response(Object body) {
        return Stash.stash(
                "status", StatusCode.OK.code,
                "headers", Stash.stash(),
                "body", body
        );
    }

    static Stash status(Stash response, StatusCode status) {
        return response.update("status", status.code);
    }

    static <T> Stash header(Stash response, String key, T value) {
        Stash headers = response.get("headers", stash());
        headers.update(key, value);
        response.update("headers", headers);
        return response;
    }

    static boolean isSafePath(String root, String path) {
        return rethrow(() -> new File(root, path)
                .getCanonicalPath()
                .startsWith(
                        new File(root)
                                .getCanonicalPath()));
    }

    static Optional<File> findFileNamed(File dir, String filename) {
        File path = new File(dir, filename);
        return path.isFile() ? optional(path) : optional();
    }

    static boolean isDirectoryTraversal(String path) {
        return set(path.split("/|\\\\")).contains("..");
    }

    static Optional<File> findFileStartingWith(File dir, String prefix) {
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

    static Optional<File> findIndexFile(File dir) {
        Optional<Supplier<Optional<File>>> optionalSupplier = takeWhile(list(
                lazy(() -> findFileNamed(dir, "index.html")),
                lazy(() -> findFileNamed(dir, "index.htm")),
                lazy(() -> findFileStartingWith(dir, "index."))), (x) -> x.get().isPresent());

        return optionalSupplier.isPresent() ? optionalSupplier.get().get() : optional();
    }

    static File safelyFindFile(String path, Stash opts) {
        String root = opts.get("root", "");
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

    static File findFile(String path, Stash opts) {
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

    static Stash fileData(File file) {
        return Stash.stash(
                "content", file,
                "content-length", file.length(),
                "last-modified", lastModifiedDate(file));
    }

    static Stash contentLength(Stash resp, long len) {
        return header(resp, "Content-Length", len);
    }

    static Stash lastModified(Stash resp, Date lastModified) {
        return header(resp, "Last-Modified", formatDate(lastModified));
    }

    static Optional<Stash> fileResponse(String filePath) {
        return fileResponse(filePath, Stash.stash());
    }

    static Stash contentType(Stash resp, String contentType) {
        return header(resp, "Content-Type", contentType);
    }

    static Stash charset(Stash resp, String charset) {
        return updateHeader(resp, "Content-Type",
                (ct) -> maybeNull(ct, c -> c, () -> "text/plain")
                        .replace(";\\s*charset=[^;]*", "") + "; charset=" + charset);
    }

    static Optional<Stash> fileResponse(String filePath, Stash opts) {
        File file = findFile(filePath, opts);
        if (file != null) {
            Stash data = fileData(file);
            return optional(lastModified(
                    contentLength(
                            response(data.get("content", File.class)),
                            data.l("content-length")),
                    data.get("last-modified")));
        }
        return optional();
    }

    static Stash setCookie(Stash resp, String name, String value) {
        return setCookie(resp, name, value, Stash.stash());
    }

    static Stash setCookie(Stash resp, String name, String value, Stash opts) {
        Stash cookies = resp.get("cookies", stash());
        cookies.update(name,opts.update("value", value));;
        return resp.update("cookies", cookies);
    }

    static File urlAsFile(URL u) {
        return new File(
                rethrow(
                        () -> decode(
                                u
                                        .getFile()
                                        .replace('/', File.separatorChar)
                                        .replace('+', rethrow(
                                                () -> encode("+", "UTF-8")).charAt(0)), "UTF-8")));
    }

    static Optional<String> getHeader(Stash resp, String headerName) {
        return ((Stash) resp.get("headers")).maybe(headerName);
    }

    static Stash updateHeader(Stash resp, String headerName, Function<String, String> upD) {
        Stash headers = resp.get("headers");
        return headers.update(headerName, upD.apply(headers.get(headerName)));
    }

    static Optional<String> getCharset(Stash resp) {
        Optional<String> contentType = getHeader(resp, "Content-Type");
        if(contentType.isPresent()){
            Matcher hasFoundCharset = reCharset.matcher(contentType.get());
            if (hasFoundCharset.matches()) {
                return optional(hasFoundCharset.group(1));
            }
        }

        return optional();
    }

    static boolean isReponse(Stash resp) {
        return resp.maybe("status").isPresent() &&
                resp.maybe("headers").isPresent();
    }

    static Optional<Stash> resourceData(URL url) {
        switch (url.getProtocol().toLowerCase()) {
            case "url":
            case "file":
                return fileResourceData(url);
            case "jar":
                return jarResourceData(url);
            default:
                return optional(stash(
                        "content", "",
                        "content-length", null, "last-modified", null));
        }
    }

    static Optional<Stash> fileResourceData(URL url) {
        File file = urlAsFile(url);
        if (file.exists()) {
            if (!file.isDirectory()) {
                return optional(fileData(file));
            }
        }
        return optional();
    }

    static Optional<Stash> jarResourceData(URL url) {
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

    static String addEndingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    static boolean isJARDirectory(JarURLConnection conn) {
        JarFile jarFile = rethrow(conn::getJarFile);
        String entryName = conn.getEntryName();
        ZipEntry entry = jarFile.getEntry(addEndingSlash(entryName));
        return entry != null && entry.isDirectory();
    }

    static Optional<Integer> connectionContentLength(URLConnection conn) {
        int contentLength = conn.getContentLength();
        return 0 <= contentLength ? optional(contentLength) : optional();
    }

    static Optional<Long> connectionLastModified(URLConnection conn) {
        long lastModified = conn.getLastModified();
        return lastModified != 0 ? optional(lastModified) : optional();
    }


    static Optional<Stash> urlResponse(URL url) {
        Optional<Stash> odata = resourceData(url);
        if (odata.isPresent()) {
            Stash data = odata.get();
            Object content = data.get("content", "");
            Long contentLength = data.get("content-length", 0L);
            Date lastModified = data.get("last-modified", new Date());
            return optional(
                    lastModified(
                            contentLength(
                                    response(content),
                                    contentLength),
                            lastModified));

        }
        return odata;
    }

    static Optional<Stash> resourceResponse(String path, String root, ClassLoader loader) {
        String base = (root + "/" + path)
                .replace("//", "/")
                .replaceAll("^/", "");
        URL url = loader != null ? loader.getResource(base) :
                Object.class.getResource(base);
        return urlResponse(url);

    }
}


