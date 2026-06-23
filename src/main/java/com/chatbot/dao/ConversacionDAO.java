package com.chatbot.dao;

import com.chatbot.model.Conversacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConversacionDAO {

    public void guardarContexto(
            String usuario,
            int productoId,
            String intencion,
            String categoria,
            String preferencia
    ) {

        String sql = """
                INSERT INTO conversaciones(
                    usuario,
                    ultimo_producto_id,
                    ultima_intencion,
                    ultima_categoria,
                    preferencia_precio
                )
                VALUES(?,?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    ultimo_producto_id = VALUES(ultimo_producto_id),
                    ultima_intencion = VALUES(ultima_intencion),
                    ultima_categoria = VALUES(ultima_categoria),
                    preferencia_precio = VALUES(preferencia_precio)
                """;

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, usuario);
            ps.setInt(2, productoId);
            ps.setString(3, intencion);
            ps.setString(4, categoria);
            ps.setString(5, preferencia);

            ps.executeUpdate();

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Error guardando conversación",
                    e
            );
        }
    }

    public Conversacion obtener(String usuario) {

        String sql = """
                SELECT *
                FROM conversaciones
                WHERE usuario = ?
                """;

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, usuario);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    Conversacion c = new Conversacion();

                    c.setUsuarioId(
                            rs.getString("usuario")
                    );

                    c.setUltimoProductoId(
                            rs.getInt("ultimo_producto_id")
                    );

                    c.setUltimaIntencion(
                            rs.getString("ultima_intencion")
                    );

                    c.setUltimaCategoria(
                            rs.getString("ultima_categoria")
                    );

                    c.setPreferenciaPrecio(
                            rs.getString("preferencia_precio")
                    );

                    return c;
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Error obteniendo conversación",
                    e
            );
        }

        return null;
    }
}