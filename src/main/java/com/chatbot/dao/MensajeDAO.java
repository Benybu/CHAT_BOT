package com.chatbot.dao;

import com.chatbot.model.Mensaje;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MensajeDAO {

    private Mensaje map(ResultSet rs) throws SQLException {
        return new Mensaje(
                rs.getInt("id"),
                rs.getString("mensaje_entrada"),
                rs.getString("respuesta_generada"),
                rs.getString("canal"),
                rs.getTimestamp("fecha_registro").toLocalDateTime()
        );
    }

    public void guardar(String mensajeEntrada, String respuestaGenerada) {

        String sql = """
                INSERT INTO mensajes(
                    mensaje_entrada,
                    respuesta_generada,
                    canal
                ) VALUES (?, ?, ?)
                """;

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, mensajeEntrada);
            ps.setString(2, respuestaGenerada);
            ps.setString(3, "WEB");

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error guardando mensaje", e);
        }
    }

    public List<Mensaje> ultimos() {

        List<Mensaje> lista = new ArrayList<>();

        String sql = """
                SELECT *
                FROM mensajes
                ORDER BY fecha_registro DESC
                LIMIT 20
                """;

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            while (rs.next()) {
                lista.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo mensajes", e);
        }

        return lista;
    }

    public int contar() {

        String sql = "SELECT COUNT(*) FROM mensajes";

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            rs.next();
            return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException("Error contando mensajes", e);
        }
    }
}