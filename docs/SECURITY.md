# Politica de seguridad - PlayStop Backend

## Modelo de seguridad

PlayStop usa JWT (JSON Web Tokens) para autenticacion sin estado, firmados con
HMAC-SHA256 (`jwt.expiration`, actualmente 8 horas). El token viaja como
cookie httpOnly + Secure + SameSite=None (`playstop_token`), nunca en el
cuerpo JSON de las respuestas, para que un XSS en el frontend no pueda leerlo
con JavaScript. Como alternativa para clientes que no manejan cookies
(Postman, integraciones), también se acepta `Authorization: Bearer <token>`.

Cada usuario tiene un `tokenVersion`: cambiar la contraseña, resetearla o
cerrar sesión lo incrementa, invalidando de inmediato cualquier token emitido
antes de ese momento.

## Endpoints publicos

- POST /api/auth/register/player
- POST /api/auth/register/owner
- POST /api/auth/login
- POST /api/auth/google
- POST /api/auth/forgot-password
- POST /api/auth/reset-password
- GET  /api/courts, /api/courts/** (solo lectura)
- GET  /api/reviews/court/**
- GET  /api/match
- GET  /uploads/**
- POST /api/payments/webhook (verificado por firma Stripe, no por JWT)
- /ws/** (handshake WebSocket; la sesión igual se autentica por cookie/JWT,
  ver WsAuthHandshakeHandler)

## Endpoints protegidos

Todos los demás requieren sesión válida (cookie `playstop_token` o header
`Authorization: Bearer <token>`).

## Autorizacion por rol

- **USER**: reservas propias, chat de sus reservas, reseñas propias, amigos,
  matchmaking.
- **OWNER**: canchas y reservas de sus propias canchas, analíticas propias,
  retiros de su cuenta.
- **ADMIN**: gestión global (`/api/admin/**`), moderación de chat, retiros.

Todas las operaciones de escritura sobre un recurso específico (reserva,
cancha, chat) verifican en el servicio que el usuario autenticado es el dueño
o participante de ese recurso — el rol por sí solo no basta para acceder a
recursos de otro usuario.

## WebSocket (chat y notificaciones en tiempo real)

El handshake de `/ws` se autentica igual que el resto de la API (cookie o
header). Además, cada suscripción (`SUBSCRIBE`) a un topic privado
(`/topic/chat/{reservationId}...`, `/topic/notifications/{userId}`) se valida
contra el recurso concreto: solo el jugador y el propietario de una reserva
pueden suscribirse a su chat, y solo cada usuario puede suscribirse a su
propio canal de notificaciones.

## Reset de contraseña

Código de 6 dígitos enviado por email, válido 15 minutos, de un solo uso.
Limitado por IP (`RateLimitingFilter`) y además por email objetivo: tras
varios códigos incorrectos seguidos para el mismo correo hay que solicitar
uno nuevo, para frenar fuerza bruta distribuida entre varias IPs.

## Login con Google

El ID token se valida contra el endpoint de Google y se comprueba que el
campo `aud` coincida con nuestro `GOOGLE_CLIENT_ID`, para no aceptar tokens
legítimos emitidos para otras aplicaciones.

## Reporte de vulnerabilidades

Contactar a: <principemerinosheila@gmail.com>
