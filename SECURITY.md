# Política de seguridad

## Alcance de la superficie de ataque

`identificadores-ar` es una librería de validación, parseo y formateo de
identificadores argentinos (CUIT/CUIL, CBU/CVU, DNI, alias bancario), sin
dependencias de runtime fuera de la stdlib de Kotlin.

Lo que eso implica en términos de superficie de ataque:

- **Sin I/O.** La librería no hace llamadas de red, no lee ni escribe
  archivos, no accede a variables de entorno ni a ningún recurso del sistema.
  Todo el cómputo es local: recibe un `String`, lo valida y devuelve un value
  object o `null`/excepción.
- **Sin dependencias de runtime.** Nada que la librería arrastre puede
  introducir una vulnerabilidad transitiva; la única dependencia real es la
  stdlib de Kotlin.
- **Sin deserialización ni evaluación de código.** No interpreta el `String`
  de entrada como código, expresión regular provista por el usuario, ni
  ningún formato serializado (JSON, XML, etc.).
- **Entrada no confiable esperada.** La librería está diseñada para recibir
  input arbitrario de usuario (`parseOrNull`, `isValid`) sin asumir que ya fue
  saneado; un input malformado o adversarial debe resultar en `null` o en una
  excepción documentada, nunca en un comportamiento indefinido.

Dado este alcance, la clase de vulnerabilidad más relevante es una que rompa
alguna de estas garantías: por ejemplo, una entrada que cause un bucle
infinito o un consumo de recursos desproporcionado (denegación de servicio
local vía regex catastrófico o similar), o un caso en el que `isValid`
devuelva `true` para un identificador que no debería validar (con las
salvedades ya documentadas en el README bajo "Qué no hace": la librería no
consulta al padrón de ARCA ni verifica existencia real de un identificador,
solo su forma).

## Versiones soportadas

| Versión | Soportada |
| ------- | --------- |
| 1.x     | ✅        |
| < 1.0.0 | ❌        |

Las versiones previas a 1.0.0 (`0.1.0`, `0.1.1`) fueron publicaciones de
desarrollo en JitPack, sin garantía de compatibilidad ni de soporte. Solo la
serie 1.x, publicada en Maven Central, recibe parches de seguridad.

## Cómo reportar una vulnerabilidad

**No abras un issue público.** Reportá en privado a través de las
[GitHub Security Advisories de este repositorio](https://github.com/Ninsumb/identificadores-ar/security/advisories/new)
("Security" → "Report a vulnerability"). Esto notifica directamente a la
mantenedora sin exponer el detalle antes de que haya un fix disponible.

Incluí, si es posible:

- La versión afectada (coordenada de Maven Central o JitPack, y número de
  versión).
- El identificador o input concreto que dispara el problema.
- El comportamiento esperado vs. el observado.

Al tratarse de una librería sin superficie de red ni de sistema, la mayoría
de los reportes válidos van a ser sobre corrección de la validación (falsos
positivos/negativos) o sobre consumo excesivo de recursos con una entrada
específica. Vas a recibir una respuesta reconociendo el reporte; el tiempo
hasta el fix depende de la severidad, pero al no haber dependencias ni
despliegue de por medio, el parche se limita a esta librería.
