package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stringmap.StringMap;
import com.tailoredshapes.underbar.Strings;
import com.tailoredshapes.underbar.UnderBar;

import java.io.File;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

import static com.tailoredshapes.stringmap.StringMap.smap;
import static com.tailoredshapes.underbar.Die.rethrow;
import static com.tailoredshapes.underbar.UnderBar.*;
import static com.tailoredshapes.valkyrie.util.IO.lastModifiedDate;
import static com.tailoredshapes.valkyrie.util.Time.formatDate;

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

        RedirectStatusCode(int statusCode){
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

    public static StringMap created(String url, String body){
        return smap(
                "status", 201,
                "headers", smap("Location", url),
                "body", body
        );
    }

    public static StringMap created(String url){
        return created(url, null);
    }

    public static StringMap notFound(String body){
        return smap(
                "status", 404,
                "headers", smap(),
                "body", body
        );
    }

    public static StringMap response(String body){
        return smap(
                "status", 200,
                "headers", smap(),
                "body", body
        );
    }

    public static StringMap status(StringMap response, RedirectStatusCode status){
        return response.put("status", status);
    }

    public static <T> StringMap header(StringMap response, String key, T value){
        return response.smap("headers").put(key, value);
    }

    private static boolean isSafePath(String root, String path){
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

    public static boolean isDirectoryTraversal(String path){
        return set(path.split("/|\\\\")).contains("..");
    }

    public static Optional<File> findFileStartingWith(File dir, String prefix){
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

    public static File safelyFindFile(String path, StringMap opts){
        String root = opts.string("root");
        if(Strings.hasContent(root)){
            if((isSafePath(root, path)) || (opts.bool("allowSymlinks?") && !isDirectoryTraversal(path))){
                return new File(root, path);
            } else {
                return null;
            }
        }else {
            return new File(path);
        }
    }

    public static File findFile(String path, StringMap opts){
        return maybeNull(safelyFindFile(path, opts), f -> {
            if(f.isDirectory()){
                if(opts.bool("indexFiles?", true)){
                    return optionally_(findIndexFile(f), (o)->o, ()->null);
                }
            } else if(f.exists()){
                return f;
            }
            return null;
        }, () -> null);
    }

    public static StringMap fileData(File file){
        return smap(
                "content", file,
                "content-length", file.length(),
                "last-modified", lastModifiedDate(file));
    }

    public static StringMap contentLength(StringMap resp, int len){
        return header(resp, "Content-Length", len);
    }

    public static StringMap lastModified(StringMap resp, Date lastModified){
        return header(resp, "Last-Modified", formatDate(lastModified));
    }

    public static Optional<StringMap> fileResponse(String filePath){
        return fileResponse(filePath, smap());
    }

    public static Optional<StringMap> fileResponse(String filePath, StringMap opts){
        File file = findFile(filePath, opts);
        if(file != null) {
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
    
}

