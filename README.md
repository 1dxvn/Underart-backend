# Underart Backend

API REST de un marketplace de subastas de arte. Los artistas publican obras con imágenes y video,
la IA sugiere un precio y etiquetas, y los compradores pujan o compran al instante. Cuando una subasta
termina, el sistema la cierra solo, crea la orden del ganador y acompaña el pago, el envío, la entrega y
la reseña, avisando a cada parte con notificaciones.

## Stack

- Java 17, Spring Boot 3.3, Maven Wrapper
- PostgreSQL (JPA + Flyway) para el negocio y MongoDB para actividad, historial de IA y notificaciones
- Spring Security con JWT de 15 minutos y refresh token rotativo
- Groq como proveedor de IA principal y OpenAI como respaldo
- Cloudflare R2 (compatible con S3) para imágenes y video
- Stripe (modo de prueba) o pago simulado
- Arquitectura hexagonal verificada con ArchUnit ([ADR 0001](docs/adr/0001-arquitectura-hexagonal.md))
- Docker, Render, GitHub Actions y Dependabot

## Ejecutar en local

Requisitos: Java 17 y Docker. No hace falta instalar Maven.

```bash
docker compose up -d        # PostgreSQL 16 y MongoDB 7
./mvnw spring-boot:run      # perfil dev con valores por defecto
```

El perfil `dev` apunta a `localhost:5432/underart` (`postgres`/`postgres`) y `localhost:27017/underart`.
Puedes sobrescribir cualquier valor con las variables de `.env.example`; Spring no lee el archivo `.env`
por sí solo, así que expórtalas en tu terminal o en tu IDE. Flyway crea las tablas al arrancar y en `dev`
se crea el usuario `demo@underart.com` / `password123`.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html` (el botón **Authorize** acepta el `accessToken`).
- Ejemplos listos para ejecutar: [docs/api.http](docs/api.http).

## Pruebas y calidad

```bash
./mvnw clean verify
```

- Pruebas unitarias de las reglas de negocio y de los adaptadores.
- `ApiIntegrationTest` levanta PostgreSQL y MongoDB reales con Testcontainers y recorre por HTTP el flujo
  completo: sesión, obras, multimedia, favoritos, subastas, pujas, cierre, pago, envío, entrega, reseñas,
  notificaciones y estadísticas. Se salta sola si no hay Docker.
- `ArchitectureTest` falla si se rompen las reglas de capas.
- El build exige 80 % de cobertura de líneas y 70 % de ramas (`target/site/jacoco/index.html`).

Con Docker Desktop en Mac, Testcontainers necesita encontrar el socket:

```bash
export DOCKER_HOST=unix://$HOME/.docker/run/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## Convenciones de la API

- Todas las rutas están bajo `/api/v1`. Ids en UUID, fechas en ISO-8601 (UTC), dinero con 2 decimales.
- Autenticación con `Authorization: Bearer <accessToken>`.
- Listados paginados con `?page=0&size=20` (máximo 100): `{items, page, size, totalItems, totalPages}`.
- Cada respuesta lleva `X-Request-Id`; también aparece en los logs y en los errores.
- Errores: `{status, error, message, path, requestId, timestamp, fieldErrors}`. Códigos: 400 validación o regla
  de negocio, 401 sin sesión, 402 pago rechazado, 403 no eres el dueño, 404, 405, 409 conflicto,
  413 archivo grande, 429 demasiados intentos (con `Retry-After`), 503 IA o almacenamiento no disponibles.

## Endpoints

### Públicos
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` y `/actuator/health`, `/actuator/info` (raíz) | Estado, salud y versión |
| POST | `/users/register` | Registro |
| POST | `/auth/login` | Login: `accessToken` (15 min) y `refreshToken` |
| POST | `/auth/refresh` | Renovar la sesión (rota el refresh token) |
| POST | `/auth/logout` | Cerrar sesión |
| GET | `/artworks?artistId&technique&tag&q&minPrice&maxPrice&sort` | Catálogo de obras |
| GET | `/artworks/{id}` | Obra con galería y favoritos |
| GET | `/artworks/{id}/media` | Imágenes y videos de una obra |
| GET | `/auctions?status&type&artworkId&artistId&endingBefore&minPrice&maxPrice&sort` | Subastas con título, portada y precio actual |
| GET | `/auctions/{id}` | Detalle con puja más alta y puja mínima |
| GET | `/users/{id}` | Perfil público con valoración media |
| GET | `/users/{id}/reviews` | Reseñas de un vendedor |

Órdenes permitidos: obras `createdAt`, `suggestedPrice`, `title`; subastas `endDate`, `startDate`, `basePrice`,
seguidos de `,asc` o `,desc`.

### Mi cuenta
| Método | Ruta | Descripción |
|---|---|---|
| GET, PUT | `/users/me` | Ver y actualizar mi perfil |
| PUT | `/users/me/password` | Cambiar la contraseña (cierra todas las sesiones) |
| PUT | `/users/me/payment-method` | Registrar el método de pago |
| GET | `/users/me/artworks`, `/auctions`, `/bids`, `/favorites` | Mis obras, subastas, pujas y favoritos |
| GET | `/users/me/orders`, `/users/me/sales` | Mis compras y ventas |
| GET | `/users/me/stats` | Estadísticas de vendedor y comprador |
| GET | `/users/me/activity` | Historial de actividad |
| GET | `/users/me/notifications?unreadOnly` | Notificaciones |
| GET | `/users/me/notifications/unread-count` | Número de no leídas |
| POST | `/users/me/notifications/{id}/read`, `/read-all` | Marcar como leídas |

### Obras y multimedia
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/artworks` | Crear obra (la IA sugiere precio y etiquetas) |
| PUT, DELETE | `/artworks/{id}` | Editar o borrar (solo el autor; borrar exige que no tenga subastas) |
| POST, DELETE | `/artworks/{id}/favorite` | Marcar o quitar de favoritos |
| POST | `/artworks/{id}/media` | Subir imagen o video corto (multipart, campo `file`) |
| POST | `/artworks/{id}/media/upload-url` | URL firmada para subir un video grande directo a R2 |
| POST | `/artworks/{id}/media/{mediaId}/confirm` | Confirmar una subida directa |
| PUT | `/artworks/{id}/media/order` | Reordenar (el primero es la portada) |
| DELETE | `/artworks/{id}/media/{mediaId}` | Borrar un archivo |
| POST | `/ai/valuations` | Tasación con IA sin crear la obra |

Imágenes JPEG, PNG o WEBP de hasta 10 MB; video MP4, WEBM o MOV de hasta 50 MB por multipart o 200 MB por
subida directa; máximo 15 archivos por obra ([ADR 0004](docs/adr/0004-multimedia-en-cloudflare-r2.md)).

### Subastas, pujas y órdenes
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auctions` | Crear subasta (solo el autor de la obra) |
| DELETE | `/auctions/{id}` | Cancelar (solo el autor y sin pujas) |
| GET, POST | `/auctions/{id}/bids` | Historial de pujas y pujar (`{"amount": 150}`) |
| POST | `/auctions/{id}/buy-now` | Compra inmediata |
| GET | `/orders/{id}` | Detalle (solo comprador o vendedor) |
| POST | `/orders/{id}/pay` | Pagar |
| POST | `/orders/{id}/ship` | Marcar enviada con transportista y guía (vendedor) |
| POST | `/orders/{id}/confirm-delivery` | Confirmar la entrega (comprador) |
| POST | `/orders/{id}/review` | Reseñar al vendedor tras la entrega (comprador) |

## Reglas de negocio

- La identidad siempre sale del JWT: nadie actúa a nombre de otro.
- No se puede pujar ni comprar la obra propia, ni pujar fuera de las fechas de la subasta.
- La primera puja puede igualar el precio base; cada puja siguiente debe superar a la más alta en un 5 %
  (mínimo 1.00). Las pujas simultáneas se serializan con un bloqueo de fila sobre la subasta.
- Una puja en los últimos 2 minutos prorroga el cierre 2 minutos.
- Una obra solo puede tener una subasta activa a la vez.
- Cada minuto se cierran las subastas vencidas y se crea la orden del ganador.
- Orden: subtotal + envío (15.00) + comisión del 10 % sobre el subtotal. Ciclo: pago → envío → entrega → reseña.
- Pagos: `PAYMENT_PROVIDER=simulated` (por defecto; los tokens `tok_fail...` se rechazan) o `stripe`.
- Seguridad: 5 intentos fallidos de login bloquean ese email 15 minutos; la IA admite 10 usos por hora por
  usuario ([ADR 0003](docs/adr/0003-sesiones-con-refresh-token-rotativo.md)).

## Documentación

- [Guía de despliegue](docs/DESPLIEGUE.md): Render, Neon (PostgreSQL), MongoDB Atlas, Cloudflare R2 y Stripe.
- [Decisiones de arquitectura](docs/adr/README.md).
- [Guía de contribución](CONTRIBUTING.md) y [registro de cambios](CHANGELOG.md).

## Pendiente (deuda técnica)

- IA, R2 y Stripe están probados con dobles, no contra los servicios reales; falta verificarlos con credenciales.
- Los límites de intentos viven en memoria: válidos con una sola instancia; con varias habría que usar Redis.
- No hay notificaciones por email ni en tiempo real (el frontend consulta cada cierto tiempo).
- La escritura en MongoDB y en PostgreSQL no es atómica: puede quedar algún registro huérfano de actividad o de IA.
