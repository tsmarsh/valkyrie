package com.tailoredshapes.valkyrie.core;

import com.tailoredshapes.stringmap.StringMap;

import java.io.OutputStream;

public interface StreamableResponseBody {
    void writeBodyToStream(String body, StringMap response, OutputStream outputStream);
}
