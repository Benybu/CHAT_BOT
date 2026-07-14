<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Login - Chatbot Marketplace</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600&family=JetBrains+Mono:wght@500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="css/estilos.css?v=green2">
</head>
<body class="login-body">
    <div class="login-card">
        <h1>Chatbot Marketplace</h1>
        <p>Acceso administrador o lector</p>

        <%
            String error = request.getParameter("error");
            if ("1".equals(error)) {
        %>
            <div class="alert">Usuario o contraseña incorrectos.</div>
        <% } %>

        <form action="LoginServlet" method="post">
            <label>Usuario</label>
            <input type="text" name="usuario" required>

            <label>Contraseña</label>
            <input type="password" name="password" required>

            <button type="submit">Ingresar</button>
        </form>

        <div class="hint">
            Demo: <strong>admin / admin123</strong> o <strong>lector / lector123</strong>
        </div>
    </div>
</body>
</html>
