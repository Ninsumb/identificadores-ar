# identificadores-ar

Librería Kotlin/JVM para validar y parsear identificadores argentinos:
CUIT, CUIL, CBU, CVU, DNI y alias bancario.

> ⚠️ En desarrollo activo. La API puede cambiar hasta la versión 1.0.0.

## Estado

En construcción. Ver [ALCANCE.md](ALCANCE.md) para el alcance del proyecto y
las decisiones de diseño.

## Qué no hace

- No consulta el padrón de ARCA (ex AFIP): la validación es matemática, no
  verifica existencia ni vigencia.
- No resuelve alias a CBU: no existe mecanismo público para hacerlo.
- No hace llamadas de red. Todo el cómputo es local.

## Licencia

MIT
