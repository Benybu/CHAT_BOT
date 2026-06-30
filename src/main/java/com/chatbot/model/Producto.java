package com.chatbot.model;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class Producto {

    private int id;
    private String nombre;
    private BigDecimal precio;
    private int stock;
    private String descripcion;
    private boolean activo;
    private String sku;

    // NUEVOS CAMPOS
    private String categoria;
    private String marca;
    private String modelo;
    private String medida;
    private String atributos;
    private String tags;
    private String imagen;
    // Índice utilizado por el buscador inteligente
    private Set<String> indiceBusqueda = new HashSet<>();

    public Producto() {
    }

    public Producto(
            int id,
            String sku,
            String nombre,
            BigDecimal precio,
            int stock,
            String descripcion,
            boolean activo,
            String categoria,
            String marca,
            String tags,
            String imagen
    ) {
        this.id = id;
        this.sku = sku;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.descripcion = descripcion;
        this.activo = activo;
        this.categoria = categoria;
        this.marca = marca;
        this.tags = tags;
        this.imagen = imagen;
    }

    public Producto(
            String sku,
            String nombre,
            BigDecimal precio,
            int stock,
            String descripcion,
            boolean activo,
            String categoria,
            String marca,
            String tags,
            String imagen
    ) {
        this.sku = sku;
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock;
        this.descripcion = descripcion;
        this.activo = activo;
        this.categoria = categoria;
        this.marca = marca;
        this.tags = tags;
        this.imagen = imagen;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

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

    public String getAtributos() {
        return atributos;
    }

    public void setAtributos(String atributos) {
        this.atributos = atributos;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getImagen() {
    return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Set<String> getIndiceBusqueda() {
        return indiceBusqueda;
    }

    public void setIndiceBusqueda(Set<String> indiceBusqueda) {
        this.indiceBusqueda = indiceBusqueda;
    }
}