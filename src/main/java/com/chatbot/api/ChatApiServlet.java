package com.chatbot.api;

import com.chatbot.service.ChatbotService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/api/chat")
public class ChatApiServlet extends HttpServlet {

    private final ChatbotService chatbotService = new ChatbotService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write("""
        {
            "estado":"ok",
            "mensaje":"API del chatbot funcionando"
        }
        """);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        StringBuilder json = new StringBuilder();

        BufferedReader reader = request.getReader();
        String line;

        while ((line = reader.readLine()) != null) {
            json.append(line);
        }

        String body = json.toString();

        String mensaje = "";

        if (body.contains("mensaje")) {
            mensaje = body
                    .replace("{", "")
                    .replace("}", "")
                    .replace("\"", "")
                    .replace("mensaje:", "")
                    .trim();
        }

        String respuesta = chatbotService.procesarMensaje(mensaje);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write("""
        {
            "respuesta":"%s"
        }
        """.formatted(respuesta));
    }
}