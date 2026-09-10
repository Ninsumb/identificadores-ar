package io.github.ninsumb.identificadores

import java.time.LocalDate

/**
 * Fecha en que se revisó por última vez el contenido de los catálogos
 * embebidos ([CatalogoEntidades.EMBEBIDO] y [CatalogoPsp.EMBEBIDO]). Ambos
 * están vacíos en esta versión; ver `docs/decisiones/0008-catalogos-de-nombres.md`.
 */
private val VIGENCIA_EMBEBIDO: LocalDate = LocalDate.of(2026, 9, 10)

/**
 * Catálogo de nombres de entidades bancarias, indexado por el código de
 * entidad de 3 dígitos — las posiciones 1 a 3 de un [Cbu], es decir
 * [Cbu.codigoEntidad].
 *
 * Separa dos hechos con estatus distinto (ver
 * `docs/decisiones/0003-catalogo-de-entidades.md`): el **código** es
 * estructural, no puede desactualizarse, y lo expone el value object; el
 * **nombre** sale de una nómina mutable del BCRA y se resuelve acá, fuera del
 * value object, detrás de esta interfaz. `Cbu` y `Cvu` **no** exponen el
 * nombre: no tocan datos perecederos.
 *
 * Es una interfaz distinta de [CatalogoPsp] —no una sola con dos métodos—
 * porque el código de entidad (3 dígitos) y el de PSP (4 dígitos) viven en
 * espacios de nombres separados, las dos nóminas se actualizan por separado, y
 * cada catálogo tiene su propia [vigencia]. Ver
 * `docs/decisiones/0008-catalogos-de-nombres.md`.
 */
public interface CatalogoEntidades {

    /**
     * Fecha a la que se verificó el contenido de este catálogo. Permite a
     * quien lo consume evaluar si los datos le sirven, sin depender de un
     * comentario en el código. Una implementación contra una base de datos
     * devuelve la fecha de corte de sus datos.
     */
    public val vigencia: LocalDate

    /**
     * Nombre de la entidad para un código de entidad de 3 dígitos, o `null`
     * si este catálogo no tiene un nombre para ese código.
     *
     * `null` **no** significa "el código no existe": significa "este catálogo
     * no lo tiene". La tabla embebida puede ser parcial o estar vacía (ver
     * [EMBEBIDO]). Un código con forma inválida —longitud distinta de 3,
     * caracteres no numéricos, cadena vacía— también devuelve `null`, no una
     * excepción: esto es un lookup, no un validador.
     */
    public fun nombre(codigo: String): String?

    /** Puerta de entrada al catálogo embebido: [EMBEBIDO]. */
    public companion object {

        /**
         * Catálogo embebido de nombres de entidades bancarias.
         *
         * **Vacío en la versión 1.0.0 por falta de acceso a la fuente, no por
         * decisión de diseño.** Para los bancos existe fuente oficial del BCRA
         * —la Comunicación "B" de "Cuentas Corrientes, Especiales y
         * Restringidas abiertas en el BCRA", Cuadros I y III—, pero se publica
         * como PDF escaneado y no se pudo transcribir y verificar código por
         * código al construir esta versión. Se prefirió una tabla vacía a una
         * plausible: es el único punto de la librería donde un dato puede
         * estar mal sin que ningún test lo detecte. La tabla se carga en una
         * versión MINOR posterior.
         *
         * Mientras tanto, [nombre] siempre devuelve `null`. Quien tenga una
         * nómina confiable inyecta su propia implementación de
         * [CatalogoEntidades].
         *
         * Ver `docs/decisiones/0008-catalogos-de-nombres.md`.
         */
        @JvmStatic
        public val EMBEBIDO: CatalogoEntidades = CatalogoVacio(VIGENCIA_EMBEBIDO)
    }
}

/**
 * Catálogo de nombres de proveedores de servicios de pago (PSP), indexado por
 * el código de PSP de 4 dígitos — las posiciones 4 a 7 de un [Cvu], es decir
 * [Cvu.codigoPsp].
 *
 * Mismo mecanismo que [CatalogoEntidades] (ver
 * `docs/decisiones/0003-catalogo-de-entidades.md`), aplicado a los CVU. `Cvu`
 * **no** expone el nombre del PSP: no toca datos perecederos.
 *
 * Es una interfaz distinta de [CatalogoEntidades] a propósito: los códigos de
 * entidad y de PSP viven en espacios separados, las nóminas se actualizan por
 * separado, y cada catálogo tiene su propia [vigencia]. Ver
 * `docs/decisiones/0008-catalogos-de-nombres.md`.
 */
public interface CatalogoPsp {

    /**
     * Fecha a la que se verificó el contenido de este catálogo. Permite a
     * quien lo consume evaluar si los datos le sirven, sin depender de un
     * comentario en el código. Una implementación contra una base de datos
     * devuelve la fecha de corte de sus datos.
     */
    public val vigencia: LocalDate

    /**
     * Nombre del PSP para un código de PSP de 4 dígitos, o `null` si este
     * catálogo no tiene un nombre para ese código.
     *
     * `null` **no** significa "el código no existe": significa "este catálogo
     * no lo tiene". Un código con forma inválida —longitud distinta de 4,
     * caracteres no numéricos, cadena vacía— también devuelve `null`, no una
     * excepción: esto es un lookup, no un validador.
     */
    public fun nombre(codigo: String): String?

    /** Puerta de entrada al catálogo embebido: [EMBEBIDO]. */
    public companion object {

        /**
         * Catálogo embebido de nombres de PSP.
         *
         * **Vacío en la versión 1.0.0 por decisión, no por omisión.** No
         * existe una fuente oficial pública del código de ruteo de 4 dígitos
         * de un PSP: lo asigna la cámara compensadora (BCRA, Comunicación "A"
         * 6510; Boletín CIMPRA 518) y no se publica como dato abierto. El BCRA
         * publica el "Registro de proveedores de servicios de pago" con
         * nombre, CUIT y número de registro, pero no el código del CVU. Los
         * mapeos de terceros que circulan son ingeniería inversa, y esta
         * librería no publica datos que no puede respaldar.
         *
         * La asimetría de costos lo decide: un nombre equivocado acá haría que
         * alguien crea que está transfiriendo a donde quería; `null` hace que
         * el consumidor muestre "no identificada".
         *
         * [nombre] siempre devuelve `null`. Quien tenga datos confiables
         * inyecta su propia implementación de [CatalogoPsp].
         *
         * Ver `docs/decisiones/0008-catalogos-de-nombres.md`.
         */
        @JvmField
        public val EMBEBIDO: CatalogoPsp = CatalogoVacio(VIGENCIA_EMBEBIDO)
    }
}

/**
 * Implementación de [CatalogoEntidades] y [CatalogoPsp] sin ninguna entrada:
 * [nombre] siempre devuelve `null`, para cualquier input. Es lo que usan los
 * dos `EMBEBIDO` en la versión 1.0.0.
 *
 * Implementa las dos interfaces porque su comportamiento —"no sé ningún
 * nombre"— es idéntico para ambas; que sean tipos públicos distintos es lo que
 * importa, no que tengan clases de respaldo distintas. Ver
 * `docs/decisiones/0008-catalogos-de-nombres.md`.
 */
private class CatalogoVacio(
    override val vigencia: LocalDate,
) : CatalogoEntidades, CatalogoPsp {

    override fun nombre(codigo: String): String? = null
}
