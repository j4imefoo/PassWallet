# PassWallet — guía de renderizado de pases

Objetivo: que cada pase se entienda de un vistazo, se pueda escanear sin deslizar y tenga una apariencia limpia, fiel al `.pkpass` y bonita en Android.

Esta guía manda sobre preferencias puntuales salvo que una pantalla concreta justifique una excepción.

## Principios generales

1. El QR/código de barras es funcional, no decorativo: debe estar completo, centrado y visible sin hacer scroll en la vista de detalle.
2. La portada del pase muestra solo información accionable o identificadora. El texto largo va a detalles.
3. Respetar la identidad visual del pase: `backgroundColor`, `foregroundColor`, `labelColor`, logo e imágenes.
4. No inventar campos visibles. Datos técnicos como `Tipo: Evento` no pertenecen a la portada.
5. No duplicar información: si ya aparece en la tarjeta principal, no repetirla justo debajo salvo en detalles expandibles.
6. La lista global es un resumen visual. Las acciones viven en la vista de detalle o dentro del pase cuando proceda.
7. Cada tipo de pase necesita jerarquía propia: evento, embarque, cupón, fidelización y genérico no deben renderizarse igual.
8. Si el pase trae datos feos, se puede limpiar visualmente sin modificar el contenido original.

## Estructura `.pkpass` a respetar

Orden semántico base:

- `logo` / `logoText`: marca o emisor.
- `headerFields`: datos cortos arriba, normalmente fecha/hora, saldo, puerta, etc.
- `primaryFields`: protagonista visual del pase.
- `secondaryFields`: datos principales de uso.
- `auxiliaryFields`: datos complementarios de acceso/ubicación.
- `barcode` / `barcodes`: código escaneable y `altText`.
- `backFields`: detalles, condiciones, información larga y texto secundario.

Colores:

- Usar `backgroundColor` como fondo de la tarjeta del pase.
- Usar `foregroundColor` si existe; si no, calcular blanco/negro por contraste.
- Usar `labelColor` si existe; si no, usar foreground con alfa reducido.
- El panel del QR debe ser blanco o casi blanco, con margen suficiente y esquinas redondeadas.

## Vista global de lista

La lista sirve para identificar rápido el pase y abrirlo.

Debe mostrar:

- Tarjeta con color real del pase.
- Logo/emisor si existe y cabe bien.
- Título principal según tipo de pase.
- Una o dos líneas de contexto máximo.
- Indicador visual de categoría solo si no compite con el logo.

No debe mostrar:

- Botones de compartir, editar o actualizar.
- QR/código de barras grande.
- Campos técnicos generados por la app.
- Back fields.
- Más de tres piezas de información textual.

Regla visual:

- Altura compacta pero respirada.
- Nada de scroll horizontal.
- Título con peso visual claro.
- Subtítulo y metadatos en tamaño menor.
- Si el color del pase es muy oscuro, texto blanco. Si es claro, texto negro.

## Vista detalle

La vista detalle debe permitir enseñar el pase en una puerta, caja, cine, aeropuerto o control sin pelearse con la pantalla. El detalle no reutiliza la cabecera compacta de lista (`pass_top`): esa cabecera duplica fecha, título e icono y solo pertenece a la lista global.

Debe mostrar en el primer pantallazo:

- Marca/logo.
- Información principal del pase.
- Datos mínimos necesarios para orientarse.
- QR/código completo y escaneable.
- `altText` o localizador si existe.

El usuario no debería tener que deslizar para enseñar el QR.

Si no cabe todo:

1. Reducir información textual de portada.
2. Mover campos secundarios a detalles.
3. Reducir espacios muertos.
4. Mantener QR completo antes que mostrar más texto.

Nunca recortar el QR.

## QR y códigos de barras

Reglas duras:

- Debe aparecer completo en la vista detalle sin scroll en teléfonos normales.
- Debe tener panel blanco con margen interior suficiente.
- Debe estar centrado.
- Debe tener tamaño mínimo razonable para escaneo.
- Debe permitir tocar para ampliar a pantalla completa.
- `altText` debe aparecer justo debajo del código si existe.

Tamaño recomendado:

- QR cuadrado: entre 56% y 66% del ancho menor disponible, según densidad de información de la tarjeta.
- Código lineal: ancho casi completo, altura proporcional, nunca forzar cuadrado.

Si el QR compite con demasiados campos, gana el QR.

## Campos y limpieza visual

No modificar el `.pkpass`, pero sí se puede mejorar la presentación.

Limpiezas permitidas en UI:

- Espacios ausentes: `(LASERREX)19:00` -> `(LASERREX) 19:00`.
- Valores con separador vacío: `3 -` -> `3` si la segunda parte está vacía.
- Labels con espacios sobrantes: `Sala - Zona ` -> `Sala - Zona`.
- Listas de asientos detectables: `6-18, 6-17, 6-16, 6-15` puede mostrarse como `Fila 6 · Asientos 15-18` si el patrón es inequívoco.

Limpiezas no permitidas:

- Cambiar significado.
- Reordenar asientos si el orden puede ser significativo y no hay patrón claro.
- Ocultar campos críticos como fecha, puerta, asiento, sala, saldo o caducidad.

## Back fields

Los `backFields` son detalles, no portada.

Mostrar en sección expandible o pantalla de detalles:

- Condiciones.
- Detalle de entradas.
- Detalle de compra.
- Localizador si no está ya bajo el QR.
- Mensajes largos.
- URLs, teléfonos o emails.

No repetir en portada:

- Evento si ya está en primary.
- Recinto si ya aparece como label/contexto.
- Fecha si ya aparece en header.
- Número de entradas si ya aparece en secondary.

## Credentials / carnets

Tipo PassKit frecuente: `eventTicket` o `generic`, pero el tipo visual debe ser `CREDENTIAL` cuando el pase parezca carnet, acreditación o tarjeta identificativa.

Detectar por señales combinadas, no por una sola palabra:

- `passTypeIdentifier`, `description` u organización con `carnet`, `credential`, `identity`, `colegial`, `membership`.
- Campos de identidad como `colegiado`, `dni`, `nif`, `member number`.
- Campo de titular como `nombre`, `name`, `titular`, `holder`.

### Lista

Prioridad:

1. Nombre/titular.
2. Número de colegiado, socio o identificador.
3. Organización solo como fallback.

### Detalle

Primer pantallazo:

- `strip.png` como marca/cabecera institucional si existe.
- `logo.png` como foto del titular cuando el pase la use así.
- Número de colegiado o socio visible y etiquetado aunque el `headerField` no traiga `label`.
- Nombre grande.
- DNI/NIF o identificador secundario compacto.
- QR/código completo y tocable.

No tratar estos pases como eventos aunque vengan dentro de `eventTicket`: PassKit usa tipos técnicos muy pobres para muchos carnets reales.

## Event tickets

Tipo: `eventTicket`.

Usar para cine, teatro, conciertos, deporte y reservas de entrada.

### Lista

Prioridad:

1. Evento: normalmente `primaryFields.value`.
2. Recinto: normalmente `primaryFields.label`.
3. Fecha/hora: normalmente primer `headerField` o campo con fecha.

Ejemplo:

- Título: `PROYECTO SALVACIÓN`
- Subtítulo: `CINES ARAGONIA`
- Meta: `31/03/2026 (LASERREX) 19:00`

No mostrar:

- QR.
- Botones.
- `Tipo: Evento`.
- Detalle completo de entradas.

### Detalle

Primer pantallazo:

- Logo/emisor arriba.
- Fecha/hora arriba a la derecha si cabe.
- Evento grande.
- Recinto como contexto.
- Sala/zona, nº entradas, fila/asiento en bloques compactos.
- QR completo con localizador.

Campos recomendados en portada:

- `primaryFields`: evento y recinto.
- `headerFields`: fecha/hora.
- `secondaryFields`: sala/zona, número de entradas, sección.
- `auxiliaryFields`: fila/asiento, puerta, acceso.

Campos recomendados en detalles:

- Desglose por entrada.
- Menús/bar/complementos.
- Condiciones.
- Texto largo.

## Boarding passes

Tipo: `boardingPass`.

La estructura debe parecer una tarjeta de embarque.

### Lista

Prioridad:

1. Ruta: origen -> destino.
2. Fecha/hora de salida.
3. Compañía o número de vuelo/tren.

### Detalle

Primer pantallazo:

- Origen -> destino como protagonista, con icono semántico de transporte entre ambos cuando el `.pkpass` trae `boardingPass.transitType` (`PKTransitTypeAir`, `Train`, `Bus`, `Boat`; flecha genérica si falta).
- Hora, puerta, asiento, grupo/zona.
- Barcode completo.
- Nombre pasajero si existe.

No mezclar con layout de evento.

## Store cards y fidelización

Tipo: `storeCard`.

### Lista

Prioridad:

1. Marca/programa.
2. Nivel, saldo o puntos si existen.
3. Identificador corto si aporta valor.

### Detalle

Primer pantallazo:

- Marca grande.
- Saldo/puntos/nivel si existen.
- Barcode completo.
- Número o identificador alternativo.

Condiciones y ventajas largas van a detalles.

## Coupons

Tipo: `coupon`.

### Lista

Prioridad:

1. Oferta/descuento.
2. Marca.
3. Caducidad.

### Detalle

Primer pantallazo:

- Oferta grande.
- Caducidad clara.
- Barcode completo.
- Condición principal si es corta.

Condiciones largas y letra pequeña van a detalles.

## Generic

Tipo: `generic`.

Usar una jerarquía conservadora:

1. `primaryFields` como título.
2. `secondaryFields` como contexto.
3. `auxiliaryFields` como datos compactos.
4. Barcode completo si existe.
5. Back fields en detalles.

## Acciones

Lista global:

- Solo tocar tarjeta para abrir.
- Long press para organización si ya existe.
- No mostrar acciones por tarjeta salvo que sean imprescindibles.

Detalle:

- Compartir.
- Editar.
- Actualizar.
- Eliminar/mover a papelera.

Las acciones destructivas siempre con confirmación clara.

## Criterios de aceptación visual

Un pase está bien renderizado si:

- Se identifica qué es en menos de 2 segundos.
- El QR/código se puede enseñar sin deslizar.
- El QR/código no está cortado ni pegado al borde.
- La información crítica aparece en el primer pantallazo.
- La portada no parece una tabla de campos.
- No hay campos inventados ni redundantes.
- La tarjeta respeta color y marca del pase.
- La lista no parece una vista de edición ni de detalle.

## Implementación recomendada

Crear renderers por tipo:

- `EventTicketRenderer`
- `BoardingPassRenderer`
- `StoreCardRenderer`
- `CouponRenderer`
- `GenericPassRenderer`

Cada renderer debe definir:

- Título de lista.
- Subtítulo de lista.
- Metadato de lista.
- Campos visibles en detalle.
- Campos enviados a detalles expandibles.
- Reglas de limpieza visual.

Evitar seguir añadiendo excepciones dispersas en Activities o ViewHolders. Centralizar decisiones de presentación en una capa de rendering/formatting.

## Fuentes de referencia

- Apple Wallet / PassKit: `Pass Design and Creation`.
- Apple PassKit package format: estilos `boardingPass`, `coupon`, `eventTicket`, `storeCard`, `generic`; campos `headerFields`, `primaryFields`, `secondaryFields`, `auxiliaryFields`, `backFields`.
- Google Wallet Event Tickets: jerarquía limpia, filas de campos, barcode visible y datos específicos de evento.
