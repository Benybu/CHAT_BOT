<%@ page import="java.util.*,com.chatbot.model.*,java.text.DecimalFormat" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    String rol = (String) request.getAttribute("rol");
    if (rol == null) rol = "LECTOR";
    boolean esAdmin = "ADMIN".equalsIgnoreCase(rol);
    String usuarioActual = (String) request.getAttribute("usuarioActual");

    List<Producto> productos = (List<Producto>) request.getAttribute("productos");
    if (productos == null) productos = new ArrayList<>();

    List<Respuesta> respuestas = (List<Respuesta>) request.getAttribute("respuestas");
    if (respuestas == null) respuestas = new ArrayList<>();

    List<Mensaje> mensajes = (List<Mensaje>) request.getAttribute("mensajes");
    if (mensajes == null) mensajes = new ArrayList<>();

    Integer totalProductos = (Integer) request.getAttribute("totalProductos");
    Integer totalRespuestas = (Integer) request.getAttribute("totalRespuestas");
    Integer totalMensajes = (Integer) request.getAttribute("totalMensajes");
    if (totalProductos == null) totalProductos = 0;
    if (totalRespuestas == null) totalRespuestas = 0;
    if (totalMensajes == null) totalMensajes = 0;

    DecimalFormat df = new DecimalFormat("#,##0.00");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Panel - Chatbot Marketplace</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/estilos.css?v=green2">
</head>
<body>
<div class="topbar">
    <div>
        <h1>Panel MVC - Chatbot para Marketplace</h1>
        <p>Usuario: <strong><%= usuarioActual %></strong> | Rol: <strong><%= rol %></strong></p>
    </div>
    <div>
        <% if (request.getSession(false) != null && request.getSession(false).getAttribute("usuario") != null) { %>
            <a class="btn" href="LogoutServlet">Cerrar sesión</a>
        <% } else { %>
            <a class="btn" href="login.jsp">Iniciar sesión</a>
        <% } %>
    </div>
</div>

<div class="cards">
    <div class="card">
        <h3>Productos</h3>
        <p><%= totalProductos %></p>
    </div>
    <div class="card">
        <h3>Respuestas</h3>
        <p><%= totalRespuestas %></p>
    </div>
    <div class="card">
        <h3>Mensajes procesados</h3>
        <p><%= totalMensajes %></p>
    </div>
</div>

<div class="grid">
    <div class="panel">
        <h2>Chatbot de prueba</h2>
        <form class="chat-form" action="ChatbotServlet" method="post" target="_blank">
            <input type="hidden" name="canal" value="WEB">
            <input type="text" name="mensaje" placeholder="Escribe: tienen monitor de 24?" required>
            <button type="submit">Responder</button>
        </form>
        <p class="small">Este endpoint es el que luego puedes conectar con Webhook de Meta/Messenger.</p>
    </div>

    <div class="panel">
        <h2>Modo lectura</h2>
        <p>Este panel puede verse sin permisos de administración. Solo muestra productos, respuestas y actividad.</p>
    </div>
</div>

<div class="panel">
    <details class="collapsible">
        <summary>
            <h2>Productos disponibles</h2>
            <div class="summary-meta">
                <span class="chip-count"><%= totalProductos %> productos</span>
                <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
            </div>
        </summary>
        <div class="collapsible-body">
    <div class="table-responsive">
    <table>
        <tr>
            <th>Imagen</th>
            <th>Nombre</th>
            <th>Categoría</th>
            <th>Marca</th>
            <th>Precio</th>
            <th>Stock</th>
            <th>Descripción</th>
            <th>Tags IA</th>
            <% if (esAdmin) { %>
                <th>Acciones</th>
            <% } %>
        </tr>
        <% for (Producto p : productos) { %>
        <tr>
            <td>
                <img
                    src="<%= p.getImagen() %>"
                    width="80"
                    height="80"
                    style="
                        border-radius:10px;
                        object-fit:cover;
                    "
                >
            </td>
            <td><%= p.getNombre() %></td>
            <td>
                <%= p.getCategoria() != null
                    ? p.getCategoria()
                    : "-" %>
            </td>
            <td>
                <%= p.getMarca() != null
                    ? p.getMarca()
                    : "-" %>
            </td>
            <td>S/ <%= df.format(p.getPrecio()) %></td>
            <td><%= p.getStock() %></td>
            <td><%= p.getDescripcion() %></td>
            <td>
                <%= p.getTags() != null
                    ? p.getTags()
                    : "-" %>
            </td>
            <% if (esAdmin) { %>
            <td>
                <form action="admin/productos" method="post" style="display:inline;">
                    <input type="hidden" name="accion" value="eliminar">
                    <input type="hidden" name="id" value="<%= p.getId() %>">
                    <button type="submit">Eliminar</button>
                </form>
            </td>
            <% } %>
        </tr>
        <% } %>
    </table>
    </div>
        </div>
    </details>

</div>

<div class="panel">
    <h2>Respuestas automáticas</h2>
    <div class="table-responsive">
    <table>
        <tr>
            <th>Palabras clave</th>
            <th>Respuesta</th>
            <% if (esAdmin) { %><th>Acciones</th><% } %>
        </tr>
        <% for (Respuesta r : respuestas) { %>
        <tr>
            <td><%= r.getPalabrasClave() %></td>
            <td><%= r.getRespuesta() %></td>
            <% if (esAdmin) { %>
            <td>
                <form action="admin/respuestas" method="post" style="display:inline;">
                    <input type="hidden" name="accion" value="eliminar">
                    <input type="hidden" name="id" value="<%= r.getId() %>">
                    <button type="submit">Eliminar</button>
                </form>
            </td>
            <% } %>
        </tr>
        <% } %>
    </table>
    </div>
</div>

<% if (esAdmin) { %>
<div class="grid">
    <div class="panel">
        <h2>Administrar productos</h2>
        <form action="admin/productos" method="post" class="admin-form">
            <input type="text" name="nombre" placeholder="Nombre" required>
            <input type="number" step="0.01" name="precio" placeholder="Precio" required>
            <input type="number" name="stock" placeholder="Stock" required>
            <input type="text" name="descripcion" placeholder="Descripción">
            <select name="categoria" required>
                <option value="">Selecciona categoría</option>
                <option value="Procesador">Procesador</option>
                <option value="Monitor">Monitor</option>
                <option value="Placa Madre">Placa Madre</option>
                <option value="Case Gamer">Case Gamer</option>
                <option value="Tarjeta de Video">Tarjeta de Video</option>
                <option value="Teclado">Teclado</option>
                <option value="Disco Sólido">Disco Sólido</option>
                <option value="Memoria Ram">Memoria Ram</option>
                <option value="USB">USB</option>
                <option value="Fuente de Poder">Fuente de Poder</option>
                <option value="Redes">Redes</option>
                <option value="Estabilizador">Estabilizador</option>
                <option value="Silla Gamer">Silla Gamer</option>
                <option value="Impresora">Impresora</option>
                <option value="Mouse">USB</option>
                <option value="Cooler">Cooler</option>
                <option value="Audifonos">Audifonos</option>
                <option value="Parlantes">Parlantes</option>
                <option value="Rack">Rack</option>
            </select>
            <input type="text" name="marca" placeholder="Marca">
            <textarea 
                name="tags"
                placeholder="Ejemplo: gamer,rgb,streaming,oficina,económico">
            </textarea>
            <input type="text"
            name="imagen"
            placeholder="URL de imagen">
            <button type="submit">Guardar producto</button>
        </form>
        <h2>Importar catálogo</h2>
        <form
        action="admin/importar-excel"
        method="post"
        enctype="multipart/form-data">

            <input
                type="file"
                name="archivo"
                accept=".xlsx"
                required>

            <button type="submit">
                Importar Excel
            </button>

</form>
    </div>

    <div class="panel">
        <h2>Administrar respuestas</h2>
        <form action="admin/respuestas" method="post" class="admin-form">
            <input type="text" name="palabras_clave" placeholder="Palabras clave separadas por coma" required>
            <textarea name="respuesta" placeholder="Respuesta automática" required></textarea>
            <button type="submit">Guardar respuesta</button>
        </form>
    </div>
</div>

<div class="panel">
    <h2>Últimos mensajes</h2>
    <div class="table-responsive">
    <table>
        <tr>
            <th>Entrada</th>
            <th>Respuesta</th>
            <th>Canal</th>
            <th>Fecha</th>
        </tr>
        <% for (Mensaje m : mensajes) { %>
        <tr>
            <td><%= m.getMensajeEntrada() %></td>
            <td><%= m.getRespuestaGenerada() %></td>
            <td><%= m.getCanal() %></td>
            <td><%= m.getFechaRegistro() %></td>
        </tr>
        <% } %>
    </table>
    </div>
</div>
<% } %>
</body>
</html>
