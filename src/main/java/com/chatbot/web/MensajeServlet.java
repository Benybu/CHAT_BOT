package com.chatbot.web;

import com.chatbot.dao.MensajeDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/mensajes")
public class MensajeServlet extends HttpServlet {

    private final MensajeDAO mensajeDAO = new MensajeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/DashboardServlet");
    }
}
