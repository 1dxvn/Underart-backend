# Cómo contribuir

## Preparar el entorno

1. Java 17 y Docker. No hace falta instalar Maven: usa `./mvnw`.
2. Levanta las bases de datos de desarrollo:
   ```bash
   docker compose up -d
   ```
3. Arranca la API con el perfil `dev` (valores por defecto incluidos):
   ```bash
   ./mvnw spring-boot:run
   ```
   Swagger queda en `http://localhost:8080/swagger-ui/index.html`.

## Reglas de arquitectura

El proyecto sigue una arquitectura hexagonal ([ADR 0001](docs/adr/0001-arquitectura-hexagonal.md)):

- `domain` es Java puro: sin Spring, Jakarta, Lombok ni Jackson.
- `application` solo depende de `domain`. Si un caso de uso necesita algo externo, se crea un puerto en
  `domain.port.out` y un adaptador en `infrastructure`.
- Lombok se permite en `application` e `infrastructure`.
- Cada cambio de esquema es una nueva migración `src/main/resources/db/migration/V<n>__descripcion.sql`.
  Nunca se edita una migración ya publicada.

`ArchitectureTest` falla si se rompe alguna de estas reglas.

## Pruebas

```bash
./mvnw clean verify
```

- Las reglas de negocio llevan pruebas unitarias con dobles de los puertos.
- `ApiIntegrationTest` recorre el flujo completo contra PostgreSQL y MongoDB reales (Testcontainers);
  se salta sola si no hay Docker. En Docker Desktop para Mac exporta antes:
  ```bash
  export DOCKER_HOST=unix://$HOME/.docker/run/docker.sock
  export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
  ```
- El build exige al menos 80 % de cobertura de líneas y 70 % de ramas (informe en `target/site/jacoco/index.html`).

## Commits y pull requests

- Mensajes en español con el formato de [Conventional Commits](https://www.conventionalcommits.org/es/v1.0.0/):
  `feat(auctions): ...`, `fix: ...`, `docs: ...`, `test: ...`, `build: ...`, `ci: ...`, `chore: ...`.
- Un commit por cambio lógico, y cada commit compila y pasa las pruebas.
- Antes de abrir un pull request: `./mvnw clean verify` en verde, documentación y `CHANGELOG.md` actualizados
  si cambia el comportamiento de la API.
