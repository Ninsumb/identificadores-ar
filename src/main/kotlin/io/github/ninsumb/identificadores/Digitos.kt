package io.github.ninsumb.identificadores

/**
 * Determina si un carácter es un dígito ASCII (`0` a `9`).
 *
 * Deliberadamente más estricto que `Char.isDigit()`, que en Kotlin/JVM acepta
 * cualquier dígito Unicode de la categoría `Nd` (arábigo-índico, devanagari,
 * etc.). Aceptar esos dígitos rompería la promesa de los value objects de
 * este paquete de contener solo `0`-`9`, y `Char.digitToInt()` no lo
 * delataría: también sabe interpretarlos y les asigna su valor numérico sin
 * quejarse.
 *
 * Punto único de esta decisión para todo el paquete: lo usan [Cuit] y
 * [ClaveBancaria] (con sus subtipos [Cbu] y [Cvu]).
 */
internal fun esDigitoAscii(c: Char): Boolean = c in '0'..'9'

/**
 * Quita los separadores tolerados en la entrada: guiones, puntos y cualquier
 * espacio en blanco (vía `Char.isWhitespace()`, que sí puede reconocer
 * espacios Unicode no ASCII; eso no compromete [esDigitoAscii], que decide
 * por separado qué cuenta como dígito).
 *
 * No valida longitud ni contenido: eso es responsabilidad de quien llama.
 */
internal fun quitarSeparadores(input: String): String =
    input.filterNot { it == '-' || it == '.' || it.isWhitespace() }
