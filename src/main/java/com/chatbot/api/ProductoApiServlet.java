package com.chatbot.api;

import com.chatbot.dao.ProductoDAO;
import com.chatbot.model.Producto;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/productos")
public class ProductoApiServlet extends HttpServlet {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        List<Producto> productos = productoDAO.listarActivos();

        String json = gson.toJson(productos);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        resp.getWriter().write(json);
    }
}