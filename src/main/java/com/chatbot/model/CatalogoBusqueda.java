package com.chatbot.model;

import java.util.HashSet;
import java.util.Set;

public class CatalogoBusqueda {

    private Set<String> categorias = new HashSet<>();
    private Set<String> marcas = new HashSet<>();
    private Set<String> modelos = new HashSet<>();
    private Set<String> atributos = new HashSet<>();

    public Set<String> getCategorias() {
        return categorias;
    }

    public Set<String> getMarcas() {
        return marcas;
    }

    public Set<String> getModelos() {
        return modelos;
    }

    public Set<String> getAtributos() {
        return atributos;
    }

    public void agregarCategoria(String categoria) {

        if (categoria != null && !categoria.isBlank()) {
            categorias.add(categoria.toLowerCase());
        }

    }

    public void agregarMarca(String marca) {

        if (marca != null && !marca.isBlank()) {
            marcas.add(marca.toLowerCase());
        }

    }

    public void agregarModelo(String modelo) {

        if (modelo != null && !modelo.isBlank()) {
            modelos.add(modelo.toLowerCase());
        }

    }

    public void agregarAtributo(String atributo) {

        if (atributo != null && !atributo.isBlank()) {
            atributos.add(atributo.toLowerCase());
        }

    }

}