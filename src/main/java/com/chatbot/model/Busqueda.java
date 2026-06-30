package com.chatbot.model;
import java.util.ArrayList;
import java.util.List;

public class Busqueda {

    private String categoria;
    private String marca;
    private String modelo;
    private String medida;
    private List<String> atributos = new ArrayList<>();

    private boolean barato;
    private boolean premium;

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getMedida() {
        return medida;
    }

    public void setMedida(String medida) {
        this.medida = medida;
    }

    public boolean isBarato() {
        return barato;
    }

    public void setBarato(boolean barato) {
        this.barato = barato;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }
    public List<String> getAtributos() {
        return atributos;
    }

    public void setAtributos(List<String> atributos) {
        this.atributos = atributos;
    }

    public void agregarAtributo(String atributo) {

        if (!atributos.contains(atributo)) {
            atributos.add(atributo);
        }

    }
}