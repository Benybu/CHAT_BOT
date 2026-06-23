package com.chatbot.web;

import com.chatbot.dao.UsuarioDAO;
import com.chatbot.model.Usuario;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String usuario = request.getParameter("usuario");
        String password = request.getParameter("password");

        Usuario u = usuarioDAO.autenticar(usuario, password);

        if (u != null) {
            HttpSession session = request.getSession(true);
            session.setAttribute("usuario", u.getUsuario());
            session.setAttribute("usuario_obj", u);
            session.setAttribute("rol", u.getRol());
            response.sendRedirect("DashboardServlet");
        } else {
            response.sendRedirect("login.jsp?error=1");
        }
    }
}
