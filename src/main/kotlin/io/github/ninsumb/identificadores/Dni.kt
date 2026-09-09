package io.github.ninsumb.identificadores

private const val LONGITUD_CANONICA: Int = 8
private const val LONGITUD_MINIMA: Int = 1

/**
 * Los ocho dígitos en cero: no es un valor de la escala, es la ausencia de
 * matrícula. Ningún documento tiene número 0, sin importar cuántos ceros se
 * le antepongan. Ver `docs/decisiones/0006-dni-validacion-estructural.md`.
 */
private const val MATRICULA_NULA: String = "00000000"

/**
 * Documento Nacional de Identidad.
 *
 * Un `Dni` es válido por construcción: la única forma de crear uno es a
 * través de [parse] o [parseOrNull].
 *
 * Sin dígito verificador: RENAPER asigna los números correlativamente, sin
 * ningún mecanismo matemático de autocontrol. La validación es puramente
 * estructural -longitud y composición- y, a propósito, casi vacía: deja
 * pasar cualquier número de 1 a 8 dígitos que no sea todo ceros.
 *
 * El valor de este tipo no está en lo que filtra en tiempo de ejecución
 * -filtra casi nada-, sino en existir como un tipo propio en el sistema de
 * tipos, distinto de un `String` arbitrario: *parse, don't validate*. Ver
 * `docs/decisiones/0006-dni-validacion-estructural.md`.
 *
 * @property valor los 8 dígitos ASCII (`0` a `9`), siempre con ceros a la
 *   izquierda hasta completar 8 -la misma forma canónica que expone
 *   [Cuit.numero] para personas físicas-, sin importar cuántos dígitos haya
 *   escrito quien lo ingresó.
 */
public class Dni private constructor(
    public val valor: String,
) {

    /**
     * Devuelve el DNI con puntos de miles, agrupando de a tres desde la
     * derecha sobre los dígitos significativos -sin los ceros de relleno de
     * [valor]-. Ejemplo: `Dni.parse("1234567").formateado()` da
     * `"1.234.567"`, **no** `"01.234.567"`.
     *
     * A diferencia de [Cuit.formateado], los puntos acá no delimitan campos
     * fijos: son un separador de miles sobre un número, así que no tiene
     * sentido mostrar el cero de relleno que solo existe para que [valor]
     * tenga un ancho uniforme. Un DNI de 8 dígitos como `"70000001"` cae en
     * dos grupos de 3 y uno de 2, igual que cualquier número de esa
     * magnitud: `"70.000.001"`.
     */
    public fun formateado(): String {
        val significativos = valor.trimStart('0').ifEmpty { "0" }
        return significativos.reversed().chunked(3).joinToString(".").reversed()
    }

    /** Devuelve [valor]: los 8 dígitos, siempre con ceros a la izquierda. */
    override fun toString(): String = valor

    /** Dos [Dni] son iguales si tienen el mismo [valor]. */
    override fun equals(other: Any?): Boolean =
        this === other || (other is Dni && valor == other.valor)

    /** Coherente con [equals]: se basa en [valor]. */
    override fun hashCode(): Int = valor.hashCode()

    /** Puerta de entrada para crear o validar un [Dni]: [parse], [parseOrNull], [isValid]. */
    public companion object {

        /**
         * Parsea un DNI. Acepta guiones, puntos y espacios como
         * separadores, o ninguno. Solo reconoce dígitos ASCII (`0` a `9`).
         *
         * El [valor] resultante siempre tiene 8 dígitos: un input con menos
         * (documentos viejos, por ejemplo `"1234567"`) se completa con
         * ceros a la izquierda.
         *
         * @throws IllegalArgumentException si el input no es un DNI válido.
         */
        @JvmStatic
        public fun parse(input: String): Dni =
            parseOrNull(input) ?: throw IllegalArgumentException("DNI inválido: $input")

        /**
         * Igual que [parse], pero devuelve `null` en vez de tirar excepción.
         * Es la variante apropiada para input de usuario.
         */
        @JvmStatic
        public fun parseOrNull(input: String): Dni? {
            val canonico = normalizar(input) ?: return null
            return Dni(canonico)
        }

        /**
         * Indica si el input es un DNI válido, sin construir la instancia.
         * Útil para filtrar colecciones.
         */
        @JvmStatic
        public fun isValid(input: String): Boolean = normalizar(input) != null

        /**
         * Quita separadores, valida longitud y composición, y devuelve la
         * forma canónica de 8 dígitos (con ceros a la izquierda si hacen
         * falta). Devuelve `null` si el input no puede ser un DNI: longitud
         * fuera de 1-8, algún carácter que no sea un dígito ASCII, o
         * [MATRICULA_NULA] una vez completado con ceros.
         *
         * Ver `docs/decisiones/0006-dni-validacion-estructural.md`.
         */
        private fun normalizar(input: String): String? {
            val limpio = quitarSeparadores(input)
            if (limpio.length !in LONGITUD_MINIMA..LONGITUD_CANONICA) return null
            if (!limpio.all(::esDigitoAscii)) return null

            val canonico = limpio.padStart(LONGITUD_CANONICA, '0')
            if (canonico == MATRICULA_NULA) return null

            return canonico
        }
    }
}
