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

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| POST | /api/v1/users/register | Registro de usuario |
| GET | /api/v1/users/{id} | Consultar usuario por id |
| POST | /api/v1/auth/login | Login (devuelve JWT) |
| POST | /api/v1/artworks | Crear obra (dispara IA) |
| GET | /api/v1/auctions?status= | Listar subastas (filtro opcional) |
| POST | /api/v1/auctions/{id}/bids | Pujar en subasta |
| POST | /api/v1/auctions/{id}/buy-now?buyerId= | Compra inmediata |

## Pendiente

- GET /api/v1/artworks/{id} — requiere FindArtworkUseCase
- GET /api/v1/orders/{id} — requiere FindOrderUseCase

Swagger UI: `/swagger-ui.html`

## Ejecución

Copia `.env.example` a `.env`, ajusta los valores y ejecuta `mvn spring-boot:run`.
