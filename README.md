# identificadores-ar

[![CI](https://github.com/Ninsumb/identificadores-ar/actions/workflows/ci.yml/badge.svg)](https://github.com/Ninsumb/identificadores-ar/actions/workflows/ci.yml)

Librería Kotlin/JVM para validar, parsear y formatear identificadores argentinos:
CUIT, CUIL, CBU, CVU, DNI y alias bancario.

## Instalación

Coordenada de Maven Central: `io.github.ninsumb:identificadores-ar:1.0.0`.

Gradle (Kotlin DSL):

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.ninsumb:identificadores-ar:1.0.0")
}
```

Maven:

```xml
<dependency>
  <groupId>io.github.ninsumb</groupId>
  <artifactId>identificadores-ar</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Alternativa: JitPack

La librería se sigue publicando también en [JitPack](https://jitpack.io) -así
se distribuyó antes de llegar a Maven Central, Fase 7 de `ALCANCE.md`-, con
una coordenada distinta:

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Ninsumb:identificadores-ar:1.0.0")
}
```

> **La coordenada de JitPack no es la del `group` del build.** JitPack la
> deriva de la cuenta de GitHub: `com.github.Ninsumb` con `N` mayúscula (no
> `io.github.ninsumb`, que es la de Maven Central). La versión, igual que en
> Central, va sin el prefijo `v` del tag: el tag es `v1.0.0`, se pide `1.0.0`.

Para desarrollo local sin depender de ninguno de los dos: `./gradlew publishToMavenLocal`
en un checkout de este repo, y `mavenLocal()` entre los `repositories` de quien
consume.

### Requisitos

- **JVM 17 o superior** en tiempo de ejecución. Es el mínimo: la librería está
  compilada con target 17, así que un consumidor en JDK 11 o anterior no puede
  usarla, y Gradle lo va a rechazar en resolución de dependencias, no recién
  al ejecutar. Verificada en runtime sobre **JDK 17 y JDK 21** (ambas LTS),
  caminos Kotlin y Java, contra el artefacto publicado en JitPack.
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
// Los dos valores de abajo están verificados a mano (módulo 10, ponderador
// cíclico 3,1,7,9 aplicado de derecha a izquierda; ver ADR 0005) y son los
// mismos que usa la suite de tests (ClaveBancariaTest):
//   CBU: entidad 011 + sucursal 0059 -> cuerpo1 "0110059", suma 36, dv1 = 4;
//        cuenta 0000000000001 -> suma 3, dv2 = 7.
//   CVU: centinela 000 + PSP 0001 -> cuerpo1 "0000001", suma 3, dv1 = 7;
//        misma cuenta -> dv2 = 7.

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
CatalogoEntidades.EMBEBIDO.vigencia                     // LocalDate: fecha de corte de esta tabla

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

Publicada y estable desde la versión 1.0.0: todo el alcance de
[ALCANCE.md](ALCANCE.md) implementado, con cobertura de tests concretos y
property-based.

| Identificador | Estado |
|---|---|
| CUIT / CUIL | ✅ Implementado |
| CBU / CVU | ✅ Implementado |
| DNI | ✅ Implementado |
| Alias bancario | ✅ Implementado |

### Decisiones de diseño

- **Value objects válidos por construcción.** `Cuit`, `ClaveBancaria` (`Cbu`/`Cvu`), `Dni` y `AliasBancario` no tienen constructor público: si tenés una instancia, ya es válida. La única puerta de entrada es `parse` / `parseOrNull` / `isValid`.
- **Cero dependencias de runtime.** Una librería de validación que arrastra dependencias es un conflicto de versiones ajeno esperando a pasar.
- **El `parse` normaliza, no solo valida.** Guiones, puntos, espacios o mayúsculas de más se resuelven ahí mismo; el valor que queda adentro ya está en su forma canónica, así que `formateado()` y `toString()` son deterministas.
- **Lo estructural está separado de lo tabular.** Validar un CBU (dígitos, dígito verificador) no depende de saber qué banco es el código `011`; esa traducción vive en catálogos inyectables (`CatalogoEntidades` / `CatalogoPsp`) aparte del tipo que valida.
- **Las tablas embebidas vienen vacías a propósito**, no por faltar tiempo: no hay fuente pública confiable para transcribir bancos ni PSPs sin arriesgar un dato mal cargado.

El porqué de cada una, con las alternativas descartadas, está en los [ADRs](docs/decisiones/).

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
