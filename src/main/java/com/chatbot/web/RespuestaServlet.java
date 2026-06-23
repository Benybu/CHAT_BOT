package com.chatbot.web;

import com.chatbot.dao.RespuestaDAO;
import com.chatbot.model.Respuesta;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin/respuestas")
public class RespuestaServlet extends HttpServlet {

    private final RespuestaDAO respuestaDAO = new RespuestaDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accion = request.getParameter("accion");
        String id = request.getParameter("id");
        String palabrasClave = request.getParameter("palabras_clave");
        String respuesta = request.getParameter("respuesta");

        Respuesta r = new Respuesta();
        r.setPalabrasClave(palabrasClave);
        r.setRespuesta(respuesta);
        r.setActivo(true);

        if ("eliminar".equalsIgnoreCase(accion)) {
            respuestaDAO.eliminar(Integer.parseInt(id));
        } else if (id != null && !id.isBlank()) {
            r.setId(Integer.parseInt(id));
            respuestaDAO.actualizar(r);
        } else {
            respuestaDAO.guardar(r);
        }

        response.sendRedirect(request.getContextPath() + "/DashboardServlet");
    }
}
