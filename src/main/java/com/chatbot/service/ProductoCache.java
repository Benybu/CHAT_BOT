package com.chatbot.service;

import com.chatbot.model.Producto;
import com.chatbot.dao.IndiceBusquedaBuilder;

import java.util.ArrayList;
import java.util.List;

public class ProductoCache {

    private static List<Producto> productos = new ArrayList<>();

    public static void cargar(List<Producto> listaProductos) {

        productos.clear();

        for (Producto p : listaProductos) {

            p.setIndiceBusqueda(
                    IndiceBusquedaBuilder.construir(p)
            );

            productos.add(p);
        }

    }

    public static List<Producto> obtenerProductos() {

        return productos;

    }

    public static void limpiar() {

        productos.clear();

    }

}