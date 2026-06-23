package com.chatbot.web;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        String uri = req.getRequestURI();
        String rol = session == null ? null : (String) session.getAttribute("rol");
        boolean logged = session != null && session.getAttribute("usuario") != null;

        boolean publicRoutes =
                uri.endsWith("login.jsp") ||
                uri.endsWith("LoginServlet") ||
                uri.endsWith("DashboardServlet") ||
                uri.endsWith("ChatbotServlet") ||
                uri.endsWith("index.jsp") ||
                uri.contains("/css/") ||
                uri.contains("/js/") ||
                uri.contains("/img/");

        if (publicRoutes) {
            chain.doFilter(request, response);
            return;
        }

        boolean adminRoute = uri.contains("/admin/");

        if (adminRoute && (!logged || rol == null || !rol.equalsIgnoreCase("ADMIN"))) {
            res.sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }

        chain.doFilter(request, response);
    }
}
