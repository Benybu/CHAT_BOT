package com.chatbot.model;

public class Respuesta {
    private int id;
    private String palabrasClave;
    private String respuesta;
    private boolean activo;

    public Respuesta() {}

    public Respuesta(int id, String palabrasClave, String respuesta, boolean activo) {
        this.id = id;
        this.palabrasClave = palabrasClave;
        this.respuesta = respuesta;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPalabrasClave() { return palabrasClave; }
    public void setPalabrasClave(String palabrasClave) { this.palabrasClave = palabrasClave; }

    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
