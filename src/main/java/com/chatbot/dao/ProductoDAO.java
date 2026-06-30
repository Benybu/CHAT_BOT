package com.chatbot.dao;

import com.chatbot.model.Producto;
import com.chatbot.model.Busqueda;

import java.math.BigDecimal;
import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;


public class ProductoDAO {

    private Producto map(ResultSet rs) throws SQLException {

        return new Producto(
                rs.getInt("id"),
                rs.getString("sku"),
                rs.getString("nombre"),
                rs.getBigDecimal("precio"),
                rs.getInt("stock"),
                rs.getString("descripcion"),
                rs.getBoolean("estado"),
                rs.getString("categoria"),
                rs.getString("marca"),
                rs.getString("tags"),
                rs.getString("imagen")
        );
    }

    public Producto buscarCoincidenciaConPresupuesto(
            String mensaje,
            double presupuesto
    ) {

        Producto mejor = null;

        int mejorScore = -1;

        String texto =
                normalizar(mensaje);

        for (Producto p : listarActivos()) {

            if (
                    p.getPrecio().doubleValue()
                    > presupuesto
            ) {
                continue;
            }

            int score = 0;

            String contenido =
                    (
                            p.getNombre() + " " +
                            p.getCategoria() + " " +
                            p.getMarca() + " " +
                            p.getTags()
                    ).toLowerCase();

            for (String palabra : texto.split("\\s+")) {

                if (
                        contenido.contains(palabra)
                ) {
                    score += 10;
                }
            }

            if (score > mejorScore) {

                mejorScore = score;
                mejor = p;
            }
        }

        return mejor;
    }

    public List<Producto> listarActivos() {

        List<Producto> lista = new ArrayList<>();

        String sql = """
                SELECT
                id,
                sku,
                nombre,
                precio,
                stock,
                descripcion,
                estado,
                categoria,
                marca,
                tags,
                imagen
                FROM productos
                WHERE estado = 1
                ORDER BY id DESC
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
            throw new RuntimeException("Error listando productos", e);
        }

        return lista;
    }

    public int contarActivos() {

        String sql = "SELECT COUNT(*) FROM productos WHERE estado = 1";

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            rs.next();

            return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException("Error contando productos", e);
        }
    }

    public Producto buscarPorId(int id) {

        String sql = """
                SELECT
                id,
                sku,
                nombre,
                precio,
                stock,
                descripcion,
                estado,
                categoria,
                marca,
                tags,
                imagen
                FROM productos
                WHERE id = ?
                """;

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return map(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error buscando producto", e);
        }

        return null;
    }

    public void guardar(Producto p) {

        String sql = """
                INSERT INTO productos(
                sku,
                nombre,
                precio,
                stock,
                descripcion,
                estado,
                categoria,
                marca,
                tags,
                imagen
                )
                VALUES(?,?,?,?,?,?,?,?,?,?)
                """;

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, p.getSku());

            ps.setString(2, p.getNombre());

            ps.setBigDecimal(
                    3,
                    p.getPrecio() == null
                            ? BigDecimal.ZERO
                            : p.getPrecio()
            );

            ps.setInt(4, p.getStock());

            ps.setString(5, p.getDescripcion());

            ps.setBoolean(6, p.isActivo());

            ps.setString(7, p.getCategoria());

            ps.setString(8, p.getMarca());

            ps.setString(9, p.getTags());

            ps.setString(10, p.getImagen());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error guardando producto", e);
        }
    }

    public void actualizar(Producto p) {

        String sql = """
                UPDATE productos SET
                sku=?,
                nombre=?,
                precio=?,
                stock=?,
                descripcion=?,
                estado=?,
                categoria=?,
                marca=?,
                tags=?,
                imagen=?
                WHERE id=?
                """;

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setString(1, p.getSku());

            ps.setString(2, p.getNombre());

            ps.setBigDecimal(
                    3,
                    p.getPrecio() == null
                            ? BigDecimal.ZERO
                            : p.getPrecio()
            );

            ps.setInt(4, p.getStock());

            ps.setString(5, p.getDescripcion());

            ps.setBoolean(6, p.isActivo());

            ps.setString(7, p.getCategoria());

            ps.setString(8, p.getMarca());

            ps.setString(9, p.getTags());

            ps.setString(10, p.getImagen());

            ps.setInt(11, p.getId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error actualizando producto", e);
        }
    }

    public void guardarOActualizar(Producto p) {
        Producto existente = buscarPorSku(p.getSku());

        if (existente != null) {
            p.setId(existente.getId());
            actualizar(p);
        } else {
            guardar(p);
        }
    }

    public void eliminar(int id) {

        String sql = "UPDATE productos SET estado = 0 WHERE id = ?";

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error eliminando producto", e);
        }
    }

    public Producto buscarCoincidencia(String mensaje) {

        String texto = normalizar(mensaje);

        if (contieneCategoriaInexistente(texto)) {
            return null;
        }

        Busqueda busqueda = analizarMensaje(mensaje);

        String[] palabrasUsuario = texto.split("\\s+");

        String[] ignorar = {
            "tienen","tienes","tenga","hay",
            "quiero","busco","necesito",
            "un","una","unos","unas",
            "de","del","para","con",
            "por","favor","el","la",
            "los","las","me","que","en"
        };

        List<Producto> productos = listarActivos();

        productos = filtrarCategoria(productos, busqueda);
        productos = filtrarMarca(productos, busqueda);
        productos = filtrarMedida(productos, busqueda);

        return elegirMejorProducto(
                productos,
                palabrasUsuario,
                ignorar,
                busqueda);

    }

    private String normalizar(String texto) {

        if (texto == null) {
            return "";
        }

        String n = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        );

        n = n.replaceAll("\\p{M}", "");

        return n.toLowerCase().trim();
    }

    private Busqueda analizarMensaje(String mensaje) {

        Busqueda busqueda = new Busqueda();

        String texto = normalizar(mensaje);

        // ----------------------------
        // Detectar marca
        // ----------------------------
        for (Producto p : listarActivos()) {

            if (p.getMarca() == null)
                continue;

            String marca = normalizar(p.getMarca());

            if (!marca.isBlank() && texto.contains(marca)) {
                busqueda.setMarca(marca);
                break;
            }
        }

        // ----------------------------
        // Detectar categoría
        // ----------------------------
        String[] categorias = {
                "monitor",
                "mouse",
                "teclado",
                "laptop",
                "audifono",
                "microfono",
                "parlante",
                "silla",
                "fuente",
                "placa",
                "procesador",
                "memoria",
                "ssd",
                "case",
                "cooler"
        };

        for (String categoria : categorias) {

            if (texto.contains(categoria)) {
                busqueda.setCategoria(categoria);
                break;
            }

        }

        // ----------------------------
        // Detectar medida
        // ----------------------------
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile("(\\d{2})\\s*(pulgada|pulgadas|')")
                        .matcher(texto);

        if (matcher.find()) {
            busqueda.setMedida(matcher.group(1));
        }

        // ----------------------------
        // Detectar barato
        // ----------------------------
        busqueda.setBarato(
                texto.contains("barato") ||
                texto.contains("economico") ||
                texto.contains("económico")
        );

        // ----------------------------
        // Detectar premium
        // ----------------------------
        busqueda.setPremium(
                texto.contains("premium") ||
                texto.contains("pro") ||
                texto.contains("alta gama")
        );

        return busqueda;

    }

    private List<Producto> filtrarCategoria(
        List<Producto> productos,
        Busqueda busqueda) {

    if (busqueda.getCategoria() == null) {
        return productos;
    }

    List<Producto> resultado = new ArrayList<>();

        for (Producto p : productos) {

            String categoria = normalizar(p.getCategoria());

            if (categoria == null)
                continue;

            switch (busqueda.getCategoria()) {

                case "monitor":

                    if (categoria.contains("monitor"))
                        resultado.add(p);

                    break;

                case "mouse":

                    if (categoria.contains("mouse"))
                        resultado.add(p);

                    break;

                case "teclado":

                    if (categoria.contains("teclado"))
                        resultado.add(p);

                    break;

                case "laptop":

                    if (categoria.contains("laptop"))
                        resultado.add(p);

                    break;

                default:

                    if (categoria.contains(busqueda.getCategoria()))
                        resultado.add(p);

            }

        }

        return resultado;

    }

    private List<Producto> filtrarMarca(
            List<Producto> productos,
            Busqueda busqueda) {

        if (busqueda.getMarca() == null) {
            return productos;
        }

        List<Producto> resultado = new ArrayList<>();

        for (Producto p : productos) {

            String marca = normalizar(p.getMarca());

            if (marca == null)
                continue;

            if (marca.contains(busqueda.getMarca())) {
                resultado.add(p);
            }

        }

        return resultado;

    }

    private List<Producto> filtrarMedida(
            List<Producto> productos,
            Busqueda busqueda) {

        if (busqueda.getMedida() == null) {
            return productos;
        }

        List<Producto> resultado = new ArrayList<>();

        for (Producto p : productos) {

            String nombre = normalizar(p.getNombre());
            String categoria = normalizar(p.getCategoria());
            String descripcion = normalizar(p.getDescripcion());
            String tags = normalizar(p.getTags());

            String todo =
                    nombre + " " +
                    categoria + " " +
                    descripcion + " " +
                    tags;

            if (todo.contains(busqueda.getMedida())) {
                resultado.add(p);
            }

        }

        return resultado;

    }

    private int calcularScore(
            Producto p,
            String[] palabrasUsuario,
            String[] ignorar,
            Busqueda busqueda) {

        String nombre = normalizar(p.getNombre());
        String categoria = normalizar(p.getCategoria());
        String marca = normalizar(p.getMarca());
        String tags = normalizar(p.getTags());
        String descripcion = normalizar(p.getDescripcion());

        int score = 0;
        int coincidencias = 0;

        for (String palabra : palabrasUsuario) {

            palabra = palabra.trim();

            boolean ignorarPalabra = false;

            for (String x : ignorar) {

                if (palabra.equals(x)) {
                    ignorarPalabra = true;
                    break;
                }

            }

            if (ignorarPalabra)
                continue;

            if (palabra.length() <= 2)
                continue;

            if (palabra.matches("\\d+"))
                continue;

            boolean encontro = false;

            if (nombre.contains(palabra)) {

                score += 80;
                encontro = true;

            }

            if (categoria.contains(palabra)) {

                score += 100;
                encontro = true;

            }

            if (marca.contains(palabra)) {

                score += 70;
                encontro = true;

            }

            if (tags.contains(palabra)) {

                score += 40;
                encontro = true;

            }

            if (descripcion.contains(palabra)) {

                score += 20;
                encontro = true;

            }

            if (encontro)
                coincidencias++;

        }

        score += coincidencias * 40;

        if (busqueda.isBarato() &&
                p.getPrecio().doubleValue() <= 100) {

            score += 50;

        }

        if (busqueda.isPremium() &&
                p.getPrecio().doubleValue() >= 500) {

            score += 40;

        }

        score += p.getStock() / 10;

        return score;

    }

    private Producto elegirMejorProducto(
            List<Producto> productos,
            String[] palabrasUsuario,
            String[] ignorar,
            Busqueda busqueda) {

        Producto mejorProducto = null;
        int mejorScore = -1;

        for (Producto p : productos) {

            int score = calcularScore(
                    p,
                    palabrasUsuario,
                    ignorar,
                    busqueda);

            if (score > mejorScore) {

                mejorScore = score;
                mejorProducto = p;

            }

        }

        System.out.println("Mejor score = " + mejorScore);

        if (mejorProducto != null) {

            System.out.println("Producto: " + mejorProducto.getNombre());

        } else {

            System.out.println("NO ENCONTRO PRODUCTO");

        }

        if (mejorScore < 20) {
            return null;
        }

        return mejorProducto;

    }

    private boolean contieneCategoriaInexistente(String texto) {

        String[] categoriasNoExistentes = {
                "play",
                "playstation",
                "ps5",
                "ps4",
                "xbox",
                "nintendo",
                "switch",
                "laptop",
                "iphone",
                "tablet"
        };

        for (String palabra : categoriasNoExistentes) {

            if (texto.contains(palabra)) {
                return true;
            }
        }

        return false;
    }

    private int distanciaLevenshtein(String a, String b) {

        int[][] dp =
                new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {

            for (int j = 1; j <= b.length(); j++) {

                int costo =
                        (a.charAt(i - 1) == b.charAt(j - 1))
                                ? 0
                                : 1;

                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1
                        ),
                        dp[i - 1][j - 1] + costo
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    public List<Producto> buscarPorPresupuesto(
            String categoria,
            double maximo
    ) {

        List<Producto> resultado =
                new ArrayList<>();

        for (Producto p : listarActivos()) {

            if (
                    p.getCategoria() != null &&
                    p.getCategoria().equalsIgnoreCase(categoria) &&
                    p.getPrecio().doubleValue() <= maximo
            ) {

                resultado.add(p);
            }
        }

        return resultado;
    }

    public void eliminarTodos() {

        String sql = "TRUNCATE TABLE productos";

        try (
                Connection con = Conexion.getConexion();
                PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Producto buscarAlternativa(
        String categoria,
        String preferencia,
        int productoActualId
    ) {

        List<Producto> productos =
                listarActivos();

        Producto mejor = null;

        for (Producto p : productos) {

            /*
            EVITA REPETIR EL MISMO PRODUCTO
            */
            if (p.getId() == productoActualId) {
                continue;
            }

            if (
                    p.getCategoria() != null &&
                    p.getCategoria().equalsIgnoreCase(categoria)
            ) {

                /*
                PRODUCTOS BARATOS
                */
                if (
                        preferencia.equals("BARATO") &&
                        p.getPrecio().doubleValue() <= 300
                ) {

                    return p;
                }

                /*
                PRODUCTOS PREMIUM
                */
                if (
                        preferencia.equals("PREMIUM") &&
                        p.getPrecio().doubleValue() >= 500
                ) {

                    return p;
                }

                /*
                PRODUCTO NORMAL
                */
                if (mejor == null) {
                    mejor = p;
                }
            }
        }

        return mejor;
    }
    public Producto buscarPorSku(
            String sku
    ) {

        String sql = """
            SELECT
            id,
            sku,
            nombre,
            precio,
            stock,
            descripcion,
            estado,
            categoria,
            marca,
            tags,
            imagen
            FROM productos
            WHERE sku = ?
            """;

        try (
                Connection con =
                        Conexion.getConexion();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    sku
            );

            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                if (rs.next()) {

                    return map(rs);
                }
            }

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Error buscando SKU",
                    e
            );
        }

        return null;
    }
}