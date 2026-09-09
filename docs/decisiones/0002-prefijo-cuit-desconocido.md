# 0002 — Un prefijo de CUIT desconocido no invalida el identificador

- **Estado:** Aceptada
- **Fecha:** 2026-09-08

## Contexto

Los dos primeros dígitos del CUIT identifican el tipo de contribuyente: 20, 23,
24 y 27 para personas físicas; 30, 33 y 34 para personas jurídicas. Hay otros
prefijos en circulación, y la lista se modifica con el tiempo.

Ese mapeo es una convención administrativa de ARCA (ex AFIP), no una regla
estructural: **el prefijo no participa del cálculo del dígito verificador**. La
validez matemática de un CUIT depende únicamente de su longitud y de que el
dígito de control cierre.

La pregunta era qué hacer cuando llega un CUIT con dígito verificador correcto
pero prefijo no reconocido.

## Decisión

El identificador es **válido**. El tipo de persona se reporta como
`TipoPersona.DESCONOCIDO`.

    enum class TipoPersona { FISICA, JURIDICA, DESCONOCIDO }

El estado desconocido se modela como **miembro del enum, no como nulo**.

### Alternativas descartadas

**Rechazar el CUIT.** Significaría que una versión publicada hoy empieza a
producir falsos negativos en cuanto ARCA incorpore un prefijo nuevo. Una
librería de validación que rechaza identificadores legítimos es peor que una
que no valida nada, porque falla en silencio y con confianza.

**Usar `TipoPersona?` con `null` para desconocido.** El `null` cargaría dos
significados distintos ("no lo sé" y "no aplica") e invita al `!!` del
consumidor apurado. Con el miembro explícito, el `when` es exhaustivo y quien
use la librería tiene que decidir conscientemente qué hace con ese caso.

## Consecuencias

- La validación estructural y la clasificación semántica quedan desacopladas.
  Un prefijo nuevo no rompe la validación.
- La inferencia de tipo es **orientativa**. ARCA es la fuente de verdad
  definitiva y así se documenta en el KDoc y en el README.
- Agregar un prefijo a la lista conocida es un cambio MINOR: amplía el
  comportamiento sin romper nada.

## Principio general

Este ADR y el [0001](0001-cbu-vs-cvu.md) responden a preguntas de forma
aparentemente idéntica —"¿qué hago con un código que no reconozco?"— y llegan a
conclusiones opuestas. La diferencia está en el estatus del hecho codificado:

| | Prefijo → tipo de persona | `000` → es CVU |
|---|---|---|
| Naturaleza | Convención administrativa mutable | Regla normativa estructural |
| ¿Aparecen valores nuevos? | Sí, y va a pasar | No sin cambiar la norma |
| Tratamiento | Estado `DESCONOCIDO` | Discriminador total, sin escape |

**Regla:** separar lo que es verdad por estructura de lo que es verdad por
tabla. Lo estructural admite tipos cerrados y garantías fuertes. Lo tabular
necesita siempre un estado de escape, porque la tabla de hoy no es la de mañana.

## Nota histórica

Hasta 2021 el prefijo de personas físicas se correlacionaba con el género (20
masculino, 27 femenino), y 23/24 aparecían al resolver colisiones. Desde
entonces la asignación es aleatoria entre 20, 23, 24 y 27, en línea con la Ley
de Identidad de Género (26.743). **La librería no infiere género del prefijo**,
ni expone ninguna API que lo sugiera.
