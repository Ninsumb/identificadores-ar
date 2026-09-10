package io.github.ninsumb.identificadores

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.LocalDate

/**
 * Property-based testing para los catálogos de nombres.
 *
 * Lo que hay que probar acá: que la tabla embebida vacía devuelve `null` para
 * *cualquier* input, y que el lookup de una implementación inyectada coincide
 * exactamente con su `Map` de respaldo.
 */
class CatalogosPropertyTest : StringSpec({

    "el catálogo embebido de entidades devuelve null para cualquier string" {
        checkAll(Arb.string(0..30)) { codigo ->
            CatalogoEntidades.EMBEBIDO.nombre(codigo).shouldBeNull()
        }
    }

    "el catálogo embebido de PSP devuelve null para cualquier string" {
        checkAll(Arb.string(0..30)) { codigo ->
            CatalogoPsp.EMBEBIDO.nombre(codigo).shouldBeNull()
        }
    }

    "un catálogo inyectado devuelve el nombre si el código está en su Map, y null si no" {
        val datos: Arb<Map<String, String>> =
            Arb.map(Arb.string(1..8), Arb.string(1..20), maxSize = 20)

        checkAll(datos, Arb.string(0..10)) { mapa, consulta ->
            val catalogo = CatalogoDePrueba(LocalDate.of(2026, 5, 15), mapa)

            catalogo.nombre(consulta) shouldBe mapa[consulta]
        }
    }

    "la vigencia de un catálogo inyectado es la que declara su implementación" {
        checkAll(Arb.map(Arb.string(1..8), Arb.string(1..20), maxSize = 10)) { mapa ->
            val fecha = LocalDate.of(2020, 1, 1)
            CatalogoDePrueba(fecha, mapa).vigencia shouldBe fecha
        }
    }
})
