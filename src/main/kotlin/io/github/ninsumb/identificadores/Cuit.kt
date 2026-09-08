package io.github.ninsumb.identificadores

/**
 * Clave Única de Identificación Tributaria / Laboral.
 *
 * Un `Cuit` es válido por construcción: si tenés una instancia en la mano, su
 * dígito verificador ya fue comprobado. La única forma de crear una es a través
 * de [parse] o [parseOrNull].
 *
 * La validación es puramente estructural. **No** verifica que el CUIT exista ni
 * que esté vigente ante ARCA.
 *
 * @property valor los 11 dígitos, sin separadores.
 */
public class Cuit private constructor(
    public val valor: String,
) {

    /** Los dos primeros dígitos. Ejemplo: `"20"`. */
    public val prefijo: String
        get() = valor.substring(0, 2)

    /** Los ocho dígitos centrales. En personas físicas coincide con el DNI. */
    public val numero: String
        get() = valor.substring(2, 10)

    /** El último dígito, el de control. */
    public val digitoVerificador: Int
        get() = valor[10].digitToInt()

    /**
     * Tipo de persona inferido a partir del [prefijo].
     *
     * Es orientativo: la lista de prefijos es una convención administrativa
     * mutable, no una regla estructural. Un prefijo no reconocido devuelve
     * [TipoPersona.DESCONOCIDO], no invalida el CUIT.
     *
     * Ver `docs/decisiones/0002-prefijo-cuit-desconocido.md`.
     */
    public val tipoPersona: TipoPersona
        get() = when (prefijo) {
            in PREFIJOS_FISICAS -> TipoPersona.FISICA
            in PREFIJOS_JURIDICAS -> TipoPersona.JURIDICA
            else -> TipoPersona.DESCONOCIDO
        }

    /** Devuelve el CUIT con guiones. Ejemplo: `"20-12345678-6"`. */
    public fun formateado(): String = "$prefijo-$numero-$digitoVerificador"

    override fun toString(): String = valor

    override fun equals(other: Any?): Boolean =
        this === other || (other is Cuit && valor == other.valor)

    override fun hashCode(): Int = valor.hashCode()

    public companion object {

        /** Pesos del módulo 11, en orden de izquierda a derecha. */
        private val PESOS: IntArray = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

        private const val LONGITUD: Int = 11
        private const val LONGITUD_CUERPO: Int = 10
        private const val MODULO: Int = 11

        /**
         * Prefijos conocidos de personas físicas.
         *
         * La lista es deliberadamente conservadora: un prefijo ausente no
         * invalida el CUIT, solo lo deja en [TipoPersona.DESCONOCIDO].
         */
        private val PREFIJOS_FISICAS: Set<String> = setOf("20", "23", "24", "27")

        /** Prefijos conocidos de personas jurídicas. */
        private val PREFIJOS_JURIDICAS: Set<String> = setOf("30", "33", "34")

        /**
         * Parsea un CUIT. Acepta guiones, puntos y espacios como separadores.
         *
         * @throws IllegalArgumentException si el input no es un CUIT válido.
         */
        @JvmStatic
        public fun parse(input: String): Cuit =
            parseOrNull(input)
                ?: throw IllegalArgumentException("CUIT inválido: $input")

        /**
         * Igual que [parse], pero devuelve `null` en vez de tirar excepción.
         * Es la variante apropiada para input de usuario.
         */
        @JvmStatic
        public fun parseOrNull(input: String): Cuit? {
            val digitos = normalizar(input) ?: return null
            return if (cierraElVerificador(digitos)) Cuit(digitos) else null
        }

        /**
         * Indica si el input es un CUIT válido, sin construir la instancia.
         * Útil para filtrar colecciones.
         */
        @JvmStatic
        public fun isValid(input: String): Boolean {
            val digitos = normalizar(input) ?: return false
            return cierraElVerificador(digitos)
        }

        /**
         * Calcula el dígito verificador esperado para los primeros 10 dígitos.
         *
         * Casos límite del módulo 11:
         * - resto 0 -> el dígito es 0 (en aritmética modular, 11 ≡ 0 mod 11).
         * - resto 1 -> `11 - 1 = 10`, que no es un dígito. Ningún CUIT válido
         *   cae acá: ARCA reasigna el prefijo a 23 al generarlo. Al validar,
         *   corresponde rechazar.
         *
         * Ver `docs/decisiones/0004-casos-limite-modulo-11.md`.
         *
         * @param cuerpo exactamente 10 dígitos.
         * @return el dígito esperado (0-9), o `null` si el resto es 1.
         */
        internal fun calcularDigitoVerificador(cuerpo: String): Int? {
            require(cuerpo.length == LONGITUD_CUERPO) {
                "El cuerpo debe tener $LONGITUD_CUERPO dígitos, tiene ${cuerpo.length}"
            }
            require(cuerpo.all { it.isDigit() }) {
                "El cuerpo debe contener solo dígitos"
            }

            var suma = 0
            for (i in 0 until LONGITUD_CUERPO) {
                suma += cuerpo[i].digitToInt() * PESOS[i]
            }

            return when (val resto = suma % MODULO) {
                0 -> 0
                1 -> null
                else -> MODULO - resto
            }
        }

        /**
         * Comprueba que el último dígito coincida con el calculado a partir de
         * los diez primeros.
         *
         * @param digitos exactamente 11 dígitos, ya normalizados.
         */
        private fun cierraElVerificador(digitos: String): Boolean {
            val esperado = calcularDigitoVerificador(digitos.substring(0, LONGITUD_CUERPO))
                ?: return false
            return esperado == digitos[LONGITUD_CUERPO].digitToInt()
        }

        /**
         * Quita separadores y valida la forma. Devuelve 11 dígitos limpios,
         * o `null` si el input no puede ser un CUIT.
         *
         * No comprueba el dígito verificador: eso es responsabilidad de quien
         * llama.
         */
        private fun normalizar(input: String): String? {
            val limpio = input.filterNot { it == '-' || it == '.' || it.isWhitespace() }
            if (limpio.length != LONGITUD) return null
            if (!limpio.all { it.isDigit() }) return null
            return limpio
        }
    }
}

/** Tipo de persona inferido del prefijo del CUIT. Ver [Cuit.tipoPersona]. */
public enum class TipoPersona {
    FISICA,
    JURIDICA,

    /**
     * El prefijo no está en las listas conocidas. El CUIT sigue siendo
     * estructuralmente válido: la lista de prefijos cambia con el tiempo y
     * rechazar por desconocimiento produciría falsos negativos.
     */
    DESCONOCIDO,
}
