package io.github.ninsumb.identificadores

private const val LONGITUD: Int = 22
private const val LONGITUD_BLOQUE1: Int = 8
private const val LONGITUD_CUERPO_BLOQUE1: Int = 7
private const val LONGITUD_CUERPO_BLOQUE2: Int = 13

/**
 * Ponderador de la "clave 10", aplicado cíclicamente de derecha a izquierda:
 * el dígito más a la derecha de cualquier bloque pesa 3, el siguiente 1, el
 * siguiente 7, el siguiente 9, y se repite. Es una única regla que sirve para
 * cuerpos de cualquier longitud (acá: 7 y 13 dígitos) — no dos secuencias
 * hardcodeadas.
 *
 * Ver `docs/decisiones/0005-modulo-10-cbu-cvu.md`.
 */
private val PONDERADOR_9713: IntArray = intArrayOf(3, 1, 7, 9)

/**
 * Centinela que identifica un CVU: sus tres primeras posiciones son siempre
 * `"000"`, nunca un código de entidad real. Es una regla normativa —no una
 * tabla que pueda desactualizarse—, así que sirve como discriminador total
 * entre [Cbu] y [Cvu], sin estado de escape.
 *
 * Supuesto explícito, según BCRA, Comunicación "A" 6510 (2018): todo CVU
 * tiene `000` como código de entidad. Si el BCRA asignara alguna vez códigos
 * distintos de `000` a un PSP, este supuesto se rompe.
 *
 * Ver `docs/decisiones/0001-cbu-vs-cvu.md`.
 */
internal const val CENTINELA_CVU: String = "000"

/**
 * Calcula el dígito verificador de un bloque con el algoritmo "clave 10 con
 * ponderador 9713" (BCRA, Comunicación "A" 2622): se multiplica cada dígito
 * por su peso según [PONDERADOR_9713], se suman los productos, y el dígito
 * verificador es `(10 - suma % 10) % 10`.
 *
 * A diferencia del módulo 11 del CUIT ([Cuit.calcularDigitoVerificador]),
 * acá no hace falta ningún caso límite: el segundo `% 10` siempre devuelve un
 * dígito entre 0 y 9, para cualquier resto posible. Ver
 * `docs/decisiones/0005-modulo-10-cbu-cvu.md`.
 *
 * Sirve tanto para el bloque de 7 dígitos como para el de 13: el peso de cada
 * posición depende únicamente de su distancia al último dígito, así que la
 * misma función cubre ambos largos sin duplicar la secuencia de pesos.
 *
 * @param cuerpo cualquier cantidad de dígitos ASCII (`0` a `9`), al menos uno.
 * @return el dígito verificador (0-9).
 */
internal fun calcularDigitoVerificadorModulo10(cuerpo: String): Int {
    require(cuerpo.isNotEmpty()) { "El cuerpo no puede estar vacío" }
    require(cuerpo.all(::esDigitoAscii)) {
        "El cuerpo debe contener solo dígitos ASCII (0-9)"
    }

    var suma = 0
    val longitud = cuerpo.length
    for (i in 0 until longitud) {
        val distanciaDesdeLaDerecha = longitud - i
        val peso = PONDERADOR_9713[(distanciaDesdeLaDerecha - 1) % PONDERADOR_9713.size]
        suma += cuerpo[i].digitToInt() * peso
    }

    return (10 - suma % 10) % 10
}

/**
 * Comprueba que los dos dígitos verificadores (posición 8 y posición 22)
 * coincidan con los calculados a partir de sus respectivos cuerpos.
 *
 * @param digitos exactamente 22 dígitos, ya normalizados.
 */
private fun cierranLosDosVerificadores(digitos: String): Boolean {
    val cuerpoBloque1 = digitos.substring(0, LONGITUD_CUERPO_BLOQUE1)
    val dv1Esperado = calcularDigitoVerificadorModulo10(cuerpoBloque1)
    if (dv1Esperado != digitos[LONGITUD_CUERPO_BLOQUE1].digitToInt()) return false

    val cuerpoBloque2 = digitos.substring(LONGITUD_BLOQUE1, LONGITUD_BLOQUE1 + LONGITUD_CUERPO_BLOQUE2)
    val dv2Esperado = calcularDigitoVerificadorModulo10(cuerpoBloque2)
    return dv2Esperado == digitos[LONGITUD - 1].digitToInt()
}

/**
 * Quita separadores y valida la forma. Devuelve 22 dígitos ASCII limpios, o
 * `null` si el input no puede ser una clave bancaria.
 *
 * No comprueba los dígitos verificadores ni el centinela: eso es
 * responsabilidad de quien llama.
 */
private fun normalizarClaveBancaria(input: String): String? {
    val limpio = quitarSeparadores(input)
    if (limpio.length != LONGITUD) return null
    if (!limpio.all(::esDigitoAscii)) return null
    return limpio
}

/**
 * Clave Bancaria Uniforme (CBU) o Clave Virtual Uniforme (CVU).
 *
 * 22 dígitos en dos bloques, cada uno con su propio dígito verificador
 * (módulo 10, "clave 10 con ponderador 9713"; BCRA, Comunicación "A" 2622).
 * Bloque 1: 8 dígitos (entidad/centinela + sucursal/PSP + verificador).
 * Bloque 2: 14 dígitos (número de cuenta + verificador).
 *
 * [Cbu] y [Cvu] comparten longitud, estructura y algoritmo; se diferencian
 * por el centinela [CENTINELA_CVU] en las tres primeras posiciones. La
 * validación es puramente estructural. **No** verifica que la clave exista
 * ni que esté activa.
 *
 * Ver `docs/decisiones/0001-cbu-vs-cvu.md`.
 */
public sealed interface ClaveBancaria {

    /** Los 22 dígitos, sin separadores. */
    public val valor: String

    /** Los primeros 8 dígitos: entidad/centinela (3) + sucursal/PSP (4) + dígito verificador (1). */
    public val bloque1: String

    /** Los últimos 14 dígitos: número de cuenta (13) + dígito verificador (1). */
    public val bloque2: String

    /** Los 13 dígitos de cuenta, sin su dígito verificador. */
    public val numeroCuenta: String

    /** El dígito verificador del primer bloque (posición 8). */
    public val digitoVerificadorBloque1: Int

    /** El dígito verificador del segundo bloque, el último dígito (posición 22). */
    public val digitoVerificadorBloque2: Int

    /**
     * Devuelve la clave con [bloque1] y [bloque2] separados por un espacio.
     * Ejemplo: `"01100594 00000000000017"`.
     *
     * Definida acá, no en [Cbu] ni en [Cvu], porque la división en dos
     * bloques es común a ambos subtipos.
     *
     * La división sale de la estructura que fija
     * `docs/decisiones/0001-cbu-vs-cvu.md` -bloque de 8 dígitos y bloque de
     * 14-, **no** de una convención de visualización normalizada para
     * CBU/CVU: a diferencia de [Cuit.formateado], no hay una fuente que
     * confirme que el sistema bancario muestre estas claves de alguna forma
     * en particular. El separador es un espacio -no el guion de
     * `Cuit.formateado()`, para no sugerir que es el mismo tipo de división
     * sobre un formato distinto-, y nada más: ninguna subdivisión en grupos
     * más chicos (de a cuatro, por ejemplo) tendría respaldo estructural.
     */
    public fun formateado(): String = "$bloque1 $bloque2"

    /** Puerta de entrada para crear o validar una [ClaveBancaria] de cualquier subtipo: [parse], [parseOrNull], [isValid]. */
    public companion object {

        /**
         * Parsea una clave bancaria, sea CBU o CVU. Acepta guiones, puntos y
         * espacios como separadores.
         *
         * Para exigir un subtipo específico, usar [Cbu.parse] o [Cvu.parse].
         *
         * @throws IllegalArgumentException si el input no es una clave
         *   bancaria válida.
         */
        @JvmStatic
        public fun parse(input: String): ClaveBancaria =
            parseOrNull(input)
                ?: throw IllegalArgumentException("Clave bancaria inválida: $input")

        /**
         * Igual que [parse], pero devuelve `null` en vez de tirar excepción.
         * Es la variante apropiada para input de usuario.
         */
        @JvmStatic
        public fun parseOrNull(input: String): ClaveBancaria? {
            val digitos = normalizarClaveBancaria(input) ?: return null
            if (!cierranLosDosVerificadores(digitos)) return null
            return if (digitos.startsWith(CENTINELA_CVU)) Cvu(digitos) else Cbu(digitos)
        }

        /**
         * Indica si el input es una clave bancaria válida (CBU o CVU), sin
         * construir la instancia. Útil para filtrar colecciones.
         */
        @JvmStatic
        public fun isValid(input: String): Boolean = parseOrNull(input) != null
    }
}

/**
 * Clave Bancaria Uniforme.
 *
 * Un `Cbu` es válido por construcción: si tenés una instancia en la mano, sus
 * dos dígitos verificadores ya fueron comprobados y su centinela **no** es
 * [CENTINELA_CVU]. La única forma de crear una es a través de [parse] o
 * [parseOrNull].
 *
 * Ver `docs/decisiones/0001-cbu-vs-cvu.md`.
 *
 * @property valor los 22 dígitos ASCII (`0` a `9`), sin separadores.
 */
public class Cbu internal constructor(
    override val valor: String,
) : ClaveBancaria {

    override val bloque1: String
        get() = valor.substring(0, LONGITUD_BLOQUE1)

    override val bloque2: String
        get() = valor.substring(LONGITUD_BLOQUE1, LONGITUD)

    override val numeroCuenta: String
        get() = valor.substring(LONGITUD_BLOQUE1, LONGITUD - 1)

    override val digitoVerificadorBloque1: Int
        get() = valor[LONGITUD_CUERPO_BLOQUE1].digitToInt()

    override val digitoVerificadorBloque2: Int
        get() = valor[LONGITUD - 1].digitToInt()

    /** Código de la entidad bancaria (BCRA). Los tres primeros dígitos. */
    public val codigoEntidad: String
        get() = valor.substring(0, 3)

    /** Código de sucursal. Dígitos 4 a 7. */
    public val sucursal: String
        get() = valor.substring(3, LONGITUD_CUERPO_BLOQUE1)

    /** Devuelve [valor]: los 22 dígitos sin separadores. */
    override fun toString(): String = valor

    /** Dos [Cbu] son iguales si tienen el mismo [valor]. */
    override fun equals(other: Any?): Boolean =
        this === other || (other is Cbu && valor == other.valor)

    /** Coherente con [equals]: se basa en [valor]. */
    override fun hashCode(): Int = valor.hashCode()

    /** Puerta de entrada para crear o validar un [Cbu]: [parse], [parseOrNull], [isValid]. */
    public companion object {

        /**
         * Parsea un CBU. Acepta guiones, puntos y espacios como separadores.
         * Rechaza una clave que sea un CVU válido: para eso usar [Cvu.parse]
         * o [ClaveBancaria.parse].
         *
         * @throws IllegalArgumentException si el input no es un CBU válido.
         */
        @JvmStatic
        public fun parse(input: String): Cbu =
            parseOrNull(input) ?: throw IllegalArgumentException("CBU inválido: $input")

        /**
         * Igual que [parse], pero devuelve `null` en vez de tirar excepción.
         * Es la variante apropiada para input de usuario.
         */
        @JvmStatic
        public fun parseOrNull(input: String): Cbu? {
            val digitos = normalizarClaveBancaria(input) ?: return null
            if (digitos.startsWith(CENTINELA_CVU)) return null
            if (!cierranLosDosVerificadores(digitos)) return null
            return Cbu(digitos)
        }

        /**
         * Indica si el input es un CBU válido, sin construir la instancia.
         * Útil para filtrar colecciones.
         */
        @JvmStatic
        public fun isValid(input: String): Boolean = parseOrNull(input) != null
    }
}

/**
 * Clave Virtual Uniforme.
 *
 * Un `Cvu` es válido por construcción: si tenés una instancia en la mano, sus
 * dos dígitos verificadores ya fueron comprobados y su centinela **es**
 * [CENTINELA_CVU]. La única forma de crear una es a través de [parse] o
 * [parseOrNull].
 *
 * Ver `docs/decisiones/0001-cbu-vs-cvu.md`.
 *
 * @property valor los 22 dígitos ASCII (`0` a `9`), sin separadores.
 */
public class Cvu internal constructor(
    override val valor: String,
) : ClaveBancaria {

    override val bloque1: String
        get() = valor.substring(0, LONGITUD_BLOQUE1)

    override val bloque2: String
        get() = valor.substring(LONGITUD_BLOQUE1, LONGITUD)

    override val numeroCuenta: String
        get() = valor.substring(LONGITUD_BLOQUE1, LONGITUD - 1)

    override val digitoVerificadorBloque1: Int
        get() = valor[LONGITUD_CUERPO_BLOQUE1].digitToInt()

    override val digitoVerificadorBloque2: Int
        get() = valor[LONGITUD - 1].digitToInt()

    /** Código del proveedor de servicios de pago (PSP). Dígitos 4 a 7. */
    public val codigoPsp: String
        get() = valor.substring(3, LONGITUD_CUERPO_BLOQUE1)

    /** Devuelve [valor]: los 22 dígitos sin separadores. */
    override fun toString(): String = valor

    /** Dos [Cvu] son iguales si tienen el mismo [valor]. */
    override fun equals(other: Any?): Boolean =
        this === other || (other is Cvu && valor == other.valor)

    /** Coherente con [equals]: se basa en [valor]. */
    override fun hashCode(): Int = valor.hashCode()

    /** Puerta de entrada para crear o validar un [Cvu]: [parse], [parseOrNull], [isValid]. */
    public companion object {

        /**
         * Parsea un CVU. Acepta guiones, puntos y espacios como separadores.
         * Rechaza una clave que sea un CBU válido: para eso usar [Cbu.parse]
         * o [ClaveBancaria.parse].
         *
         * @throws IllegalArgumentException si el input no es un CVU válido.
         */
        @JvmStatic
        public fun parse(input: String): Cvu =
            parseOrNull(input) ?: throw IllegalArgumentException("CVU inválido: $input")

        /**
         * Igual que [parse], pero devuelve `null` en vez de tirar excepción.
         * Es la variante apropiada para input de usuario.
         */
        @JvmStatic
        public fun parseOrNull(input: String): Cvu? {
            val digitos = normalizarClaveBancaria(input) ?: return null
            if (!digitos.startsWith(CENTINELA_CVU)) return null
            if (!cierranLosDosVerificadores(digitos)) return null
            return Cvu(digitos)
        }

        /**
         * Indica si el input es un CVU válido, sin construir la instancia.
         * Útil para filtrar colecciones.
         */
        @JvmStatic
        public fun isValid(input: String): Boolean = parseOrNull(input) != null
    }
}
