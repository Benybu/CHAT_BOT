package com.chatbot.web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Endpoint liviano pensado exclusivamente para servicios de keep-alive
 * (cron-job.org, UptimeRobot, etc).
 *
 * No toca la base de datos ni ninguna otra capa: solo responde 200 OK
 * lo más rápido posible, para evitar que el "pinguear" el sitio dependa
 * de que la base de datos esté disponible en ese instante.
 */
@WebServlet("/ping")
public class PingServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("OK");
    }
}
