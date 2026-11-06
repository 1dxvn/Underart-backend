# Changelog

Todos los cambios relevantes de este proyecto se documentan aquí.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa
[versionado semántico](https://semver.org/lang/es/).

## [Sin publicar]

### Añadido
- Sesiones con access token de 15 minutos y refresh token rotativo (`/auth/refresh`, `/auth/logout`),
  con detección de reutilización y limpieza diaria de tokens caducados.
- Bloqueo temporal del login tras 5 intentos fallidos y límite de 10 usos de IA por hora (429 con `Retry-After`).
- Política de contraseñas (al menos una letra y un número).
- Notificaciones dentro de la app: puja superada, subasta ganada o vendida, subasta sin pujas, resultado
  del pago, envío, entrega y reseñas nuevas.
- Favoritos de obras, con contador en el detalle.
- Envío de órdenes: el vendedor marca enviado (transportista y guía) y el comprador confirma la entrega.
- Reseñas de vendedores tras la entrega y valoración media en el perfil público.
- Estadísticas de vendedor y comprador en `/users/me/stats`.
- Incremento mínimo de puja (5 %, mínimo 1.00) y prórroga de 2 minutos ante pujas de último momento.
- Listados de subastas con título, portada, precio actual y número de pujas; detalle de obra con galería.
- Filtros por rango de precio y ordenamiento en obras y subastas.
- Pagos con Stripe en modo de prueba, seleccionables con `PAYMENT_PROVIDER`.
- Catálogo público de obras y subastas sin necesidad de sesión.
- `X-Request-Id` en respuestas y logs; errores con ruta, `requestId` y detalle por campo.
- `/actuator/info` con versión y fecha de compilación; OpenAPI documentado por áreas.
- Maven Wrapper, cobertura con JaCoCo (mínimo 80 % de líneas y 70 % de ramas), reglas de arquitectura con
  ArchUnit, Dependabot, `docker-compose` para desarrollo y registros de decisiones (ADR).

### Cambiado
- El login devuelve `accessToken`, `refreshToken`, `tokenType` y `expiresIn` en lugar de `token`.
- `GET /artworks/{id}` y `GET /auctions/{id}` devuelven un objeto de detalle (`{artwork, media, ...}` y
  `{auction, highestBid, minimumNextBid}`).
- La primera puja puede igualar el precio base; las siguientes deben respetar el incremento mínimo.
- El número de guía de una orden lo informa el vendedor al enviarla (antes se generaba al crearla).
- La imagen Docker se compila con el Maven Wrapper.

### Seguridad
- Los JWT incluyen y validan emisor, audiencia, tipo e id.
- Cambiar la contraseña cierra todas las sesiones del usuario.

## [0.1.0] - 2026-10-09

### Añadido
- API REST con arquitectura hexagonal: registro, login con JWT, obras con valoración por IA (Groq con
  respaldo en OpenAI), subastas, pujas, compra inmediata y órdenes.
- Persistencia en PostgreSQL y MongoDB; despliegue con Docker en Render.
