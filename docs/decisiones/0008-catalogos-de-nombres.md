# 0008 — Catálogos de nombres: dos interfaces inyectables, tablas embebidas vacías en 1.0.0

- **Estado:** Aceptada
- **Fecha:** 2026-09-10

## Contexto

El [ADR 0003](0003-catalogo-de-entidades.md) estableció que el nombre de una
entidad (o de un PSP) no vive en el value object: sale de una nómina mutable y
se resuelve detrás de una interfaz, con una implementación embebida por
defecto, una fecha de vigencia declarada, y `null` para un código no
encontrado. El issue #5 es implementarlo.

Antes de cargar datos, cuatro preguntas:

1. Alcance de la tabla embebida.
2. Cómo se declara la fecha de vigencia — consultable en runtime, no un
   comentario.
3. ¿Un catálogo o dos? Los códigos de entidad (3 dígitos, CBU) y de PSP
   (4 dígitos, CVU) viven en espacios distintos.
4. ¿Qué devuelve un código desconocido?

Restricción que no se negocia: el nombre **no** se expone en `Cbu` ni en `Cvu`.

## Decisión

### Dos interfaces separadas, no una

`CatalogoEntidades` (bancos; código de 3 dígitos; `Cbu.codigoEntidad`) y
`CatalogoPsp` (PSP; código de 4 dígitos; `Cvu.codigoPsp`).

Razón decisiva: **las dos nóminas envejecen por separado.** La de bancos y la
de PSP se actualizan en momentos distintos, con fuentes distintas, y cada una
necesita su propia [`vigencia`](#vigencia-en-la-interfaz). Una sola interfaz
con una sola `vigencia` mentiría sobre la mitad de los datos.

Razones secundarias:

- Son espacios de nombres distintos (3 vs 4 dígitos). `nombre("011")` en una
  interfaz única sería ambiguo.
- Las fuentes tienen procedencia distinta (BCRA oficial vs COELSA no público),
  y bundlearlas lo oculta.

El ADR 0003 dice "el mismo mecanismo cubre los códigos de PSP". "Mismo
mecanismo" = el mismo patrón (interfaz + `EMBEBIDO` + escape a `null`),
aplicado dos veces — no la misma interfaz.

**Sin super-interfaz común.** Un `Catalogo { vigencia; nombre() }` del que las
dos hereden daría uniformidad pero permitiría pasar el catálogo equivocado.
Que sean tipos nominalmente distintos.

### `vigencia` en la interfaz

```kotlin
public val vigencia: LocalDate
```

- Va en la **interfaz**, no solo en `EMBEBIDO`: "preguntarle al catálogo cuán
  viejo es" incluye los catálogos inyectados.
- `java.time.LocalDate` — del JDK, no es dependencia; comparable; sin
  ambigüedad de parseo. `EMBEBIDO.vigencia` devuelve una constante: la fecha
  en que se revisó por última vez el contenido del catálogo. Una
  implementación contra una base de datos devuelve su fecha de corte.
- No nulo. Un `EMBEBIDO` vacío igual tiene una `vigencia` honesta: "a esta
  fecha, sin entradas".

### Código desconocido → `null`

Confirmado (ADR 0002 y 0003). El diseño de dos interfaces no lo cambia.

- Con tabla parcial o vacía, `null` significa **"este catálogo no tiene un
  nombre para ese código"** — puede ser porque el código no existe **o**
  porque no se incluyó. La indistinción es deliberada: no afirmamos saber
  cuál de las dos.
- Un código con forma inválida (`"11"`, `"abcd"`, `""`) también devuelve
  `null`, no una excepción. Esto es un lookup, no un validador; `Cbu.codigoEntidad`
  ya garantiza la forma en el camino real de uso.

### `nombre()` solo en los catálogos

`Cbu` y `Cvu` siguen exponiendo únicamente `codigoEntidad` / `codigoPsp` —
dato estructural, no perecedero—. `nombre()` vive solo en las interfaces de
catálogo, que son objetos separados. Nada en este diseño empuja hacia poner
un nombre en el value object.

## Alcance de las tablas embebidas en 1.0.0: **ambas vacías**

### `CatalogoPsp.EMBEBIDO`: vacío por decisión, no por omisión

No existe una fuente oficial pública del código de ruteo de 4 dígitos de un
PSP. La asignación la hace COELSA (BCRA, Comunicación "A" 6510, 2018; Boletín
CIMPRA 518, 17/07/2018) y **no se publica como dato abierto**. El BCRA publica
un "Registro de proveedores de servicios de pago" con nombre, CUIT y número
de registro, pero **no** el código del CVU. Los mapeos que circulan (`0031`
Mercado Pago, `0079` Ualá, `0761` Personal Pay…) son ingeniería inversa de
terceros a partir de CVU observados.

Embeberlos sería publicar datos sin respaldo con la misma cara con la que se
publica el módulo 11, que es un teorema. Y la **asimetría de costos** lo
decide: `null` hace que el consumidor muestre "no identificada"; un nombre
equivocado hace que alguien crea que está transfiriendo a donde quería. Nadie
lee el KDoc antes de mostrar el string en pantalla.

Es el mismo criterio del [ADR 0006](0006-dni-validacion-estructural.md):
descartar la afirmación que no se puede respaldar, en vez de repetirla.

### `CatalogoEntidades.EMBEBIDO`: vacío por falta de acceso a la fuente

Para los bancos **sí hay** fuente oficial del BCRA:

- **Comunicación "B" 13179/2026** (actualización al 15/05/2026), que reemplaza
  a la B 13098. Cuadro I: numeración de cuentas corrientes y especiales en
  pesos de bancos — ese número de cuenta corriente **es** el código de
  entidad de 3 dígitos del CBU. Cuadro III: códigos de 3 dígitos de las
  entidades financieras no bancarias.
- Marco: texto ordenado "Cuentas Corrientes y Otras Cuentas a la Vista de las
  Entidades Financieras y Cambiarias en el BCRA", punto 2.2.

Al implementar este ADR, esa fuente **no se pudo abrir**: los anexos del BCRA
se publican como PDF escaneado (imagen, sin capa de texto) y los hosts
`www2.bcra.gob.ar` y las páginas `.asp` son inaccesibles desde el entorno de
trabajo. Se decidió **no cargar la tabla de memoria**: es el único punto de la
librería donde un dato puede estar mal sin que ningún test lo detecte, y una
tabla vacía es preferible a una plausible.

La tabla se carga en una versión MINOR posterior, transcrita del anexo oficial
y verificada código por código.

### Transformación de 5 dígitos a 3

Para cuando se cargue la tabla de bancos, y porque es una transformación
nuestra y no un dato publicado así:

El **dataset estadístico** de "Entidades Financieras" del BCRA (el `.7z`/`.txt`
mensual) usa un código de **5 dígitos con ceros a la izquierda** (`00011`
Nación, `00072` Santander). El CBU usa la forma de **3 dígitos** (`011`,
`072`). Quitar los ceros de relleno hasta 3 dígitos es válido porque **el CBU
reserva exactamente 3 posiciones para la entidad**: ninguna entidad autorizada
tiene un código > 999.

Si en cambio la tabla se carga desde el Cuadro I / Cuadro III de la
Comunicación "B" de cuentas corrientes, el código ya viene en la forma de 3
dígitos (`007`, `011`, `014`) y no hace falta transformación. La de 5 dígitos
aparece solo en el dataset estadístico.

## Lo que no se pudo verificar / acceder

- **Contenido actual de la nómina de bancos.** Fuente identificada
  (Comunicación "B" 13179/2026, Cuadros I y III), pero los anexos son PDF
  escaneado y los hosts del BCRA inaccesibles desde el entorno. No se
  transcribió nada.
- **Código de ruteo de PSP.** No hay fuente oficial pública. No es un problema
  de acceso: no existe publicado.
- **Fecha exacta y cadencia** del dataset de datos abiertos de "Entidades
  Financieras" (el `.7z`/`.txt` mensual): corroborado por varias fuentes
  secundarias, no verificado de primera mano.

## Verificación

| Llamada | Resultado |
|---|---|
| `CatalogoEntidades.EMBEBIDO.nombre("011")` | `null` (tabla vacía) |
| `CatalogoEntidades.EMBEBIDO.nombre("abcd")` | `null` (forma inválida, sin excepción) |
| `CatalogoEntidades.EMBEBIDO.vigencia` | `2026-09-10` (fecha de revisión) |
| `CatalogoPsp.EMBEBIDO.nombre("0031")` | `null` (tabla vacía) |
| `CatalogoPsp.EMBEBIDO.vigencia` | `2026-09-10` |
| catálogo inyectado con datos, `nombre(k)` | el nombre si `k` está, `null` si no; `vigencia` la que declare la implementación |

## Consecuencias

- El mecanismo del ADR 0003 existe y es usable desde 1.0.0. Los datos llegan
  después sin cambio de API — cargar cualquiera de las dos tablas es MINOR,
  con su ítem en el CHANGELOG y su `vigencia` actualizada.
- `Cbu` y `Cvu` no cambian.
- Si aparece una fuente oficial pública del código de PSP, se carga con el
  mismo mecanismo. Hasta entonces, `CatalogoPsp.EMBEBIDO` queda vacío a
  propósito, y su KDoc lo dice.
- Exponer `java.time.LocalDate` en el API pública no viola "cero dependencias
  de runtime": es JDK, no una librería arrastrada.

## Fuentes

Separadas por estatus, como en el ADR 0006:

- **Autoridad normativa (bancos):** BCRA, Comunicación "B" 13179/2026
  (actualización al 15/05/2026), Cuadros I y III —
  [Boletín Oficial](https://www.boletinoficial.gob.ar/detalleAviso/primera/342693/20260602).
  Identificada como la fuente correcta para el código de entidad del CBU; el
  anexo con los códigos no se pudo abrir (PDF escaneado). Marco:
  [texto ordenado t-ccbcra.pdf](https://www.bcra.gob.ar/archivos/Pdfs/Texord/t-ccbcra.pdf),
  punto 2.2 (última comunicación incorporada: "A" 8131, 12/11/2024).
- **Autoridad normativa (PSP, asignación del código):** BCRA, Comunicación
  "A" 6510 (2018) y Boletín CIMPRA 518 (17/07/2018): el código de 4 dígitos
  lo asigna COELSA. No hay lista pública.
- **Referencia histórica:** [Comunicación "B" 5474 (18/10/1993)](https://www.bcra.gob.ar/archivos/Pdfs/comytexord/B5474.pdf),
  origen de la numeración de cuentas corrientes que hoy son el código de
  entidad del CBU (`007` Galicia, `011` Nación, `014` Provincia BA…). Leída
  directamente; obsoleta como fuente de datos.
- **Corroboración, no autoridad:** dataset de datos abiertos de "Entidades
  Financieras" del BCRA (`.7z`/`.txt`, actualización mensual); Wikipedia
  (Clave Virtual Uniforme) para ejemplos de códigos de PSP de terceros.
