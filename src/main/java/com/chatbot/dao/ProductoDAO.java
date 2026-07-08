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

    /*
    COLORES CONOCIDOS. NO HAY UN CAMPO "color" EN LA TABLA
    DE PRODUCTOS: EL COLOR VIENE COMO TEXTO LIBRE DENTRO DEL
    NOMBRE (ej. "TITAN COLOR NEGRO - NEGRO M-CUERINA"). ESTA
    LISTA SE USA PARA DETECTAR QUE COLOR PIDE EL CLIENTE Y
    PARA VERIFICAR SI EL PRODUCTO ENCONTRADO REALMENTE LO
    TIENE, EN VEZ DE TRATAR EL COLOR COMO UNA PALABRA MAS
    QUE SOLO SUMA PUNTOS SIN FILTRAR NADA.
    */
    private static final String[] COLORES = {
        "negro", "blanco", "gris", "plomo",
        "rojo", "azul", "celeste", "verde",
        "amarillo", "naranja", "morado", "violeta",
        "rosado", "rosa", "marron", "café", "cafe",
        "dorado", "plateado", "beige"
    };

    /*
    DEVUELVE EL COLOR QUE EL CLIENTE MENCIONA EN EL TEXTO
    (EL MENCIONADO MAS AL FINAL, IGUAL QUE CATEGORIA/MARCA),
    O null SI NO MENCIONA NINGUNO.
    */
    private String detectarColor(String texto) {

        String mejorColor = null;
        int mejorPosicion = -1;

        for (String color : COLORES) {

            int posicion = posicionUltimaCoincidencia(texto, color);

            if (posicion > mejorPosicion) {
                mejorPosicion = posicion;
                mejorColor = color;
            }

        }

        return mejorColor;

    }

    /*
    EXPUESTO PARA QUE ChatbotService PUEDA AVISAR AL CLIENTE
    CUANDO EL COLOR QUE PIDIO NO ES EL DEL PRODUCTO QUE SE
    LE VA A MOSTRAR (ej. PIDIO "verde" Y SOLO HAY "negro"),
    EN VEZ DE MOSTRARLO CALLADAMENTE COMO SI FUERA UNA
    COINCIDENCIA EXACTA.
    */
    public String detectarColorEnTexto(String mensaje) {
        return detectarColor(normalizar(mensaje));
    }

    public boolean nombreContieneColor(String nombre, String color) {

        if (nombre == null || color == null) {
            return false;
        }

        return contienePalabra(normalizar(nombre), normalizar(color));

    }

    private void construirCatalogo() {

        catalogo = new CatalogoBusqueda();

        for (Producto p : listarActivos()) {

            catalogo.agregarCategoria(p.getCategoria());

            /*
            ALGUNOS PRODUCTOS (ej. SILLAS) TIENEN EL CAMPO
            "marca" CARGADO CON EL MISMO VALOR QUE SU PROPIA
            CATEGORIA (marca="Silla", categoria="Silla"), EN
            VEZ DE UNA MARCA REAL O VACIO. SI ESO SE AGREGA AL
            CATALOGO DE MARCAS, LA PALABRA "silla" SE TRATA
            COMO SI FUERA UNA MARCA VALIDA: CUALQUIER MENSAJE
            QUE MENCIONE LA CATEGORIA ("quiero una silla...")
            TERMINA FILTRANDO TAMBIEN POR "marca = silla" Y
            DESCARTA DE ENTRADA CUALQUIER PRODUCTO DE ESA
            CATEGORIA QUE NO TENGA ESE MISMO DATO MAL CARGADO,
            ANTES DE SIQUIERA CONSIDERAR LO QUE EL CLIENTE
            REALMENTE PIDIO (ej. "gamer", "verde").

            ANTES: SOLO SE EXCLUIA CUANDO marca Y categoria ERAN
            IDENTICAS COMO CADENA COMPLETA (ej. "Silla" == "Silla").
            ESO NO CUBRIA EL CASO REAL DETECTADO EN PRODUCCION:
            FILAS CON categoria="Silla Gamer" (MAS ESPECIFICA) Y
            marca="Silla" (EL MISMO DATO MAL CARGADO, PERO SOLO
            LA PALABRA BASE). COMO "silla" != "silla gamer", LA
            MARCA CORRUPTA SE SEGUIA REGISTRANDO COMO VALIDA Y
            ARRASTRABA EL MISMO BUG: BUSQUEDAS DE "silla gamer"
            TERMINABAN FILTRANDO SOLO LOS PRODUCTOS CON ESE DATO
            MAL CARGADO Y DESCARTANDO LAS SILLAS GAMER REALES.

            AHORA: TAMBIEN SE EXCLUYE SI marca COINCIDE CON
            CUALQUIERA DE LAS PALABRAS QUE COMPONEN LA categoria
            DE ESA MISMA FILA (ej. "silla" ES UNA PALABRA DE
            "silla gamer"), NO SOLO CUANDO SON IDENTICAS.
            */
            String marca = p.getMarca();
            String categoriaProducto = p.getCategoria();

            boolean marcaEsPalabraDeSuCategoria = false;

            if (marca != null && categoriaProducto != null) {

                String marcaNormalizada =
                        marca.trim().toLowerCase();

                for (String palabraCategoria :
                        categoriaProducto.trim().toLowerCase().split("\\s+")) {

                    if (marcaNormalizada.equals(palabraCategoria)) {
                        marcaEsPalabraDeSuCategoria = true;
                        break;
                    }

                }

            }

            if (!marcaEsPalabraDeSuCategoria) {
                catalogo.agregarMarca(marca);
            }

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
    /*
    ANTES: DEVOLVIA SOLO true/false, Y LA CATEGORIA GANADORA
    ERA LA PRIMERA QUE APARECIERA AL RECORRER catalogo.getCategorias(),
    QUE ES UN HashSet SIN ORDEN GARANTIZADO. SI EL TEXTO TRAIA
    MEZCLADAS DOS CATEGORIAS (ej. "mouse" DE CONTEXTO VIEJO Y
    "monitor" DEL MENSAJE NUEVO DEL CLIENTE), PODIA GANAR LA
    VIEJA POR PURA CASUALIDAD DE ORDEN INTERNO, IGNORANDO LO
    QUE EL CLIENTE ACABABA DE ESCRIBIR.

    AHORA: DEVUELVE LA POSICION DE LA MENCION MAS RECIENTE
    (IGUAL QUE YA SE HACE CON MARCA/ATRIBUTOS), O -1 SI NO
    COINCIDE. QUIEN LLAMA A ESTO SE QUEDA CON LA CATEGORIA
    CUYA POSICION SEA LA MAS ALTA (LA MENCIONADA MAS AL FINAL).
    */
    private int posicionCategoria(String texto, String categoria) {

        String[] stopwords = {"de", "del", "la", "el", "los", "las", "para", "pc", "y"};

        boolean tieneAlMenosUnaPalabraSignificativa = false;

        int posicionMasReciente = -1;

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

            int posicion =
                    posicionUltimaCoincidencia(texto, palabra);

            if (posicion == -1) {

                String singular = quitarPlural(palabra);

                if (!singular.equals(palabra)) {
                    posicion = posicionUltimaCoincidencia(texto, singular);
                }
            }

            /*
            TODAS LAS PALABRAS SIGNIFICATIVAS DE LA CATEGORIA
            DEBEN ESTAR PRESENTES (AND, NO OR).
            */
            if (posicion == -1) {
                return -1;
            }

            if (posicion > posicionMasReciente) {
                posicionMasReciente = posicion;
            }

        }

        if (!tieneAlMenosUnaPalabraSignificativa) {
            return -1;
        }

        return posicionMasReciente;

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
        return buscarCoincidencia(mensaje, null);
    }

    /*
    SOBRECARGA QUE PERMITE EXCLUIR UNA MARCA DE LOS
    RESULTADOS. SE USA CUANDO EL CLIENTE PIDE "OTRA MARCA":
    SIN ESTO, SI NO SE MENCIONA UNA MARCA NUEVA EXPLICITA,
    LA BUSQUEDA POR CATEGORIA/MEDIDA PODIA SEGUIR ELIGIENDO
    EL MISMO PRODUCTO (MISMA MARCA) QUE YA SE HABIA MOSTRADO.
    */
    public Producto buscarCoincidencia(String mensaje, String marcaExcluida) {

        String texto = normalizar(mensaje);

        if (contieneCategoriaInexistente(texto)) {
            return null;
        }

        Busqueda busqueda = analizarMensaje(mensaje);

        if (
                marcaExcluida != null &&
                !marcaExcluida.isBlank() &&
                !marcaExcluida.equalsIgnoreCase(busqueda.getMarca())
        ) {
            busqueda.setMarcaExcluida(marcaExcluida);
        }

        System.out.println("=== DIAGNOSTICO BUSQUEDA ===");
        System.out.println("Mensaje: " + mensaje);
        System.out.println("Categoria detectada: " + busqueda.getCategoria());
        System.out.println("Marca detectada: " + busqueda.getMarca());
        System.out.println("Modelo detectado: " + busqueda.getModelo());
        System.out.println("Medida detectada: " + busqueda.getMedida());
        System.out.println("Color detectado: " + busqueda.getColor());
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

        if (
                busqueda.getMarcaExcluida() != null &&
                !busqueda.getMarcaExcluida().isBlank()
        ) {

            List<Producto> sinMarcaExcluida = new ArrayList<>();

            for (Producto p : productos) {

                if (
                        p.getMarca() == null ||
                        !p.getMarca().equalsIgnoreCase(busqueda.getMarcaExcluida())
                ) {
                    sinMarcaExcluida.add(p);
                }
            }

            productos = sinMarcaExcluida;

            System.out.println("Despues de excluir marca '" + busqueda.getMarcaExcluida() + "': " + productos.size());
        }

        productos = filtrarModelo(productos, busqueda);
        System.out.println("Despues de modelo: " + productos.size());

        productos = filtrarMedida(productos, busqueda);
        System.out.println("Despues de medida: " + productos.size());

        productos = filtrarAtributos(productos, busqueda);
        System.out.println("Despues de atributos: " + productos.size());

        productos = filtrarColor(productos, busqueda);
        System.out.println("Despues de color: " + productos.size());

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

    /*
    EXPUESTO PARA QUE ChatbotService PUEDA AVISAR AL CLIENTE
    CUANDO LA MEDIDA QUE PIDIO (ej. "24 PULGADAS") NO ES LA
    DEL PRODUCTO QUE SE LE VA A MOSTRAR (ej. PIDIO 24" Y SOLO
    HAY DISPONIBLE 27" EN ESA MARCA), EN VEZ DE MOSTRARLO
    CALLADAMENTE COMO SI FUERA UNA COINCIDENCIA EXACTA.
    MISMO PATRON YA USADO CON detectarColorEnTexto/nombreContieneColor.
    */
    public String detectarMedidaEnTexto(String mensaje) {

        String texto = normalizar(mensaje);

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile("(\\d{2})\\s*(pulgadas?|\"|')")
                        .matcher(texto);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;

    }

    public boolean productoContieneMedida(Producto producto, String medida) {

        if (producto == null ||
            producto.getMedida() == null ||
            medida == null) {
            return false;
        }

        return normalizar(producto.getMedida()).contains(medida);

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
        int mejorPosicionCategoria = -1;

        for (String categoria : catalogo.getCategorias()) {

            int posicion = posicionCategoria(texto, categoria);

            if (posicion > mejorPosicionCategoria) {

                mejorPosicionCategoria = posicion;
                busqueda.setCategoria(categoria);

            }

        }

        // ----------------------------
        // Detectar color
        // ----------------------------
        busqueda.setColor(detectarColor(texto));

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
        /*
        ANTES: SE AGREGABA CUALQUIER ATRIBUTO QUE APARECIERA EN
        EL TEXTO, SIN IMPORTAR QUE ALGUNOS SON MUTUAMENTE
        EXCLUYENTES (UN MONITOR NO PUEDE SER "144HZ" Y "240HZ"
        A LA VEZ). COMO "texto" TRAE MEZCLADO EL NOMBRE COMPLETO
        DEL PRODUCTO ANTERIOR (CON SUS SPECS VIEJAS) JUNTO CON EL
        MENSAJE NUEVO DEL CLIENTE, SI EL CLIENTE PEDIA UNA
        CARACTERISTICA DISTINTA (ej. PASAR DE 144HZ A 240HZ), SE
        TERMINABA BUSCANDO UN PRODUCTO QUE CUMPLIERA AMBOS
        VALORES A LA VEZ, ALGO QUE NUNCA EXISTE, Y SIEMPRE
        RESPONDIA "NO ENCONTRO PRODUCTO".

        AHORA: LOS ATRIBUTOS QUE PERTENECEN AL MISMO GRUPO
        (PANEL, RESOLUCION, TASA DE REFRESCO, FORMA) SE TRATAN
        COMO EXCLUYENTES ENTRE SI, Y SOLO SE CONSERVA EL QUE
        APARECE MAS AL FINAL DEL TEXTO (EL MAS RECIENTE), IGUAL
        QUE YA SE HACE CON CATEGORIA Y MARCA.
        */

        String[][] gruposAtributosExcluyentes = {

            { "ips", "va", "tn", "oled" },

            { "fhd", "qhd", "uhd", "4k" },

            {
                "75hz", "100hz", "120hz", "144hz",
                "165hz", "170hz", "180hz", "240hz", "360hz"
            },

            { "curvo", "plano" }

        };

        for (String[] grupo : gruposAtributosExcluyentes) {

            String mejorAtributo = null;
            int mejorPosicionAtributo = -1;

            for (String atributo : grupo) {

                int posicion = posicionUltimaCoincidencia(texto, atributo);

                if (posicion > mejorPosicionAtributo) {
                    mejorPosicionAtributo = posicion;
                    mejorAtributo = atributo;
                }

            }

            if (mejorAtributo != null) {
                busqueda.agregarAtributo(mejorAtributo);
            }

        }

        /*
        ESTOS ATRIBUTOS SI PUEDEN COEXISTIR EN UN MISMO
        PRODUCTO (UN MONITOR PUEDE TENER HDMI Y DISPLAYPORT A
        LA VEZ), ASI QUE SE SIGUEN ACUMULANDO TODOS LOS QUE
        APAREZCAN EN EL TEXTO.
        */
        String[] atributosNoExcluyentes = {

            "hdmi",
            "displayport",
            "dp",
            "usb-c",
            "usbc"

        };

        for (String atributo : atributosNoExcluyentes) {

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

        /*
        IGUAL QUE CON EL COLOR: SI LA MEDIDA VIENE DE
        CONTEXTO VIEJO (ej. "27 PULGADAS" ARRASTRADO DE UNA
        PREGUNTA ANTERIOR SOBRE MONITORES) Y LA CATEGORIA
        ACTUAL NI SIQUIERA USA MEDIDA (ej. MOUSE), NO TIENE
        SENTIDO DESCARTAR TODO. SE MANTIENE LA LISTA ORIGINAL
        EN VEZ DE DEVOLVER CERO RESULTADOS.
        */
        if (resultado.isEmpty()) {
            return productos;
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

        /*
        MISMO CRITERIO QUE MEDIDA Y COLOR: SI LOS ATRIBUTOS
        VIENEN DE CONTEXTO VIEJO (ej. "144HZ" DE UNA PREGUNTA
        ANTERIOR SOBRE MONITORES) Y LA CATEGORIA ACTUAL NO
        TIENE ESOS ATRIBUTOS (ej. MOUSE), NO SE DESCARTA TODO.
        */
        if (resultado.isEmpty()) {
            return productos;
        }

        return resultado;

    }

    /*
    A DIFERENCIA DE LOS OTROS FILTROS, EL COLOR NO DESCARTA
    TODO CUANDO NO HAY COINCIDENCIA. SI EL CLIENTE PIDE
    "verde" Y NINGUN PRODUCTO DE LOS QUE QUEDAN LO TIENE,
    SE MANTIENE LA LISTA ORIGINAL (PARA SEGUIR OFRECIENDO
    LA MEJOR OPCION DISPONIBLE EN OTRO COLOR) EN VEZ DE
    DEVOLVER CERO RESULTADOS. LA HONESTIDAD SOBRE EL COLOR
    NO DISPONIBLE SE MANEJA APARTE, EN ChatbotService, USANDO
    detectarColorEnTexto/nombreContieneColor.
    */
    private List<Producto> filtrarColor(
            List<Producto> productos,
            Busqueda busqueda) {

        if (busqueda.getColor() == null) {
            return productos;
        }

        List<Producto> conColor = new ArrayList<>();

        for (Producto p : productos) {

            if (nombreContieneColor(p.getNombre(), busqueda.getColor())) {
                conColor.add(p);
            }

        }

        if (conColor.isEmpty()) {
            return productos;
        }

        return conColor;

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
        // COLOR
        // ==========================
        if (busqueda.getColor() != null &&
                nombreContieneColor(p.getNombre(), busqueda.getColor())) {

            score += 150;

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
        return buscarAlternativa(categoria, null, preferencia, productoActualId);
    }

    /*
    SOBRECARGA QUE PRIORIZA MANTENER LA MISMA MARCA DEL
    PRODUCTO ACTUAL. SE USA CUANDO EL CLIENTE PIDE "OTRO
    COLOR" O "OTRO MODELO": SIN ESTO, buscarAlternativa PODIA
    DEVOLVER CUALQUIER PRODUCTO DE LA MISMA CATEGORIA (ej. UN
    MOUSE DE OTRA MARCA), EN VEZ DE LA OTRA VARIANTE DEL MISMO
    PRODUCTO (ej. EL MISMO MOUSE LOGITECH EN OTRO COLOR).
    */
    public Producto buscarAlternativa(
        String categoria,
        String marcaPreferida,
        String preferencia,
        int productoActualId
    ) {

        List<Producto> productos =
                listarActivos();

        Producto mejor = null;

        Producto mejorMismaMarca = null;

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

                if (
                        mejorMismaMarca == null &&
                        marcaPreferida != null &&
                        !marcaPreferida.isBlank() &&
                        p.getMarca() != null &&
                        p.getMarca().equalsIgnoreCase(marcaPreferida)
                ) {
                    mejorMismaMarca = p;
                }

                /*
                PRODUCTO NORMAL
                */
                if (mejor == null) {
                    mejor = p;
                }
            }
        }

        if (mejorMismaMarca != null) {
            return mejorMismaMarca;
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