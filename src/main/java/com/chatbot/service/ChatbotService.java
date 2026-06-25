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

import java.util.List;
import java.util.Random;

public class ChatbotService {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final RespuestaDAO respuestaDAO = new RespuestaDAO();
    private final MensajeDAO mensajeDAO = new MensajeDAO();
    private final ConversacionDAO conversacionDAO = new ConversacionDAO();
    private final OpenAIService openAIService = new OpenAIService();
    private final Random random = new Random();
    private final PersonalidadChatbot personalidad = PersonalidadChatbot.GAMER;

        private Double extraerPresupuesto(String texto) {

        Pattern pattern =
                Pattern.compile("(\\d{2,6})");

        Matcher matcher =
                pattern.matcher(texto);

        if (matcher.find()) {

                return Double.parseDouble(
                        matcher.group(1)
                );
        }

        return null;
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

        String texto = mensaje.toLowerCase();

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
                productoDAO.buscarCoincidencia(texto);
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

                return aplicarPersonalidad(respuesta);
                }
        }
        }

        if (buscaProductoInexistente(texto, producto)) {

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

        String contenido =
                (
                        producto.getNombre() + " " +
                        producto.getCategoria() + " " +
                        producto.getTags()
                ).toLowerCase();

        String[] palabras = texto.split("\\s+");

        int coincidencias = 0;

        for (String palabra : palabras) {

                palabra = palabra.trim();

                if (
                        palabra.length() > 3 &&
                        contenido.contains(palabra)
                ) {
                coincidencias++;
                }
        }

        return coincidencias == 0;
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
                RespuestaChat chat = new RespuestaChat(); 
                String respuesta = 
                        procesarMensaje(mensaje); 
                chat.setRespuesta(respuesta); 
                Producto producto = 
                        productoDAO.buscarCoincidencia(mensaje); 
                if (producto != null) { 
                        chat.setNombreProducto( 
                                producto.getNombre() 
                                ); 
                chat.setImagen( 
                        producto.getImagen() 
                ); 
                chat.setPrecio( 
                        producto.getPrecio().toString() 
                ); 
                } 
                return chat; 
        }
}