package com.chatbot.dao;

import com.chatbot.model.Producto;
import com.chatbot.model.Busqueda;
import com.chatbot.model.CatalogoBusqueda;
import com.chatbot.service.ProductoCache;

import java.math.BigDecimal;
import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;


public class ProductoDAO {
    private CatalogoBusqueda catalogo = new CatalogoBusqueda();

    private Producto map(ResultSet rs) throws SQLException {

        Producto p = new Producto();

        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setNombre(rs.getString("nombre"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setStock(rs.getInt("stock"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setActivo(rs.getBoolean("estado"));

        p.setCategoria(rs.getString("categoria"));
        p.setMarca(rs.getString("marca"));

        p.setModelo(rs.getString("modelo"));
        p.setMedida(rs.getString("medida"));
        p.setAtributos(rs.getString("atributos"));

        p.setTags(rs.getString("tags"));
        p.setImagen(rs.getString("imagen"));

        return p;
    }

    public Producto buscarCoincidenciaConPresupuesto(
            String mensaje,
            double presupuesto
    ) {

        Producto mejor = null;

        int mejorScore = -1;

        String texto =
                normalizar(mensaje);

        for (Producto p : ProductoCache.obtenerProductos()) {

            if (
                    p.getPrecio().doubleValue()
                    > presupuesto
            ) {
                continue;
            }

            int score = 0;

            /*
            SE LIMPIA PUNTUACION (ej. "pulgadas??" -> "pulgadas")
            PARA QUE LA COMPARACION CONTRA EL INDICE SEA CORRECTA
            */
            for (String palabra : texto.split("[^a-z0-9]+")) {

                palabra = palabra.trim();

                if (palabra.length() <= 2) {
                    continue;
                }

                if (p.getIndiceBusqueda().contains(palabra)) {
                    score += 10;
                }

            }

            if (score > mejorScore) {

                mejorScore = score;
                mejor = p;
            }
        }

        /*
        SI NINGUN PRODUCTO BARATO TIENE RELACION REAL
        CON LO QUE PIDIO EL CLIENTE, NO DEVOLVER NADA
        EN VEZ DE "EL PRIMERO BARATO QUE ENCONTRÓ".
        */
        if (mejorScore <= 0) {
            return null;
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
                modelo,
                medida,
                atributos,
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

    private void construirCatalogo() {

        catalogo = new CatalogoBusqueda();

        for (Producto p : listarActivos()) {

            catalogo.agregarCategoria(p.getCategoria());

            catalogo.agregarMarca(p.getMarca());

            catalogo.agregarModelo(p.getModelo());

            if (p.getAtributos() != null) {

                String[] lista = p.getAtributos().split(",");

                for (String atributo : lista) {

                    catalogo.agregarAtributo(atributo.trim());

                }

            }

        }

    }

    /*
    COMPARA UNA CATEGORIA DEL CATALOGO (ej: "monitores",
    "procesadores", "teclados de pc") CONTRA EL TEXTO DEL
    CLIENTE, TOLERANDO SINGULAR/PLURAL (ej: "monitor" debe
    encontrar "monitores").
    */
    private boolean coincideConCategoria(String texto, String categoria) {

        String[] stopwords = {"de", "del", "la", "el", "los", "las", "para", "pc", "y"};

        boolean tieneAlMenosUnaPalabraSignificativa = false;

        for (String palabra : categoria.split("\\s+")) {

            if (palabra.length() <= 2) {
                continue;
            }

            boolean esStopword = false;

            for (String sw : stopwords) {
                if (palabra.equals(sw)) {
                    esStopword = true;
                    break;
                }
            }

            if (esStopword) {
                continue;
            }

            tieneAlMenosUnaPalabraSignificativa = true;

            boolean encontrada =
                    contienePalabra(texto, palabra);

            if (!encontrada) {

                String singular = quitarPlural(palabra);

                if (!singular.equals(palabra)) {
                    encontrada = contienePalabra(texto, singular);
                }
            }

            /*
            ANTES: BASTABA CON QUE UNA SOLA PALABRA DE UNA
            CATEGORIA COMPUESTA (ej. "silla gamer") APARECIERA
            EN EL TEXTO PARA QUE TODA LA CATEGORIA SE DIERA
            POR VALIDA. ESO HACIA QUE, POR EJEMPLO, LA PALABRA
            "gamer" (presente en casi cualquier mensaje sobre
            monitores gamer) HICIERA MATCH CON LA CATEGORIA
            "silla gamer" AUNQUE EL CLIENTE NUNCA MENCIONO "silla".

            AHORA: TODAS LAS PALABRAS SIGNIFICATIVAS DE LA
            CATEGORIA DEBEN ESTAR PRESENTES (AND EN VEZ DE OR).
            */
            if (!encontrada) {
                return false;
            }

        }

        return tieneAlMenosUnaPalabraSignificativa;

    }

    /*
    QUITA UN SUFIJO PLURAL SIMPLE EN ESPAÑOL
    (monitores -> monitor, teclados -> teclado, discos -> disco)
    */
    private String quitarPlural(String palabra) {

        if (palabra.endsWith("es") && palabra.length() >= 6) {
            return palabra.substring(0, palabra.length() - 2);
        }

        if (palabra.endsWith("s") && palabra.length() >= 5) {
            return palabra.substring(0, palabra.length() - 1);
        }

        return palabra;

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
                modelo,
                medida,
                atributos,
                tags,
                imagen
                )
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
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

            ps.setString(9, p.getModelo());

            ps.setString(10, p.getMedida());

            ps.setString(11, p.getAtributos());

            ps.setString(12, p.getTags());

            ps.setString(13, p.getImagen());

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
                modelo=?,
                medida=?,
                atributos=?,
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

            ps.setString(9, p.getModelo());

            ps.setString(10, p.getMedida());

            ps.setString(11, p.getAtributos());

            ps.setString(12, p.getTags());

            ps.setString(13, p.getImagen());

            ps.setInt(14, p.getId());

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

        System.out.println("=== DIAGNOSTICO BUSQUEDA ===");
        System.out.println("Mensaje: " + mensaje);
        System.out.println("Categoria detectada: " + busqueda.getCategoria());
        System.out.println("Marca detectada: " + busqueda.getMarca());
        System.out.println("Modelo detectado: " + busqueda.getModelo());
        System.out.println("Medida detectada: " + busqueda.getMedida());
        System.out.println("Atributos detectados: " + busqueda.getAtributos());
        System.out.println("============================");

        String[] palabrasUsuario = texto.split("\\s+");

        String[] ignorar = {
            "tienen","tienes","tenga","hay",
            "quiero","busco","necesito",
            "un","una","unos","unas",
            "de","del","para","con",
            "por","favor","el","la",
            "los","las","me","que","en"
        };

        List<Producto> productos = new ArrayList<>(
            ProductoCache.obtenerProductos()
        );
        System.out.println("Total productos en cache: " + productos.size());

        productos = filtrarCategoria(productos, busqueda);
        System.out.println("Despues de categoria: " + productos.size());

        productos = filtrarMarca(productos, busqueda);
        System.out.println("Despues de marca: " + productos.size());

        productos = filtrarModelo(productos, busqueda);
        System.out.println("Despues de modelo: " + productos.size());

        productos = filtrarMedida(productos, busqueda);
        System.out.println("Despues de medida: " + productos.size());

        productos = filtrarAtributos(productos, busqueda);
        System.out.println("Despues de atributos: " + productos.size());

        productos = filtrarPorIndice(
                productos,
                palabrasUsuario
        );
        System.out.println("Despues de indice: " + productos.size());

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

    /*
    COMPARA SI "palabra" APARECE COMPLETA DENTRO DE "texto",
    NO COMO FRAGMENTO DE OTRA PALABRA.
    EJEMPLO DE BUG QUE ESTO EVITA: LA MARCA "LG" NO DEBE
    "ENCONTRARSE" DENTRO DE LA PALABRA "PULGADAS".
    */
    private boolean contienePalabra(String texto, String palabra) {

        if (palabra == null || palabra.isBlank()) {
            return false;
        }

        String regex =
                "\\b" +
                java.util.regex.Pattern.quote(palabra) +
                "\\b";

        return java.util.regex.Pattern
                .compile(regex)
                .matcher(texto)
                .find();

    }

    /*
    DEVUELVE LA POSICION DONDE EMPIEZA LA ULTIMA APARICION
    DE "palabra" COMO PALABRA COMPLETA DENTRO DE "texto",
    O -1 SI NO APARECE. SIRVE PARA SABER CUAL DE VARIAS
    MARCAS/CATEGORIAS PRESENTES EN EL TEXTO FUE MENCIONADA
    MAS RECIENTEMENTE POR EL CLIENTE.
    */
    private int posicionUltimaCoincidencia(String texto, String palabra) {

        if (palabra == null || palabra.isBlank()) {
            return -1;
        }

        String regex =
                "\\b" +
                java.util.regex.Pattern.quote(palabra) +
                "\\b";

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile(regex)
                        .matcher(texto);

        int ultimaPosicion = -1;

        while (matcher.find()) {
            ultimaPosicion = matcher.start();
        }

        return ultimaPosicion;

    }

    private Busqueda analizarMensaje(String mensaje) {

        Busqueda busqueda = new Busqueda();

        construirCatalogo();

        String texto = normalizar(mensaje);

        // ----------------------------
        // Detectar marca
        // ----------------------------
        /*
        ANTES: SE TOMABA LA PRIMERA MARCA QUE HICIERA MATCH
        SEGUN EL ORDEN DE catalogo.getMarcas(), QUE ES UN
        HashSet SIN ORDEN GARANTIZADO. COMO EL TEXTO PUEDE
        TRAER MEZCLADA UNA MARCA DE CONTEXTO ANTERIOR (ej.
        "halion" DEL PRODUCTO YA MOSTRADO) JUNTO CON LA MARCA
        NUEVA QUE EL CLIENTE ACABA DE ESCRIBIR (ej. "teros"),
        PODIA QUEDARSE CON LA MARCA VIEJA E IGNORAR LA NUEVA.

        AHORA: SI HAY VARIAS MARCAS PRESENTES EN EL TEXTO,
        SE ELIGE LA QUE APARECE MAS AL FINAL (LA MAS RECIENTE
        MENCIONADA POR EL CLIENTE).
        */
        int mejorPosicionMarca = -1;

        for (String marca : catalogo.getMarcas()) {

            int posicion = posicionUltimaCoincidencia(texto, marca);

            if (posicion > mejorPosicionMarca) {

                mejorPosicionMarca = posicion;
                busqueda.setMarca(marca);

            }

        }

        // ----------------------------
        // Detectar categoría
        // ----------------------------
        for (String categoria : catalogo.getCategorias()) {

            if (coincideConCategoria(texto, categoria)) {

                busqueda.setCategoria(categoria);
                break;

            }

        }

        // ----------------------------
        // Detectar modelo
        // ----------------------------
        for (String modelo : catalogo.getModelos()) {

            if (contienePalabra(texto, modelo)) {

                busqueda.setModelo(modelo);
                break;

            }

        }

        // ----------------------------
        // Detectar medida
        // ----------------------------
        /*
        SE INCLUYE LA COMILLA DOBLE (") ADEMAS DE LA SIMPLE (')
        PORQUE LOS NOMBRES DE LOS PRODUCTOS USAN LA COMILLA DOBLE
        COMO SIMBOLO DE PULGADAS (ej. `27" HS2703FC`). SIN ESTO,
        CUANDO EL CONTEXTO ANTERIOR TRAIA EL NOMBRE DEL PRODUCTO,
        LA MEDIDA NO SE RECONOCIA Y LA BUSQUEDA IGNORABA EL TAMAÑO.
        */
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile("(\\d{2})\\s*(pulgadas?|\"|')")
                        .matcher(texto);

        if (matcher.find()) {
            busqueda.setMedida(matcher.group(1));
        }

        // ----------------------------
        // Detectar atributos
        // ----------------------------

        String[] atributosDetectables = {

            "ips",
            "va",
            "tn",
            "oled",

            "fhd",
            "qhd",
            "uhd",
            "4k",

            "75hz",
            "100hz",
            "120hz",
            "144hz",
            "165hz",
            "170hz",
            "180hz",
            "240hz",
            "360hz",

            "hdmi",
            "displayport",
            "dp",
            "usb-c",
            "usbc",

            "curvo",
            "plano"

        };

        for (String atributo : atributosDetectables) {

            if (texto.contains(atributo)) {

                busqueda.agregarAtributo(atributo);

            }

        }

        // ----------------------------
        // Detectar modelo
        // ----------------------------
        for (Producto p : ProductoCache.obtenerProductos()) {

            if (p.getNombre() == null)
                continue;

            String nombre = normalizar(p.getNombre());

            String[] palabras = nombre.split("\\s+");

            for (String palabra : palabras) {

                if (palabra.length() < 5)
                    continue;

                if (!palabra.matches(".*\\d.*"))
                    continue;

                if (texto.contains(palabra)) {

                    busqueda.setModelo(palabra);
                    break;

                }

            }

            if (busqueda.getModelo() != null)
                break;

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
        // Detectar atributos
        // ----------------------------

        String[] atributos = {

            "ips",
            "va",
            "oled",
            "tn",

            "fhd",
            "full hd",
            "qhd",
            "2k",
            "4k",
            "uhd",

            "75hz",
            "100hz",
            "120hz",
            "144hz",
            "165hz",
            "170hz",
            "180hz",
            "240hz",
            "360hz",

            "1ms",
            "5ms",

            "hdmi",
            "displayport",
            "dp",
            "vga",
            "dvi",
            "usb",
            "usb-c",
            "type-c",

            "curvo",
            "plano"

        };

        for (String atributo : atributos) {

            if (texto.contains(atributo)) {

                busqueda.agregarAtributo(atributo);

            }

        }

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

        if (busqueda.getMedida() == null ||
            busqueda.getMedida().isBlank()) {
            return productos;
        }

        List<Producto> resultado = new ArrayList<>();

        for (Producto p : productos) {

            if (p.getMedida() == null)
                continue;

            String medida = normalizar(p.getMedida());

            if (medida.contains(busqueda.getMedida())) {

                resultado.add(p);

            }

        }

        return resultado;

    }

    private List<Producto> filtrarAtributos(
            List<Producto> productos,
            Busqueda busqueda) {

        if (busqueda.getAtributos() == null ||
            busqueda.getAtributos().isEmpty()) {

            return productos;
        }

        List<Producto> resultado = new ArrayList<>();

        for (Producto p : productos) {

            if (p.getAtributos() == null)
                continue;

            String atributos = normalizar(p.getAtributos());

            boolean cumple = true;

            for (String atributo : busqueda.getAtributos()) {

                if (!atributos.contains(normalizar(atributo))) {

                    cumple = false;
                    break;

                }

            }

            if (cumple) {

                resultado.add(p);

            }

        }

        return resultado;

    }

    private List<Producto> filtrarModelo(
            List<Producto> productos,
            Busqueda busqueda) {

        if (busqueda.getModelo() == null) {
            return productos;
        }

        List<Producto> resultado = new ArrayList<>();

        for (Producto p : productos) {

            if (p.getModelo() == null)
                continue;

            String modelo = normalizar(p.getModelo());

            if (modelo.equals(busqueda.getModelo())) {

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

        // ==========================
        // MODELO (máxima prioridad)
        // ==========================
        if (busqueda.getModelo() != null &&
                nombre.contains(busqueda.getModelo())) {

            score += 500;

        }

        // ==========================
        // NOMBRE
        // ==========================
        for (String palabra : palabrasUsuario) {

            if (palabra.length() <= 2)
                continue;

            if (nombre.contains(palabra)) {

                score += 300;

            }

        }

        // ==========================
        // MARCA
        // ==========================
        if (busqueda.getMarca() != null &&
                marca.contains(busqueda.getMarca())) {

            score += 180;

        }

        // ==========================
        // CATEGORIA
        // ==========================
        if (busqueda.getCategoria() != null &&
                categoria.contains(busqueda.getCategoria())) {

            score += 120;

        }

        // ==========================
        // ATRIBUTOS
        // ==========================
        for (String atributo : busqueda.getAtributos()) {

            if (nombre.contains(atributo)
                    || tags.contains(atributo)
                    || descripcion.contains(atributo)) {

                score += 100;

            }

        }

        // ==========================
        // TAGS
        // ==========================
        for (String palabra : palabrasUsuario) {

            if (palabra.length() <= 2)
                continue;

            if (tags.contains(palabra)) {

                score += 60;

            }

        }

        // ==========================
        // DESCRIPCION
        // ==========================
        for (String palabra : palabrasUsuario) {

            if (palabra.length() <= 2)
                continue;

            if (descripcion.contains(palabra)) {

                score += 20;

            }

        }

        // ==========================
        // BARATO
        // ==========================
        if (busqueda.isBarato() &&
                p.getPrecio().doubleValue() <= 100) {

            score += 50;

        }

        // ==========================
        // PREMIUM
        // ==========================
        if (busqueda.isPremium() &&
                p.getPrecio().doubleValue() >= 500) {

            score += 40;

        }

        // ==========================
        // STOCK
        // ==========================
        score += p.getStock() / 10;

        return score;

    }

    private List<Producto> filtrarPorIndice(
            List<Producto> productos,
            String[] palabrasUsuario
    ) {

        List<Producto> candidatos = new ArrayList<>();

        for (Producto p : productos) {

            int coincidencias = 0;

            for (String palabra : palabrasUsuario) {

                palabra = normalizar(palabra);

                if (palabra.length() <= 2) {
                    continue;
                }

                if (p.getIndiceBusqueda().contains(palabra)) {
                    coincidencias++;
                }

            }

            if (coincidencias > 0) {
                candidatos.add(p);
            }

        }

        return candidatos;

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