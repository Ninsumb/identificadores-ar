# 0007 — Alias bancario: validación de forma, identidad en la forma canónica en minúsculas

- **Estado:** Aceptada
- **Fecha:** 2026-09-09

## Contexto

El "alias CBU" (o alias CBU/CVU) es una etiqueta legible que el BCRA permite
asociar a una CBU o CVU para recibir transferencias, en reemplazo de tipear
los 22 dígitos. Lo creó la Comunicación "A" 6044 (2016); las reglas de forma
vigentes están en el texto ordenado "Sistema Nacional de Pagos – Servicios de
pago", Sección 3 "Identificación por alias", punto 3.7.2.1, según la
Comunicación "A" 8114 (08/10/2024).

A diferencia de la CBU/CVU, el alias no tiene dígito verificador ni ningún
mecanismo de autocontrol. Y a diferencia del DNI, ni siquiera tiene una
longitud o un rango estructural del que sacar algo: es texto libre dentro de
un charset acotado. Toda la validación posible offline es de forma.

Preguntas antes de implementar:

1. ¿Qué reglas de forma fija la normativa: longitud, caracteres, mayúsculas?
2. El alias es insensible a mayúsculas por norma. ¿Qué implica eso para la
   identidad del value object — `equals`, `hashCode`, `toString`?
3. ¿Se guarda el texto tal como se ingresó, o normalizado?
4. ¿Hay que rechazar un alias que "parece" una CBU/CVU?
5. ¿Qué relación tiene con la jerarquía sellada `ClaveBancaria`?

## Decisión

### Reglas de forma (texto ordenado 3.7.2.1.i)

- **Longitud: de 6 a 20 caracteres.** La Comunicación "A" 6044 original fijaba
  un máximo de 14; quedó ampliado en el texto ordenado vigente.
- **Caracteres admitidos, y ningún otro:** dígitos ASCII `0`-`9`, letras ASCII
  `A`-`Z` y `a`-`z`, y los dos signos `.` (punto) y `-` (guion medio). El texto
  es explícito: *"El resto de los caracteres se considerarán inválidos."* No
  hay guion bajo, ni espacios, ni acentos, ni `ñ`.
- **Mayúsculas indistintas.** Textual: *"El uso de mayúsculas y minúsculas será
  indistinto (no se distinguirá entre uno y otro)."* `Mi.Alias` y `mi.alias`
  son el mismo alias para el sistema financiero.

Único tratamiento de entrada: se recortan los espacios de los extremos
(`trim`), ruido inequívoco de copiar y pegar. No se quita ni se altera nada
más — en particular, el `.` y el `-` son parte del alias, no separadores como
en `Cuit` o `Dni`, así que `AliasBancario` **no** usa `quitarSeparadores`.

### La identidad es la forma canónica en minúsculas; `equals` queda trivial

**`AliasBancario.valor` guarda la forma canónica: el texto recortado, pasado a
minúsculas con `lowercase(Locale.ROOT)`.** La normalización ocurre una sola
vez, en el `parse`.

Con eso, `equals` y `hashCode` se definen sobre `valor` exactamente como en
`Cbu`, `Cvu` y `Dni` — `valor == other.valor`, `valor.hashCode()` — sin ningún
caso especial de comparación insensible a mayúsculas. La insensibilidad ya
está resuelta antes, en la canonización.

Esto es la aplicación de un principio que este proyecto ya usó dos veces y que
acá queda asentado como **regla transversal**:

> La normalización vive en el `parse`. La identidad de todo value object es su
> forma canónica. `equals`, `hashCode` y `toString` se mantienen triviales
> —sobre `valor`— en todos los tipos.

El `Dni` lo hace rellenando con ceros a la izquierda hasta ocho dígitos (ver
[ADR 0006](0006-dni-validacion-estructural.md)); el `Cuit` normaliza
separadores; el alias baja a minúsculas. En los tres casos, dos entradas que
el dominio considera el mismo identificador producen instancias `equals`, y
ningún método público sin argumentos las distingue.

**`toString()` devuelve `valor`** (la canónica), igual que todos los demás
tipos y coherente con `equals`.

#### `lowercase(Locale.ROOT)`, no `lowercase()`

Obligatorio. `String.lowercase()` sin argumento usa el locale por defecto de
la JVM. En una JVM con locale turco (`tr-TR`), la `I` ASCII (U+0049) no baja a
`i` (U+0069) sino a `ı` (i sin punto, U+0131). Una canonización dependiente
del locale de la máquina donde corre es un bug silencioso: el mismo alias
produciría `valor` distintos en dos servidores. `Locale.ROOT` fija la regla.
Va con el motivo en un comentario en el código, y hay un test property-based
que verifica que `valor` es estable para cualquier locale por defecto.

### `original`: informativa, fuera de la identidad

Se expone además **`AliasBancario.original`: el texto recortado con su
capitalización intacta**, para mostrarle a la persona lo que escribió (por
ejemplo en una pantalla de confirmación).

`original` **no participa de la identidad.** `equals`, `hashCode` y `toString`
la ignoran por completo. Que un value object tenga dos propiedades tipo
`String` invita a suponer que las dos lo definen; no es el caso, y el KDoc de
la propiedad lo dice explícitamente, no solo este ADR.

Se preserva porque la capitalización la eligió la persona a propósito —no es
ruido como los guiones de un CUIT— y es barato guardarla. Con `explicitApi()`
y compromiso de compatibilidad hacia 1.0, incluirla ahora evita un cambio
breaking después.

### `formateado()` devuelve `valor`

La regla del proyecto exige `formateado()` en todo value object. Un alias **no
tiene separadores que insertar**: el `.` y el `-` ya son parte del valor, y no
hay ninguna convención de visualización que el sistema bancario aplique. Así
que `formateado()` devuelve lo mismo que `valor` y que `toString()`.

Existe igual, y por una sola razón: **uniformidad de API.** Permite escribir
código que llama `.formateado()` sobre cualquier identificador de la librería
sin conocer el tipo concreto. El KDoc lo dice así —que devuelve la forma
canónica sin cambios porque no hay nada que formatear—, no finge que hace un
trabajo que no hace.

No devuelve `original`, y es deliberado:

- Dos instancias `equals` deben producir el mismo resultado en un método
  público sin argumentos. `AliasBancario.parse("Juan.Perez")` y
  `AliasBancario.parse("juan.perez")` son iguales; no pueden diferir en
  `formateado()`.
- En los otros tres tipos, `formateado()` es una **función pura de la forma
  canónica**. Si acá dependiera del input, el mismo nombre significaría dos
  cosas distintas en la misma librería.

El caso de uso de mostrar lo que escribió la persona lo cubre `original`.

### Un alias con forma de CBU/CVU no se rechaza

La normativa no prohíbe un alias íntegramente numérico. Los dígitos son
caracteres válidos (3.7.2.1.i), no hay ninguna regla de "mínimo una letra", y
el propio generador automático del BCRA (3.8) puede reemplazar una palabra por
"un número de hasta cuatro dígitos".

- Un string de **22 dígitos** —la forma de una CBU o CVU— ya es inválido **por
  longitud**: excede el máximo de 20. Lo rechaza una regla real del BCRA, no
  una nuestra.
- Un string numérico de **6 a 20 dígitos es un alias válido** y se acepta.
  Rechazarlo por "parece una clave bancaria" sería inventar una regla y
  atribuírsela al BCRA — el mismo error que el
  [ADR 0002](0002-prefijo-cuit-desconocido.md) evita con los prefijos de CUIT,
  y que además acá contradice el texto normativo.

### `AliasBancario` es independiente de `ClaveBancaria`

No entra en la jerarquía sellada. Es un tipo hermano de `Cuit`, `Dni` y
`ClaveBancaria`, no un subtipo de nada.

La interfaz sellada `ClaveBancaria` modela una clave de 22 dígitos con doble
verificador módulo 10: `Cbu` y `Cvu` comparten longitud, estructura en
bloques, algoritmo y dígito verificador, y por eso esos miembros viven en la
interfaz. Un alias no comparte **nada** de eso: otra longitud, otro charset,
sin bloques, sin verificador, insensible a mayúsculas. Meterlo bajo
`ClaveBancaria` rompería la exhaustividad del `when` sin `else` —el beneficio
central del [ADR 0001](0001-cbu-vs-cvu.md)— y lo obligaría a heredar miembros
sin sentido (`bloque1`, `numeroCuenta`, `digitoVerificadorBloque1`).

Lo único en común es semántico: un alias y una CBU pueden identificar la misma
cuenta destino. Pero un alias **apunta** a una CBU/CVU sin ser una, y esta
librería no puede resolver a cuál: no hace I/O y no existe un mecanismo
público de resolución (ver [ADR 0003](0003-catalogo-de-entidades.md) y
`ALCANCE.md`). La forma de API compartida (`valor`, `parse` / `parseOrNull` /
`isValid`, `formateado()`, los overrides) es una convención de toda la
librería, no herencia.

## Qué es este módulo, y qué no

Es el más flojo de los cuatro identificadores, y hay que decirlo con la misma
crudeza que el [ADR 0006](0006-dni-validacion-estructural.md) puso para el DNI.

Sin dígito verificador y sin poder consultar nada, **la validación es
únicamente de forma**: longitud entre 6 y 20, y caracteres dentro de
`[A-Za-z0-9.-]`, con canonización a minúsculas. Eso es todo lo que
`AliasBancario` garantiza.

Un alias sintácticamente perfecto **puede no existir, no estar asignado a
ninguna cuenta, o estar prohibido.** En particular:

- La normativa (3.7.2.1.ii) establece que hay **una lista de alias que no se
  pueden usar** —lenguaje ofensivo, y marcas registradas—. Esa lista la
  administra la cámara compensadora (CEC-BV), **no es pública** y es mutable.
  `AliasBancario` no la aplica ni puede aplicarla offline.
- La **unicidad** ("único e irrepetible para todo el sistema financiero",
  3.6) y la **no preexistencia en la base** (3.7.2.1.ii) requieren consultar
  el registro central. Fuera de alcance.

Igual que en `Dni`, el valor de este tipo no está en lo que filtra en tiempo
de ejecución —filtra poco— sino en existir como un tipo propio, distinto de un
`String` arbitrario, en la frontera del API. *Parse, don't validate.*

## Lo que no se pudo verificar

Se listan aparte porque no son decisiones sino límites de lo que la normativa
dice, o de lo que la investigación pudo establecer:

- **Restricciones de forma sobre los separadores.** El texto ordenado 3.7.2.1
  no dice nada sobre si un alias puede empezar o terminar con `.` o `-`, ni
  sobre separadores consecutivos (`..`, `--`, `.-`). Algunas implementaciones
  de home banking agregan esas restricciones, pero no salen de la normativa.
  Siguiendo el criterio del ADR 0006: no se inventan. `AliasBancario` acepta
  `.mi.alias`, `mi.alias.` y `mi..alias` mientras respeten longitud y charset.
- **Contenido de la lista de alias bloqueados.** Su existencia está en
  3.7.2.1.ii; su contenido no es público. No implementable.
- **Qué comunicación intermedia amplió el máximo de 14 a 20 y agregó el
  charset explícito.** El texto ordenado bajo la Comunicación "A" 8114 lista
  varios orígenes para el punto 3.7 (Com. "A" 6109, 6510, 7533, 7783 y Boletín
  CIMPRA 516) y no se pudo aislar cuál introdujo la regla de conformación
  vigente. El texto ordenado es autoridad suficiente para la regla; el rastro
  exacto de la enmienda es un límite de esta investigación.

## Verificación

No hay dígito verificador que calcular. Ejemplos de canonización y validez:

| Entrada | `valor` | `original` | ¿Válido? |
|---|---|---|---|
| `"mi.alias"` | `"mi.alias"` | `"mi.alias"` | Sí |
| `"Mi.Alias"` | `"mi.alias"` | `"Mi.Alias"` | Sí |
| `"  Juan-Perez  "` | `"juan-perez"` | `"Juan-Perez"` | Sí (se recortan los extremos) |
| `"MI.ALIAS"` en JVM locale `tr-TR` | `"mi.alias"` | `"MI.ALIAS"` | Sí (`Locale.ROOT`, no `ı`) |
| `"1234567890"` | `"1234567890"` | `"1234567890"` | Sí (numérico, 10 dígitos) |
| `".mi.alias."` | `".mi.alias."` | `".mi.alias."` | Sí (la norma no lo prohíbe) |
| `"corto"` | — | — | No (5 caracteres) |
| `"este.alias.es.demasiado.largo"` | — | — | No (28 caracteres) |
| `"mi_alias"` | — | — | No (`_` no admitido) |
| `"mi alias"` | — | — | No (espacio interior) |
| `"aliás"` | — | — | No (`á` no admitida) |
| `"0110000700000000000123"` | — | — | No (22 caracteres, excede 20) |
| `""` | — | — | No |

`"Mi.Alias"` y `"mi.alias"` producen instancias `equals`, con el mismo
`hashCode`, el mismo `toString()` y el mismo `formateado()`; difieren solo en
`original`.

## Consecuencias

- `AliasBancario` sigue la forma de API del resto de la librería sin heredar
  de `ClaveBancaria`. El `when` exhaustivo sobre `ClaveBancaria` no cambia.
- `equals` / `hashCode` / `toString` quedan idénticos en estructura a los de
  `Cbu`, `Cvu` y `Dni`: sobre `valor`. El principio "normalizar en el parse,
  identidad canónica, equals trivial" queda como regla explícita del proyecto,
  ya aplicada en `Cuit`, `Dni` y ahora `AliasBancario`.
- `original` es API pública desde 1.0. Sacarla después sería breaking; por eso
  se incluye ahora aunque su rol sea solo informativo.
- Si el BCRA cambiara la longitud, el charset o la regla de mayúsculas, es un
  cambio que necesita su propio ADR.
- Si en algún momento se quisiera aplicar la lista de bloqueados o la
  verificación de unicidad, sería con el mecanismo del
  [ADR 0003](0003-catalogo-de-entidades.md) —una interfaz inyectable con
  estado de escape—, nunca embebido en el value object. Va a issue con
  etiqueta `futuro`.

## Fuentes

Separadas por estatus, como en el ADR 0006:

- **Autoridad normativa:** [BCRA — texto ordenado "Sistema Nacional de Pagos –
  Servicios de pago"](https://www.bcra.gob.ar/archivos/Pdfs/comytexord/A8114.pdf),
  Sección 3 "Identificación por alias", puntos 3.6, 3.7.2.1 (i) y (ii), y 3.8,
  según **Comunicación "A" 8114 (08/10/2024)**. Leído directamente del PDF
  oficial para este ADR: fija longitud 6–20, el charset `[A-Za-z0-9.-]` con
  "el resto de los caracteres se considerarán inválidos", y "el uso de
  mayúsculas y minúsculas será indistinto".
- **Autoridad normativa, referencia histórica:** [BCRA — Comunicación "A" 6044
  (17/08/2016)](https://www.bcra.gob.ar/archivos/Pdfs/comytexord/A6044.pdf),
  Anexo, punto 5. Creó la facilidad "alias CBU" y fijaba un máximo de 14
  caracteres, sin mínimo, sin charset y sin regla de mayúsculas. Leída
  directamente; se cita para mostrar que la regla de forma vigente es
  posterior.
- **Corroboración, no autoridad:** [argentina.gob.ar — "Ley simple: Alias
  CBU"](https://www.argentina.gob.ar/justicia/derechofacil/leysimple/alias-cbu)
  y prensa (La Nación). Coinciden en "6 a 20 caracteres, letras, números,
  punto y guion medio". Son divulgación, no fuente normativa; se usan solo
  como confirmación independiente de la lectura del texto del BCRA.
