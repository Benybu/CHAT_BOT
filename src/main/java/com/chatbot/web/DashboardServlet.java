package com.chatbot.web;

import com.chatbot.dao.MensajeDAO;
import com.chatbot.dao.ProductoDAO;
import com.chatbot.dao.RespuestaDAO;
import com.chatbot.model.Mensaje;
import com.chatbot.model.Producto;
import com.chatbot.model.Respuesta;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/DashboardServlet")
public class DashboardServlet extends HttpServlet {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final RespuestaDAO respuestaDAO = new RespuestaDAO();
    private final MensajeDAO mensajeDAO = new MensajeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        boolean logged = session != null && session.getAttribute("usuario") != null;

        request.setAttribute("logueado", logged);
        request.setAttribute("rol", logged ? session.getAttribute("rol") : "LECTOR");
        request.setAttribute("usuarioActual", logged ? session.getAttribute("usuario") : "Invitado");

        List<Producto> productos = productoDAO.listarActivos();
        List<Respuesta> respuestas = respuestaDAO.listarActivos();
        List<Mensaje> mensajes = mensajeDAO.ultimos();

        request.setAttribute("productos", productos);
        request.setAttribute("respuestas", respuestas);
        request.setAttribute("mensajes", mensajes);
        request.setAttribute("totalProductos", productoDAO.contarActivos());
        request.setAttribute("totalRespuestas", respuestaDAO.contarActivas());
        request.setAttribute("totalMensajes", mensajeDAO.contar());

        RequestDispatcher rd = request.getRequestDispatcher("dashboard.jsp");
        rd.forward(request, response);
    }
}
