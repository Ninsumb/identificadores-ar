package io.github.ninsumb.identificadores

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

/**
 * Property-based testing.
 *
 * En vez de enumerar casos a mano, se declaran propiedades que deben valer para
 * *todo* input, y Kotest genera cientos de casos buscando un contraejemplo.
 * Cuando encuentra uno, lo reduce al mínimo que sigue fallando (shrinking).
 */
class CuitPropertyTest : StringSpec({

    /** Cualquier cuerpo de 10 dígitos. */
    val cuerpo: Arb<String> =
        Arb.list(Arb.int(0..9), 10..10).map { it.joinToString("") }

    /**
     * Cuerpos que admiten dígito verificador, o sea todos menos los de resto 1.
     * Aproximadamente 10 de cada 11.
     */
    val cuerpoConDv: Arb<String> =
        cuerpo.filter { Cuit.calcularDigitoVerificador(it) != null }

    /** CUIT completos y válidos, construidos a partir del algoritmo. */
    val cuitValido: Arb<String> =
        cuerpoConDv.map { it + Cuit.calcularDigitoVerificador(it) }

    // ---------------------------------------------------------------

    "todo CUIT construido con su dígito calculado es válido" {
        checkAll(cuitValido) { cuit ->
            Cuit.isValid(cuit) shouldBe true
        }
    }

    "el dígito verificador calculado siempre está entre 0 y 9" {
        checkAll(cuerpoConDv) { c ->
            val dv = Cuit.calcularDigitoVerificador(c)!!
            (dv in 0..9) shouldBe true
        }
    }

    /**
     * Ésta es la propiedad que justifica el algoritmo entero.
     *
     * El módulo 11 detecta **todos** los errores de un solo dígito. La razón es
     * aritmética: al cambiar el dígito de la posición `p`, la suma varía en
     * `peso[p] * diferencia`. Los pesos van de 2 a 7 y la diferencia está entre
     * -9 y 9, así que ninguno de los dos es múltiplo de 11. Como 11 es primo,
     * el producto tampoco lo es, y el resto necesariamente cambia.
     */
    "alterar cualquier dígito individual invalida el CUIT" {
        checkAll(cuitValido, Arb.int(0..10), Arb.int(1..9)) { cuit, pos, delta ->
            val original = cuit[pos].digitToInt()
            val alterado = (original + delta) % 10
            val roto = cuit.substring(0, pos) + alterado + cuit.substring(pos + 1)

            Cuit.isValid(roto) shouldBe false
        }
    }

    "parsear y formatear no pierde información" {
        checkAll(cuitValido) { cuit ->
            val parseado = Cuit.parse(cuit)
            Cuit.parse(parseado.formateado()) shouldBe parseado
        }
    }

    "los separadores no cambian el resultado" {
        checkAll(cuitValido) { cuit ->
            val conGuiones = "${cuit.take(2)}-${cuit.substring(2, 10)}-${cuit.last()}"
            val conPuntos = conGuiones.replace('-', '.')
            val conEspacios = conGuiones.replace('-', ' ')

            Cuit.parse(conGuiones) shouldBe Cuit.parse(cuit)
            Cuit.parse(conPuntos) shouldBe Cuit.parse(cuit)
            Cuit.parse(conEspacios) shouldBe Cuit.parse(cuit)
        }
    }

    "isValid y parseOrNull siempre coinciden" {
        checkAll(cuerpo, Arb.int(0..9)) { c, dv ->
            val candidato = c + dv
            Cuit.isValid(candidato) shouldBe (Cuit.parseOrNull(candidato) != null)
        }
    }

    "los componentes reconstruyen el valor original" {
        checkAll(cuitValido) { cuit ->
            val c = Cuit.parse(cuit)
            (c.prefijo + c.numero + c.digitoVerificador) shouldBe c.valor
        }
    }

    "un cuerpo de resto 1 nunca produce un CUIT válido" {
        val cuerpoSinDv = cuerpo.filter { Cuit.calcularDigitoVerificador(it) == null }

        checkAll(cuerpoSinDv, Arb.int(0..9)) { c, dv ->
            Cuit.isValid(c + dv) shouldBe false
        }
    }
})
