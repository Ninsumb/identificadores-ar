# Alcance — identificadores-ar

Librería Kotlin/JVM para validar, parsear y formatear identificadores argentinos.
Sin dependencias de runtime. Usable desde Java.

Este documento define **qué entra y qué no**. Las justificaciones de cada
decisión viven en [`docs/decisiones/`](docs/decisiones/). Todo lo que se me
ocurra después va a un issue etiquetado `futuro`, no al código.

---

## Dentro del alcance

### CUIT / CUIL
- [x] Validación del dígito verificador (módulo 11)
- [x] Parseo de componentes: prefijo, número, dígito verificador
- [x] Inferencia de tipo de persona (física / jurídica / desconocido) a partir del prefijo
- [x] Formateo con y sin separadores
- [x] Tolerancia de entrada: guiones, puntos, espacios, o sin separadores

### CBU / CVU
- [x] Validación de la estructura de 22 dígitos en dos bloques
- [x] Validación del doble dígito verificador
- [x] Extracción de código de entidad y sucursal (CBU), código de PSP (CVU), y número de cuenta
- [x] Formateo con los dos bloques separados por un espacio
- [x] Lookup del nombre de la entidad a partir del código (mecanismo:
  interfaces `CatalogoEntidades` / `CatalogoPsp`, con `vigencia` y `EMBEBIDO`).
  Las dos tablas embebidas vienen **vacías** en 1.0.0 —la de PSP por no haber
  fuente oficial pública, la de bancos por no poder transcribir el anexo del
  BCRA—; se cargan en una MINOR. Ver
  [ADR 0008](docs/decisiones/0008-catalogos-de-nombres.md).

### DNI
- [x] Validación estructural (longitud y composición, sin dígito verificador)
- [x] Tolerancia de entrada: guiones, puntos, espacios, o sin ceros a la izquierda
- [x] Formateo con puntos de miles

### Alias bancario
- [x] Validación de formato (longitud y caracteres permitidos)
- [x] Canonización a minúsculas (el alias es insensible a mayúsculas por norma del BCRA)

---

## Fuera del alcance — explícito

Estas cosas NO las hace la librería. Van documentadas en el README porque son la
fuente principal de confusión de quien la use.

- **No consulta el padrón de ARCA (ex AFIP).** La validación es puramente
  matemática: verifica que el identificador esté bien formado, no que exista ni
  que esté activo.
- **No genera identificadores nuevos.** No implementa la derivación DNI → CUIL
  con reasignación de prefijo. Ver [ADR 0004](docs/decisiones/0004-casos-limite-modulo-11.md).
- **No resuelve alias a CBU.** No existe un mecanismo público para hacerlo.
- **No aplica la lista de alias reservados ni valida su unicidad.** La nómina de
  alias prohibidos (lenguaje ofensivo, marcas) y el registro de unicidad los
  administra la cámara compensadora y no son públicos. `AliasBancario` valida
  solo la forma. Ver [ADR 0007](docs/decisiones/0007-alias-bancario-validacion-de-forma.md).
- **No valida titularidad de cuentas.** Esa información no es pública.
- **No hace llamadas de red.** Cero I/O. Todo el cómputo es local y offline.
- **No incluye identificadores de otros países.**
- **No incluye validación de teléfonos ni direcciones.**

---

## Decisiones técnicas

Decisiones de proyecto e implementación. Las de dominio, que necesitan
justificación extensa, están en [`docs/decisiones/`](docs/decisiones/).

| Decisión | Elección | Motivo |
|---|---|---|
| Modelado | Value objects, no strings | *Parse, don't validate*: si tenés un `Cuit`, es válido por construcción |
| API de entrada | `parse` / `parseOrNull` / `isValid` | Cada una sirve a un contexto distinto (bug, input de usuario, filtrado) |
| Dependencias de runtime | Ninguna | Una dependencia arrastrada es un conflicto de versiones potencial |
| Interop con Java | `@JvmStatic` en los companion | Que se vea como `Cuit.parse()` y no como `Cuit.Companion.parse()` |
| Idioma | Verbos de entrada (`parse` / `parseOrNull` / `isValid`) y overrides de la JVM (`equals` / `hashCode` / `toString`) en inglés; tipos y vocabulario de dominio (`Cuit`, `valor`, `prefijo`, `digitoVerificador`, `formateado`…) en castellano | Los verbos de entrada y los overrides son API idiomática de Kotlin/JVM; los identificadores y sus componentes son constructos legales sin traducción |
| Modo de API | `explicitApi()` activado | Todo lo público es un compromiso de compatibilidad |
| Módulos | Uno solo | No hay razón para fragmentar algo de este tamaño |
| Target JVM | 17 | LTS, ampliamente adoptado |
| Formato de commits | Conventional Commits | Permite derivar el CHANGELOG y la versión semántica automáticamente |
| Documentación de decisiones | ADRs numerados en `docs/decisiones/` | El alcance se estabiliza; las decisiones crecen sin techo. Mezclarlos vuelve ilegible al primero |

---

## Definición de "terminado"

La versión 1.0.0 sale cuando se cumple todo esto:

- [x] Todos los ítems del alcance implementados
- [x] Cobertura de tests con casos concretos **y** property-based testing
- [x] `./gradlew build` pasa en limpio
- [x] CI en GitHub Actions corriendo los tests en cada push
- [x] README con: qué hace, qué no hace, cómo se instala, ejemplos de uso,
  y un resumen de las decisiones de diseño
- [x] **Fase 6 — JitPack.** Publicada en JitPack (coordenada
  `com.github.Ninsumb:identificadores-ar`, versión derivada del tag de Git) y
  verificada desde un proyecto de prueba externo que la consuma **desde Kotlin
  y desde Java**. Ver la sección Instalación del README para la coordenada
  exacta y la distinción entre el tag (`v0.1.1`) y la versión resuelta
  (`0.1.1`).
- [x] **Sacar el aviso de inestabilidad, en el mismo commit que tagea 1.0.0.**
  El `⚠️ En desarrollo activo...` del encabezado y el `En construcción` de la
  sección Estado del README tienen que salir (o reescribirse) en ese commit,
  no después. Una versión estable publicada en Central que se sigue
  anunciando como inestable es una contradicción que queda para siempre.
- [x] **Fase 7 — Maven Central.** Publicada en Maven Central (coordenada
  `io.github.ninsumb:identificadores-ar:1.0.0`, POM completo, javadoc JAR con
  Dokka y firma GPG) y verificada desde un proyecto de prueba externo que la
  consuma **desde Kotlin y desde Java**, sobre **JDK 17 y JDK 21**. Ver la
  sección Instalación del README para la coordenada y el link a la
  documentación en javadoc.io.

La publicación en **Maven Central** bajo `io.github.ninsumb` (Fase 7) ya está
hecha: ver el ítem de arriba. El `group` del build apunta ahí.
