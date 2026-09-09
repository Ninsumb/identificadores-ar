# identificadores-ar

Librería Kotlin/JVM para validar y parsear identificadores argentinos:
CUIT, CUIL, CBU, CVU, DNI y alias bancario.

> ⚠️ En desarrollo activo. La API puede cambiar hasta la versión 1.0.0.


## Uso

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

## Estado

En construcción. Ver [ALCANCE.md](ALCANCE.md) para el alcance del proyecto y
las decisiones de diseño.

| Identificador | Estado |
|---|---|
| CUIT / CUIL | ✅ Implementado |
| CBU / CVU | 🚧 Pendiente |
| DNI | 🚧 Pendiente |
| Alias bancario | 🚧 Pendiente |

## Qué no hace

- No consulta el padrón de ARCA (ex AFIP): la validación es matemática, no
  verifica existencia ni vigencia.
- No garantiza el tipo de persona: la inferencia de `tipoPersona` es
  orientativa, a partir de una lista de prefijos conocidos. ARCA (ex AFIP) es
  la fuente de verdad definitiva.
- No resuelve alias a CBU: no existe mecanismo público para hacerlo.
- No hace llamadas de red. Todo el cómputo es local.

## Licencia

MIT
