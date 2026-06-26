# Registro de Incidencias - PlayStop Backend

## Formato: Fecha | Descripcion | Causa | Solucion | Estado

## Resueltas

### 2026-06-01 - Timeout de base de datos
Causa: Pool de conexiones agotado
Solucion: Ajustar hikari.idle-timeout a 300000ms
Estado: RESUELTO

### 2026-06-03 - Emails no enviados
Causa: Contrasena de aplicacion Gmail expirada
Solucion: Regenerar contrasena en cuenta Gmail
Estado: RESUELTO
