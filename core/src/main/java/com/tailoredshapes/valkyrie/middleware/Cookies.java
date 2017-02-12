package com.tailoredshapes.valkyrie.middleware;

import com.tailoredshapes.stash.Stash;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.UnderBar.hash;
import static com.tailoredshapes.underbar.UnderBar.list;
import static com.tailoredshapes.underbar.UnderReg.groups;
import static com.tailoredshapes.underbar.UnderReg.pattern;
import static com.tailoredshapes.valkyrie.util.Parsing.reToken;

public interface Cookies {

    /**
     * RFC6265 cookie octet
     */
    Pattern cookieOctet = pattern("[!#$%&'()*+\\-./0-9:<=>?@A-Z\\[\\]\\^_`a-z\\{\\|\\}~]");

    Pattern cookieValue = pattern("\"" + cookieOctet + "*\"|" + cookieOctet + "*");

    Pattern cookie = pattern("\\s*(" + reToken + ")=(" + cookieValue + ")\\s*[;,]?");


    Stash setCookieAttributes = stash(
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
}
