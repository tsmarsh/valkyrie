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


public interface Time {
    static DateFormat formatter(HTTPDateFormats format){
        SimpleDateFormat dateFormat = new SimpleDateFormat(format.format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    static Date attemptParse(String date, HTTPDateFormats format){
        return rethrow(() -> formatter(format).parse(date));
    }

    static String trimQuotes(String s){
        return s.replaceAll("^'|'$", "");
    }

    static Date parseDate(String httpDate){
        return optionally(takeWhile(
                map(
                        HTTPDateFormats.values(),
                        key -> lazy(() -> attemptParse(trimQuotes(httpDate), key))),
                (lv) -> lv.get() != null), Supplier::get, ()->null);
    }

    static String formatDate(Date date){
        return formatter(HTTPDateFormats.RFC1123).format(date);
    }
}
