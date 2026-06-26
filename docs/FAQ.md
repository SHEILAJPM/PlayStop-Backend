# Preguntas Frecuentes - PlayStop Backend

## Como resetear la base de datos en desarrollo?
Cambiar spring.jpa.hibernate.ddl-auto=create y reiniciar la aplicacion.

## Por que los emails no llegan?
Verificar que MAIL_PASSWORD sea la contrasena de APP de Gmail.

## Como agregar un nuevo rol?
1. Agregar el valor en el enum Role.java
2. Actualizar SecurityConfig con las nuevas rutas y permisos

## Como cambiar el tiempo de expiracion del JWT?
Modificar jwt.expiration en application.properties (en milisegundos).
