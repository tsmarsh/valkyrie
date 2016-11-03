package com.tailoredshapes.valkyrie.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.tailoredshapes.underbar.Die.rethrow;
import static com.tailoredshapes.underbar.UnderBar.*;


/**
 * Created by tmarsh on 11/1/16.
 */
public class Time {

    public enum HTTPDateFormats {
        RFC1123("EEE, dd MMM yyyy HH:mm:ss zzz"),
        RFC1036("EEEE, dd-MMM-yy HH:mm:ss zzz"),
        ASCTIME("EEE MMM d HH:mm:ss yyyy");

        String format;

        HTTPDateFormats(String format){ this.format = format;}
    }

    public static DateFormat formatter(HTTPDateFormats format){
        SimpleDateFormat dateFormat = new SimpleDateFormat(format.format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    public static Date attemptParse(String date, HTTPDateFormats format){
        return rethrow(() -> formatter(format).parse(date));
    }

    public static String trimQuotes(String s){
        return s.replaceAll("^'|'$", "");
    }

    public static Date parseDate(String httpDate){
        return optionally(takeWhile(
                map(
                        HTTPDateFormats.values(),
                        key -> lazy(() -> attemptParse(trimQuotes(httpDate), key))),
                (lv) -> lv.get() != null), Supplier::get, ()->null);
    }

    public static String formatDate(Date date){
        return formatter(HTTPDateFormats.RFC1123).format(date);
    }
}
