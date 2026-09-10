# identificadores-ar

[![CI](https://github.com/Ninsumb/identificadores-ar/actions/workflows/ci.yml/badge.svg)](https://github.com/Ninsumb/identificadores-ar/actions/workflows/ci.yml)

Librería Kotlin/JVM para validar y parsear identificadores argentinos:
CUIT, CUIL, CBU, CVU, DNI y alias bancario.

> ⚠️ En desarrollo activo. La API puede cambiar hasta la versión 1.0.0.


## Uso

### CUIT / CUIL

```kotlin
// Validar sin construir nada
Cuit.isValid("20-12345678-6")     // true

// Parsear (tira si es inválido)
val cuit = Cuit.parse("20-12345678-6")
cuit.prefijo                       // "20"
cuit.numero                        // "12345678"
cuit.tipoPersona                   // TipoPersona.FISICA (orientativo, ver más abajo)
cuit.formateado()                  // "20-12345678-6"

// Para input de usuario
val quizas = Cuit.parseOrNull(loQueEscribioElUsuario)
```

Acepta guiones, puntos y espacios como separadores, o ninguno.

`tipoPersona` es orientativo, no una fuente de verdad: se infiere de una lista
de prefijos administrativos que puede cambiar con el tiempo. ARCA (ex AFIP) es
la fuente de verdad definitiva.

### CBU / CVU

```kotlin
// Validar sin construir nada
Cbu.isValid("0110059400000000000017")     // true
Cvu.isValid("0000001700000000000017")     // true

// Parsear un CBU específico (tira si no es CBU, aunque sea un CVU válido)
val cbu = Cbu.parse("0110059400000000000017")
cbu.codigoEntidad                          // "011"
cbu.sucursal                                // "0059"
cbu.numeroCuenta                            // "0000000000001"

// Parsear un CVU específico
val cvu = Cvu.parse("0000001700000000000017")
cvu.codigoPsp                               // "0001"
cvu.numeroCuenta                            // "0000000000001"

// Sin saber de antemano si es CBU o CVU: ClaveBancaria es una interfaz
// sellada, así que el `when` sobre el subtipo es exhaustivo sin `else`.
val clave: ClaveBancaria = ClaveBancaria.parse("0110059400000000000017")
val descripcion = when (clave) {
    is Cbu -> "Banco ${clave.codigoEntidad}, sucursal ${clave.sucursal}"
    is Cvu -> "Billetera virtual, PSP ${clave.codigoPsp}"
}
// descripcion == "Banco 011, sucursal 0059"

// Para input de usuario
val quizasClave = ClaveBancaria.parseOrNull(loQueEscribioElUsuario)
```

Acepta guiones, puntos y espacios como separadores, o ninguno.

Ni `Cbu` ni `Cvu` resuelven el nombre del banco o la billetera: eso está a
propósito fuera del value object. El nombre se resuelve en un catálogo aparte:

```kotlin
// El código es estructural (vive en el value object); el nombre no.
CatalogoEntidades.EMBEBIDO.nombre(cbu.codigoEntidad)   // null: la tabla embebida está vacía en esta versión
CatalogoEntidades.EMBEBIDO.vigencia                     // 2026-09-10

// Con tu propia nómina:
class MiCatalogo(/* ... */) : CatalogoEntidades {
    override val vigencia = /* fecha de corte de tus datos */
    override fun nombre(codigo: String): String? = /* tu lookup */
}
MiCatalogo(/* ... */).nombre(cbu.codigoEntidad)
```

`CatalogoEntidades` (bancos, código de 3 dígitos) y `CatalogoPsp` (PSP, código
de 4 dígitos) son interfaces separadas: las dos nóminas se actualizan por
separado y cada una tiene su `vigencia`. Ambas tablas embebidas vienen vacías
en esta versión, ver "Qué no hace" más abajo.

### DNI

```kotlin
// Validar sin construir nada
Dni.isValid("12345678")     // true

// Parsear (tira si es inválido)
val dni = Dni.parse("1234567")
dni.valor                    // "01234567": siempre 8 dígitos, con ceros a la izquierda
dni.formateado()             // "1.234.567": puntos de miles, sin el cero de relleno

// Para input de usuario
val quizasDni = Dni.parseOrNull(loQueEscribioElUsuario)
```

Acepta guiones, puntos y espacios como separadores, o ninguno, y completa
con ceros a la izquierda hasta 8 dígitos: `Dni.parse("6")` y
`Dni.parse("00000006")` son el mismo `Dni`.

### Alias bancario

```kotlin
// Validar sin construir nada
AliasBancario.isValid("juan.perez.mp")     // true

// Parsear (tira si es inválido)
val alias = AliasBancario.parse("Juan.Perez.Ahorro")
alias.valor         // "juan.perez.ahorro": forma canónica, en minúsculas
alias.original      // "Juan.Perez.Ahorro": lo que se escribió, para mostrar
alias.formateado()  // "juan.perez.ahorro": igual que valor; un alias no tiene nada que formatear

// El uso de mayúsculas es indistinto: estas dos son el mismo alias
AliasBancario.parse("Mi.Alias") == AliasBancario.parse("mi.alias")   // true

// Para input de usuario
val quizasAlias = AliasBancario.parseOrNull(loQueEscribioElUsuario)
```

Longitud de 6 a 20 caracteres; letras, dígitos, `.` y `-`, nada más. Solo
recorta los espacios de los extremos. `original` no participa de la igualdad:
`equals`, `hashCode` y `toString` se basan únicamente en `valor`.

`AliasBancario` no resuelve a qué CBU o CVU apunta el alias: eso está fuera
del alcance, ver "Qué no hace" más abajo.

## Estado

En construcción. Ver [ALCANCE.md](ALCANCE.md) para el alcance del proyecto y
las decisiones de diseño.

| Identificador | Estado |
|---|---|
| CUIT / CUIL | ✅ Implementado |
| CBU / CVU | ✅ Implementado |
| DNI | ✅ Implementado |
| Alias bancario | ✅ Implementado |

## Qué no hace

- No consulta el padrón de ARCA (ex AFIP): la validación es matemática, no
  verifica existencia ni vigencia.
- No garantiza el tipo de persona: la inferencia de `tipoPersona` es
  orientativa, a partir de una lista de prefijos conocidos. ARCA (ex AFIP) es
  la fuente de verdad definitiva.
- No resuelve alias a CBU: no existe mecanismo público para hacerlo.
- El alias bancario no tiene dígito verificador y la librería no consulta el
  registro central, así que `AliasBancario` valida solo la forma: longitud
  6-20 y caracteres `[A-Za-z0-9.-]`, con canonización a minúsculas. Un alias
  bien formado puede no existir, no estar asignado a ninguna cuenta, o estar
  en la lista de alias prohibidos (lenguaje ofensivo, marcas) que administra
  la cámara compensadora y que no es pública.
- No resuelve el nombre del banco ni del PSP a partir del código: expone
  `codigoEntidad`/`codigoPsp`, pero traducirlos a un nombre es responsabilidad
  de un catálogo (`CatalogoEntidades` / `CatalogoPsp`), reemplazable y con su
  propia fecha de vigencia. **Las dos tablas embebidas vienen vacías en esta
  versión**: la de PSP por decisión (no hay fuente oficial pública del código
  de ruteo), la de bancos porque no se pudo transcribir el anexo oficial del
  BCRA todavía. `EMBEBIDO.nombre(...)` devuelve `null` hasta que inyectes tu
  propia implementación. Ver
  [ADR 0008](docs/decisiones/0008-catalogos-de-nombres.md).
- No hace llamadas de red. Todo el cómputo es local.
- El DNI no tiene dígito verificador, así que `Dni` valida solo longitud y
  composición: acepta prácticamente cualquier número de 1 a 8 dígitos que no
  sea todo ceros. Su valor como tipo no está en filtrar input -filtra casi
  nada-, sino en existir como un tipo propio, distinto de un `String`
  arbitrario.

## Licencia

MIT
