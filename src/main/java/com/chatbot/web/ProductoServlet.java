package com.chatbot.web;

import com.chatbot.dao.ProductoDAO;
import com.chatbot.model.Producto;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/admin/productos")
public class ProductoServlet extends HttpServlet {

    private final ProductoDAO productoDAO =
            new ProductoDAO();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String accion =
                request.getParameter("accion");

        String id =
                request.getParameter("id");

        /*
        DATOS PRODUCTO
        */
        String nombre =
                request.getParameter("nombre");

        String precio =
                request.getParameter("precio");

        String stock =
                request.getParameter("stock");

        String descripcion =
                request.getParameter("descripcion");

        String categoria =
                request.getParameter("categoria");

        String marca =
                request.getParameter("marca");

        String tags =
                request.getParameter("tags");

        String imagen =
                request.getParameter("imagen");

        /*
        CREAR PRODUCTO
        */
        Producto p = new Producto();

        p.setNombre(nombre);

        p.setPrecio(
                precio == null || precio.isBlank()
                        ? BigDecimal.ZERO
                        : new BigDecimal(precio)
        );

        p.setStock(
                stock == null || stock.isBlank()
                        ? 0
                        : Integer.parseInt(stock)
        );

        p.setDescripcion(descripcion);

        p.setCategoria(categoria);

        p.setMarca(marca);

        p.setTags(tags);

        p.setImagen(imagen);

        p.setActivo(true);

        /*
        ELIMINAR
        */
        if ("eliminar".equalsIgnoreCase(accion)) {

            productoDAO.eliminar(
                    Integer.parseInt(id)
            );
        }

        /*
        ACTUALIZAR
        */
        else if (
                id != null &&
                !id.isBlank()
        ) {

            p.setId(Integer.parseInt(id));

            productoDAO.actualizar(p);
        }

        /*
        GUARDAR
        */
        else {

            productoDAO.guardar(p);
        }

        response.sendRedirect(
                request.getContextPath()
                        + "/DashboardServlet"
        );
    }
}