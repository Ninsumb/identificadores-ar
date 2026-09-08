# Alcance — identificadores-ar

Librería Kotlin/JVM para validar, parsear y formatear identificadores argentinos.
Sin dependencias de runtime. Usable desde Java.

Este documento define **qué entra y qué no**. Las justificaciones de cada
decisión viven en [`docs/decisiones/`](docs/decisiones/). Todo lo que se me
ocurra después va a un issue etiquetado `futuro`, no al código.

---

## Dentro del alcance

### CUIT / CUIL
- [ ] Validación de dígito verificador (módulo 11)
- [ ] Parseo de componentes: prefijo, cuerpo, dígito verificador
- [ ] Inferencia de tipo de persona (física / jurídica / desconocido) a partir del prefijo
- [ ] Formateo con y sin separadores
- [ ] Tolerancia de entrada: guiones, puntos, espacios, o sin separadores
- [ ] Cálculo del dígito verificador esperado para un cuerpo de 10 dígitos

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
- **No genera identificadores nuevos.** No implementa la derivación DNI → CUIL
  con reasignación de prefijo. Ver [ADR 0004](docs/decisiones/0004-casos-limite-modulo-11.md).
- **No resuelve alias a CBU.** No existe un mecanismo público para hacerlo.
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
| Idioma | Tipos en castellano, miembros en inglés | Los identificadores son constructos legales sin traducción |
| Modo de API | `explicitApi()` activado | Todo lo público es un compromiso de compatibilidad |
| Módulos | Uno solo | No hay razón para fragmentar algo de este tamaño |
| Target JVM | 17 | LTS, ampliamente adoptado |
| Formato de commits | Conventional Commits | Permite derivar el CHANGELOG y la versión semántica automáticamente |
| Documentación de decisiones | ADRs numerados en `docs/decisiones/` | El alcance se estabiliza; las decisiones crecen sin techo. Mezclarlos vuelve ilegible al primero |

---

## Definición de "terminado"

La versión 1.0.0 sale cuando se cumple todo esto:

- [ ] Todos los ítems del alcance implementados
- [ ] Cobertura de tests con casos concretos **y** property-based testing
- [ ] `./gradlew build` pasa en limpio
- [ ] CI en GitHub Actions corriendo los tests en cada push
- [ ] README con: qué hace, qué no hace, cómo se instala, ejemplos de uso,
  y un resumen de las decisiones de diseño
- [ ] Publicada y consumible desde un proyecto externo
