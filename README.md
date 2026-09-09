# identificadores-ar

Librería Kotlin/JVM para validar y parsear identificadores argentinos:
CUIT, CUIL, CBU, CVU, DNI y alias bancario.

> ⚠️ En desarrollo activo. La API puede cambiar hasta la versión 1.0.0.


## Uso

### CUIT / CUIL

```kotlin
// Validar sin construir nada
Cuit.isValid("20-12345678-6")     // true

// Parsear (tira si es inválido)
val cuit = Cuit.parse("20-12345678-6")
cuit.prefijo                       // "20"
cuit.numero                        // "12345678"
cuit.tipoPersona                   // TipoPersona.FISICA (orientativo, ver más abajo)
cuit.formateado()                  // "20-12345678-6"

// Para input de usuario
val quizas = Cuit.parseOrNull(loQueEscribioElUsuario)
```

Acepta guiones, puntos y espacios como separadores, o ninguno.

`tipoPersona` es orientativo, no una fuente de verdad: se infiere de una lista
de prefijos administrativos que puede cambiar con el tiempo. ARCA (ex AFIP) es
la fuente de verdad definitiva.

### CBU / CVU

```kotlin
// Validar sin construir nada
Cbu.isValid("0110059400000000000017")     // true
Cvu.isValid("0000001700000000000017")     // true

// Parsear un CBU específico (tira si no es CBU, aunque sea un CVU válido)
val cbu = Cbu.parse("0110059400000000000017")
cbu.codigoEntidad                          // "011"
cbu.sucursal                                // "0059"
cbu.numeroCuenta                            // "0000000000001"

// Parsear un CVU específico
val cvu = Cvu.parse("0000001700000000000017")
cvu.codigoPsp                               // "0001"
cvu.numeroCuenta                            // "0000000000001"

// Sin saber de antemano si es CBU o CVU: ClaveBancaria es una interfaz
// sellada, así que el `when` sobre el subtipo es exhaustivo sin `else`.
val clave: ClaveBancaria = ClaveBancaria.parse("0110059400000000000017")
val descripcion = when (clave) {
    is Cbu -> "Banco ${clave.codigoEntidad}, sucursal ${clave.sucursal}"
    is Cvu -> "Billetera virtual, PSP ${clave.codigoPsp}"
}
// descripcion == "Banco 011, sucursal 0059"

// Para input de usuario
val quizasClave = ClaveBancaria.parseOrNull(loQueEscribioElUsuario)
```

Acepta guiones, puntos y espacios como separadores, o ninguno.

Ni `Cbu` ni `Cvu` resuelven el nombre del banco o la billetera: eso está a
propósito fuera del value object, ver "Qué no hace" más abajo.

## Estado

En construcción. Ver [ALCANCE.md](ALCANCE.md) para el alcance del proyecto y
las decisiones de diseño.

| Identificador | Estado |
|---|---|
| CUIT / CUIL | ✅ Implementado |
| CBU / CVU | ✅ Implementado |
| DNI | 🚧 Pendiente |
| Alias bancario | 🚧 Pendiente |

## Qué no hace

- No consulta el padrón de ARCA (ex AFIP): la validación es matemática, no
  verifica existencia ni vigencia.
- No garantiza el tipo de persona: la inferencia de `tipoPersona` es
  orientativa, a partir de una lista de prefijos conocidos. ARCA (ex AFIP) es
  la fuente de verdad definitiva.
- No resuelve alias a CBU: no existe mecanismo público para hacerlo.
- No resuelve el nombre del banco ni del PSP a partir del código: expone
  `codigoEntidad`/`codigoPsp`, pero traducirlos a un nombre es
  responsabilidad de un catálogo externo, reemplazable y con su propia
  fecha de vigencia.
- No hace llamadas de red. Todo el cómputo es local.

## Licencia

MIT
