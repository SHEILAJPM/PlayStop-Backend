# Politica de seguridad - PlayStop Backend

## Modelo de seguridad

PlayStop usa JWT (JSON Web Tokens) para autenticacion sin estado.
Los tokens se firman con HMAC-SHA256 y expiran en 24 horas.

## Endpoints publicos

- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/forgot
- POST /api/auth/reset
- GET  /api/courts (solo lectura)

## Endpoints protegidos

Todos los demas requieren header: Authorization: Bearer <token>

## Autorizacion por rol

- USER/OWNER: pueden acceder a sus propios recursos
- ADMIN: puede gestionar recursos de su instalacion
- SUPER_ADMIN: acceso total al sistema

## Reporte de vulnerabilidades

Contactar a: principemerinosheila@gmail.com
