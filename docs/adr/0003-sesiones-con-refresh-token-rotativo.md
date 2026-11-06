# 0003. Sesiones con access token corto y refresh token rotativo

- **Estado:** aceptada
- **Fecha:** 2026-11-05

## Contexto

El primer login entregaba un JWT válido 24 horas, sin forma de revocarlo. Un token robado daba acceso
durante un día entero, cerrar sesión no tenía efecto real y cambiar la contraseña no expulsaba a nadie.

## Decisión

- El **access token** es un JWT de 15 minutos con emisor, audiencia, tipo `access` e id único; el backend
  rechaza cualquier token que no cumpla las cuatro condiciones.
- El **refresh token** es un valor aleatorio de 256 bits. En la base solo se guarda su hash SHA-256, con
  usuario, familia y caducidad (30 días).
- **Rotación:** cada `POST /auth/refresh` revoca el refresh token usado y entrega uno nuevo de la misma familia.
- **Detección de reutilización:** si llega un refresh token ya revocado, se revoca la familia completa;
  el atacante y el usuario legítimo tienen que volver a iniciar sesión.
- Cerrar sesión revoca la familia; cambiar la contraseña revoca todas las sesiones del usuario.
- Tras 5 intentos fallidos en 15 minutos, el login de ese email responde 429 con `Retry-After`.

## Consecuencias

- Un access token robado sirve como mucho 15 minutos, y un refresh token robado se detecta en cuanto
  cualquiera de las dos partes lo vuelve a usar.
- El frontend debe renovar la sesión ante un 401 y guardar siempre el refresh token más reciente.
- El limitador de intentos vive en memoria: funciona con una sola instancia (como en Render gratuito).
  Con varias instancias habría que llevarlo a Redis.
- Una tarea diaria borra los refresh tokens caducados hace más de 7 días.
