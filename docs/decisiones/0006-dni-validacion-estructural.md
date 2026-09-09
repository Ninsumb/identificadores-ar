# 0006 — DNI: validación estructural sin dígito verificador, forma canónica con ceros

- **Estado:** Aceptada
- **Fecha:** 2026-09-09

## Contexto

El Documento Nacional de Identidad no lleva dígito verificador: es un número de
matrícula correlativo asignado por RENAPER, sin ningún mecanismo matemático de
autocontrol. Confirmado independientemente por la definición de tipo de
información confidencial de Microsoft Purview para el DNI argentino
("Suma de comprobación: No").

Dos preguntas estructurales, antes de implementar:

1. ¿Cuántos dígitos admite un DNI válido, contemplando documentos viejos con
   menos dígitos y ceros a la izquierda?
2. ¿Corresponde validar algún rango numérico?

## Decisión

### Longitud: de 1 a 8 dígitos; forma canónica con ceros a la izquierda hasta 8

Los DNI actuales tienen 8 dígitos: desde septiembre de 2023 los recién
nacidos reciben números que arrancan en 70.000.000 (ver más abajo).

Se acepta cualquier entrada de 1 a 8 dígitos, pero **no** porque "los
documentos viejos tengan menos dígitos". Un número no tiene una cantidad de
dígitos propia, solo una representación con o sin ceros de relleno — y el
relleno que esta misma implementación aplica lo demuestra: el DNI `6` y el
DNI `00000006` no son dos números de distinta longitud, son el mismo número.

La razón real es **tolerancia de entrada**, con el mismo espíritu que
`Cuit.parse` aceptando guiones y puntos: esos separadores no cambian el
número, solo la forma en que alguien lo escribió, y por eso se toleran. Acá
pasa lo mismo con los ceros de relleno: a quien tiene un documento viejo, o
simplemente un número chico, no se le puede exigir que sepa que hace falta
completarlo a ocho cifras para que la librería lo acepte.

El techo de 8 dígitos sí es estructural: es el ancho del número más grande
que RENAPER emite hoy. El piso de 1 dígito no lo es — no hay ningún número
de una sola cifra que sea estructuralmente distinto de uno de ocho —, es
simplemente cuánto hay que tolerar hacia abajo. Ponerle un mínimo más alto
sin una razón estructural que lo respalde sería inventar el mismo tipo de
límite que el [ADR 0002](0002-prefijo-cuit-desconocido.md) evita para los
prefijos de CUIT: nada llegado de afuera obliga a rechazar un número de
cuatro cifras y no uno de tres.

**La forma canónica de `Dni.valor` se completa con ceros a la izquierda
hasta 8 dígitos** — no se guarda tal cual la escribió quien la ingresó.
`Dni.parse("1234567").valor` es `"01234567"`, no `"1234567"`.

La razón es la consistencia entre los tipos de esta librería, no una
preferencia estética. `Cuit.numero` ya devuelve ocho dígitos con sus ceros, y
su propio KDoc dice: "En personas físicas coincide con el DNI." Esa
afirmación deja de ser cierta si `Dni` canonizara al revés: para la persona
titular del CUIT `20-00000006-0` (uno de los casos de test de `Cuit`, DNI
número 6), `cuit.numero` es `"00000006"` pero `Dni.parse("6").valor` sería
`"6"` si no se rellenara — la comparación `cuit.numero == dni.valor` daría
`false` para la misma persona. Con el relleno, da `true`. Además, la forma de
ocho dígitos con ceros ya es la que usa el propio CUIT/CUIL para embeber el
número de documento (es, literalmente, el campo `numero` de `Cuit`): no se
inventa una convención nueva, se adopta la que el organismo ya usa.

### Rechazo del valor cero

`"0"`, `"00000000"` y cualquier variante que canonice a puros ceros son
**inválidas**. No es una validación de rango: es rechazar el elemento nulo de
la numeración, no un valor dentro de una escala que pueda desplazarse. Ningún
documento tiene matrícula 0, y esa afirmación no depende de cuánto crezca la
numeración con el tiempo — no corre el riesgo de envejecer que sí corre
cualquier techo o piso numérico.

### No se valida ningún rango numérico

La razón decisiva es la misma que en el ADR 0002: **cualquier techo que hoy
sea correcto va a rechazar documentos legítimos en cuanto RENAPER emita más
allá**. Es un falso negativo con fecha de vencimiento garantizada, no una
posibilidad remota — ya pasó una vez: la numeración cruzó los 59.999.999 y
en vez de seguir a los 60.000.000 saltó a los 70.000.000.

Ese salto es él mismo relevante, pero por un motivo distinto y más puntual:
muestra que ni siquiera compraría lo que un chequeo de rango promete. La
[Disposición 4678/2019 de RENAPER](https://www.boletinoficial.gob.ar/detalleAviso/primera/223593/20191210)
—leída directamente en el Boletín Oficial para este ADR, no solo por
referencia— reserva los números de 60.000.000 a 69.999.999 exclusivamente
para la adjudicación de CUIL y CUIT **provisorios** de extranjeros: números
que, dentro de ese rango, **no son DNI de nadie**. Un chequeo de rango que
solo mirara "¿está por debajo del máximo emitido?" aceptaría como DNI válido
un número que en ese tramo nunca lo es. No se trata de excluir esa franja
puntualmente —sería la misma tabla perecedera con otro nombre, y ya cambió
una vez—, sino de notar que un rango "plausible" ni siquiera filtra bien los
falsos positivos de hoy, además de estar condenado a producir falsos
negativos mañana.

**Conclusión: sin chequeo de rango.** Solo estructura: longitud entre 1 y 8
dígitos, únicamente dígitos ASCII (ver `Digitos.kt`), sin dígito verificador.

## Qué es este módulo, y qué no

Sin dígito verificador, la validación estructural es casi vacía: comprobar
longitud y que sean dígitos deja pasar prácticamente cualquier número de 1 a
8 cifras que no sea todo ceros. Esto es deliberado, no una limitación de la
implementación — ver la sección anterior.

El valor de `Dni` como tipo **no está en lo que filtra en tiempo de
ejecución** —filtra casi nada—, sino en que existe como un tipo propio en el
sistema de tipos, distinto de `String`. *Parse, don't validate*: una vez que
se tiene un `Dni`, el compilador impide confundirlo con un string arbitrario
en una firma de función, aunque la garantía en tiempo de ejecución sea
mínima. El costo de ese tipo no se paga en validación, se paga en
legibilidad y en la frontera del API.

## Verificación

Ejemplos de canonización, sin cálculo porque no hay dígito verificador que
calcular:

| Entrada | `valor` | ¿Válido? |
|---|---|---|
| `"1"` | `"00000001"` | Sí |
| `"1234567"` | `"01234567"` | Sí |
| `"00000001"` | `"00000001"` (sin cambios) | Sí |
| `"70000001"` | `"70000001"` (sin cambios, ya tiene 8 dígitos) | Sí |
| `"0"` | — | No (valor cero) |
| `"00000000"` | — | No (valor cero) |
| `"123456789"` | — | No (9 dígitos, excede el máximo actual) |
| `""` | — | No (0 dígitos) |

## Consecuencias

- `Dni.valor` siempre tiene 8 caracteres, igual que `Cuit.numero`. La
  comparación `cuit.numero == dni.valor` es válida para verificar que un
  `Cuit` de persona física y un `Dni` corresponden al mismo número,
  independientemente de cuántos dígitos haya escrito quien los ingresó.
- Si RENAPER agota los 8 dígitos actuales y pasa a 9, el límite superior de
  longitud es un cambio MINOR que necesita su propio ADR — igual que agregar
  un prefijo nuevo de CUIT (ADR 0002).
- Esta librería no ofrece (ni ofrecerá bajo este ADR) ninguna función que
  intente inferir año de nacimiento, género u otra información a partir del
  número de DNI. Nada de lo investigado acá lo sugiere como una operación
  confiable, y sería exactamente el tipo de conocimiento tabular y perecedero
  que el ADR 0002 ya identificó como riesgoso.

## Fuentes

Ítem aparte porque tienen estatus distinto:

- **Autoridad normativa:** [Disposición 4678/2019, RENAPER](https://www.boletinoficial.gob.ar/detalleAviso/primera/223593/20191210)
  (Boletín Oficial de la República Argentina, 10/12/2019). Leída
  directamente para este ADR: reserva 60.000.000–69.999.999 para CUIL/CUIT
  provisorios de extranjeros, y establece que la numeración de personas
  continúa desde 70.000.000 una vez agotada la de hasta 59.999.999.
- **Corroboración, no autoridad:** [Microsoft Purview — definición de tipo de
  información confidencial para el DNI argentino](https://learn.microsoft.com/es-es/purview/sit-defn-argentina-national-identity-numbers)
  (2023): formato de ocho dígitos, sin suma de comprobación. Es
  documentación de un producto de terceros, no una fuente normativa
  argentina; se usa acá solo como confirmación independiente de que no hay
  dígito verificador.
- **Corroboración, no autoridad:** [El Cronista — "Cambia el DNI: por qué los
  nuevos documentos no tienen 60 millones y se pasan a los 70
  millones"](https://www.cronista.com/informacion-gral/cambia-el-dni-por-que-los-nuevos-documentos-no-tienen-60-millones-y-se-pasan-a-los-70-millones/):
  nota periodística que sirvió para ubicar la disposición de RENAPER; el dato
  normativo se cita de la disposición misma, no de la nota.
