package com.tailoredshapes.valkyrie.core;

import com.tailoredshapes.stash.Stash;
import com.tailoredshapes.underbar.function.RegularFunctions;

import java.util.function.Function;

public interface AsyncHandler extends RegularFunctions.TriFunction<Stash, Handler, Function<Throwable, Stash>, Stash> {}
