# Underart Backend

API REST de una plataforma de subastas de arte. Los artistas publican obras con imágenes y
video, la IA sugiere un precio y etiquetas, y los compradores pujan o compran al instante.
Cuando una subasta termina, el sistema la cierra solo y genera la orden para el ganador.

## Stack

- Java 17, Spring Boot 3.3, Maven
- PostgreSQL (JPA + Flyway) para el negocio y MongoDB para logs de actividad e historial de IA
- Spring Security con JWT (sin sesiones)
- Groq como proveedor de IA principal y OpenAI como respaldo
- Cloudflare R2 (compatible con S3) para imágenes y video
- Arquitectura hexagonal: `domain` (sin Spring ni Lombok), `application`, `infrastructure`
- Docker, Render y GitHub Actions

## Ejecutar en local

Requisitos: Java 17, Maven, PostgreSQL y MongoDB (por ejemplo, los contenedores `underart-pg` y `underart-mongo`).

```bash
mvn spring-boot:run
```

El perfil `dev` trae valores por defecto (`localhost:5432/underart`, `postgres`/`postgres`,
`localhost:27017/underart`), así que no necesitas variables de entorno. Puedes sobrescribirlos
con las variables de `.env.example`. Spring no lee el archivo `.env` por sí solo: exporta las
variables en tu terminal o en tu IDE.

Flyway crea las tablas al arrancar. En `dev` se crea el usuario `demo@underart.com` / `password123`.
En producción no hay usuario demo: regístrate con `POST /api/v1/users/register`.

Swagger UI: `/swagger-ui/index.html`. El botón **Authorize** acepta el JWT del login.

## Pruebas

```bash
mvn clean verify
```

Incluye pruebas unitarias de las reglas de negocio y una prueba de integración que levanta
Postgres y Mongo reales con Testcontainers y recorre el flujo completo por HTTP. Si Docker no
está disponible, la prueba de integración se salta sola.

Con Docker Desktop en Mac, Testcontainers necesita encontrar el socket:

```bash
export DOCKER_HOST=unix://$HOME/.docker/run/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

Usa siempre `mvn clean` antes de empaquetar: algunos IDEs escriben clases incompletas en `target/`.

## Endpoints

Todas las rutas están bajo `/api/v1` y requieren `Authorization: Bearer <token>`, salvo las públicas.
El catálogo se puede consultar sin cuenta; pujar, comprar, crear o editar siempre exige token.
Los listados son paginados con `?page=0&size=20` y devuelven `{items, page, size, totalItems, totalPages}`.

### Públicos
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/` (raíz) | Estado de la aplicación |
| GET | `/actuator/health` (raíz) | Health check |
| POST | `/users/register` | Registro |
| POST | `/auth/login` | Login, devuelve el JWT |
| GET | `/artworks`, `/artworks/{id}` | Catálogo y detalle de obras |
| GET | `/artworks/{id}/media` | Imágenes y videos de una obra |
| GET | `/auctions`, `/auctions/{id}` | Subastas y su precio actual |
| GET | `/users/{id}` | Perfil público (sin email ni dirección) |

El historial de pujas (`GET /auctions/{id}/bids`) sigue requiriendo token porque muestra quién pujó.

### Usuarios
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/users/me` | Mi perfil completo |
| PUT | `/users/me` | Actualizar nombre, ciudad y dirección |
| PUT | `/users/me/password` | Cambiar contraseña |
| PUT | `/users/me/payment-method` | Registrar método de pago (token) |
| GET | `/users/me/artworks` | Mis obras |
| GET | `/users/me/auctions` | Subastas de mis obras |
| GET | `/users/me/bids` | Mis pujas |
| GET | `/users/me/orders` | Mis compras |
| GET | `/users/me/sales` | Mis ventas |
| GET | `/users/me/activity` | Mi historial de actividad |

### Obras y multimedia
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/artworks` | Crear obra (la IA sugiere precio y etiquetas) |
| GET | `/artworks?artistId=&technique=&tag=&q=` | Buscar obras |
| GET | `/artworks/{id}` | Detalle |
| PUT | `/artworks/{id}` | Editar (solo el autor) |
| DELETE | `/artworks/{id}` | Borrar (solo el autor y sin subastas) |
| POST | `/artworks/{id}/media` | Subir imagen o video corto (multipart, campo `file`) |
| POST | `/artworks/{id}/media/upload-url` | Pedir URL firmada para subir un video grande directo a R2 |
| POST | `/artworks/{id}/media/{mediaId}/confirm` | Confirmar una subida directa |
| GET | `/artworks/{id}/media` | Listar archivos (el primero es la portada) |
| PUT | `/artworks/{id}/media/order` | Reordenar archivos |
| DELETE | `/artworks/{id}/media/{mediaId}` | Borrar un archivo |
| POST | `/ai/valuations` | Tasación con IA sin crear la obra |

Límites: imágenes JPEG, PNG o WEBP de hasta 10 MB; video MP4, WEBM o MOV de hasta 50 MB por
multipart o 200 MB por subida directa; máximo 15 archivos por obra.

### Subastas, pujas y órdenes
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auctions` | Crear subasta (solo el autor de la obra) |
| GET | `/auctions?status=&type=&artworkId=&artistId=&endingBefore=` | Buscar subastas |
| GET | `/auctions/{id}` | Detalle con precio actual, puja más alta y número de pujas |
| DELETE | `/auctions/{id}` | Cancelar (solo el autor y sin pujas) |
| POST | `/auctions/{id}/bids` | Pujar (`{"amount": 150}`) |
| GET | `/auctions/{id}/bids` | Historial de pujas, la más reciente primero |
| POST | `/auctions/{id}/buy-now` | Compra inmediata |
| GET | `/orders/{id}` | Detalle (solo comprador o vendedor) |
| POST | `/orders/{id}/pay` | Pagar la orden |

## Reglas de negocio principales

- La identidad siempre sale del JWT: nadie puede pujar, comprar ni crear obras a nombre de otro.
- No se puede pujar ni comprar la obra propia, ni pujar fuera de las fechas de la subasta.
- Cada puja debe superar el precio base y la puja más alta. Las pujas simultáneas se serializan
  con un bloqueo de fila sobre la subasta.
- Una obra solo puede tener una subasta activa a la vez.
- Una tarea programada cierra cada minuto las subastas vencidas y crea la orden del ganador.
- Orden: subtotal + envío (15.00) + comisión del 10 % sobre el subtotal.
- El pago es **simulado**: un token que empiece por `tok_fail` se rechaza (402); cualquier otro se aprueba.

## Errores

Todas las respuestas de error tienen el formato `{"status", "message", "timestamp"}`:
400 (validación o regla de negocio), 401 (sin token o token inválido), 402 (pago rechazado),
403 (no eres el dueño), 404, 405, 409 (conflicto), 413 (archivo grande), 503 (IA o almacenamiento no disponibles).

## Despliegue

Ver [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md): Render, Neon (Postgres), MongoDB Atlas y Cloudflare R2.

## Pendiente (deuda técnica)

- El pago es simulado; falta integrar una pasarela real (por ejemplo, Stripe en modo prueba).
- La IA no se ha verificado con claves reales de Groq y OpenAI. El modelo se configura con `GROQ_MODEL` y `OPENAI_MODEL`.
- La subida a R2 no se ha probado contra un bucket real.
- Sin refresh tokens ni límite de intentos de login.
- No hay notificaciones por email (por ejemplo, al ganar una subasta).
- El registro de IA en Mongo y la obra en Postgres no son atómicos: si falla el segundo, queda un registro huérfano.
