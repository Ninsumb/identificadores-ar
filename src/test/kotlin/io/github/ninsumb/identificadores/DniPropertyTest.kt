package io.github.ninsumb.identificadores

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based testing para DNI.
 *
 * Sin dígito verificador no hay una propiedad aritmética que probar como en
 * Cuit o ClaveBancaria. Lo que hay que probar es lo que sí varía acá:
 * longitud, composición, y sobre todo que la cantidad de ceros a la
 * izquierda con la que alguien escribe el número no cambia su identidad.
 */
class DniPropertyTest : StringSpec({

    /** Cualquier valor canónico de 8 dígitos que no sea la matrícula nula. */
    val dniValidoCanonico: Arb<String> =
        Arb.list(Arb.int(0..9), 8..8)
            .map { it.joinToString("") }
            .filter { it != "00000000" }

    /**
     * Cualquier cuerpo de 1 a 8 dígitos que no sea todo ceros: el universo
     * completo de entradas que [Dni.isValid] tiene que aceptar.
     */
    val cuerpoDeLongitudVariable: Arb<String> = Arb.choice(
        (1..8).map { longitud ->
            Arb.list(Arb.int(0..9), longitud..longitud)
                .map { it.joinToString("") }
                .filter { cuerpo -> cuerpo.any { it != '0' } }
        }
    )

    // ---------------------------------------------------------------

    "cualquier cuerpo de 1 a 8 dígitos que no sea todo ceros es un DNI válido" {
        checkAll(cuerpoDeLongitudVariable) { cuerpo ->
            Dni.isValid(cuerpo) shouldBe true
        }
    }

    "el valor canónico de todo Dni válido tiene siempre 8 dígitos" {
        checkAll(cuerpoDeLongitudVariable) { cuerpo ->
            Dni.parse(cuerpo).valor.length shouldBe 8
        }
    }

    /**
     * Esta es la propiedad que justifica por qué la forma canónica rellena
     * con ceros en vez de preservar la entrada tal cual: cuántos ceros haya
     * antepuestos no puede cambiar de qué DNI se trata. Ver
     * `docs/decisiones/0006-dni-validacion-estructural.md`.
     */
    "quitar los ceros a la izquierda de un DNI válido no cambia su identidad" {
        checkAll(dniValidoCanonico) { canonico ->
            val sinCeros = canonico.trimStart('0')
            Dni.parse(sinCeros) shouldBe Dni.parse(canonico)
        }
    }

    "parsear el valor canónico de un Dni da el mismo Dni" {
        checkAll(dniValidoCanonico) { canonico ->
            val dni = Dni.parse(canonico)
            Dni.parse(dni.valor) shouldBe dni
        }
    }

    "parsear el resultado de formateado() da el mismo Dni" {
        checkAll(dniValidoCanonico) { canonico ->
            val dni = Dni.parse(canonico)
            Dni.parse(dni.formateado()) shouldBe dni
        }
    }

    "los separadores no cambian el resultado" {
        checkAll(dniValidoCanonico, Arb.int(1..7)) { canonico, corte ->
            val conGuion = canonico.substring(0, corte) + "-" + canonico.substring(corte)
            val conPunto = conGuion.replace('-', '.')
            val conEspacio = conGuion.replace('-', ' ')

            Dni.parse(conGuion) shouldBe Dni.parse(canonico)
            Dni.parse(conPunto) shouldBe Dni.parse(canonico)
            Dni.parse(conEspacio) shouldBe Dni.parse(canonico)
        }
    }

    "isValid y parseOrNull siempre coinciden" {
        checkAll(Arb.string(0..20)) { candidato ->
            Dni.isValid(candidato) shouldBe (Dni.parseOrNull(candidato) != null)
        }
    }

    "ningún string arbitrario produce un Dni con caracteres fuera de 0-9" {
        checkAll(Arb.string(0..20)) { s ->
            val dni = Dni.parseOrNull(s)
            if (dni != null) {
                dni.valor.all { it in '0'..'9' } shouldBe true
            }
        }
    }
})
