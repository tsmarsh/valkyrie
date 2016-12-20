package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tailoredshapes.underbar.UnderBar.*;
import static com.tailoredshapes.underbar.UnderReg.*;
import static com.tailoredshapes.underbar.UnderString.join;
import static com.tailoredshapes.valkyrie.util.Parsing.reCharset;

/**
 * Created by tmarsh on 11/14/16.
 */
public class Request {

    Pattern charsetPattern = pattern(join(
            ";(?:.*\\s)?(?i:charset)=(",
            Parsing.value,
            ")\\s*(?:;|$)"));

    public static String requestURL(Stash request) {

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

    public static Optional<String> contentType(Stash request) {
        if (request.<Stash>get("headers").contains("content-type")) {
            String type = request.<Stash>get("headers").get("content-type");
            Matcher matcher = pattern("^(.*?)(?:;|$)").matcher(type);
            if (matcher.matches()) {
                return optional(matcher.group(1));
            }
        }
        return optional();
    }

    public static Optional<String> characterEncoding(Stash request) {
        if(request.contains("headers")){
            Stash headers = request.get("headers", Stash.class);
            if(headers.contains("content-type")){
                String type = headers.asString("content-type");
                return optional(first(groups(matcher(reCharset, type))));
            }
        }
        return optional();
    }

    public static boolean isURLEncodedForm(Stash request){
        return optionally(contentType(request),
                (type) -> type.startsWith("application/x-www-form-urlencoded"),
                () -> false);
    }

}
