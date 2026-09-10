package io.github.ninsumb.identificadores

import java.util.Locale

private const val LONGITUD_MINIMA: Int = 6
private const val LONGITUD_MAXIMA: Int = 20

/**
 * Determina si un carácter puede formar parte de un alias bancario: dígito
 * ASCII (`0` a `9`), letra ASCII (`A` a `Z` o `a` a `z`), o uno de los dos
 * signos `.` y `-`.
 *
 * Lo fija el texto ordenado del BCRA "Sistema Nacional de Pagos – Servicios de
 * pago", punto 3.7.2.1.i (Comunicación "A" 8114): enumera exactamente estos
 * caracteres y agrega *"El resto de los caracteres se considerarán inválidos."*
 * No hay guion bajo, ni espacios, ni acentos, ni `ñ`.
 *
 * Ver `docs/decisiones/0007-alias-bancario-validacion-de-forma.md`.
 */
private fun esCaracterDeAlias(c: Char): Boolean =
    c in '0'..'9' || c in 'a'..'z' || c in 'A'..'Z' || c == '.' || c == '-'

/**
 * Recorta los espacios de los extremos, valida longitud y composición, y
 * devuelve el texto ya recortado —**sin** canonizar a minúsculas—. Devuelve
 * `null` si el input no puede ser un alias: longitud fuera de 6-20 una vez
 * recortado, o algún carácter que no cumpla [esCaracterDeAlias].
 *
 * El recorte de los extremos es el único tratamiento de entrada: el `.` y el
 * `-` son parte del alias, no separadores como en `Cuit` o `Dni`, así que acá
 * no se usa `quitarSeparadores`.
 *
 * Ver `docs/decisiones/0007-alias-bancario-validacion-de-forma.md`.
 */
private fun normalizarAlias(input: String): String? {
    val recortado = input.trim()
    if (recortado.length !in LONGITUD_MINIMA..LONGITUD_MAXIMA) return null
    if (!recortado.all(::esCaracterDeAlias)) return null
    return recortado
}

/**
 * Alias bancario (o "alias CBU"): una etiqueta legible asociada a una CBU o
 * CVU para recibir transferencias, en reemplazo de tipear los 22 dígitos.
 *
 * Un `AliasBancario` es válido por construcción: la única forma de crear uno es
 * a través de [parse] o [parseOrNull].
 *
 * **La validación es únicamente de forma.** El alias no tiene dígito
 * verificador ni ningún mecanismo de autocontrol, y esta librería no hace I/O:
 * no puede consultar el registro central. `AliasBancario` solo garantiza que
 * el texto tiene entre 6 y 20 caracteres y que todos están en `[A-Za-z0-9.-]`.
 * Un alias sintácticamente perfecto **puede no existir, no estar asignado a
 * ninguna cuenta, o estar en la lista de alias prohibidos** (lenguaje
 * ofensivo, marcas registradas) que administra la cámara compensadora y que no
 * es pública. Tampoco resuelve a qué CBU o CVU apunta: no existe un mecanismo
 * público para hacerlo.
 *
 * El uso de mayúsculas y minúsculas es indistinto por norma del BCRA:
 * `AliasBancario.parse("Mi.Alias")` y `AliasBancario.parse("mi.alias")` son
 * iguales. La forma canónica ([valor]) está en minúsculas; el texto tal como
 * se ingresó se conserva aparte en [original], que no participa de la
 * identidad.
 *
 * El valor de este tipo, igual que el de `Dni`, no está en lo que filtra en
 * tiempo de ejecución —filtra poco— sino en existir como un tipo propio,
 * distinto de un `String` arbitrario, en la frontera del API. *Parse, don't
 * validate.*
 *
 * Reglas de forma: BCRA, texto ordenado "Sistema Nacional de Pagos – Servicios
 * de pago", punto 3.7.2.1.i (Comunicación "A" 8114). Ver
 * `docs/decisiones/0007-alias-bancario-validacion-de-forma.md`.
 *
 * @property valor la forma canónica: el alias recortado y pasado a minúsculas
 *   con `Locale.ROOT`. Es la identidad del value object — [equals], [hashCode]
 *   y [toString] se basan únicamente en esto.
 * @property original el alias tal como lo ingresó quien lo escribió, con su
 *   capitalización intacta (solo se recortan los espacios de los extremos).
 *   Sirve para mostrarle a la persona lo que tipeó, por ejemplo en una
 *   pantalla de confirmación. **No participa de la identidad:** [equals],
 *   [hashCode] y [toString] lo ignoran por completo, así que dos alias que
 *   difieren solo en mayúsculas son iguales y únicamente se distinguen mirando
 *   esta propiedad. Es informativa, no define al value object.
 */
public class AliasBancario private constructor(
    public val valor: String,
    public val original: String,
) {

    /**
     * Devuelve [valor] sin cambios: un alias no tiene separadores que insertar
     * (el `.` y el `-` ya son parte del valor) ni una convención de
     * visualización que aplicar, así que no hay nada que formatear.
     *
     * Existe por uniformidad de API — para poder llamar `formateado()` sobre
     * cualquier identificador de esta librería sin conocer el tipo concreto —.
     * **No** devuelve [original]: dos instancias iguales deben producir el
     * mismo resultado en un método público sin argumentos, y en el resto de
     * los tipos `formateado()` es una función pura de la forma canónica.
     */
    public fun formateado(): String = valor

    /** Devuelve [valor]: la forma canónica, en minúsculas. */
    override fun toString(): String = valor

    /**
     * Dos [AliasBancario] son iguales si tienen el mismo [valor] — es decir, si
     * son el mismo alias sin distinguir mayúsculas de minúsculas. [original] no
     * interviene.
     */
    override fun equals(other: Any?): Boolean =
        this === other || (other is AliasBancario && valor == other.valor)

    /** Coherente con [equals]: se basa en [valor], no en [original]. */
    override fun hashCode(): Int = valor.hashCode()

    /** Puerta de entrada para crear o validar un [AliasBancario]: [parse], [parseOrNull], [isValid]. */
    public companion object {

        /**
         * Parsea un alias bancario. Recorta los espacios de los extremos; no
         * admite ningún carácter fuera de `[A-Za-z0-9.-]`, ni menos de 6 ni
         * más de 20 caracteres.
         *
         * @throws IllegalArgumentException si el input no es un alias válido.
         */
        @JvmStatic
        public fun parse(input: String): AliasBancario =
            parseOrNull(input)
                ?: throw IllegalArgumentException("Alias bancario inválido: $input")

        /**
         * Igual que [parse], pero devuelve `null` en vez de tirar excepción.
         * Es la variante apropiada para input de usuario.
         */
        @JvmStatic
        public fun parseOrNull(input: String): AliasBancario? {
            val recortado = normalizarAlias(input) ?: return null
            // Locale.ROOT y no lowercase() a secas: con el locale por defecto,
            // en una JVM turca la 'I' ASCII baja a 'ı' (U+0131), no a 'i', y la
            // forma canónica dejaría de ser estable entre máquinas.
            val canonico = recortado.lowercase(Locale.ROOT)
            return AliasBancario(canonico, recortado)
        }

        /**
         * Indica si el input es un alias bancario válido, sin construir la
         * instancia. Útil para filtrar colecciones.
         */
        @JvmStatic
        public fun isValid(input: String): Boolean = normalizarAlias(input) != null
    }
}
