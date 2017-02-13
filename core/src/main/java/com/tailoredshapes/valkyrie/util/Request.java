package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;

import java.io.File;
import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.IO.slurp;
import static com.tailoredshapes.underbar.UnderBar.*;
import static com.tailoredshapes.underbar.UnderReg.*;
import static com.tailoredshapes.underbar.UnderString.join;
import static com.tailoredshapes.valkyrie.util.Parsing.reCharset;

/**
 * Created by tmarsh on 11/14/16.
 */
public interface Request {

    static String requestURL(Stash request) {

        String url = join(
                request.<String>get("scheme"),
                "://",
                request.<Stash>get("headers").<String>get("host"),
                request.<String>get("uri"));


        if (request.contains("query-string")) {
            return url + "?" + request.get("query-string");
        }

        return url;
    }

    static Optional<String> contentType(Stash request) {
        if (request.<Stash>get("headers").contains("content-type")) {
            String type = request.<Stash>get("headers").get("content-type");
            Matcher matcher = pattern("^(.*?)(?:;|$)").matcher(type);
            if (matcher.matches()) {
                return optional(matcher.group(1));
            }
        }
        return optional();
    }

    static Optional<String> characterEncoding(Stash request) {
        if (request.contains("headers")) {
            Stash headers = request.get("headers", Stash.class);
            if (headers.contains("content-type")) {
                String type = headers.asString("content-type");
                return optional(first(groups(matcher(reCharset, type))));
            }
        }
        return optional();
    }

    static boolean isURLEncodedForm(Stash request) {
        return optionally(contentType(request),
                (type) -> type.startsWith("application/x-www-form-urlencoded"),
                () -> false);
    }

    static Optional<String> bodyString(Stash request) {
        Optional body = request.maybe("body");

        if (body.isPresent()) {
            Object bodyObject = body.get();
            if (bodyObject instanceof String) {
                return optional((String) bodyObject);
            }

            if (bodyObject instanceof File) {
                return optional(slurp((File) bodyObject));
            }

            if (bodyObject instanceof InputStream) {
                return optional(slurp((InputStream) bodyObject));
            }
        }

        return optional();
    }

    static Optional<String> pathInfo(Stash request) {
        Optional<String> pathInfo = request.maybe("path-info");
        if (pathInfo.isPresent()) return pathInfo;
        Optional<String> uri = request.maybe("uri");
        if (uri.isPresent()) return uri;
        return optional();
    }

    static boolean hasContext(Stash request, String context){
        Optional<String> uri = request.maybe("uri");
        if(uri.isPresent()){
            return uri.get().startsWith(context);
        }
        return false;
    }

    static Stash setContext(Stash request, String context){
        String uri = request.get("uri");
        return request.assoc("context", context).assoc("path-info", uri.substring(context.length()));
    }

    static <T> T getHeader(Stash request, String name) {
        Stash headers = request.get("headers", stash());
        return headers.contains(name) ? headers.get(name) : null;
    }
}
