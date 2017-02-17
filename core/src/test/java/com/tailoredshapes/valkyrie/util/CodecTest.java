package com.tailoredshapes.valkyrie.util;

import org.junit.Test;

import static com.tailoredshapes.stash.Stash.stash;
import static com.tailoredshapes.valkyrie.util.Codec.formDecodeStash;
import static com.tailoredshapes.valkyrie.util.Codec.formEncode;
import static org.junit.Assert.*;

/**
 * Created by tmarsh on 2/16/17.
 */
public class CodecTest {
    @Test
    public void shouldDecodeAnEncodedString() throws Exception {
        String formEncoded = formEncode(stash("foo", "bar", "eggs", 1));
        assertEquals(stash("foo", "bar", "eggs", "1"), formDecodeStash(formEncoded, "UTF-8"));
    }
}