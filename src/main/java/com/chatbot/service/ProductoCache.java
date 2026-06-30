package com.chatbot.service;

import com.chatbot.dao.ProductoDAO;
import com.chatbot.model.Producto;

import java.util.ArrayList;
import java.util.List;

public class ProductoCache {

    private static final ProductoDAO productoDAO = new ProductoDAO();

    private static List<Producto> productos = new ArrayList<>();

    public static void cargar() {

        productos = productoDAO.listarActivos();

    }

    public static List<Producto> obtenerProductos() {

        return productos;

    }

}