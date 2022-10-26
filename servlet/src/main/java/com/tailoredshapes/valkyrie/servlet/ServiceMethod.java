package com.tailoredshapes.valkyrie.servlet;

import com.tailoredshapes.underbar.ocho.function.RegularFunctions;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public interface ServiceMethod extends RegularFunctions.TriConsumer<HttpServlet, HttpServletRequest, HttpServletResponse> {}
