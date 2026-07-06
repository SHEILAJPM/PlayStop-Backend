# PlayStop Backend

> Plataforma de reservas deportivas — API REST

API REST construida con Spring Boot que gestiona autenticación, canchas, reservas,
pagos, matchmaking, referidos y notificaciones para la plataforma PlayStop.

## Stack tecnológico

| Componente | Tecnología |
|------------|------------|
| Framework | Spring Boot 4.0 / Java 17 |
| Seguridad | Spring Security + JWT |
| Persistencia | JPA + Hibernate + PostgreSQL |
| Email | Brevo (API HTTP) |
| Notificaciones | WhatsApp Business API |
| Build | Maven Wrapper |
| Deploy | Docker + Render |

## Ejecución local

```bash
# Linux / Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Requiere un archivo `application-local.properties` con las credenciales de base de datos y servicios externos.

## Estructura de paquetes

```
com.playstop.backend/
├── config/          # SecurityConfig, CORS, beans globales
├── controller/      # Endpoints REST por dominio
│   ├── AuthController
│   ├── CourtController
│   ├── ReservationController
│   ├── ReviewController
│   ├── UserController
│   ├── MatchSlotController   # Partidos abiertos (matchmaking)
│   └── ReferralController    # Sistema de referidos
├── dto/
│   ├── request/     # Payloads de entrada
│   └── response/    # Payloads de salida
├── entity/          # Entidades JPA
│   ├── User
│   ├── Court
│   ├── Reservation
│   ├── Review
│   ├── MatchSlot             # Partido abierto
│   ├── MatchSlotParticipant  # Jugadores inscritos al partido
│   └── Referral              # Registro de referidos
├── enums/           # Estados y tipos (roles, deportes, etc.)
├── exception/       # Manejo global de errores
├── repository/      # Interfaces Spring Data JPA
├── security/        # JWT filter, UserDetailsService
└── service/         # Lógica de negocio
    ├── CourtService
    ├── ReservationService
    ├── ReviewService
    ├── MatchSlotService      # Lógica de matchmaking
    ├── ReferralService       # Generación y aplicación de códigos
    ├── ReminderScheduler     # Recordatorios automáticos programados
    └── WhatsAppService       # Envío de notificaciones por WhatsApp
```

## Módulos principales

- **Autenticación** — registro, login y recuperación de contraseña con JWT
- **Canchas** — CRUD completo con fotos, horarios y disponibilidad en tiempo real
- **Reservas** — creación, confirmación, cancelación y verificación por QR
- **Reseñas** — valoración con estrellas y comentario por reserva completada
- **Matchmaking** — creación de partidos abiertos y gestión de participantes
- **Referidos** — generación de código único, aplicación y registro de beneficios
- **Notificaciones** — recordatorios por email y WhatsApp antes de la reserva
- **Gamificación** — puntos y logros por actividad en la plataforma

## Endpoints principales

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/register` | Registro de usuario |
| POST | `/api/auth/login` | Inicio de sesión |
| GET | `/api/courts` | Listar canchas con filtros |
| POST | `/api/reservations` | Crear reserva |
| GET | `/api/reservations/{id}/qr` | Obtener QR de reserva |
| GET | `/api/matchslots` | Listar partidos abiertos |
| POST | `/api/matchslots` | Crear partido abierto |
| POST | `/api/referrals/apply` | Aplicar código de referido |
| GET | `/api/referrals/my-code` | Ver código personal |

---
PlayStop © 2026 — Sheila JPM
