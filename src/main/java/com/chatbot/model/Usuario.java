package com.chatbot.model;

public class Usuario {
    private int id;
    private String usuario;
    private String password;
    private String rol;
    private boolean activo;

    public Usuario() {}

    public Usuario(int id, String usuario, String password, String rol, boolean activo) {
        this.id = id;
        this.usuario = usuario;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
