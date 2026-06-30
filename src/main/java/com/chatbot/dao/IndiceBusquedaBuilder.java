package com.chatbot.dao;

import com.chatbot.model.Producto;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

public class IndiceBusquedaBuilder {

    private static final Set<String> STOP_WORDS = Set.of(
            "de","del","la","las","el","los",
            "para","con","sin","por","y",
            "o","un","una","unos","unas"
    );

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

            palabra = palabra.trim();

            if (palabra.length() <= 1) {
                continue;
            }

            if (STOP_WORDS.contains(palabra)) {
                continue;
            }

            indice.add(palabra);

            // Separar letras y números
            String letras = palabra.replaceAll("[0-9]", "");
            String numeros = palabra.replaceAll("[^0-9]", "");

            if (letras.length() > 1) {
                indice.add(letras);
            }

            if (numeros.length() > 0) {
                indice.add(numeros);
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