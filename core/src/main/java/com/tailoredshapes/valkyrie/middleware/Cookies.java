package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.valkyrie.util.Codec;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.Die.dieIf;
import static com.tailoredshapes.underbar.UnderBar.*;
import static com.tailoredshapes.underbar.UnderReg.groups;
import static com.tailoredshapes.underbar.UnderReg.pattern;
import static com.tailoredshapes.underbar.UnderString.join;
import static com.tailoredshapes.valkyrie.util.Codec.formEncode;
import static com.tailoredshapes.valkyrie.util.Parsing.reToken;
import static com.tailoredshapes.valkyrie.util.Parsing.value;
import static com.tailoredshapes.valkyrie.util.Request.getHeader;

public interface Cookies {

    /**
     * RFC6265 cookie octet
     */
    Pattern cookieOctet = pattern("[!#$%&'()*+\\-./0-9:<=>?@A-Z\\[\\]\\^_`a-z\\{\\|\\}~]");

    Pattern cookieValue = pattern("\"" + cookieOctet + "*\"|" + cookieOctet + "*");

    Pattern cookie = pattern("\\s*(" + reToken + ")=(" + cookieValue + ")\\s*[;,]?");


    Map<String, String> setCookieAttributes = hash(
            "domain", "Domain",
            "max-age", "Max-Age",
            "path", "Path",
            "secure", "Secure",
            "expires", "Expires",
            "http-only", "HttpOnly",
            "same-site", "SameSite"
    );

    Stash sameSiteValues = stash(
            "strict", "Strict",
            "lax", "Lax"
    );

    DateFormat rfc822Formatter =new SimpleDateFormat("yyyy-mm-DD'T'hh:mm:ssZ", Locale.US);

    static Stash parseCookieHeader(String header) {
        Matcher matcher = cookie.matcher(header);
        Map<String, Object> cookies = hash();

        while(matcher.find()){
            cookies.put(matcher.group(1), matcher.group(2));
        }
        return new Stash(cookies);
    }

    static String stripQuotes(String value){
       return value.replaceAll("^\"|\"$", "");
    }

    static <T> Stash decodeValues(Stash cookies, BiFunction<String, String[], T> decoder){
        return new Stash(
                mapFromEntry(
                        cookies.map(
                                (name, value) -> entry(name, decoder.apply(stripQuotes((String) value), array()))),
                        (i) -> i));
    }

    static <T> Stash parseCookies(Stash request, BiFunction<String, String[], T> encoder){
        String cookie = getHeader(request, "cookie");
        if(cookie != null){
            Stash cookies = parseCookieHeader(cookie);
            return decodeValues(cookies, encoder);
        }

        return stash();
    }

    static String writeValue(String key, Object value, Function<Stash, String> encoder){
        return encoder.apply(stash(key, value));
    }


    static boolean isValidAttribute(String key, Object value){
        Predicate<String> checkValuesTypes = s -> {
            switch (key) {
                case "max-age":
                    return value instanceof Instant || value instanceof Long;
                case "expires":
                    return value instanceof Date || value instanceof String;
                default:
                    return true;
            }
        };

        return setCookieAttributes.containsKey(key) &&
                !value.toString().contains(";") &&
                checkValuesTypes.test(key);

    }

    static String writeAttrMap(Stash attributes){
        for(String key : attributes.keys()) {
            dieIf(!isValidAttribute(key, attributes.get(key)),
                    () -> "cookie attributes are not valid: " + key + "" + attributes.get(key));
        }

        List<String> attrStrings = attributes.map((k, v) -> {
            String attr = setCookieAttributes.get(k);
            if(v instanceof Instant){
                return ";" + attr + "=" + ((Instant) v).getEpochSecond();
            }else if (v instanceof Date) {
                return ";" + attr + "=" + rfc822Formatter.format((Date)v);
            }else if (v instanceof Boolean){
                return (Boolean)v ? ";" + attr : "";
            } else {
                return ";" + attr + "=" + value;
            }
        });

        return join(attrStrings);
    }

    static List<String> writeCookies(Stash cookies, Function<Stash, String> encoder){
        return cookies.map((k, v) -> {
            if(v instanceof Stash){
                return writeValue(k, v, encoder) + writeAttrMap((Stash)v);
            } else {
                return writeValue(k, v, encoder);
            }
        });
    }

    static Stash setCookies(Stash response, Function<Stash, String> encoder){
        if(response.contains("cookies")){
            Stash headers = response.get("headers");
            headers.update("Set-Cookie", join(writeCookies(response.get("cookies"), encoder)));
        }
        return response;
    }

    /**
     * Parses cookies in the request map. See: wrap-cookies.
     */
    static Stash cookiesRequest(Stash request){
        return cookiesRequest(request, stash());
    }

    static Stash cookiesRequest(Stash request, Stash options){
        BiFunction<String, String[], String> encoder = options.get("encoder", Codec::formDecodeString);
        if(request.contains("cookies")){
            return request;
        } else {
            return request.update("cookies", parseCookies(request, encoder));
        }
    }
}
