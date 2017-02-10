package com.tailoredshapes.valkyrie.util;

public enum HTTPDateFormats {
    RFC1123("EEE, dd MMM yyyy HH:mm:ss zzz"),
    RFC1036("EEEE, dd-MMM-yy HH:mm:ss zzz"),
    ASCTIME("EEE MMM d HH:mm:ss yyyy");

    String format;

    HTTPDateFormats(String format){ this.format = format;}
}
