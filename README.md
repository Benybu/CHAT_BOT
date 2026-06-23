# Chatbot MVC para Facebook Marketplace

Proyecto Java Web con MVC, roles `ADMIN` / `LECTOR`, Docker y conexión a MySQL en Aiven.

## Variables de entorno

Configura estas variables en Render:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

## Configuración en Aiven

1. Crea el servicio MySQL en Aiven.
2. Crea la base `chatbot_marketplace`.
3. Ejecuta `db/schema.sql`.
4. Copia los datos de conexión en Render.

## Compilar WAR

```bash
mvn clean package
```

El archivo final queda en `target/chatbot-mvc.war`.

## Acceso de prueba

- Administrador: `admin / admin123`
- Lector: `lector / lector123`
