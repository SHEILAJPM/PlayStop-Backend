# Guia de despliegue - PlayStop Backend

## Variables de entorno en Render

Configurar las siguientes en el dashboard de Render:

DB_URL           -> URL de conexion a PostgreSQL (Neon Tech)
DB_USERNAME      -> Usuario de la base de datos
DB_PASSWORD      -> Contrasena de la base de datos
JWT_SECRET       -> Clave secreta para firmar tokens JWT (min 256 bits)
MAIL_USERNAME    -> Cuenta de Gmail para envio de emails
MAIL_PASSWORD    -> Contrasena de aplicacion de Gmail
MAIL_FROM        -> Direccion de correo del remitente
APP_BASE_URL     -> URL publica del backend desplegado
UPLOAD_DIR       -> Directorio para archivos subidos

## Notas importantes
- El JWT_SECRET debe ser aleatorio y seguro
- MAIL_PASSWORD es la contrasena de aplicacion, no la de Gmail
- APP_BASE_URL se usa para generar enlaces en los correos
