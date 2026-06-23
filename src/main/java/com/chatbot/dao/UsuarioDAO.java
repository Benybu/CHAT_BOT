package com.chatbot.dao;

import com.chatbot.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    public Usuario autenticar(String usuario, String password) {
        String sql = "SELECT id, usuario, password, rol, activo FROM usuarios WHERE usuario = ? AND password = ? AND activo = 1";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, usuario);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Usuario(
                            rs.getInt("id"),
                            rs.getString("usuario"),
                            rs.getString("password"),
                            rs.getString("rol"),
                            rs.getBoolean("activo")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error autenticando usuario", e);
        }
        return null;
    }
}
