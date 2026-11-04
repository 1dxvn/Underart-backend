# Despliegue de Underart Backend

Arquitectura gratuita recomendada:

| Pieza | Servicio |
|---|---|
| API (Docker) | Render, plan gratuito |
| PostgreSQL | Neon, plan gratuito |
| MongoDB | MongoDB Atlas, clúster M0 gratuito |
| Imágenes y video | Cloudflare R2, capa gratuita |

Los límites de los planes gratuitos cambian: revisa los actuales en cada panel.

## 1. PostgreSQL en Neon

1. Crea un proyecto en [neon.tech](https://neon.tech). Elige una región cercana a la de tu servicio en Render.
2. En **Connection Details**, desactiva **Connection pooling** para obtener el host directo
   (sin `-pooler`). La app ya usa su propio pool pequeño y Flyway funciona mejor con conexión directa.
3. Neon muestra una cadena como `postgresql://usuario:clave@ep-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require`.
   Sepárala en tres variables:
   - `DB_URL=jdbc:postgresql://ep-xxx.us-east-2.aws.neon.tech/neondb?sslmode=require`
   - `DB_USER=usuario`
   - `DB_PASSWORD=clave`

   Si la cadena trae `channel_binding=require`, quítalo de `DB_URL`.

Flyway crea todas las tablas en el primer arranque. Si migras una base existente creada por
Hibernate, Flyway la marca como versión 1 y aplica solo las migraciones nuevas.

Para copiar datos de la base anterior (opcional):

```bash
pg_dump --no-owner --no-privileges "postgresql://ANTERIOR" | psql "postgresql://NEON"
```

## 2. MongoDB Atlas

1. Crea un clúster **M0** en [mongodb.com/atlas](https://www.mongodb.com/atlas).
2. En **Database Access**, crea un usuario con contraseña.
3. En **Network Access**, permite `0.0.0.0/0`: Render gratuito no tiene IP fija.
4. Copia la cadena de conexión y **añade el nombre de la base** antes del `?`:
   `MONGO_URI=mongodb+srv://usuario:clave@cluster0.xxxx.mongodb.net/underart?retryWrites=true&w=majority`

## 3. Cloudflare R2

1. En el panel de Cloudflare, ve a **R2** y crea un bucket, por ejemplo `underart-media`.
2. Acceso público:
   - Recomendado: **Settings → Custom Domains** y conecta un subdominio, como `media.tu-dominio.com`.
   - Solo para pruebas: activa la URL `r2.dev`, que tiene límites de uso.

   Esa URL base es `R2_PUBLIC_URL`, sin barra final.
3. **Manage R2 API Tokens → Create API Token** con permiso *Object Read & Write* limitado a tu bucket.
   Guarda el `Access Key ID` y el `Secret Access Key`: el secreto solo se muestra una vez.
4. El **Account ID** aparece en la página principal de R2.
5. Para la subida directa de video desde el navegador, configura el **CORS del bucket**
   (Settings → CORS Policy):

   ```json
   [
     {
       "AllowedOrigins": ["https://tu-frontend.com", "http://localhost:5173"],
       "AllowedMethods": ["PUT", "GET"],
       "AllowedHeaders": ["Content-Type"],
       "MaxAgeSeconds": 3600
     }
   ]
   ```

Variables: `R2_ACCOUNT_ID`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`, `R2_BUCKET`, `R2_PUBLIC_URL`.
Sin ellas la app arranca igual, pero los endpoints de multimedia responden 503.

## 4. Pagos con Stripe (opcional)

Por defecto el pago es simulado (`PAYMENT_PROVIDER=simulated`): sirve para demos y no cobra nada.
Para usar Stripe en **modo de prueba** (sin dinero real):

1. Crea una cuenta en [stripe.com](https://stripe.com) y entra al panel en **modo de prueba** (interruptor "Test mode").
2. En **Developers → API keys**, copia la **Secret key** (`sk_test_...`). Nunca uses la clave `sk_live_...` hasta tener un negocio verificado.
3. En Render define `PAYMENT_PROVIDER=stripe` y `STRIPE_SECRET_KEY=sk_test_...`. Opcional: `STRIPE_CURRENCY` (por defecto `usd`).
   Si activas `stripe` sin clave, la app se niega a arrancar.

Cómo funciona: `PUT /users/me/payment-method` recibe el **id de un PaymentMethod de Stripe** (`pm_...`), que el
frontend obtiene con Stripe.js sin que los datos de la tarjeta pasen por tu servidor. El backend crea un cliente en
Stripe, le asocia el método y guarda `cliente:método`. `POST /orders/{id}/pay` cobra con un PaymentIntent
(el monto en centavos, con clave de idempotencia por orden y método de pago).

Para probar sin frontend, Stripe ofrece métodos de prueba que puedes enviar directamente:

| Método de prueba | Resultado al pagar |
|---|---|
| `pm_card_visa` | Aprobado |
| `pm_card_chargeCustomerFail` | Se guarda bien, pero el cobro es rechazado (402) |
| `pm_card_threeDSecure2Required` | Requiere autenticación adicional: la orden no se aprueba |

## 5. Render

En el servicio, **Environment**, define:

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (ya viene en `render.yaml`) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | De Neon |
| `MONGO_URI` | De Atlas |
| `JWT_SECRET` | Al menos 32 caracteres. Genera uno con `openssl rand -base64 48` |
| `GROQ_API_KEY`, `OPENAI_API_KEY` | Claves reales de IA |
| `CORS_ALLOWED_ORIGINS` | Dominios del frontend separados por comas, p. ej. `https://underart.vercel.app,http://localhost:5173`. Admite comodines como `https://*.vercel.app` |
| `R2_*` | De Cloudflare |
| `PAYMENT_PROVIDER`, `STRIPE_SECRET_KEY` | `simulated` por defecto; ver la sección de Stripe |

Opcionales: `GROQ_MODEL` (por defecto `llama-3.3-70b-versatile`), `OPENAI_MODEL` (`gpt-4o-mini`),
`JWT_EXPIRATION_MS` (24 h), `DB_POOL_SIZE` (5), `AUCTIONS_CLOSING_INTERVAL_MS` (60000).

Si `JWT_SECRET` tiene menos de 32 caracteres, la app se niega a arrancar con un mensaje claro en los logs.

El health check de Render apunta a `/actuator/health` (está en `render.yaml`). Si creaste el servicio
a mano y no desde el blueprint, configúralo en **Settings → Health Check Path**.

## 6. Desplegar y verificar

1. Haz push a `main`. Render despliega solo y GitHub Actions corre las pruebas.
2. Comprueba:

   ```bash
   curl https://underart-backend.onrender.com/actuator/health   # {"status":"UP",...}
   curl https://underart-backend.onrender.com/                  # JSON de Underart
   ```

3. Abre `https://underart-backend.onrender.com/swagger-ui/index.html`, regístrate, haz login y pulsa
   **Authorize** con el token.

En el plan gratuito, el servicio se duerme tras un rato sin tráfico y la primera petición tarda
entre 30 y 60 segundos. Antes de una demo, abre la URL unos minutos antes.

## Probar la imagen Docker en local

```bash
docker build -t underart-backend .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/underart \
  -e DB_USER=postgres -e DB_PASSWORD=postgres \
  -e MONGO_URI=mongodb://host.docker.internal:27017/underart \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  -e GROQ_API_KEY=x -e OPENAI_API_KEY=x \
  underart-backend
```
