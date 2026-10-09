# Underart Backend

API REST de una plataforma de subastas de arte. Los artistas publican obras
(con descripciones generadas por IA) y los compradores pujan o compran al instante.

## Stack

- Java 17, Spring Boot 3.3
- PostgreSQL (JPA) y MongoDB (logs de actividad e historial de IA)
- Spring Security + JWT
- Groq / OpenAI como proveedores de IA
- Arquitectura hexagonal: `domain`, `application`, `infrastructure`
- Docker y Render para el despliegue

## Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | /api/v1/users/register | No | Registro de usuario |
| GET | /api/v1/users/{id} | Sí | Consultar usuario por id |
| GET | /api/v1/users/me | Sí | Perfil del usuario autenticado |
| POST | /api/v1/auth/login | No | Login (devuelve JWT) |
| POST | /api/v1/artworks | Sí | Crear obra (dispara IA) |
| GET | /api/v1/artworks | Sí | Listar obras (filtro ?artistId=) |
| GET | /api/v1/artworks/{id} | Sí | Detalle de obra |
| POST | /api/v1/auctions | Sí | Crear subasta |
| GET | /api/v1/auctions?status= | Sí | Listar subastas (filtro opcional) |
| DELETE | /api/v1/auctions/{id} | Sí | Cancelar subasta activa |
| POST | /api/v1/auctions/{id}/bids | Sí | Pujar en subasta |
| GET | /api/v1/auctions/{id}/bids | Sí | Historial de pujas |
| POST | /api/v1/auctions/{id}/buy-now?buyerId= | Sí | Compra inmediata |

## Pendiente (deuda técnica)

- Ownership check en DELETE /auctions/{id} — hoy cualquier usuario autenticado puede cancelar.
- Race condition en pujas concurrentes — resolver con @Version si escala.
- Handler específico para PaymentFailedException (no se lanza todavía).
- buyerId viene del cliente en POST /auctions/{id}/bids y buy-now —
  debería extraerse del JWT para evitar suplantación entre usuarios autenticados.

Swagger UI: `/swagger-ui.html`

## Ejecución

Copia `.env.example` a `.env`, ajusta los valores y ejecuta `mvn spring-boot:run`.

En desarrollo (perfil `dev`) se crea el usuario `demo@underart.com` / `password123`.
En producción no existe ningún usuario demo: regístrate con `POST /api/v1/users/register`
y luego inicia sesión con `POST /api/v1/auth/login`.
