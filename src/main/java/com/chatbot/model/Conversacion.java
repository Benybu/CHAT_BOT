package com.chatbot.model;

public class Conversacion {
    
    private String usuarioId;
    private int ultimoProductoId;
    private String ultimaIntencion;
    private String ultimaCategoria;
    private String preferenciaPrecio;

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getUltimoProductoId() {
        return ultimoProductoId;
    }

    public void setUltimoProductoId(int ultimoProductoId) {
        this.ultimoProductoId = ultimoProductoId;
    }

    public String getUltimaIntencion() {
        return ultimaIntencion;
    }

    public void setUltimaIntencion(String ultimaIntencion) {
        this.ultimaIntencion = ultimaIntencion;
    }

    public String getUltimaCategoria() {
        return ultimaCategoria;
    }

    public void setUltimaCategoria(String ultimaCategoria) {
        this.ultimaCategoria = ultimaCategoria;
    }

    public String getPreferenciaPrecio() {
        return preferenciaPrecio;
    }

    public void setPreferenciaPrecio(String preferenciaPrecio) {
        this.preferenciaPrecio = preferenciaPrecio;
    }
}