# 0001 — CBU y CVU son dos tipos bajo una interfaz sellada

- **Estado:** Aceptada
- **Fecha:** 2026-09-08
- **Fase:** 0

## Contexto

El CBU (Clave Bancaria Uniforme) y el CVU (Clave Virtual Uniforme) comparten
longitud (22 dígitos), estructura en dos bloques y algoritmo de doble dígito
verificador (módulo 10). Son plenamente interoperables: una transferencia trata
igual a un destino bancario que a uno de billetera virtual.

La pregunta era si conviene modelarlos como un único tipo con una propiedad que
indique cuál es, o como dos tipos distintos.

Un dato relevante que cambió la respuesta: **los dígitos 4 a 7 no significan lo
mismo en ambos.**

| Posición | CBU | CVU |
|---|---|---|
| 1-3 | Código de banco (`011`, `072`…) | Siempre `000` |
| 4-7 | Sucursal | Código de PSP |
| 8 | Dígito verificador del bloque | Dígito verificador del bloque |
| 9-22 | Cuenta + verificador | Cuenta + verificador |

## Decisión

Una interfaz sellada `ClaveBancaria` con dos implementaciones, `Cbu` y `Cvu`.
Un único `parse` valida la estructura y el doble checksum, y despacha al subtipo
según el centinela.

    sealed interface ClaveBancaria {
        val valor: String
        val numeroCuenta: String
        companion object {
            @JvmStatic fun parse(input: String): ClaveBancaria
        }
    }

    class Cbu : ClaveBancaria {
        val codigoEntidad: String
        val sucursal: String
    }

    class Cvu : ClaveBancaria {
        val codigoPsp: String
    }

### Alternativa descartada

Un solo tipo con `sucursal: String?` y `codigoPsp: String?`. Serían dos campos
nulables mutuamente excluyentes: nunca ambos presentes, nunca ambos ausentes.
Eso traslada al consumidor la responsabilidad de saber cuál puede leer, sin
ayuda del compilador. Dos campos nulables que se excluyen entre sí son la señal
clásica de dos tipos metidos adentro de uno.

## Consecuencias

- Preguntarle la sucursal a un CVU deja de ser expresable. El error se detecta
  al compilar, no en producción.
- El `when` sobre el subtipo es exhaustivo, porque la jerarquía es sellada y el
  discriminador es total: o los tres primeros dígitos son `000` o no lo son.
  No hace falta un estado desconocido.
- La interoperabilidad se preserva: quien solo necesite una cuenta destino
  acepta `ClaveBancaria` y no se entera del subtipo.
- El discriminador **no depende de ninguna tabla**. Es una regla normativa, no
  un dato mutable. Contrastar con el [ADR 0003](0003-catalogo-de-entidades.md),
  donde el nombre de la entidad sí es tabular y necesita estado de escape.

## Supuesto explícito

Se asume que **todo CVU tiene `000` como código de entidad**, según la
Comunicación "A" 6510 del BCRA (2018). El supuesto se codifica como constante
con nombre, con un test que lo ejercita:

    internal const val CENTINELA_CVU: String = "000"

Si la normativa cambiara y el BCRA asignara códigos distintos de `000` a algún
PSP, este supuesto se rompe y hay que revisar esta decisión.

## Fuentes

- BCRA — Clave Virtual Uniforme (CVU): el primer bloque (dígitos 1 a 8)
  identifica al PSP; el segundo (9 a 22) identifica a la persona usuaria.
- BCRA — Comunicación "A" 6510 (2018), que crea el CVU.
