package com.tailoredshapes.valkyrie.util;

import java.util.regex.Pattern;

import static com.tailoredshapes.underbar.UnderReg.pattern;

public interface Parsing {
    String token = "[!#$%&'*\\-+.0-9A-Z\\^_`a-z\\|~]+";
    String quoted = "\"(\\\"|[^\"])*\"";
    String value = token + "|" + quoted;

    Pattern reToken = pattern(token);
    Pattern reQuoted = pattern(quoted);
    Pattern reValue = pattern(value);

    Pattern reCharset = pattern(".*;(?:.*\\s)?(?i:charset)=(" + value + ")\\s*(?:;|$)");
}