package com.tailoredshapes.valkyrie.util;

import java.util.regex.Pattern;

/**
 * Created by tmarsh on 11/1/16.
 */
public class Parsing {
    public static String token = "[!#$%&'*\\-+.0-9A-Z\\^_`a-z\\|~]+";
    public static String quoted = "\"(\"|[^\"])*\"";
    public static String value = token + "|" + quoted;

    public static Pattern reToken = Pattern.compile(token);
    public static Pattern reQuoted = Pattern.compile(quoted);
    public static Pattern reValue = Pattern.compile(value);

    public static Pattern reCharset = Pattern.compile(";(?:.*\\s)?(?i:charset)=(" + value + ")\\s*(?:;|$)");
}