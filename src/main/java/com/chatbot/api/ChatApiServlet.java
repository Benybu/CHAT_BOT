package com.chatbot.api;

import com.chatbot.service.ChatbotService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import com.chatbot.model.RespuestaChat;

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

        RespuestaChat chat =
            chatbotService.procesarMensajeApi(mensaje);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        /*
        CORS: PERMITE QUE EL USERSCRIPT QUE CORRE DENTRO DE
        facebook.com/messenger.com PUEDA LLAMAR A ESTA API
        DESDE EL NAVEGADOR SI EN ALGUN MOMENTO SE HACE CON
        fetch() EN VEZ DE GM_xmlhttpRequest.
        */
        response.setHeader("Access-Control-Allow-Origin", "*");

        response.getWriter().write("""
            {
                "respuesta":"%s", 
                "nombre":"%s", 
                "imagen":"%s", 
                "precio":"%s"
            }
            """.formatted( 
                escapeJson(chat.getRespuesta()), 
                escapeJson(chat.getNombreProducto()), 
                escapeJson(chat.getImagen()), 
                escapeJson(chat.getPrecio()) ));

    }

    /*
    ESCAPA COMILLAS, BARRAS INVERTIDAS Y SALTOS DE LINEA PARA
    QUE EL JSON DE SALIDA SIGA SIENDO VALIDO. SIN ESTO, CUALQUIER
    RESPUESTA DEL BOT QUE TENGA UN SALTO DE LINEA (LA MAYORIA LAS
    TIENE, ej. LA FICHA DE UBICACION) ROMPIA EL JSON Y CUALQUIER
    CLIENTE QUE LO PARSEE (COMO EL USERSCRIPT) FALLABA.
    */
    private static String escapeJson(String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}