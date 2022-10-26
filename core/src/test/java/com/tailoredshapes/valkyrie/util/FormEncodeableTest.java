package com.tailoredshapes.valkyrie.util;

import com.tailoredshapes.stash.Stash;
import org.junit.Test;

import java.util.Map;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.underbar.ocho.UnderBar.hash;
import static com.tailoredshapes.underbar.ocho.UnderBar.list;
import static com.tailoredshapes.valkyrie.util.FormEncodeable.formEncode;
import static org.junit.Assert.assertEquals;

/**
 * Created by tmarsh on 2/15/17.
 */
public class FormEncodeableTest {
    @Test
    public void shouldEncodeAString() throws Exception {
        String test = "Приве́т नमस्ते שָׁלוֹם";
        assertEquals("%D0%9F%D1%80%D0%B8%D0%B2%D0%B5%CC%81%D1%82+%E0%A4%A8%E0%A4%AE%E0%A4%B8%E0%A5%8D%E0%A4%A4%E0%A5%87+%D7%A9%D6%B8%D7%81%D7%9C%D7%95%D6%B9%D7%9D",
                formEncode(test, "UTF-8"));
    }

    @Test
    public void shouldEncodeAnObject(){
        Object test = new Object(){
            @Override
            public String toString() {
                return "Hello, World";
            }
        };

        assertEquals("Hello%2C+World", formEncode(test, "UTF-8"));
    }

    @Test
    public void shouldEncodeAMap() throws Exception {
        Map<String, Object> foo = hash("foo", "bar", "eggs", list("a", "b", "c"));

        assertEquals("eggs=a&eggs=b&eggs=c&foo=bar", formEncode(foo, "UTF-16"));
    }

    @Test
    public void shouldEncodeAStash() throws Exception {
        Stash foo = stash("foo", "bar", "eggs", list("a", "b", "c"));

        assertEquals("eggs=a&eggs=b&eggs=c&foo=bar", formEncode(foo, "UTF-16"));
    }
}