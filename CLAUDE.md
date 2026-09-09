# identificadores-ar

Librería Kotlin/JVM para validar identificadores argentinos.
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
- Todo tipo público lleva KDoc. Es parte del producto: de ahí sale el
  javadoc JAR que exige Maven Central. No borrar KDoc nunca.
- Los value objects son válidos por construcción: constructor privado
  o internal, y `parse` / `parseOrNull` / `isValid` como única entrada.
- `@JvmStatic` en los companion, para interoperar con Java.

## Antes de dar algo por terminado

1. `./gradlew build` en verde.
2. Marcar el checkbox correspondiente en `ALCANCE.md`.
3. Tildar el módulo en el issue #6 (property testing).

## Qué NO hacer

- No agregar dependencias sin preguntar.
- No implementar nada fuera de `ALCANCE.md`; va a issue con etiqueta `futuro`.
- No inventar valores de prueba: todo CUIT o CBU de test tiene que estar
  verificado a mano, con el cálculo en un comentario.
- No configurar CI ni publicación todavía (Fases 5 y 6).
