# Alcance — identificadores-ar

Librería Kotlin para validar, parsear y formatear identificadores argentinos.
Sin dependencias de runtime. Usable desde Java.

Este documento define qué entra y qué no. Todo lo que se me ocurra después va a un
issue etiquetado `futuro`, no al código.

---

## Dentro del alcance

### CUIT / CUIL
- [ ] Validación de dígito verificador (módulo 11)
- [ ] Parseo de componentes: prefijo, cuerpo, dígito verificador
- [ ] Inferencia de tipo de persona (física / jurídica / otro) a partir del prefijo
- [ ] Formateo con y sin separadores
- [ ] Tolerancia de entrada: guiones, espacios, o sin separadores
- [ ] Cálculo del dígito verificador a partir de prefijo + cuerpo

### CBU / CVU
- [ ] Validación de la estructura de 22 dígitos en dos bloques
- [ ] Validación del doble dígito verificador
- [ ] Extracción de entidad, sucursal y número de cuenta
- [ ] Lookup del nombre de la entidad a partir del código

### DNI
- [ ] Validación estructural

### Alias bancario
- [ ] Validación de formato (longitud y caracteres permitidos)

---

## Fuera del alcance — explícito

Estas cosas NO las hace la librería. Van documentadas en el README porque son la
fuente principal de confusión de quien la use.

- **No consulta el padrón de ARCA (ex AFIP).** La validación es puramente
  matemática: verifica que el identificador esté bien formado, no que exista ni
  que esté activo.
- **No resuelve alias a CBU.** No existe un mecanismo público para hacerlo.
- **No valida titularidad de cuentas.** Esa información no es pública.
- **No hace llamadas de red.** Cero I/O. Todo el cómputo es local y offline.
- **No incluye identificadores de otros países.**
- **No incluye validación de teléfonos ni direcciones.**

---

## Decisiones de diseño

| Decisión | Elección | Motivo |
|---|---|---|
| Modelado | Value objects, no strings | *Parse, don't validate*: si tenés un `Cuit`, es válido por construcción |
| API de entrada | `parse` / `parseOrNull` / `isValid` | Cada una sirve a un contexto distinto (bug, input de usuario, filtrado) |
| Dependencias de runtime | Ninguna | Una dependencia arrastrada es un conflicto de versiones potencial |
| Interop con Java | `@JvmStatic` en los companion | Que se vea como `Cuit.parse()` y no como `Cuit.Companion.parse()` |
| Idioma | Tipos en castellano, miembros en inglés | Los identificadores son constructos legales sin traducción |
| Modo de API | `explicitApi()` activado | Todo lo público es un compromiso de compatibilidad |
| Módulos | Uno solo | No hay razón para fragmentar algo de este tamaño |
| Target JVM | 17 | LTS, ampliamente adoptado |

---

## Decisiones sobre casos límite

Estas eran preguntas abiertas al inicio de la Fase 0. Se resolvieron con
investigación en fuentes primarias y quedan documentadas acá y en el README.

### 1. ¿CBU y CVU son un tipo o dos?

**Decisión: dos tipos bajo una interfaz sellada `ClaveBancaria`.**

Comparten longitud (22 dígitos), estructura en dos bloques y algoritmo de doble
dígito verificador (módulo 10). Pero los dígitos 4 a 7 significan cosas
distintas: en el CBU son la sucursal, en el CVU son el código del PSP. Un solo
tipo obligaría a exponer `sucursal: String?` y `codigoPsp: String?`, dos campos
nulables mutuamente excluyentes, dejando al consumidor la responsabilidad de
saber cuál puede leer sin ayuda del compilador.

El discriminador es el centinela `000` en los tres primeros dígitos, definido por
normativa del BCRA (Comunicación "A" 6510, 2018). Es estructural y total, no
depende de ninguna tabla, así que la jerarquía sellada permite un `when`
exhaustivo sin necesidad de un estado desconocido.

La interfaz común preserva la interoperabilidad real entre CBU y CVU: quien solo
necesite una cuenta destino acepta `ClaveBancaria` y no se entera del subtipo.

### 2. ¿Qué se hace con un prefijo de CUIT desconocido?

**Decisión: el identificador es válido; el tipo de persona es `DESCONOCIDO`.**

La validez estructural de un CUIT depende únicamente de la longitud y del dígito
verificador (módulo 11). El prefijo no participa del cálculo.

El mapeo prefijo → tipo de persona es una convención administrativa de ARCA
(ex AFIP), no una regla estructural, y la lista cambia con el tiempo. Rechazar un
CUIT por tener un prefijo que la librería no conoce significaría que una versión
publicada hoy empieza a producir falsos negativos en cuanto se incorpore un
prefijo nuevo. Eso es inaceptable en una librería de validación.

El estado desconocido se modela como miembro del enum, no como nulo:

    enum class TipoPersona { FISICA, JURIDICA, DESCONOCIDO }

Un `TipoPersona?` usaría `null` para dos significados distintos ("no lo sé" y
"no aplica") e invita al `!!` del consumidor apurado. Con el miembro explícito,
el `when` es exhaustivo y quien use la librería tiene que decidir
conscientemente qué hace con el caso desconocido.

La inferencia de tipo es orientativa. ARCA es la fuente de verdad definitiva.

### 3. ¿De dónde sale la tabla de códigos de entidad?

**Decisión: el código va en el value object; el nombre va en un catálogo
separado, embebido por defecto y reemplazable.**

Hay que distinguir dos hechos con estatus distinto:

- El **código de entidad** (`011`, `000`) se deriva de los dígitos. Es
  estructural, no puede desactualizarse, y va como propiedad del value object.
- El **nombre de la entidad** (`Banco de la Nación Argentina`) sale de la nómina
  del BCRA. Es tabular y mutable.

Embeber la tabla dentro del value object contaminaría con datos perecederos algo
que se promete inmutable y siempre correcto. Peor todavía: una tabla vencida no
falla de forma ruidosa, devuelve un nombre incorrecto con total confianza.

Por eso el nombre se resuelve por fuera, detrás de una interfaz:

    interface CatalogoEntidades {
        fun nombre(codigo: String): String?
        companion object { val EMBEBIDO: CatalogoEntidades }
    }

Consecuencias:

- El núcleo de la librería sigue siendo puro: sin I/O, sin dependencias, sin
  fecha de vencimiento.
- Hay una implementación embebida para que funcione sin configuración.
- Quien necesite datos frescos inyecta su propia implementación, contra su base
  o contra una API, sin esperar una release nuestra.
- El código no encontrado devuelve `null`. Es el estado de escape que todo dato
  tabular necesita.

La tabla embebida declara su fecha de vigencia en el KDoc y en el README, y se
actualiza en releases MINOR. El mismo mecanismo cubre los códigos de PSP para
CVU.

---

## Definición de "terminado"

La versión 1.0.0 sale cuando se cumple todo esto:

- [ ] Todos los ítems del alcance implementados
- [ ] Cobertura de tests con casos concretos **y** property-based testing
- [ ] `./gradlew build` pasa en limpio
- [ ] CI en GitHub Actions corriendo los tests en cada push
- [ ] README con: qué hace, qué no hace, cómo se instala, ejemplos de uso,
      y la sección de decisiones de diseño
- [ ] Publicada y consumible desde un proyecto externo