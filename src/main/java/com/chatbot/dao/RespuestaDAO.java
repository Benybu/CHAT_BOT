package com.chatbot.dao;

import com.chatbot.model.Respuesta;

import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class RespuestaDAO {

    private Respuesta map(ResultSet rs) throws SQLException {
        return new Respuesta(
                rs.getInt("id"),
                rs.getString("palabras_clave"),
                rs.getString("respuesta"),
                rs.getBoolean("activo")
        );
    }

    public List<Respuesta> listarActivos() {
        List<Respuesta> lista = new ArrayList<>();
        String sql = "SELECT id, palabras_clave, respuesta, activo FROM respuestas WHERE activo = 1 ORDER BY id DESC";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error listando respuestas", e);
        }
        return lista;
    }

    public int contarActivas() {
        String sql = "SELECT COUNT(*) FROM respuestas WHERE activo = 1";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error contando respuestas", e);
        }
    }

    public Respuesta buscarPorId(int id) {
        String sql = "SELECT id, palabras_clave, respuesta, activo FROM respuestas WHERE id = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error buscando respuesta", e);
        }
        return null;
    }

    public void guardar(Respuesta r) {
        String sql = "INSERT INTO respuestas(palabras_clave, respuesta, activo) VALUES(?,?,?)";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getPalabrasClave());
            ps.setString(2, r.getRespuesta());
            ps.setBoolean(3, r.isActivo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error guardando respuesta", e);
        }
    }

    public void actualizar(Respuesta r) {
        String sql = "UPDATE respuestas SET palabras_clave=?, respuesta=?, activo=? WHERE id=?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getPalabrasClave());
            ps.setString(2, r.getRespuesta());
            ps.setBoolean(3, r.isActivo());
            ps.setInt(4, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando respuesta", e);
        }
    }

    public void eliminar(int id) {
        String sql = "UPDATE respuestas SET activo = 0 WHERE id = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando respuesta", e);
        }
    }

    public Respuesta buscarPorCoincidencia(String mensaje) {
        String texto = normalizar(mensaje);
        for (Respuesta r : listarActivos()) {
            if (r.getPalabrasClave() == null) continue;
            String[] claves = r.getPalabrasClave().split(",");
            for (String clave : claves) {
                String token = normalizar(clave.trim());
                if (!token.isEmpty() && texto.contains(token)) {
                    return r;
                }
            }
        }
        return null;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String n = Normalizer.normalize(texto, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", "");
        return n.toLowerCase().trim();
    }
}
