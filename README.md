# PlayStop Backend

> Plataforma de reservas deportivas — API REST

API REST construida con Spring Boot que gestiona autenticación, canchas, reservas,
pagos, matchmaking, referidos y notificaciones para la plataforma PlayStop.

## Stack tecnológico

| Componente | Tecnología |
|------------|------------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 4.0 (Web, Security, Data JPA, Validation) |
| Seguridad | Spring Security + JWT |
| Persistencia | Hibernate + PostgreSQL (Neon) |
| Email | Brevo (API HTTP) |
| Pagos | Stripe (Checkout Sessions + Webhooks) |
| Notificaciones | WhatsApp Business API (Twilio) + Firebase Cloud Messaging |
| Imágenes | Cloudinary |
| Build | Maven Wrapper (no requiere instalar Maven aparte) |
| Deploy | Docker + Render |

## Requisitos previos

- **Java 17** ([Temurin/Adoptium](https://adoptium.net/) o cualquier JDK 17)
- **Git**
- Una base de datos Postgres — puedes crear una gratis en [neon.tech](https://neon.tech)
- Maven **no** hace falta instalarlo: el proyecto trae `mvnw`/`mvnw.cmd` (Maven Wrapper)

## Cómo clonar y ejecutar en local

```bash
git clone https://github.com/SHEILAJPM/PlayStop-Backend.git
cd PlayStop-Backend
```

Crea el archivo `src/main/resources/application-local.properties` (está en `.gitignore`, no se sube al repo) con al menos la base de datos y el JWT — ver la sección [Variables de entorno](#variables-de-entorno) más abajo para el resto.

```bash
# Linux / Mac
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Por defecto corre en `http://localhost:8080` con el perfil `local` (`spring.profiles.active` por defecto).

## Variables de entorno

En producción (Render) estas se configuran como **Environment Variables** del servicio. En local van en `application-local.properties` (formato `clave=valor`, sin el `${...}`).

### Configuradas actualmente (las que ya tiene el proyecto en Render)

| Variable | Para qué sirve | Dónde conseguirla |
|---|---|---|
| `DB_URL` | Cadena de conexión JDBC a Postgres | [neon.tech](https://neon.tech) → tu proyecto → *Connection string* (formato `jdbc:postgresql://...`) |
| `DB_USERNAME` | Usuario de la base de datos | Mismo panel de Neon |
| `DB_PASSWORD` | Contraseña de la base de datos | Mismo panel de Neon |
| `JWT_SECRET` | Clave para firmar los tokens de sesión | Invéntala tú: cualquier string aleatorio de 32+ caracteres |
| `BREVO_API_KEY` | Envío de correos (bienvenida, códigos, confirmaciones) vía API HTTP | [brevo.com](https://www.brevo.com) → regístrate → verifica un correo remitente en **Senders & IP → Senders** → genera la key en **SMTP & API → API Keys** (la que empieza `xkeysib-`, **no** la de la pestaña "SMTP") |
| `MAIL_FROM` | Correo remitente que aparece en los emails | Debe ser el mismo correo que verificaste como sender en Brevo |
| `STRIPE_SECRET_KEY` | Autenticación server-to-server con Stripe para cobrar reservas | [dashboard.stripe.com](https://dashboard.stripe.com) (modo **prueba**, sin RUC ni URL para empezar) → **Desarrolladores → Claves API** → "Clave secreta" (`sk_test_...`) |
| `STRIPE_WEBHOOK_SECRET` | Verifica que las notificaciones de pago vengan realmente de Stripe | Stripe → **Desarrolladores → Webhooks → Añadir endpoint**, URL: `<tu-backend>/api/payments/webhook`, eventos `checkout.session.completed`, `checkout.session.expired`, `customer.subscription.updated` y `customer.subscription.deleted` → copia el "Signing secret" (`whsec_...`) |
| `STRIPE_PRICE_PRO` | Precio recurrente del Plan Pro (S/ 99/mes) | Stripe → **Catálogo de productos → Añadir producto** "PlayStop Pro", precio recurrente mensual → copia el ID del precio (`price_...`) |
| `STRIPE_PRICE_ENTERPRISE` | Precio recurrente del Plan Enterprise (S/ 199/mes) | Igual que el anterior, pero producto "PlayStop Enterprise" |
| `SPRING_PROFILES_ACTIVE` | Perfil activo (`prod` en Render) | Se pone `prod` en Render; en local no hace falta, usa `local` por defecto |

### Opcionales (features que aún no están activas — no hace falta configurarlas)

| Variable | Para qué sirve | Dónde conseguirla |
|---|---|---|
| `FRONTEND_URL` | A dónde redirige Stripe tras el pago (ya trae un default correcto) | URL pública de tu frontend desplegado |
| `APP_BASE_URL` | URL pública de este backend (ya trae un default correcto) | La que te da Render al desplegar |
| `PORT` | Puerto del servidor (default `8080`) | — |
| `UPLOAD_DIR` | Carpeta para archivos subidos | — |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` | Notificaciones por WhatsApp | [twilio.com](https://www.twilio.com) → Console → Account Info |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | Subida/hosting de imágenes de canchas | [cloudinary.com](https://cloudinary.com) → Dashboard |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Login con Google | [console.cloud.google.com](https://console.cloud.google.com) → Credenciales OAuth 2.0 |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Notificaciones push (FCM) | Firebase Console → Configuración del proyecto → Cuentas de servicio → Generar clave privada (pegar el JSON completo como valor) |

Ejemplo mínimo de `application-local.properties` para arrancar solo con base de datos (sin email ni pagos):

```properties
spring.datasource.url=jdbc:postgresql://tu-host.neon.tech/neondb?sslmode=require
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
jwt.secret=cambia-esto-por-algo-aleatorio-de-32-caracteres-o-mas
```

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
│   ├── PaymentController     # Checkout y webhook de Stripe
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
    ├── PaymentService        # Checkout Sessions y webhook de Stripe
    └── WhatsAppService       # Envío de notificaciones por WhatsApp
```

## Módulos principales

- **Autenticación** — registro, login y recuperación de contraseña con JWT
- **Canchas** — CRUD completo con fotos, horarios y disponibilidad en tiempo real
- **Reservas** — creación, confirmación, cancelación y verificación por QR
- **Pagos** — la reserva queda `PENDING` hasta que Stripe confirma el cobro (Checkout Session + webhook); recién ahí se envían las notificaciones
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
| POST | `/api/reservations` | Crear reserva (queda `PENDING`) |
| GET | `/api/reservations/{id}/qr` | Obtener QR de reserva |
| POST | `/api/payments/checkout/{reservationId}` | Crear sesión de pago de Stripe |
| POST | `/api/payments/webhook` | Webhook de Stripe (confirma/cancela el pago) |
| GET | `/api/matchslots` | Listar partidos abiertos |
| POST | `/api/matchslots` | Crear partido abierto |
| POST | `/api/referrals/apply` | Aplicar código de referido |
| GET | `/api/referrals/my-code` | Ver código personal |

---
PlayStop © 2026 — Sheila JPM
