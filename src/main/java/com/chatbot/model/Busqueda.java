package com.chatbot.model;
import java.util.ArrayList;
import java.util.List;

public class Busqueda {

    private String categoria;
    private String marca;
    private String marcaExcluida;
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

    public String getMarcaExcluida() {
        return marcaExcluida;
    }

    /*
    MARCA QUE NO DEBE APARECER EN EL RESULTADO.
    SE USA CUANDO EL CLIENTE PIDE "EN OTRA MARCA",
    PARA DESCARTAR PRODUCTOS DE LA MARCA QUE YA SE
    LE MOSTRO, EN LUGAR DE VOLVER A DEVOLVER EL MISMO
    PRODUCTO.
    */
    public void setMarcaExcluida(String marcaExcluida) {
        this.marcaExcluida = marcaExcluida;
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