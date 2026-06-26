# Guia de rendimiento - PlayStop Backend

## Optimizaciones aplicadas

- HikariCP: maximo 5 conexiones, timeout 30s
- Compresion GZIP para respuestas JSON
- show-sql=false en produccion
- Imagen Docker openjdk:17-slim

## Metricas de referencia

- Tiempo de inicio: 30-60 segundos en Render plan gratuito
- Tiempo de respuesta: menos de 200ms en condiciones normales

## Recomendaciones

- Plan de pago en Render para evitar cold starts
- Redis para cache de sesiones y consultas frecuentes
