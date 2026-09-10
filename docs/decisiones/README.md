# Decisiones de arquitectura (ADRs)

Cada archivo de esta carpeta documenta una decisión de diseño: el contexto en el
que se tomó, la decisión en sí, y lo que se sigue de ella.

## Reglas

- **Un archivo por decisión**, numerado correlativamente. El número no se reusa.
- **Los ADR aceptados no se editan.** Si una decisión cambia, se escribe uno
  nuevo que la reemplaza, y el viejo pasa a estado `Reemplazada por NNNN`. El
  objetivo es conservar el rastro de por qué se pensaba lo que se pensaba.
- Se admiten correcciones de tipeo o de formato, no de contenido.
- El código puede referenciarlos: `// Ver docs/decisiones/0004-casos-limite-modulo-11.md`.
- **El encabezado de cada ADR lleva solo `Estado` y `Fecha`.** El número y la
  fecha ya ordenan cronológicamente; no se agregan más campos ahí.

## Estados posibles

| Estado | Significado |
|---|---|
| `Propuesta` | En discusión, todavía no se implementó |
| `Aceptada` | Vigente, implementada o en implementación |
| `Reemplazada por NNNN` | Ya no aplica; ver el ADR indicado |
| `Descartada` | Se evaluó y no se adoptó. Se conserva por el razonamiento |

## Índice

| # | Decisión | Estado |
|---|---|---|
| [0001](0001-cbu-vs-cvu.md) | CBU y CVU son dos tipos bajo una interfaz sellada | Aceptada |
| [0002](0002-prefijo-cuit-desconocido.md) | Un prefijo de CUIT desconocido no invalida el identificador | Aceptada |
| [0003](0003-catalogo-de-entidades.md) | El nombre de entidad se resuelve en un catálogo reemplazable | Aceptada |
| [0004](0004-casos-limite-modulo-11.md) | Casos límite del módulo 11 en CUIT/CUIL | Aceptada |
| [0005](0005-modulo-10-cbu-cvu.md) | Módulo 10 en CBU/CVU: por qué no necesita casos límite | Aceptada |
| [0006](0006-dni-validacion-estructural.md) | DNI: validación estructural sin dígito verificador, forma canónica con ceros | Aceptada |
| [0007](0007-alias-bancario-validacion-de-forma.md) | Alias bancario: validación de forma, identidad en la forma canónica en minúsculas | Aceptada |
| [0008](0008-catalogos-de-nombres.md) | Catálogos de nombres: dos interfaces inyectables, tablas embebidas vacías en 1.0.0 | Aceptada |
