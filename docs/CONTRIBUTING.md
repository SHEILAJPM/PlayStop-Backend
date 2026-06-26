# Guia de contribucion al Backend de PlayStop

## Principios de desarrollo

1. Seguir el patron de capas: Controller -> Service -> Repository
2. Usar DTOs para request y response (nunca exponer entidades directamente)
3. Validar datos de entrada con anotaciones de Bean Validation
4. Manejar errores en GlobalExceptionHandler
5. Documentar metodos publicos con comentarios descriptivos en espanol

## Tests

- Agregar tests unitarios para cada servicio nuevo
- Usar @MockBean para dependencias externas en tests
- Nombrar tests en espanol: debeCrearReservaExitosamente()

## Revisiones de codigo

- El codigo debe compilar sin errores ni warnings
- Los tests deben pasar al 100%
- Los mensajes de error de la API deben estar en espanol
