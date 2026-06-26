# Runbook de Operaciones - PlayStop Backend

## Verificar estado del servicio

curl https://playstop-backend.onrender.com/actuator/health

Respuesta esperada: { "status": "UP" }

## Reiniciar en Render

Dashboard de Render -> Manual Deploy o Restart Service

## Ver logs

En el dashboard de Render, seccion Logs.
Filtrar por nivel ERROR o WARN para diagnosticar problemas.
