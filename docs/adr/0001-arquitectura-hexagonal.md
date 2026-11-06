# 0001. Arquitectura hexagonal con reglas verificadas por pruebas

- **Estado:** aceptada
- **Fecha:** 2026-10-08

## Contexto

Underart combina varias piezas externas que pueden cambiar: dos bases de datos, dos proveedores de IA,
almacenamiento de archivos en la nube y una pasarela de pago. Queríamos que las reglas del negocio
(pujas, cierre de subastas, órdenes) no dependieran de ninguna de ellas y se pudieran probar sin levantar
infraestructura.

## Decisión

Organizamos el código en tres capas con dependencias en un solo sentido:

- **`domain`:** modelos, políticas y puertos (interfaces). Java puro: sin Spring, Jakarta, Lombok ni Jackson.
- **`application`:** casos de uso que implementan los puertos de entrada y usan los de salida. No conoce
  la infraestructura.
- **`infrastructure`:** adaptadores (controladores REST, JPA, MongoDB, IA, R2, Stripe, seguridad, tareas programadas).

Las reglas se verifican automáticamente con ArchUnit (`ArchitectureTest`): el build falla si el dominio
importa un framework, si la aplicación importa infraestructura, si un puerto no es una interfaz o si un
controlador o una entidad JPA están fuera de su paquete.

## Consecuencias

- Cambiar de proveedor (por ejemplo, de R2 a S3 o de pago simulado a Stripe) es escribir un adaptador nuevo;
  los casos de uso no cambian. Ya ocurrió con Stripe y con la IA.
- Las reglas de negocio se prueban con dobles de los puertos, en milisegundos.
- Hay más archivos y algo de mapeo entre entidades JPA y modelos de dominio. Lo aceptamos a cambio del
  aislamiento.
- La aplicación usa anotaciones de Spring para inyección y transacciones (`@Service`, `@Transactional`);
  es una concesión práctica que no rompe la dirección de las dependencias.
