# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/),
y este proyecto adhiere a [Versionado Semántico](https://semver.org/lang/es/).
Los commits siguen [Conventional Commits](https://www.conventionalcommits.org/es/),
de donde se deriva este changelog.

## [Unreleased]

## [1.0.0] - 2026-09-11

### Added

- Publicación en Maven Central bajo la coordenada
  `io.github.ninsumb:identificadores-ar:1.0.0`, con POM completo, javadoc JAR
  generado con Dokka y firma GPG de los artefactos.
- Documentación de la API publicada en
  [javadoc.io](https://javadoc.io/doc/io.github.ninsumb/identificadores-ar/latest/index.html).
- Resumen de las decisiones de diseño en el README, con enlace a los ADRs.
- Verificación de runtime sobre JDK 17 y JDK 21, desde Kotlin y desde Java.

### Changed

- La librería deja de anunciarse como "en desarrollo activo": el aviso de
  inestabilidad del README se retira en el mismo commit que corta esta
  versión.

## [0.1.1] - 2026-09-10

### Added

- Publicación en JitPack (Fase 6) bajo la coordenada
  `com.github.Ninsumb:identificadores-ar`, con la versión derivada del tag de
  Git, verificada desde un proyecto externo en Kotlin y en Java.

### Fixed

- `CatalogoPsp.EMBEBIDO` pasa a exponerse con `@JvmStatic` (antes
  `@JvmField`), para que la interoperabilidad con Java sea consistente con
  `CatalogoEntidades.EMBEBIDO`.

## [0.1.0] - 2026-09-10

Primera versión funcional, con todo el alcance de `ALCANCE.md` implementado.

### Added

- **CUIT/CUIL**: validación del dígito verificador (módulo 11), parseo de
  prefijo/número/dígito verificador, inferencia orientativa del tipo de
  persona, formateo con y sin separadores.
- **CBU/CVU**: validación de la estructura de 22 dígitos y el doble dígito
  verificador (módulo 10), extracción de entidad/sucursal (CBU), PSP (CVU) y
  número de cuenta, formateo con los dos bloques separados, y catálogos
  inyectables de nombres (`CatalogoEntidades` / `CatalogoPsp`, embebidos vacíos
  en esta versión).
- **DNI**: validación estructural, tolerancia de separadores y ceros a la
  izquierda, formateo con puntos de miles.
- **Alias bancario**: validación de formato y canonización a minúsculas.
- Tests concretos y property-based (Kotest) para los cuatro módulos.
- CI en GitHub Actions (build + tests en cada push).
