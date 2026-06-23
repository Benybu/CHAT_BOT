package com.chatbot;

import java.sql.Connection;
import java.sql.SQLException;

import com.chatbot.dao.Conexion;

public class TestConexion {
    public static void main(String[] args) {
        try (Connection cn = Conexion.getConexion()) {
            System.out.println("Conectado correctamente a Aiven.");
        } catch (SQLException e) {
            System.out.println("Error de conexión.");
            e.printStackTrace();
        }
    }
}