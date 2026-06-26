# Guia de pruebas - PlayStop Backend

## Tipos de pruebas

### Tests unitarios
Prueban servicios y utilidades de forma aislada con mocks.
Ubicacion: src/test/java/com/playstop/backend/service/

### Tests de integracion
Prueban endpoints REST con contexto Spring completo.
Ubicacion: src/test/java/com/playstop/backend/controller/

## Ejecutar pruebas

./mvnw test                    # Todos los tests
./mvnw test -Dtest=AuthService # Solo una clase

## Prueba manual con Postman

1. Importar coleccion: postman/PlayStop.postman_collection.json
2. Importar entorno: postman/PlayStop.postman_environment.json
3. Ejecutar primero POST /auth/register para obtener token
4. El token se guarda automaticamente en la variable de entorno

## Endpoints de prueba rapida

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@playstop.com","password":"Admin123"}'
