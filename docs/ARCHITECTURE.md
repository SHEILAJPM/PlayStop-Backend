# Arquitectura del Backend - PlayStop

## Vision general

El backend sigue una arquitectura en capas (layered architecture) con Spring Boot.
Cada capa tiene una responsabilidad clara y unica.

## Flujo de una peticion tipica

1. HTTP Request -> JwtAuthenticationFilter (valida token)
2. -> SecurityContext (establece usuario autenticado)
3. -> Controller (recibe y valida DTO de entrada)
4. -> Service (logica de negocio y transacciones)
5. -> Repository (consulta a PostgreSQL)
6. -> Entity -> DTO de respuesta
7. HTTP Response (JSON)

## Patrones utilizados

- Repository Pattern: acceso a datos desacoplado
- DTO Pattern: separacion entre dominio y API
- Filter Pattern: autenticacion transversal
- Template Method: plantillas de email reutilizables

## Decisiones de diseno

- JWT sin estado para escalabilidad horizontal
- BCrypt factor 12 por seguridad en contrasenas
- HikariCP optimizado para plan gratuito de Render
- Jackson configurado para fechas ISO-8601
