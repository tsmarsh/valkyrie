package com.tailoredshapes.valkyrie.servlet;

import com.tailoredshapes.underbar.function.RegularFunctions.TriConsumer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ServiceMethod extends TriConsumer<HttpServlet, HttpServletRequest, HttpServletResponse>{}
