package io.github.ninsumb.identificadores

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.date.shouldNotBeAfter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * Casos concretos de los catálogos de nombres.
 *
 * Las dos tablas embebidas están vacías en 1.0.0 (ver
 * `docs/decisiones/0008-catalogos-de-nombres.md`): estos tests fijan ese
 * comportamiento y ejercitan el mecanismo con una implementación inyectada.
 */
class CatalogosTest : StringSpec({

    // ---------------------------------------------------------------
    // CatalogoEntidades.EMBEBIDO: vacío
    // ---------------------------------------------------------------

    "el catálogo embebido de entidades no tiene ningún nombre" {
        CatalogoEntidades.EMBEBIDO.nombre("007").shouldBeNull() // Galicia, si tuviera datos
        CatalogoEntidades.EMBEBIDO.nombre("011").shouldBeNull() // Nación
        CatalogoEntidades.EMBEBIDO.nombre("014").shouldBeNull() // Provincia BA
    }

    "el catálogo embebido de entidades devuelve null ante una forma inválida, sin tirar" {
        CatalogoEntidades.EMBEBIDO.nombre("11").shouldBeNull()
        CatalogoEntidades.EMBEBIDO.nombre("0110").shouldBeNull()
        CatalogoEntidades.EMBEBIDO.nombre("abc").shouldBeNull()
        CatalogoEntidades.EMBEBIDO.nombre("").shouldBeNull()
    }

    "el catálogo embebido de entidades declara su fecha de vigencia" {
        CatalogoEntidades.EMBEBIDO.vigencia shouldBe LocalDate.of(2026, 9, 10)
    }

    // ---------------------------------------------------------------
    // CatalogoPsp.EMBEBIDO: vacío
    // ---------------------------------------------------------------

    "el catálogo embebido de PSP no tiene ningún nombre" {
        CatalogoPsp.EMBEBIDO.nombre("0031").shouldBeNull() // Mercado Pago, si tuviera datos
        CatalogoPsp.EMBEBIDO.nombre("0079").shouldBeNull() // Ualá
    }

    "el catálogo embebido de PSP devuelve null ante una forma inválida, sin tirar" {
        CatalogoPsp.EMBEBIDO.nombre("31").shouldBeNull()
        CatalogoPsp.EMBEBIDO.nombre("00031").shouldBeNull()
        CatalogoPsp.EMBEBIDO.nombre("abcd").shouldBeNull()
        CatalogoPsp.EMBEBIDO.nombre("").shouldBeNull()
    }

    "el catálogo embebido de PSP declara su fecha de vigencia" {
        CatalogoPsp.EMBEBIDO.vigencia shouldBe LocalDate.of(2026, 9, 10)
    }

    "la vigencia de los catálogos embebidos no está en el futuro" {
        val hoy = LocalDate.now()
        CatalogoEntidades.EMBEBIDO.vigencia shouldNotBeAfter hoy
        CatalogoPsp.EMBEBIDO.vigencia shouldNotBeAfter hoy
    }

    // ---------------------------------------------------------------
    // El mecanismo: una implementación inyectada
    // ---------------------------------------------------------------

    "un CatalogoEntidades inyectado resuelve nombres y devuelve null para lo que no tiene" {
        val catalogo: CatalogoEntidades = CatalogoDePrueba(
            LocalDate.of(2026, 5, 15),
            mapOf("011" to "BANCO DE LA NACION ARGENTINA"),
        )

        catalogo.nombre("011") shouldBe "BANCO DE LA NACION ARGENTINA"
        catalogo.nombre("007").shouldBeNull()
        catalogo.vigencia shouldBe LocalDate.of(2026, 5, 15)
    }

    "un CatalogoPsp inyectado resuelve nombres y devuelve null para lo que no tiene" {
        val catalogo: CatalogoPsp = CatalogoDePrueba(
            LocalDate.of(2026, 5, 15),
            mapOf("0031" to "MERCADO PAGO"),
        )

        catalogo.nombre("0031") shouldBe "MERCADO PAGO"
        catalogo.nombre("0079").shouldBeNull()
        catalogo.vigencia shouldBe LocalDate.of(2026, 5, 15)
    }

    // ---------------------------------------------------------------
    // La relación con Cbu/Cvu: el nombre NO vive en el value object
    // ---------------------------------------------------------------

    "el nombre se resuelve a partir del codigo que expone el value object, no en el value object" {
        // 0110059400000000000017: CBU de test de ClaveBancariaTest.
        val cbu = Cbu.parse("0110059400000000000017")
        val catalogo: CatalogoEntidades = CatalogoDePrueba(
            LocalDate.of(2026, 5, 15),
            mapOf("011" to "BANCO DE LA NACION ARGENTINA"),
        )

        catalogo.nombre(cbu.codigoEntidad) shouldBe "BANCO DE LA NACION ARGENTINA"
    }
})

/**
 * Implementación de prueba: un catálogo respaldado por un `Map`. Sirve para
 * las dos interfaces porque su lookup es el mismo.
 */
internal class CatalogoDePrueba(
    override val vigencia: LocalDate,
    private val datos: Map<String, String>,
) : CatalogoEntidades, CatalogoPsp {

    override fun nombre(codigo: String): String? = datos[codigo]
}
