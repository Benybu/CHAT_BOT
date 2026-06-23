package com.chatbot.web;

import com.chatbot.service.ChatbotService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import java.io.IOException;

@WebServlet("/chat")
public class ChatbotServlet extends HttpServlet {

    private final ChatbotService chatbotService = new ChatbotService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String mensaje = req.getParameter("mensaje");

        String respuesta = chatbotService.procesarMensaje(mensaje);

        req.setAttribute("respuesta", respuesta);

        req.getRequestDispatcher("/dashboard.jsp")
                .forward(req, resp);
    }
}