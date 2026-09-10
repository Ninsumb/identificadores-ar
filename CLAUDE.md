# identificadores-ar

Librería Kotlin/JVM para validar, parsear y formatear identificadores argentinos.
Sin dependencias de runtime. Publicable en Maven Central.

## Antes de tocar nada, leer

- `ALCANCE.md` — qué entra y qué no. Es vinculante.
- `docs/decisiones/` — ADRs numerados. Las decisiones ya tomadas
  NO se rediscuten; si algo cambia, se escribe un ADR nuevo.

## Reglas del proyecto

- Kotlin, JVM 17, `explicitApi()` activado.
- **Cero dependencias de runtime.** Solo de test.
- Commits en formato Conventional Commits, en imperativo y castellano.
- Todo módulo lleva tests concretos **y** property-based con Kotest.
- Todo miembro público lleva KDoc, sin excepción para overrides triviales
  (`equals`, `hashCode`, `toString`) ni para el companion object. Es parte
  del producto: de ahí sale el javadoc JAR que exige Maven Central. No
  borrar KDoc nunca.
- Los value objects son válidos por construcción: constructor privado
  o internal, y `parse` / `parseOrNull` / `isValid` como única entrada.
  Todos exponen además `formateado()` (una representación legible del valor
  canónico; que agregue separadores u otra puntuación depende del tipo —el
  alias no tiene nada que formatear y devuelve el valor tal cual, ver
  [ADR 0007](docs/decisiones/0007-alias-bancario-validacion-de-forma.md)) y los
  overrides de `toString`/`equals`/`hashCode`. En un
  módulo con más de un tipo (como `ClaveBancaria`/`Cbu`/`Cvu`), lo que sea
  común a todos los subtipos se define una sola vez en el tipo compartido,
  no repetido en cada uno.
- `@JvmStatic` en los companion, para interoperar con Java.
- Las constantes privadas de cada módulo (`private const val` / `private val`)
  van a nivel de archivo, no anidadas en el companion — incluso si el módulo
  tiene un solo tipo. Es necesario para los módulos con más de un tipo (como
  `ClaveBancaria`/`Cbu`/`Cvu`, que comparten constantes entre companions
  distintos) y se aplica parejo para que la ubicación no dependa de cuántos
  tipos tenga el archivo.

## Antes de dar algo por terminado

1. `./gradlew build` en verde.
2. Marcar el checkbox correspondiente en `ALCANCE.md`.
3. Actualizar en el mismo pase la tabla de estado del README y agregar los
   ejemplos de uso del módulo. No alcanza con tildar `ALCANCE.md`.
4. Tildar el módulo en el issue #6 (property testing).

## Qué NO hacer

- No agregar dependencias sin preguntar.
- No implementar nada fuera de `ALCANCE.md`; va a issue con etiqueta `futuro`.
- No inventar valores de prueba: todo CUIT o CBU de test tiene que estar
  verificado a mano, con el cálculo en un comentario.
- No completar la configuración de publicación todavía (Fase 6). El build ya
  aplica el plugin `maven-publish` con un esqueleto de `publishing { }`: es
  deliberado y **no se saca**, pero está incompleto (falta el POM con licencia
  y desarrolladores, la firma de artefactos y el repositorio destino). Todo eso
  es Fase 6. El CI (Fase 5) ya está: ver `.github/workflows/ci.yml`.
