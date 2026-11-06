# 0004. Multimedia en Cloudflare R2 con subida directa para videos

- **Estado:** aceptada
- **Fecha:** 2026-10-17

## Contexto

Las obras necesitan imágenes y videos. El disco de Render es efímero (se pierde en cada despliegue) y el
plan gratuito tiene 512 MB de memoria, poco para pasar videos grandes por el backend. Además, servir
archivos desde un proveedor que cobra por tráfico de salida sale caro.

## Decisión

- Los archivos se guardan en **Cloudflare R2** (compatible con S3, sin coste por descarga), detrás del
  puerto `MediaStoragePort`.
- Imágenes y videos de hasta 50 MB se suben por el backend (multipart).
- Los videos de hasta 200 MB se suben **directo a R2** con una URL firmada de 15 minutos; después el
  cliente confirma y el backend verifica tamaño y tipo consultando el objeto.
- Reglas en el dominio (`MediaPolicy`): tipos permitidos, límites por tipo y modo, máximo 15 archivos por
  obra. La portada es la primera imagen por posición.
- Sin las variables `R2_*` la app arranca igual y los endpoints de multimedia responden 503.

## Consecuencias

- El backend no guarda archivos ni retiene videos grandes en memoria.
- R2 no genera miniaturas ni transcodifica video: el frontend recibe el archivo original.
- El adaptador se probó con dobles, no contra un bucket real; está pendiente verificarlo con credenciales.
