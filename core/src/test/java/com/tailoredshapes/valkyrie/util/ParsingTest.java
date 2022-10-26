package com.tailoredshapes.valkyrie.util;

import org.junit.Test;

import java.util.regex.Matcher;

import static com.tailoredshapes.underbar.ocho.UnderBar.first;
import static com.tailoredshapes.underbar.ocho.UnderReg.groups;
import static com.tailoredshapes.underbar.ocho.UnderReg.matcher;
import static com.tailoredshapes.valkyrie.util.Parsing.*;
import static org.junit.Assert.*;

/**
 * Created by tmarsh on 11/7/16.
 */
public class ParsingTest {
    @Test
    public void canParseAToken() throws Exception {
        assertTrue(matcher(reToken, "monkey").matches());
        assertTrue(matcher(reToken, "pa$$W0rd!").matches());
        assertFalse(matcher(reToken, "p@ssW0rd!").matches());
    }

    @Test
    public void canParseAQuote() throws Exception {
        assertTrue(matcher(reQuoted, "\"foo\"").matches());
        assertFalse(matcher(reQuoted, "foo").matches());
    }

    @Test
    public void canParseAValue() throws Exception {
        assertTrue(matcher(reValue, "pa$$W0rd!").matches());
        assertTrue(matcher(reValue, "\"foo\"").matches());
    }

    @Test
    public void canParseOutACharset() throws Exception {
        Matcher matcher = matcher(reCharset, "text/plain; charset=UTF-16");
        assertTrue(matcher.matches());
        assertEquals("UTF-16", first(groups(matcher)));
    }
}