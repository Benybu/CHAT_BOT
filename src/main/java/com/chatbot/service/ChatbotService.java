package com.chatbot.service;

import com.chatbot.dao.ConversacionDAO;
import com.chatbot.dao.MensajeDAO;
import com.chatbot.dao.ProductoDAO;
import com.chatbot.dao.RespuestaDAO;
import com.chatbot.model.Conversacion;
import com.chatbot.model.Producto;
import com.chatbot.model.Respuesta;
import com.chatbot.model.PersonalidadChatbot;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.chatbot.model.RespuestaChat;
import com.chatbot.service.ProductoCache;

import java.util.List;
import java.util.Random;

public class ChatbotService {

        private String ultimaCategoria = "";
        private String ultimaMarca = "";
        private String ultimaMedida = "";
        private String ultimoContexto = "";
        /*
        MARCA QUE HAY QUE EXCLUIR DE LA PROXIMA BUSQUEDA
        PORQUE EL CLIENTE PIDIO EXPLICITAMENTE "OTRA MARCA".
        SIN ESTO, AL NO HABER NINGUNA MARCA NUEVA ESCRITA EN
        EL MENSAJE, EL CODIGO REINYECTABA LA MISMA ultimaMarca
        DE SIEMPRE Y LA BUSQUEDA TERMINABA DEVOLVIENDO OTRA VEZ
        EL MISMO PRODUCTO QUE YA SE LE HABIA MOSTRADO.
        */
        private String marcaExcluida = "";
        /*
        GUARDA EL PRODUCTO REALMENTE USADO
        PARA GENERAR LA ULTIMA RESPUESTA DE TEXTO,
        ASI procesarMensajeApi() NO VUELVE A BUSCAR
        POR SU CUENTA Y DESINCRONIZAR nombre/imagen/precio
        CON EL TEXTO DE LA RESPUESTA.
        */
        private Producto ultimoProductoEncontrado = null;
        private final ProductoDAO productoDAO = new ProductoDAO();
        private final RespuestaDAO respuestaDAO = new RespuestaDAO();
        private final MensajeDAO mensajeDAO = new MensajeDAO();
        private final ConversacionDAO conversacionDAO = new ConversacionDAO();
        private final OpenAIService openAIService = new OpenAIService();
        private final Random random = new Random();
        private final PersonalidadChatbot personalidad = PersonalidadChatbot.GAMER;

        public ChatbotService() {

        if (ProductoCache.obtenerProductos().isEmpty()) {

                ProductoCache.cargar(
                        productoDAO.listarActivos()
                );

        }

        }

        private Double extraerPresupuesto(String texto) {

        String t = texto.toLowerCase();

        /*
        SOLO CONSIDERAR "PRESUPUESTO" SI HAY UNA
        SEÑAL REAL DE QUE EL CLIENTE HABLA DE DINERO.
        SIN ESTO, CUALQUIER NUMERO (24 PULGADAS, 144HZ,
        8GB, ETC.) SE INTERPRETABA COMO UN PRESUPUESTO
        EN SOLES Y DESVIABA TODA LA BUSQUEDA.
        */
        boolean tieneSenalDePresupuesto =
                t.contains("s/") ||
                t.contains("soles") ||
                t.contains("presupuesto") ||
                t.contains("hasta") ||
                t.contains("menos de") ||
                t.contains("maximo") ||
                t.contains("máximo") ||
                t.contains("tengo");

        if (!tieneSenalDePresupuesto) {
                return null;
        }

        /*
        IGNORA NUMEROS PEGADOS A UNIDADES TECNICAS
        (PULGADAS, HZ, GB, MS, ETC.) PARA QUE NO SE
        CONFUNDAN CON UN MONTO EN SOLES.
        */
        Pattern pattern =
                Pattern.compile(
                        "(\\d{2,6})(?!\\s*(pulgadas?|\"|'|hz|ghz|mhz|gb|mb|tb|w|ms|dpi))"
                );

        Matcher matcher =
                pattern.matcher(t);

        if (matcher.find()) {

                return Double.parseDouble(
                        matcher.group(1)
                );
        }

        return null;
        }

        /*
        DEVUELVE TODAS LAS CATEGORIAS DISTINTAS (EN MINUSCULA)
        QUE EXISTEN REALMENTE EN EL CATALOGO DE PRODUCTOS, EN
        VEZ DE UNA LISTA FIJA E INCOMPLETA.
        */
        private java.util.Set<String> categoriasDelCatalogo() {

        java.util.Set<String> categorias = new java.util.HashSet<>();

        for (Producto p : ProductoCache.obtenerProductos()) {

                if (
                        p.getCategoria() != null &&
                        !p.getCategoria().isBlank()
                ) {
                categorias.add(p.getCategoria().toLowerCase());
                }
        }

        return categorias;
        }

        /*
        DEVUELVE TODAS LAS MARCAS DISTINTAS (EN MINUSCULA)
        QUE EXISTEN REALMENTE EN EL CATALOGO DE PRODUCTOS, EN
        VEZ DE UNA LISTA FIJA E INCOMPLETA.

        SE EXCLUYE CUALQUIER PRODUCTO CUYO CAMPO "marca" SEA
        IGUAL A SU PROPIO "categoria" (ej. SILLAS CARGADAS CON
        marca="Silla", categoria="Silla"). ESO ES UN DATO MAL
        CARGADO, NO UNA MARCA REAL, Y SI SE INCLUYERA, CUALQUIER
        MENSAJE QUE MENCIONE ESA CATEGORIA SE INTERPRETARIA
        TAMBIEN COMO SI PIDIERA ESA "MARCA", ARRASTRANDOLA DE
        TURNO EN TURNO IGUAL QUE UNA MARCA REAL.
        */
        private java.util.Set<String> marcasDelCatalogo() {

        java.util.Set<String> marcas = new java.util.HashSet<>();

        for (Producto p : ProductoCache.obtenerProductos()) {

                if (
                        p.getMarca() != null &&
                        !p.getMarca().isBlank()
                ) {

                boolean marcaEsIgualASuCategoria =
                        p.getCategoria() != null &&
                        p.getMarca().trim().equalsIgnoreCase(
                                p.getCategoria().trim()
                        );

                if (!marcaEsIgualASuCategoria) {
                        marcas.add(p.getMarca().toLowerCase());
                }
                }
        }

        return marcas;
        }

        /*
        DEVUELVE LA POSICION DONDE EMPIEZA LA ULTIMA APARICION
        DE "palabra" COMO PALABRA COMPLETA DENTRO DE "texto",
        O -1 SI NO APARECE. SIRVE PARA PREFERIR LA MARCA/CATEGORIA
        MENCIONADA MAS RECIENTEMENTE CUANDO HAY VARIAS PRESENTES
        (ej. UNA ARRASTRADA DEL CONTEXTO ANTERIOR Y OTRA NUEVA).
        */
        private int ultimaPosicion(String texto, String palabra) {

        if (palabra == null || palabra.isBlank()) {
                return -1;
        }

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                        .compile("\\b" + java.util.regex.Pattern.quote(palabra) + "\\b")
                        .matcher(texto);

        int ultima = -1;

        while (matcher.find()) {
                ultima = matcher.start();
        }

        return ultima;
        }

    private String saludoAleatorio() {

        String[] saludos = {
                "¡Claro! 😊",
                "¡Sí! 🔥",
                "Perfecto 👍",
                "Buena elección 😎",
                "Excelente opción 🚀",
                "¡Tenemos justo lo que buscas! 🎮"
        };

        return saludos[random.nextInt(saludos.length)];
    }

private String generarCierre() {

        String[] cierres = {
                "¿Te gustaría más información? 😊",
                "Puedo ayudarte con más detalles si deseas 🔥",
                "También puedo recomendarte productos similares 😄",
                "Si deseas, puedo mostrarte otras opciones disponibles 👌",
                "Tenemos más modelos disponibles 🚀"
        };

        return cierres[random.nextInt(cierres.length)];
}

private String generarComentarioProducto(
        Producto producto
) {

    if (
            producto == null ||
            producto.getCategoria() == null
    ) {

        return "🔥 Producto recomendado para ti.";
    }

    String categoria =
            producto.getCategoria()
                    .toLowerCase();

    /*
    MONITOR
    */
    if (categoria.contains("monitor")) {

        return "🖥️ Excelente opción para gaming, streaming y trabajo multitarea.";
    }

    /*
    PROCESADOR
    */
    if (categoria.contains("procesador")) {

        return "⚡ Gran rendimiento para gaming y aplicaciones exigentes.";
    }

    /*
    TARJETA VIDEO
    */
    if (
            categoria.contains("tarjeta") ||
            categoria.contains("video")
    ) {

        return "🎮 Ideal para juegos en alta calidad y máximo rendimiento gráfico.";
    }

    /*
    PLACA MADRE
    */
    if (
            categoria.contains("placa")
    ) {

        return "🛠️ Base sólida y estable para tu PC gamer.";
    }

    /*
    CASE
    */
    if (
            categoria.contains("case")
    ) {

        return "🔥 Diseño gamer con excelente flujo de aire y estética premium.";
    }

    /*
    TECLADO
    */
    if (
            categoria.contains("teclado")
    ) {

        return "⌨️ Perfecto para largas sesiones gaming y máxima comodidad.";
    }

    /*
    MOUSE
    */
    if (
            categoria.contains("mouse")
    ) {

        return "🖱️ Precisión ideal para juegos competitivos.";
    }

    /*
    RAM
    */
    if (
            categoria.contains("ram")
    ) {

        return "🚀 Mejora notablemente la velocidad y fluidez del sistema.";
    }

    /*
    SSD
    */
    if (
            categoria.contains("disco")
    ) {

        return "💾 Excelente velocidad de carga para juegos y programas.";
    }

    /*
    FUENTE
    */
    if (
            categoria.contains("fuente")
    ) {

        return "⚡ Energía estable y segura para todos tus componentes.";
    }

    /*
    COOLER
    */
    if (
            categoria.contains("cooler")
    ) {

        return "❄️ Mantiene temperaturas óptimas incluso en gaming intenso.";
    }

    /*
    AUDIFONOS
    */
    if (
            categoria.contains("audifono")
    ) {

        return "🎧 Sonido envolvente ideal para juegos FPS y multimedia.";
    }

    /*
    PARLANTES
    */
    if (
            categoria.contains("parlante")
    ) {

        return "🔊 Excelente calidad de sonido para música y gaming.";
    }

    /*
    SILLA GAMER
    */
    if (
            categoria.contains("silla")
    ) {

        return "🪑 Máxima comodidad para largas sesiones frente al PC.";
    }

    /*
    REDES
    */
    if (
            categoria.contains("red")
    ) {

        return "🌐 Conexión estable y rápida para gaming online.";
    }

    /*
    USB
    */
    if (
            categoria.contains("usb")
    ) {

        return "🔌 Accesorio práctico y útil para múltiples dispositivos.";
    }

    /*
    IMPRESORA
    */
    if (
            categoria.contains("impresora")
    ) {

        return "🖨️ Excelente opción para oficina y uso profesional.";
    }

    /*
    ESTABILIZADOR
    */
    if (
            categoria.contains("estabilizador")
    ) {

        return "🔋 Protección ideal para cuidar tus equipos electrónicos.";
    }

    /*
    RACK
    */
    if (
            categoria.contains("rack")
    ) {

        return "🗄️ Organización y seguridad para tus equipos.";
    }

    return "🔥 Producto muy recomendado por nuestros clientes.";
}

private String aplicarPersonalidad(String mensaje) {

    boolean mensajeNegativo =
            mensaje.toLowerCase().contains("no contamos") ||
            mensaje.toLowerCase().contains("no disponible") ||
            mensaje.toLowerCase().contains("agotado");

    switch (personalidad) {

        case GAMER:

            if (mensajeNegativo) {

                return """
                🎮 %s
                """.formatted(mensaje);
            }

            return """
            🎮 %s

            🔥 Aprovecha antes que se agote.
            """.formatted(mensaje);

        case ELEGANTE:

            return """
            ✨ %s

            Será un gusto ayudarte con cualquier consulta adicional.
            """.formatted(mensaje);

        case MARKETPLACE:

            return """
            🛒 %s

            📩 Escríbenos para coordinar compra o entrega.
            """.formatted(mensaje);

        case SOPORTE:

            return """
            🛠️ %s

            Si necesitas especificaciones técnicas,
            puedo ayudarte.
            """.formatted(mensaje);

        case PREMIUM:

            return """
            👑 %s

            Producto altamente recomendado para una experiencia premium.
            """.formatted(mensaje);

        default:
            return mensaje;
    }
}

        public String procesarMensaje(String mensaje) {

        if (mensaje == null || mensaje.isBlank()) {
            return "Escribe un mensaje.";
        }

        /*
        LIMPIA EL PRODUCTO DE LA LLAMADA ANTERIOR
        PARA NO ARRASTRAR UN RESULTADO VIEJO
        SI ESTA VEZ NO SE ENCUENTRA NADA
        */
        ultimoProductoEncontrado = null;

        String texto = mensaje.toLowerCase();

        /*
        GUARDAR CATEGORIAS

        ANTES: SOLO SE RECONOCIAN 4 CATEGORIAS FIJAS
        ("monitor", "mouse", "teclado", "laptop"). CUALQUIER
        OTRA CATEGORIA DEL CATALOGO (silla, audifono, parlante,
        etc.) NUNCA ACTUALIZABA ultimaCategoria, DEJANDO UN
        VALOR VIEJO/INCORRECTO ARRASTRADO DE TURNOS ANTERIORES.

        AHORA: SE COMPARA CONTRA TODAS LAS CATEGORIAS REALES
        DEL CATALOGO Y, SI HAY VARIAS PRESENTES, SE PREFIERE
        LA QUE APARECE MAS AL FINAL (LA MAS RECIENTE).
        */
        int posCategoria = -1;

        for (String categoria : categoriasDelCatalogo()) {

                int pos = ultimaPosicion(texto, categoria);

                if (pos > posCategoria) {
                        posCategoria = pos;
                        ultimaCategoria = categoria;
                }
        }

        /*
        GUARDAR MARCAS

        ANTES: SOLO SE RECONOCIAN 3 MARCAS FIJAS
        ("msi", "asus", "teros"). CUALQUIER OTRA MARCA DEL
        CATALOGO (halion, lg, samsung, etc.) NUNCA ACTUALIZABA
        ultimaMarca, POR LO QUE UNA MARCA VIEJA ARRASTRADA DE
        UN TURNO ANTERIOR SEGUIA APLICANDOSE AUNQUE EL CLIENTE
        HUBIERA PEDIDO OTRA MARCA DISTINTA.

        AHORA: SE COMPARA CONTRA TODAS LAS MARCAS REALES DEL
        CATALOGO Y, SI HAY VARIAS PRESENTES (ej. LA MARCA VIEJA
        ARRASTRADA POR CONTEXTO Y LA NUEVA QUE EL CLIENTE ACABA
        DE ESCRIBIR), SE PREFIERE LA QUE APARECE MAS AL FINAL.
        */
        /*
        SI EL CLIENTE PIDE "OTRA MARCA" Y NO ESCRIBE NINGUNA
        MARCA NUEVA EXPLICITA EN ESTE MISMO MENSAJE, SE GUARDA
        LA MARCA ACTUAL COMO "marcaExcluida" Y SE LIMPIA
        ultimaMarca PARA QUE NO SE VUELVA A USAR MAS ABAJO.
        SI EL CLIENTE SI NOMBRA UNA MARCA NUEVA (ej. "otra
        marca, teros"), ESA MARCA NUEVA GANA IGUAL MAS ABAJO
        Y marcaExcluida QUEDA VACIA.
        */
        boolean pideOtraMarca = quiereOtraMarca(texto);

        if (pideOtraMarca && !ultimaMarca.isBlank()) {
                marcaExcluida = ultimaMarca;
                ultimaMarca = "";
        } else if (!pideOtraMarca) {
                marcaExcluida = "";
        }

        int posMarca = -1;

        for (String marca : marcasDelCatalogo()) {

                int pos = ultimaPosicion(texto, marca);

                if (pos > posMarca) {
                        posMarca = pos;
                        ultimaMarca = marca;
                        /*
                        EL CLIENTE SI ESCRIBIO UNA MARCA NUEVA
                        CONCRETA, ASI QUE YA NO HAY QUE EXCLUIR
                        NADA: LA BUSQUEDA DEBE IR DIRECTO A ESA
                        MARCA.
                        */
                        marcaExcluida = "";
                }
        }

        /*
        GUARDAR MEDIDAS

        ANTES: EL REGEX "(\\d{2})" CAPTURABA CUALQUIER NUMERO
        DE DOS DIGITOS EN EL TEXTO (ej. "144hz", UN MODELO,
        ETC.), NO NECESARIAMENTE UNA MEDIDA EN PULGADAS.

        AHORA: SOLO SE GUARDA COMO MEDIDA SI EL NUMERO VIENE
        ACOMPAÑADO DE "pulgada(s)", COMILLA SIMPLE (') O DOBLE (")
        QUE ES COMO SE EXPRESA REALMENTE UNA MEDIDA DE PANTALLA.
        */
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern
                .compile("(\\d{2})\\s*(pulgadas?|\"|')")
                .matcher(texto);

        if (matcher.find()) {
        ultimaMedida = matcher.group(1);
        }

        /*
        COMPLETAR CONTEXTO

        ANTES: SE COMPARABA CONTRA LOS LITERALES FIJOS
        "monitor" Y "msi", SIN IMPORTAR CUAL ERA LA
        CATEGORIA/MARCA REALMENTE DETECTADA (ultimaCategoria/
        ultimaMarca). ESO PROVOCABA QUE, POR EJEMPLO, SI EL
        CLIENTE PREGUNTABA POR UNA MARCA DISTINTA A "msi"
        (ej. "teros"), SIEMPRE SE VOLVIERA A AGREGAR LA MARCA
        ANTERIOR AL TEXTO, INCLUSO SI YA ESTABA CORRECTA,
        Y EN COMBINACION CON EL CONTEXTO PREVIO PODIA TERMINAR
        PRIORIZANDO LA MARCA VIEJA EN LUGAR DE LA NUEVA.

        AHORA: SOLO SE COMPLETA CON EL CONTEXTO ANTERIOR SI
        EL TEXTO ACTUAL NO MENCIONA YA ESA MISMA CATEGORIA/MARCA.
        */
        if (
                !ultimaCategoria.isBlank() &&
                !texto.contains(ultimaCategoria)
        ) {
        texto += " " + ultimaCategoria;
        }

        if (
                !ultimaMarca.isBlank() &&
                !texto.contains(ultimaMarca)
        ) {
        texto += " " + ultimaMarca;
        }

        /*
        SI EL CLIENTE PIDIO "OTRA MARCA" SE VUELVE A AGREGAR
        LA CATEGORIA (YA HECHO ARRIBA) PERO NO LA MARCA. EN SU
        LUGAR, LA BUSQUEDA RECIBE marcaExcluida PARA DESCARTAR
        LOS PRODUCTOS DE ESA MARCA Y OFRECER UNA DISTINTA.
        */

        if (
                !texto.matches(".*\\d{2}.*") &&
                !ultimaMedida.isBlank()
        ) {
        /*
        SE AGREGA LA PALABRA "pulgadas" AL REINYECTAR LA MEDIDA
        RECORDADA, PORQUE EL DETECTOR DE MEDIDA DE ProductoDAO
        EXIGE QUE EL NUMERO VENGA ACOMPAÑADO DE "pulgada(s)" O
        UNA COMILLA PARA RECONOCERLO. SIN ESTO, EL NUMERO SOLO
        (ej. "27") SE PERDIA SILENCIOSAMENTE Y LA BUSQUEDA
        IGNORABA POR COMPLETO EL TAMAÑO PEDIDO.
        */
        texto += " " + ultimaMedida + " pulgadas";
        }

        String intencion = detectarIntencion(texto);

        /*
        SI EL MENSAJE NO BUSCA PRODUCTOS,
        RESPONDE CON IA NATURAL
        */
        if (!mensajeBuscaProducto(texto)) {

        String respuestaIA =
                openAIService.preguntar(
                        """
                        Responde como un vendedor gamer amable y natural.

                        Cliente:
                        "%s"
                        """.formatted(mensaje)
                );

        mensajeDAO.guardar(
                mensaje,
                respuestaIA
        );

        return respuestaIA;
        }

        Double presupuesto = extraerPresupuesto(texto);

        Producto producto;

        if (presupuesto != null) {

        producto =
                productoDAO.buscarCoincidenciaConPresupuesto(
                        texto,
                        presupuesto
                );

        } else {

        producto =
                productoDAO.buscarCoincidencia(texto, marcaExcluida);
        }

        /*
         SI NO ENCUENTRA PRODUCTO,
         USA EL ÚLTIMO PRODUCTO DE LA CONVERSACIÓN
        */
        boolean usarContexto =
                texto.contains("cuesta") ||
                texto.contains("precio") ||
                texto.contains("stock") ||
                texto.contains("disponible") ||
                texto.contains("ese") ||
                texto.contains("esa") ||
                texto.contains("lo quiero") ||
                texto.contains("me interesa");

        if (producto == null && usarContexto) {
        Conversacion conversacion =
                conversacionDAO.obtener("cliente1");


        if (conversacion != null) {
                producto = productoDAO.buscarPorId(
                        conversacion.getUltimoProductoId()
                );
        }
        }

        if (quiereAlternativa(texto)) {
        Conversacion conv =
                conversacionDAO.obtener("cliente1");

        if (conv != null) {

                Producto alternativo =
                        productoDAO.buscarAlternativa(
                                conv.getUltimaCategoria(),
                                conv.getPreferenciaPrecio(),
                                conv.getUltimoProductoId()
                        );

                if (alternativo != null) {

                String respuesta = """
                😊 También podría interesarte esta opción:

                🛒 %s

                💰 Precio: S/ %.2f
                📦 Stock: %d unidades

                📄 %s
                """.formatted(
                        alternativo.getNombre(),
                        alternativo.getPrecio(),
                        alternativo.getStock(),
                        alternativo.getDescripcion()
                );

                ultimoProductoEncontrado = alternativo;

                return aplicarPersonalidad(respuesta);
                }
        }
        }

        if ( 
                buscaProductoInexistente(texto, producto) 
                && producto == null 
        ) { 
                String respuesta = """ 
                😅 Actualmente no contamos con ese producto exacto. 
                Pero sí tenemos otras opciones gamer disponibles 🎮 
                ¿Te gustaría que te recomiende algo similar? 👌 
                """; 
                return aplicarPersonalidad(respuesta);
        }

        /*
         SI ENCUENTRA PRODUCTO
        */
        if (producto != null) {

            ultimoProductoEncontrado = producto;

            String respuesta;

            switch (intencion) {

                case "PRECIO":

                        respuesta = """
                        %s

                        %s

                        🛒 %s

                        💰 Precio: S/ %.2f

                        📦 Stock disponible: %d unidades

                        %s
                        """.formatted(
                                saludoAleatorio(),
                                generarComentarioProducto(producto),
                                producto.getNombre(),
                                producto.getPrecio(),
                                producto.getStock(),
                                generarCierre()
                        );

                        break;

                case "STOCK":

                        respuesta = """
                        %s
                        
                        Sí 😊 tenemos disponible:
                        
                        🛒 %s
                        
                        📦 Stock actual: %d unidades
                        
                        %s
                        """.formatted(
                                saludoAleatorio(),
                                producto.getNombre(),
                                producto.getStock(),
                                producto.getPrecio(),
                                generarCierre()
                        );

                        break;

                case "COMPRA":

                        respuesta = """
                        %s
                        
                        Excelente elección 🔥
                        
                        🛒 Producto:
                        %s
                        
                        💰 Precio: S/ %.2f
                        
                        📦 Disponibles: %d unidades
                        
                        📄 %s
                        
                        %s
                        """.formatted(
                                saludoAleatorio(),
                                producto.getNombre(),
                                producto.getPrecio(),
                                producto.getStock(),
                                producto.getDescripcion(),
                                generarCierre()
                        );

                        break;

                case "ENVIO":

                        respuesta = """
                        %s
                        
                        🚚 Sí realizamos envíos.
                        
                        Producto:
                        🛒 %s
                        
                        💰 Precio: S/ %.2f
                        
                        📦 Stock disponible: %d
                        
                        Podemos coordinar entrega inmediata 😄
                        """.formatted(
                                saludoAleatorio(),
                                producto.getNombre(),
                                producto.getPrecio(),
                                producto.getStock()
                        );

                        break;

                default:

                        respuesta = """
                        %s
                        
                        Tenemos disponible este producto 👇
                        
                        🛒 %s
                        
                        💰 Precio: S/ %.2f
                        
                        📦 Stock: %d unidades
                        
                        📄 Descripción:
                        %s
                        
                        %s
                        """.formatted(
                                saludoAleatorio(),
                                producto.getNombre(),
                                producto.getPrecio(),
                                producto.getStock(),
                                producto.getDescripcion(),
                                generarCierre()
                        );

                        break;
                }

            /*
             GUARDA CONTEXTO DE CONVERSACIÓN
            */
        String preferencia = "NORMAL";

        if (
                texto.contains("barato") ||
                texto.contains("economico") ||
                texto.contains("económico")
        ) {
        preferencia = "BARATO";
        }

        if (
                texto.contains("premium") ||
                texto.contains("pro")
        ) {
        preferencia = "PREMIUM";
        }

        conversacionDAO.guardarContexto(
                "cliente1",
                producto.getId(),
                intencion,
                producto.getCategoria(),
                preferencia
        );

            /*
             AVISO HONESTO DE COLOR
             SI EL CLIENTE PIDIO UN COLOR ESPECIFICO (ej. "verde")
             Y EL PRODUCTO ENCONTRADO NO LO TIENE EN SU NOMBRE
             (NO HAY CAMPO "color" PROPIO, SE INFIERE DEL NOMBRE),
             SE ACLARA EN VEZ DE MOSTRARLO CALLADAMENTE COMO SI
             FUERA UNA COINCIDENCIA EXACTA DE COLOR.
            */
            String colorSolicitado =
                    productoDAO.detectarColorEnTexto(texto);

            if (
                    colorSolicitado != null &&
                    !productoDAO.nombreContieneColor(
                            producto.getNombre(),
                            colorSolicitado
                    )
            ) {

                respuesta += """


                🎨 Por cierto, este modelo no lo tenemos en %s,\
                el color disponible es el que ves arriba.
                """.formatted(colorSolicitado);

            }

            /*
             GUARDA MENSAJE
            */
            mensajeDAO.guardar(
                    mensaje,
                    respuesta
            );

            return aplicarPersonalidad(respuesta);
        }

        /*
         RESPUESTAS PREDEFINIDAS
        */
        List<Respuesta> respuestas =
                respuestaDAO.listarActivos();

        for (Respuesta r : respuestas) {

            String[] palabras =
                    r.getPalabrasClave().split(",");

            for (String palabra : palabras) {

                if (texto.contains(
                        palabra.trim().toLowerCase()
                )) {

                    mensajeDAO.guardar(
                            mensaje,
                            r.getRespuesta()
                    );

                    return r.getRespuesta();
                }
            }
        }

        if (!hayRelacionConProductos(texto)) {

        mensajeDAO.guardar(
                mensaje,
                "Producto no encontrado en inventario"
        );

        return """
        😅 Actualmente no contamos con ese producto exacto.

        🔍 Nuestro chatbot solo puede recomendar
        productos reales disponibles en la tienda.

        ¿Te gustaría ver otras opciones gamer disponibles? 🎮
        """;
        }

        /*
         IA OPENAI
        */
        String respuestaIA = openAIService.preguntar(
                """
                Eres un vendedor.

                SOLO puedes responder usando
                los productos listados.

                SI NO EXISTE EL PRODUCTO:

                responde:

                "Actualmente no contamos con ese producto."

                NO inventes productos.
                NO inventes marcas.
                NO inventes precios.

                Productos:

                %s

                Cliente:

                %s
                """.formatted(
                        obtenerProductosTexto(),
                        mensaje
                )
        );

        mensajeDAO.guardar(
                mensaje,
                respuestaIA
        );

        return respuestaIA;
    }

    /*
     CONVIERTE PRODUCTOS A TEXTO PARA IA
    */
    private String obtenerProductosTexto() {

        StringBuilder sb = new StringBuilder();

        List<Producto> productos =
                productoDAO.listarActivos();

        for (Producto p : productos) {

            sb.append("""
                    Producto: %s
                    Categoría: %s
                    Marca: %s
                    Tags: %s
                    Precio: %s
                    Stock: %d
                    Descripción: %s

                    """.formatted(
                    p.getNombre(),
                    p.getCategoria(),
                    p.getMarca(),
                    p.getTags(),
                    p.getPrecio(),
                    p.getStock(),
                    p.getDescripcion()
            ));
        }

        return sb.toString();
    }

    /*
     DETECTAR INTENCIÓN
    */
    private String detectarIntencion(String texto) {

        texto = texto.toLowerCase();

        if (
                texto.contains("precio") ||
                texto.contains("cuesta") ||
                texto.contains("vale")
        ) {

            return "PRECIO";
        }

        if (
                texto.contains("stock") ||
                texto.contains("disponible") ||
                texto.contains("hay")
        ) {

            return "STOCK";
        }

        if (
        texto.contains("comprar")
        ) {

        return "COMPRA";
        }

        if (
                texto.contains("envio") ||
                texto.contains("delivery")
        ) {

            return "ENVIO";
        }

        return "GENERAL";
    }

        private String detectarPreferenciaPrecio(String texto) {

        texto = texto.toLowerCase();

        if (
                texto.contains("barato") ||
                texto.contains("económico") ||
                texto.contains("economico")
        ) {
                return "BARATO";
        }

        if (
                texto.contains("premium") ||
                texto.contains("alta gama") ||
                texto.contains("pro")
        ) {
                return "PREMIUM";
        }

        return "NORMAL";
        }

        /*
        DETECTA CUANDO EL CLIENTE PIDE UN PRODUCTO DE UNA
        MARCA DISTINTA A LA QUE YA SE LE MOSTRO (ej. "en otra
        marca", "otro fabricante"), SIN NECESARIAMENTE NOMBRAR
        LA MARCA NUEVA. EN ESE CASO HAY QUE EXCLUIR LA MARCA
        ANTERIOR EN VEZ DE REINYECTARLA COMO CONTEXTO.
        */
        private boolean quiereOtraMarca(String texto) {

        texto = texto.toLowerCase();

        return
                texto.contains("otra marca") ||
                texto.contains("otro fabricante") ||
                texto.contains("diferente marca") ||
                texto.contains("marca distinta") ||
                texto.contains("marca diferente") ||
                texto.contains("cambia de marca") ||
                texto.contains("cambiar de marca") ||
                texto.contains("otras marcas");
        }

        private boolean quiereAlternativa(String texto) {

        texto = texto.toLowerCase();

        return
                texto.contains("otra opcion") ||
                texto.contains("otra opción") ||
                texto.contains("algo mas") ||
                texto.contains("algo más") ||
                texto.contains("alternativa") ||
                texto.contains("otro producto") ||
                texto.contains("recomiendame otro") ||
                texto.contains("recomiéndame otro");
        }

        private boolean hayRelacionConProductos(String texto) {

        texto = texto.toLowerCase();

        List<Producto> productos =
                productoDAO.listarActivos();

        int coincidencias = 0;

        for (Producto p : productos) {

                String contenido =
                        (
                        p.getNombre() + " " +
                        p.getCategoria() + " " +
                        p.getMarca() + " " +
                        p.getTags()
                        ).toLowerCase();

                String[] palabras = texto.split("\\s+");

                for (String palabra : palabras) {

                palabra = palabra.trim();

                if (
                        palabra.length() > 3 &&
                        contenido.contains(palabra)
                ) {

                        coincidencias++;
                }
                }
        }

        /*
        EXIGE MÁS COINCIDENCIAS
        */
        return coincidencias >= 2;
        }

        private boolean buscaProductoInexistente( 
                String texto, 
                Producto producto 
        ) { 
                if (producto == null) { 
                        return true; 
                } 
                texto = texto.toLowerCase(); 
                String contenido = 
                        ( 
                                producto.getNombre() + " " + 
                                producto.getCategoria() + " " + 
                                producto.getTags() + " " + 
                                producto.getDescripcion() 
                        ).toLowerCase(); 
                int coincidencias = 0; 
                String[] palabras = texto.split("\\s+"); 
                for (String palabra : palabras) { 
                        palabra = palabra.trim(); 
                        /* IGNORAR PALABRAS PEQUEÑAS */ 
                        if (palabra.length() <= 2) { 
                                continue; 
                        } 
                        /* CONTAR COINCIDENCIAS */ 
                        if (contenido.contains(palabra)) { 
                                coincidencias++; 
                        } 
                } 
                /* SI TIENE AL MENOS 2 COINCIDENCIAS, EL PRODUCTO YA ES VALIDO */ 
                return coincidencias < 2; 
        }

        private boolean mensajeBuscaProducto(String texto) {

        texto = texto.toLowerCase();

        return
                texto.contains("monitor") ||
                texto.contains("mouse") ||
                texto.contains("teclado") ||
                texto.contains("laptop") ||
                texto.contains("silla") ||
                texto.contains("gamer") ||
                texto.contains("auricular") ||
                texto.contains("audifono") ||
                texto.contains("audífono") ||
                texto.contains("microfono") ||
                texto.contains("micrófono") ||
                texto.contains("procesador") ||
                texto.contains("ram") ||
                texto.contains("ssd") ||
                texto.contains("disco") ||
                texto.contains("placa") ||
                texto.contains("video") ||
                texto.contains("gpu") ||
                texto.contains("fuente") ||
                texto.contains("cooler") ||
                texto.contains("parlante") ||
                texto.contains("impresora") ||
                texto.contains("usb") ||
                texto.contains("rack");
        }

        public RespuestaChat procesarMensajeApi(String mensaje) { 
                String texto = mensaje.toLowerCase();

                /*
                SI EL CLIENTE MENCIONA EXPLICITAMENTE UNA MARCA
                DISTINTA A LA DEL ULTIMO PRODUCTO QUE REALMENTE
                SE LE MOSTRO (ej. se le mostro un "halion" y ahora
                pide "msi"), SIGNIFICA QUE QUIERE UN PRODUCTO
                DISTINTO. EN ESE CASO NO SE DEBE ARRASTRAR EL
                ultimoContexto DEL PRODUCTO ANTERIOR, PORQUE ESTE
                INCLUYE EL NOMBRE/MODELO EXACTO DE ESE PRODUCTO (ej.
                "HS2703FC") Y ESO HACIA QUE LA BUSQUEDA SIGUIERA
                FILTRANDO POR ESE MODELO VIEJO AUNQUE LA MARCA HAYA
                CAMBIADO, DEVOLVIENDO SIEMPRE "NO ENCONTRO PRODUCTO".

                IMPORTANTE: SE COMPARA CONTRA LA MARCA DEL PRODUCTO
                REALMENTE MOSTRADO (ultimoProductoEncontrado), NO
                CONTRA EL CAMPO ultimaMarca. ESE CAMPO SOLO SE
                ACTUALIZA CUANDO EL CLIENTE ESCRIBE LA MARCA, PERO
                EL PRIMER MONITOR MOSTRADO PUDO SER, POR EJEMPLO,
                "HALION" SIN QUE EL CLIENTE LO HAYA PEDIDO ASI.
                */
                int mejorPosicionMarcaNueva = -1;
                String marcaMencionada = null;

                for (String marca : marcasDelCatalogo()) {

                        int posicion = ultimaPosicion(texto, marca);

                        if (posicion > mejorPosicionMarcaNueva) {
                                mejorPosicionMarcaNueva = posicion;
                                marcaMencionada = marca;
                        }
                }

                String marcaProductoAnterior =
                        (
                                ultimoProductoEncontrado != null &&
                                ultimoProductoEncontrado.getMarca() != null
                        )
                        ? ultimoProductoEncontrado.getMarca().toLowerCase()
                        : "";

                /*
                EL CLIENTE TAMBIEN PUEDE PEDIR "OTRA MARCA" SIN
                NOMBRAR NINGUNA MARCA NUEVA. EN ESE CASO TAMBIEN
                HAY QUE DESCARTAR EL ultimoContexto, PORQUE ESTE
                TRAE EL NOMBRE DEL PRODUCTO ANTERIOR (Y POR LO
                TANTO SU MARCA) Y VOLVERIA A REINYECTARSE MAS
                ABAJO, HACIENDO QUE LA BUSQUEDA SIGA ENCONTRANDO
                LA MISMA MARCA QUE EL CLIENTE QUIERE EVITAR.
                */
                boolean pideOtraMarca = quiereOtraMarca(texto);

                if (
                        (
                                marcaMencionada != null &&
                                !marcaProductoAnterior.isBlank() &&
                                !marcaMencionada.equals(marcaProductoAnterior)
                        )
                        ||
                        pideOtraMarca
                ) {

                ultimoContexto = "";
                }

                if (pideOtraMarca && !marcaProductoAnterior.isBlank()) {
                marcaExcluida = marcaProductoAnterior;
                } else if (!pideOtraMarca) {
                marcaExcluida = "";
                }

                /*
                SI EL MENSAJE ES CORTO
                O INCOMPLETO,
                USAR CONTEXTO ANTERIOR
                */
                if (
                        texto.split("\\s+").length <= 4
                ) {

                mensaje =
                        ultimoContexto + " " + mensaje;
                }
                /*
                SI EL MENSAJE ES INCOMPLETO,
                USAR CONTEXTO ANTERIOR
                */
                boolean usarContexto = false;

                /*
                PALABRAS GENERICAS
                */
                if (
                        texto.contains("pulgada")
                        ||
                        texto.contains("monitor")
                        ||
                        texto.contains("mouse")
                        ||
                        texto.contains("teclado")
                        ||
                        texto.contains("audifono")
                        ||
                        texto.contains("laptop")
                        ||
                        texto.contains("pc")
                ) {

                usarContexto = true;
                }

                /*
                USAR EL CONTEXTO
                */
                if (
                        usarContexto
                        &&
                        ultimoContexto != null
                        &&
                        !ultimoContexto.isBlank()
                ) {

                texto =
                        ultimoContexto + " " + texto;
                }
                /*
                ANTES: SE TOMABA LA PRIMERA MARCA QUE HACIA MATCH
                SEGUN EL ORDEN DE productoDAO.listarActivos(), SIN
                CONSIDERAR QUE "texto" YA TRAE MEZCLADO EL CONTEXTO
                ANTERIOR (ej. LA MARCA DEL PRODUCTO YA MOSTRADO)
                JUNTO CON LO QUE EL CLIENTE ACABA DE ESCRIBIR. ESO
                PODIA HACER QUE SE ELIGIERA LA MARCA VIEJA EN VEZ
                DE LA NUEVA.

                AHORA: SE ELIGE LA MARCA QUE APARECE MAS AL FINAL
                DEL TEXTO (LA MENCIONADA MAS RECIENTEMENTE).
                */
                if (pideOtraMarca) {
                /*
                MIENTRAS NO APAREZCA UNA MARCA NUEVA EXPLICITA
                EN EL TEXTO, ultimaMarca SE QUEDA VACIA PARA QUE
                NO SE REINYECTE MAS ABAJO. SI EL BUCLE DE ABAJO
                ENCUENTRA UNA MARCA NUEVA ESCRITA POR EL CLIENTE,
                ESA SI SE USA (Y marcaExcluida DEJA DE APLICAR).
                */
                ultimaMarca = "";
                }

                int mejorPosicionMarcaApi = -1;

                for (Producto p : productoDAO.listarActivos()) {

                        if (p.getMarca() == null) {
                                continue;
                        }

                        String marca =
                                p.getMarca().toLowerCase();

                        if (marca.isBlank()) {
                                continue;
                        }

                        /*
                        IGNORAR PRODUCTOS CUYA "marca" ES EN
                        REALIDAD UN DATO MAL CARGADO IGUAL A SU
                        PROPIA CATEGORIA (ej. SILLAS CON
                        marca="Silla", categoria="Silla"). SI NO,
                        CUALQUIER MENSAJE QUE MENCIONE ESA
                        CATEGORIA SE INTERPRETA TAMBIEN COMO SI
                        PIDIERA ESA "MARCA".
                        */
                        if (
                                p.getCategoria() != null &&
                                marca.equalsIgnoreCase(
                                        p.getCategoria().trim().toLowerCase()
                                )
                        ) {
                                continue;
                        }

                        int posicion = ultimaPosicion(texto, marca);

                        if (posicion > mejorPosicionMarcaApi) {

                                mejorPosicionMarcaApi = posicion;
                                ultimaMarca = marca;

                                if (
                                        marcaExcluida.equalsIgnoreCase(marca)
                                ) {
                                /*
                                LA UNICA MARCA QUE APARECE EN EL
                                TEXTO ES JUSTO LA QUE SE QUIERE
                                EXCLUIR (ARRASTRADA DE OTRO LADO,
                                NO ESCRITA A PROPOSITO POR EL
                                CLIENTE EN ESTE MENSAJE), ASI QUE
                                NO CUENTA COMO "MARCA NUEVA".
                                */
                                ultimaMarca = "";
                                } else {
                                marcaExcluida = "";
                                }
                        }
                }

                if (
                        !ultimaCategoria.isEmpty()
                        &&
                        !texto.contains(ultimaCategoria)
                ) {

                mensaje =
                        ultimaCategoria + " " + mensaje;
                }
                if (
                        !ultimaMarca.isEmpty()
                        &&
                        !texto.contains(ultimaMarca)
                ) {

                mensaje =
                        mensaje + " " + ultimaMarca;
                }
                RespuestaChat chat = new RespuestaChat(); 
                String respuesta = 
                        procesarMensaje(mensaje); 
                chat.setRespuesta(respuesta); 

                /*
                USA EL MISMO PRODUCTO QUE procesarMensaje()
                YA ENCONTRÓ Y USÓ PARA ARMAR "respuesta".
                NO SE VUELVE A BUSCAR CON OTRO TEXTO, PARA
                QUE nombre/imagen/precio SIEMPRE COINCIDAN
                CON LO QUE DICE EL MENSAJE.
                */
                Producto producto = ultimoProductoEncontrado;

                if (producto != null) {

                /*
                ANTES: SE GUARDABA producto.getNombre()/getCategoria()/
                getMarca() TAL CUAL VIENEN DE LA BASE DE DATOS (ej.
                "SILLA GAMER"), MIENTRAS QUE LAS COMPARACIONES DE MAS
                ABAJO (ej. !texto.contains(ultimaCategoria)) ASUMEN
                TODO EN MINUSCULAS. AL NO COINCIDIR MAYUSCULAS/
                MINUSCULAS, EL SISTEMA CREIA QUE LA CATEGORIA/MARCA
                TODAVIA NO ESTABA EN EL TEXTO Y LA VOLVIA A PEGAR EN
                CADA TURNO, DUPLICANDO EL CONTEXTO UNA Y OTRA VEZ
                (ej. "silla gamer" REPETIDO VARIAS VECES EN EL MISMO
                MENSAJE). ESO INFLABA EL PUNTAJE POR NOMBRE DEL
                PRODUCTO ANTERIOR (CADA PALABRA REPETIDA SUMA PUNTOS
                DE NUEVO) Y TERMINABA GANANDO SIEMPRE EL MISMO
                PRODUCTO YA MOSTRADO, IGNORANDO ATRIBUTOS NUEVOS
                COMO UN COLOR DISTINTO.

                AHORA: SE GUARDA TODO EN MINUSCULAS Y SIN CONCATENAR
                "null" CUANDO ALGUN CAMPO VIENE VACIO.
                */
                StringBuilder contexto = new StringBuilder();

                if (producto.getNombre() != null) {
                        contexto.append(producto.getNombre());
                }

                if (producto.getCategoria() != null) {
                        contexto.append(" ").append(producto.getCategoria());
                }

                if (producto.getMarca() != null) {
                        contexto.append(" ").append(producto.getMarca());
                }

                ultimoContexto = contexto.toString().toLowerCase();

                if (producto.getMarca() != null) {
                        ultimaMarca = producto.getMarca().toLowerCase();
                }

                if (producto.getCategoria() != null) {
                        ultimaCategoria = producto.getCategoria().toLowerCase();
                }

                chat.setNombreProducto(
                        producto.getNombre()
                );

                chat.setImagen(
                        producto.getImagen()
                );

                chat.setPrecio(
                        producto.getPrecio().toString()
                );

                } else {

                ultimoContexto = texto;

                }
                
                return chat;
        }
}