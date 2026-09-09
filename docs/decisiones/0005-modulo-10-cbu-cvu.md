# 0005 — Módulo 10 en CBU/CVU: por qué no necesita casos límite

- **Estado:** Aceptada
- **Fecha:** 2026-09-09
- **Fase:** 3

## Contexto

El dígito verificador de cada bloque de un CBU/CVU se calcula con la "clave 10
con ponderador 9713" (BCRA, Comunicación "A" 2622): se pondera cada dígito
según su distancia al último con el ciclo `[3,1,7,9]` aplicado de derecha a
izquierda, se suman los productos, y el dígito verificador es
`(10 - suma % 10) % 10`.

El [ADR 0004](0004-casos-limite-modulo-11.md) mostró que el módulo 11 del
CUIT tiene un resto (1) que no corresponde a ningún dígito posible, y que
validar exige rechazar esos casos. La pregunta acá es simétrica: ¿el módulo
10 de CBU/CVU tiene el mismo problema?

## Decisión

**No. El módulo 10 no necesita ningún caso de escape.**

La razón no es que el 10 sea "más simple" que el 11: es que el módulo elegido
(10) **coincide exactamente** con la cantidad de símbolos del sistema decimal
(10). Un resto de dividir por 10 es, por definición, un número entre 0 y 9 —
ya es un dígito. La fórmula `(10 - resto) % 10` es la negación modular de
`resto` en Z/10Z:

| resto | `10 - resto` | `% 10` final | ¿Hace algo? |
|---|---|---|---|
| 0 | 10 | 0 | Sí: convierte el único caso fuera de rango |
| 1-9 | 9 a 1 | 9 a 1 (sin cambios) | No: ya estaba en rango |

El segundo `% 10` solo tiene trabajo que hacer en el caso `resto = 0`, y ahí
lo resuelve sin ambigüedad. Con 10 restos posibles (0 a 9) y 10 dígitos
posibles (0 a 9), la correspondencia es una **biyección total**: no sobra
ningún resto sin dígito.

El CUIT no tiene esta suerte porque su módulo (11) **no coincide** con la
base decimal. Dividir por 11 da 11 restos posibles (0 a 10), pero el sistema
decimal solo tiene 10 símbolos. Por el principio del palomar, al menos un
resto queda necesariamente sin dígito que lo represente — ARCA/DGI decidió
que fuera el resto 1 el que se rechaza (ver ADR 0004), pero el problema en sí
no depende de esa elección: existiría sin importar a cuál de los 11 restos
se lo hiciera caer.

**La causa es aritmética, no la primalidad de 11.** El desajuste nace de que
`11 ≠ 10`, no de que 11 sea primo. Un módulo compuesto que tampoco fuera 10
(el 12, por ejemplo) tendría el mismo problema de escape que el 11. La
primalidad sí importa, pero para una pregunta distinta: qué errores detecta
cada esquema.

## Contrapartida: qué detecta cada uno, y qué no

Los pesos del CUIT (`[5,4,3,2,7,6,5,4,3,2]`, mod 11) y los de CBU/CVU
(`[7,1,3,9,...]` cíclico, mod 10) persiguen lo mismo — que un error de
tipeo cambie el resto — pero con garantías distintas, precisamente porque
11 es primo y 10 no.

### Errores de un solo dígito: los dos los detectan, por motivos distintos

Cambiar un dígito en la posición `p` por otro distinto altera la suma
ponderada en `peso[p] × diferencia`, con `diferencia` entre 1 y 9 (nunca 0,
porque el dígito realmente cambió). El error se detecta si ese producto
nunca es múltiplo del módulo.

- **CUIT (mod 11):** como 11 es primo, *cualquier* peso no nulo módulo 11 es
  automáticamente coprimo con 11. No hace falta elegir los pesos con
  cuidado: cualquier secuencia de pesos entre 1 y 10 garantiza la detección.
- **CBU/CVU (mod 10):** 10 no es primo, así que esto ya no es automático.
  Un peso que comparta factor con 10 (cualquier par, o el 5) dejaría pasar
  ciertos errores sin detectar. La "clave 10" evita el problema **por
  diseño**: usa únicamente `{1, 3, 7, 9}`, que son exactamente las cuatro
  unidades de Z/10Z (los restos coprimos con 10). El resultado práctico es
  el mismo que en el CUIT —se detecta cualquier error de un solo dígito—
  pero ahí es una garantía automática de la aritmética, y acá es una
  propiedad que el algoritmo tiene que ganarse eligiendo bien los pesos.

### Transposiciones adyacentes: el CUIT las detecta todas, CBU/CVU no

Intercambiar dos dígitos adyacentes `a` y `b` (posiciones con pesos `w₁` y
`w₂`) cambia la suma en `(w₁ - w₂) × (a - b)`. Se detecta si ese producto
nunca es múltiplo del módulo.

- **CUIT (mod 11):** los pesos adyacentes de la secuencia son siempre
  distintos entre sí, y 11 es primo, así que ni `(w₁ - w₂)` ni `(a - b)`
  (ambos no nulos y menores que 11) pueden ser múltiplos de 11. El producto
  tampoco lo es: **se detecta cualquier transposición de dígitos
  adyacentes.**
- **CBU/CVU (mod 10):** acá el módulo compuesto sí cobra el precio. Los
  cuatro pesos `{1, 3, 7, 9}` son todos impares, así que la diferencia entre
  dos pesos adyacentes **siempre es par**. Si además los dos dígitos
  transpuestos difieren en exactamente 5 (los pares `0↔5`, `1↔6`, `2↔7`,
  `3↔8`, `4↔9`), el producto `par × 5` es múltiplo de 10 sin importar cuál
  sea el par de pesos. **Esa transposición puntual queda sin detectar.**

  Verificado a mano: cuerpo de 7 dígitos `0500000` (dígitos en posiciones 1
  y 2, pesos 7 y 1) da suma 5, resto 5, dígito verificador 5. Transponiendo
  esas dos posiciones, `5000000` da suma 35, resto 5, el mismo dígito
  verificador 5. La clave rota pasa la validación sin que el algoritmo lo
  note.

| | Sustitución de 1 dígito | Transposición adyacente |
|---|---|---|
| CUIT (mod 11, primo) | Detectada siempre (automático) | Detectada siempre (automático) |
| CBU/CVU (mod 10, compuesto) | Detectada siempre (por elección de pesos) | **No** detectada si los dígitos difieren en 5 |

## Verificación

Casos de resto límite, comprobados a mano con el ciclo de pesos
`[3,1,7,9]` aplicado de derecha a izquierda:

    Cuerpo 0110002 (bloque 1: entidad 011, sucursal 0002)
    Dígitos:   0  1  1  0  0  0  2
    Pesos:     7  1  3  9  7  1  3
    Productos: 0  1  3  0  0  0  6

    Suma = 10    10 % 10 = 0    (10 - 0) % 10 = 0

    Cuerpo 0000000000013 (bloque 2: cuenta)
    Suma = 10    10 % 10 = 0    dígito verificador = 0

Clave resultante: `0110002` + `0` + `0000000000013` + `0` =
`0110002000000000000130`, íntegramente válida con ambos dígitos
verificadores en 0. A diferencia del resto 1 del CUIT, acá no hay ningún
resto que quede sin dígito representable.

## Consecuencias

- `calcularDigitoVerificadorModulo10` no devuelve `Int?` como su análogo del
  CUIT: devuelve `Int` sin nulabilidad, porque no existe el caso sin
  representación.
- La contrapartida de "no hace falta rechazar nada" es que el esquema no
  detecta un tipo específico de error de tipeo (la transposición de dígitos
  que difieren en 5). Esto no es una limitación de esta implementación: es
  una propiedad del algoritmo "clave 10 con ponderador 9713" tal como lo
  define el BCRA, y por lo tanto compartida por todo software que lo
  implemente correctamente.
- La librería no compensa esa limitación (por ejemplo, agregando una
  detección de transposiciones por fuera del algoritmo oficial): haría que
  la validación dejara de coincidir con la que aplica el sistema bancario
  real, produciendo falsos negativos respecto de claves que el sistema
  bancario aceptaría.

## Fuentes

- BCRA — Comunicación "A" 2622: algoritmo de doble dígito verificador
  ("clave 10 con ponderador 9713") para CBU.
- Verificación manual de los casos de resto 0 y del ejemplo de transposición
  no detectada (arriba).
- Ver [ADR 0004](0004-casos-limite-modulo-11.md) para el caso análogo —y
  opuesto— del módulo 11 en CUIT.
