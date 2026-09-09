# 0004 — Casos límite del módulo 11 en CUIT/CUIL

- **Estado:** Aceptada
- **Fecha:** 2026-09-08

## Contexto

El dígito verificador del CUIT se calcula con módulo 11: se multiplican los
primeros 10 dígitos por los pesos `[5,4,3,2,7,6,5,4,3,2]`, se suman los
productos, y se toma el resto de dividir esa suma por 11. El dígito verificador
es `11 - resto`.

Esa fórmula produce resultados imposibles en **dos de los once restos
posibles**, porque un dígito verificador tiene que ser un único carácter de 0 a
9:

| Resto | `11 - resto` | ¿Es un dígito? |
|---|---|---|
| 0 | 11 | No |
| 1 | 10 | No |
| 2-10 | 9 a 1 | Sí |

## Decisión

**Resto 0 → el dígito verificador es 0.**

No es una excepción arbitraria: en aritmética modular, 11 ≡ 0 (mod 11). El 0 es
el representante canónico de esa clase de equivalencia. La fórmula completa y
correcta es `(11 - resto) mod 11`, y ambos casos coinciden.

**Resto 1 → el CUIT es inválido a los efectos de esta librería.**

Acá hay que distinguir dos operaciones que la mayoría de las implementaciones
confunde:

| | Generación (DNI → CUIL) | Validación (¿este CUIT está bien?) |
|---|---|---|
| Input | 8 dígitos + prefijo tentativo | 11 dígitos completos |
| Resto 1 | El prefijo pasa a 23 y se recalcula | El número es **inválido** |

Al validar, si el resto da 1 no existe ningún dígito que pueda cerrar el número,
así que el input se rechaza. Eso no es una limitación: ARCA nunca emitió un CUIT
que caiga en ese caso. Cuando el organismo aplicó la regla de reasignación, el
prefijo cambió, y con otro prefijo la suma ponderada es distinta, así que el CUIT
resultante valida por el camino normal.

Firma resultante:

    internal fun calcularDigitoVerificador(cuerpo: String): Int?

El `null` de retorno significa "ningún dígito puede cerrar este cuerpo".

## Error frecuente que esta decisión evita

Muchas implementaciones publicadas mapean el verificador 10 al dígito 9 **sin
cambiar el prefijo**. Es un malentendido de la regla oficial: el 9 aparece
únicamente como consecuencia de haber reasignado el prefijo a 23, no como
sustitución directa.

Una implementación con ese mapeo **acepta como válidos números que ARCA nunca
emitió**. Es un falso positivo silencioso, el peor tipo de bug en una librería
de validación.

## Verificación

Ambos casos comprobados a mano con la tabla de pesos, no tomados de una fuente
secundaria.

### Caso 1 — prefijo 20, DNI 00000001

    Dígitos:    2   0   0   0   0   0   0   0   0   1
    Pesos:      5   4   3   2   7   6   5   4   3   2
    Productos: 10   0   0   0   0   0   0   0   0   2

    Suma = 12    12 mod 11 = 1    -> caso límite

Reasignado a prefijo 23:

    Dígitos:    2   3   0   0   0   0   0   0   0   1
    Productos: 10  12   0   0   0   0   0   0   0   2

    Suma = 24    24 mod 11 = 2    11 - 2 = 9

Resultado: `23-00000001-9`.

### Caso 2 — prefijo 27, DNI 00000012

    Con prefijo 27:  suma = 45    45 mod 11 = 1    -> caso límite
    Con prefijo 23:  suma = 29    29 mod 11 = 7    11 - 7 = 4

Resultado: `23-00000012-4`.

Los dos coinciden con la regla documentada. Ambos entran como casos de test.

## Consecuencias

- La **generación** completa (DNI → CUIL con reasignación de prefijo) queda
  **fuera del alcance** de la 1.0.0. La librería valida y calcula el dígito
  esperado para un cuerpo dado, pero no emite identificadores nuevos.
- La regla de reasignación se documenta acá igual, porque es necesaria para
  entender por qué el resto 1 se rechaza al validar.
- Si en el futuro se agrega la generación, será un cambio MINOR y necesitará su
  propio ADR.

## Nota sobre los pesos

La secuencia `[5,4,3,2,7,6,5,4,3,2]` no es un número mágico. Es lo que resulta
de aplicar el patrón genérico de módulo 11 —multiplicadores 2, 3, 4… hasta un
tope, de derecha a izquierda, reiniciando al llegar al tope— con tope 7 sobre un
cuerpo de 10 dígitos. Saberlo ayuda a no copiar la secuencia mal.

## Fuentes

- Resolución General DGI N° 2700/1987.
- Verificación manual de los dos casos límite (arriba).
