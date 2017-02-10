package com.tailoredshapes.valkyrie.util;

import org.junit.Test;

import java.util.Date;

import static com.tailoredshapes.valkyrie.util.Time.formatDate;
import static com.tailoredshapes.valkyrie.util.Time.parseDate;
import static org.junit.Assert.*;

/**
 * Created by tmarsh on 2/9/17.
 */
public class TimeTest {
    @Test
    public void canParseAndFormatADate() throws Exception {
        Date expectedDate = new Date();
        String expectedDateString = formatDate(expectedDate);
        assertEquals((expectedDate.getTime() / 1000) * 1000, parseDate(expectedDateString).getTime());
    }
}