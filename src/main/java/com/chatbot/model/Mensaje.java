package com.chatbot.model;

import java.time.LocalDateTime;

public class Mensaje {
    private int id;
    private String mensajeEntrada;
    private String respuestaGenerada;
    private String canal;
    private LocalDateTime fechaRegistro;

    public Mensaje() {}

    public Mensaje(int id, String mensajeEntrada, String respuestaGenerada, String canal, LocalDateTime fechaRegistro) {
        this.id = id;
        this.mensajeEntrada = mensajeEntrada;
        this.respuestaGenerada = respuestaGenerada;
        this.canal = canal;
        this.fechaRegistro = fechaRegistro;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMensajeEntrada() { return mensajeEntrada; }
    public void setMensajeEntrada(String mensajeEntrada) { this.mensajeEntrada = mensajeEntrada; }

    public String getRespuestaGenerada() { return respuestaGenerada; }
    public void setRespuestaGenerada(String respuestaGenerada) { this.respuestaGenerada = respuestaGenerada; }

    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
