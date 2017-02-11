package com.tailoredshapes.valkyrie.servlet;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.underbar.function.RegularFunctions.TriConsumer;

import javax.servlet.http.HttpServletResponse;
import java.util.function.Consumer;
import java.util.function.Function;

public interface AsyncHandler extends TriConsumer<Stash, Function<Stash, HttpServletResponse>, Consumer<Throwable>> {}
