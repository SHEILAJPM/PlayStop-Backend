# Esquema de base de datos - PlayStop

## Relaciones

- users -> courts: Un propietario puede tener muchas canchas (1:N)
- users -> reservations: Un usuario puede tener muchas reservas (1:N)
- courts -> reservations: Una cancha puede tener muchas reservas (1:N)
- reservations -> payments: Una reserva tiene un pago (1:1)
- courts -> reviews: Una cancha puede tener muchas resenas (1:N)

## Configuracion JPA

ddl-auto: update (actualiza el esquema automaticamente)
dialect: PostgreSQLDialect
show-sql: false (silencioso en produccion)

## Indices recomendados

- users(email): unico, para login rapido
- courts(disponible, deporte): para filtrado
- reservations(court_id, fecha): para verificar disponibilidad
- password_reset_tokens(token): para validacion rapida
