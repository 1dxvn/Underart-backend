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
|--------|------|-------------|
| POST | /api/auth/register | Registro de usuario |
| POST | /api/auth/login | Login, devuelve JWT |
| GET | /api/users/me | Usuario autenticado |
| POST | /api/artworks | Crear obra |
| GET | /api/auctions | Listar subastas |
| POST | /api/bids | Pujar en una subasta |
| POST | /api/orders | Comprar ya |

Swagger UI: `/swagger-ui.html`

## Ejecución

Copia `.env.example` a `.env`, ajusta los valores y ejecuta `mvn spring-boot:run`.
