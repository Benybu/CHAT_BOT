package com.chatbot.dao;

import com.chatbot.model.Producto;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

public class IndiceBusquedaBuilder {

    public static Set<String> construir(Producto producto) {

        Set<String> indice = new HashSet<>();

        agregar(indice, producto.getNombre());
        agregar(indice, producto.getCategoria());
        agregar(indice, producto.getMarca());
        agregar(indice, producto.getModelo());
        agregar(indice, producto.getMedida());
        agregar(indice, producto.getAtributos());
        agregar(indice, producto.getTags());
        agregar(indice, producto.getDescripcion());

        return indice;
    }

    private static void agregar(Set<String> indice, String texto) {

        if (texto == null || texto.isBlank()) {
            return;
        }

        texto = normalizar(texto);

        String[] palabras = texto.split("[^a-z0-9]+");

        for (String palabra : palabras) {

            if (!palabra.isBlank()) {
                indice.add(palabra);
            }

        }

    }

    private static String normalizar(String texto) {

        texto = texto.toLowerCase();

        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);

        texto = texto.replaceAll("\\p{M}", "");

        return texto;
    }

}