# Documentacion de la API - PlayStop Backend

## Reservas

### POST /api/reservations
Crea una nueva reserva para una cancha.

Request body:
{
  "courtId": 1,
  "fecha": "2026-06-15",
  "horaInicio": "10:00",
  "duracionMinutos": 60
}

Response 201:
{
  "id": 42,
  "estado": "CONFIRMED",
  "qrCode": "base64...",
  "total": 50.00
}

Errores posibles:
- 400: Franja horaria no disponible
- 403: Sin permiso para reservar
- 404: Cancha no encontrada
