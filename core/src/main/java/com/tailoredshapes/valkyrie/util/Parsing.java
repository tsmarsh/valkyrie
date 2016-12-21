package com.tailoredshapes.valkyrie.util;

import java.util.regex.Pattern;

import static com.tailoredshapes.underbar.UnderReg.pattern;

/**
 * Created by tmarsh on 11/1/16.
 */
public class Parsing {
    public static String token = "[!#$%&'*\\-+.0-9A-Z\\^_`a-z\\|~]+";
    public static String quoted = "\"(\\\"|[^\"])*\"";
    public static String value = token + "|" + quoted;

    public static Pattern reToken = pattern(token);
    public static Pattern reQuoted = pattern(quoted);
    public static Pattern reValue = pattern(value);

    public static Pattern reCharset = pattern(".*;(?:.*\\s)?(?i:charset)=(" + value + ")\\s*(?:;|$)");

}