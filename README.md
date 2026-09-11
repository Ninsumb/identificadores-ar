# identificadores-ar

[![CI](https://github.com/Ninsumb/identificadores-ar/actions/workflows/ci.yml/badge.svg)](https://github.com/Ninsumb/identificadores-ar/actions/workflows/ci.yml)

Librería Kotlin/JVM para validar, parsear y formatear identificadores argentinos:
CUIT, CUIL, CBU, CVU, DNI y alias bancario.

> ⚠️ En desarrollo activo. La API puede cambiar hasta la versión 1.0.0.

## Instalación

> ⚠️ **Publicada en JitPack, todavía no en Maven Central.** Maven Central es
> Fase 7 (ver [ALCANCE.md](ALCANCE.md)): cuando llegue, la coordenada va a ser
> otra (`io.github.ninsumb:identificadores-ar`), no la de abajo.

Gradle (Kotlin DSL):

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io") // acá vive identificadores-ar, hasta Fase 7
}

dependencies {
    implementation("com.github.Ninsumb:identificadores-ar:0.1.1")
}
```

Maven:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.Ninsumb</groupId>
  <artifactId>identificadores-ar</artifactId>
  <version>0.1.1</version>
</dependency>
```

> **La coordenada de JitPack no es la del `group` del build ni la del tag de
> Git, tal cual.** JitPack la deriva de la cuenta de GitHub: `com.github.Ninsumb`
> con `N` mayúscula (no `io.github.ninsumb`, que es para Maven Central). Y la
> versión va **sin** el prefijo `v` del tag: el tag de esta versión es
> `v0.1.1`, pero se pide `0.1.1`. Pedir `com.github.Ninsumb:identificadores-ar:v0.1.1`
> (con `v`) rompe la resolución.

Para desarrollo local sin depender de JitPack: `./gradlew publishToMavenLocal`
en un checkout de este repo, y `mavenLocal()` entre los `repositories` de quien
consume.

### Requisitos

- **JVM 17 o superior** en tiempo de ejecución.
- Compilada con **Kotlin 2.1.0**; un consumidor Kotlin necesita una stdlib
  compatible (2.1.x o posterior).
- Sin dependencias de runtime fuera de la stdlib de Kotlin. La API de los
  catálogos expone `java.time.LocalDate`, que es del JDK, no una dependencia.

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
import java.time.LocalDate

class MiCatalogo(
    private val nombres: Map<String, String>,
    override val vigencia: LocalDate,
) : CatalogoEntidades {
    override fun nombre(codigo: String): String? = nombres[codigo]
}

val catalogo = MiCatalogo(
    nombres = mapOf("011" to "Banco de la Nación Argentina"),
    vigencia = LocalDate.of(2026, 9, 1),
)
catalogo.nombre(cbu.codigoEntidad)   // "Banco de la Nación Argentina"
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
[`docs/decisiones/`](docs/decisiones/) para las decisiones de diseño (ADRs).

| Identificador | Estado |
|---|---|
| CUIT / CUIL | ✅ Implementado |
| CBU / CVU | ✅ Implementado |
| DNI | ✅ Implementado |
| Alias bancario | ✅ Implementado |

## Qué no hace

Es la principal fuente de confusión de quien la usa, así que va explícito. Cada
punto tiene su justificación en [`docs/decisiones/`](docs/decisiones/).

**No confirma que el identificador exista ni que esté vigente.**

- No consulta el padrón de ARCA (ex AFIP): la validación es matemática, no
  verifica existencia ni vigencia.
- No valida titularidad: no dice de quién es una cuenta, un CUIT o un alias.
  Esa información no es pública.

**No genera ni transforma identificadores.**

- No genera identificadores nuevos ni implementa la derivación DNI → CUIL con
  reasignación de prefijo. Ver
  [ADR 0004](docs/decisiones/0004-casos-limite-modulo-11.md).
- CUIT y CUIL son la misma estructura (11 dígitos, mismo dígito verificador
  módulo 11): los cubre el tipo `Cuit`, no hay un `Cuil` aparte ni una
  conversión entre ambos.

**No clasifica con autoridad.**

- La inferencia de `tipoPersona` (física / jurídica / desconocido) es
  orientativa, a partir de una lista de prefijos conocidos que cambia con el
  tiempo. ARCA es la fuente de verdad definitiva.

**Sobre el alias bancario.**

- No resuelve a qué CBU o CVU apunta el alias: no existe un mecanismo público.
- `AliasBancario` valida **solo la forma** (longitud 6-20, caracteres
  `[A-Za-z0-9.-]`, canonización a minúsculas): no tiene dígito verificador y la
  librería no hace I/O. Un alias bien formado puede no existir, no estar
  asignado a ninguna cuenta, o estar en la lista de alias prohibidos (lenguaje
  ofensivo, marcas) que administra la cámara compensadora y no es pública.
- No verifica la **unicidad** del alias ("único e irrepetible para todo el
  sistema financiero") ni que no exista ya en el registro central: requiere
  consultar esa base. Ver
  [ADR 0007](docs/decisiones/0007-alias-bancario-validacion-de-forma.md).

**Sobre la validación estructural del DNI.**

- `Dni` valida solo longitud y composición: acepta casi cualquier número de 1 a
  8 dígitos que no sea todo ceros. Su valor está en existir como un tipo propio,
  distinto de un `String` arbitrario, no en lo que filtra. Ver
  [ADR 0006](docs/decisiones/0006-dni-validacion-estructural.md).

**No hace I/O ni sale del dominio argentino.**

- No hace llamadas de red. Todo el cómputo es local y offline.
- No incluye identificadores de otros países.
- No incluye validación de teléfonos ni direcciones.

**No traduce códigos a nombres.** `Cbu` / `Cvu` exponen `codigoEntidad` /
`codigoPsp`, pero el nombre del banco o del PSP se resuelve en un catálogo
aparte (`CatalogoEntidades` / `CatalogoPsp`), reemplazable y con su propia
`vigencia`. Las dos tablas embebidas vienen **vacías** en esta versión, así que
`EMBEBIDO.nombre(...)` devuelve siempre `null` hasta que inyectes tu propia
implementación. Ver [ADR 0008](docs/decisiones/0008-catalogos-de-nombres.md).

## Licencia

MIT
