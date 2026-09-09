package io.github.ninsumb.identificadores

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

/**
 * Property-based testing para CBU/CVU.
 *
 * Mismo enfoque que en CuitPropertyTest: declarar propiedades que deben valer
 * para *todo* input y dejar que Kotest busque un contraejemplo.
 */
class ClaveBancariaPropertyTest : StringSpec({

    /** Cualquier bloque de 3 dígitos. */
    val cuerpo3: Arb<String> = Arb.list(Arb.int(0..9), 3..3).map { it.joinToString("") }

    /** Cualquier bloque de 4 dígitos (sucursal o código de PSP). */
    val cuerpo4: Arb<String> = Arb.list(Arb.int(0..9), 4..4).map { it.joinToString("") }

    /** Cualquier bloque de 13 dígitos (número de cuenta). */
    val cuerpo13: Arb<String> = Arb.list(Arb.int(0..9), 13..13).map { it.joinToString("") }

    /** Códigos de entidad de 3 dígitos que no coinciden con el centinela de CVU. */
    val entidadNoCentinela: Arb<String> = cuerpo3.filter { it != CENTINELA_CVU }

    /** CBU completos y válidos, construidos a partir del algoritmo. */
    val cbuValido: Arb<String> =
        Arb.bind(entidadNoCentinela, cuerpo4, cuerpo13) { entidad, sucursal, cuenta ->
            val cuerpoBloque1 = entidad + sucursal
            val dv1 = calcularDigitoVerificadorModulo10(cuerpoBloque1)
            val dv2 = calcularDigitoVerificadorModulo10(cuenta)
            "$cuerpoBloque1$dv1$cuenta$dv2"
        }

    /** CVU completos y válidos, construidos a partir del algoritmo. */
    val cvuValido: Arb<String> =
        Arb.bind(cuerpo4, cuerpo13) { psp, cuenta ->
            val cuerpoBloque1 = CENTINELA_CVU + psp
            val dv1 = calcularDigitoVerificadorModulo10(cuerpoBloque1)
            val dv2 = calcularDigitoVerificadorModulo10(cuenta)
            "$cuerpoBloque1$dv1$cuenta$dv2"
        }

    /** Cualquier clave bancaria válida, CBU o CVU. */
    val claveValida: Arb<String> = Arb.choice(cbuValido, cvuValido)

    // ---------------------------------------------------------------

    "todo CBU construido con sus dígitos calculados es válido" {
        checkAll(cbuValido) { cbu ->
            Cbu.isValid(cbu) shouldBe true
            ClaveBancaria.isValid(cbu) shouldBe true
        }
    }

    "todo CVU construido con sus dígitos calculados es válido" {
        checkAll(cvuValido) { cvu ->
            Cvu.isValid(cvu) shouldBe true
            ClaveBancaria.isValid(cvu) shouldBe true
        }
    }

    "el dígito verificador calculado siempre está entre 0 y 9" {
        checkAll(cuerpo4, cuerpo13) { c4, c13 ->
            (calcularDigitoVerificadorModulo10(CENTINELA_CVU + c4) in 0..9) shouldBe true
            (calcularDigitoVerificadorModulo10(c13) in 0..9) shouldBe true
        }
    }

    /**
     * Esta es la propiedad que justifica el algoritmo entero, igual que en
     * CuitPropertyTest, pero con un argumento distinto porque acá el módulo
     * (10) no es primo.
     *
     * El ponderador {1,3,7,9} de la "clave 10" son exactamente las cuatro
     * unidades de Z/10Z (los únicos restos coprimos con 10). Al cambiar el
     * dígito de la posición p, la suma pesada varía en `peso[p] * diferencia`.
     * La diferencia está entre 1 y 9 (nunca 0, porque el dígito realmente
     * cambió), así que nunca es múltiplo de 10. Como el peso es coprimo con
     * 10 -es invertible módulo 10-, el producto `peso * diferencia` tampoco
     * puede ser múltiplo de 10: si lo fuera, existiría un inverso de peso que
     * volvería múltiplo de 10 a la diferencia misma, y eso es imposible por
     * ser la diferencia menor que 10 y no nula.
     *
     * A diferencia del argumento análogo para el CUIT, acá no hace falta que
     * el módulo sea primo: alcanza con que cada peso individual sea
     * invertible módulo 10, y 1, 3, 7 y 9 lo son (no comparten factores con
     * 10 = 2×5). Si el algoritmo hubiera usado un peso par o el 5, esta
     * garantía se rompería. Ver `docs/decisiones/0005-modulo-10-cbu-cvu.md`.
     */
    "alterar cualquier dígito individual invalida la clave" {
        checkAll(claveValida, Arb.int(0..21), Arb.int(1..9)) { clave, pos, delta ->
            val original = clave[pos].digitToInt()
            val alterado = (original + delta) % 10
            val roto = clave.substring(0, pos) + alterado + clave.substring(pos + 1)

            ClaveBancaria.isValid(roto) shouldBe false
        }
    }

    "los dos bloques reconstruyen el valor original" {
        checkAll(claveValida) { valor ->
            val clave = ClaveBancaria.parse(valor)
            (clave.bloque1 + clave.bloque2) shouldBe clave.valor
        }
    }

    "parsear el resultado de formateado() da la misma clave" {
        checkAll(claveValida) { valor ->
            val clave = ClaveBancaria.parse(valor)
            ClaveBancaria.parse(clave.formateado()) shouldBe clave
        }
    }

    "los componentes de un Cbu reconstruyen el valor original" {
        checkAll(cbuValido) { valor ->
            val cbu = Cbu.parse(valor)
            (cbu.codigoEntidad + cbu.sucursal + cbu.digitoVerificadorBloque1 +
                cbu.numeroCuenta + cbu.digitoVerificadorBloque2) shouldBe cbu.valor
        }
    }

    "los componentes de un Cvu reconstruyen el valor original" {
        checkAll(cvuValido) { valor ->
            val cvu = Cvu.parse(valor)
            (CENTINELA_CVU + cvu.codigoPsp + cvu.digitoVerificadorBloque1 +
                cvu.numeroCuenta + cvu.digitoVerificadorBloque2) shouldBe cvu.valor
        }
    }

    "los separadores no cambian el resultado" {
        checkAll(claveValida) { clave ->
            val conGuiones =
                "${clave.take(3)}-${clave.substring(3, 8)}-${clave.substring(8, 21)}-${clave.last()}"
            val conPuntos = conGuiones.replace('-', '.')
            val conEspacios = conGuiones.replace('-', ' ')

            ClaveBancaria.parse(conGuiones) shouldBe ClaveBancaria.parse(clave)
            ClaveBancaria.parse(conPuntos) shouldBe ClaveBancaria.parse(clave)
            ClaveBancaria.parse(conEspacios) shouldBe ClaveBancaria.parse(clave)
        }
    }

    "isValid y parseOrNull siempre coinciden" {
        checkAll(Arb.string(0..40)) { candidato ->
            ClaveBancaria.isValid(candidato) shouldBe (ClaveBancaria.parseOrNull(candidato) != null)
        }
    }

    "ningún string arbitrario produce una clave con caracteres fuera de 0-9" {
        checkAll(Arb.string(0..40)) { s ->
            val clave = ClaveBancaria.parseOrNull(s)
            if (clave != null) {
                clave.valor.all { it in '0'..'9' } shouldBe true
            }
        }
    }

    "toda clave válida es CBU o CVU, nunca ambos ni ninguno" {
        checkAll(claveValida) { valor ->
            val esCbu = Cbu.isValid(valor)
            val esCvu = Cvu.isValid(valor)
            (esCbu != esCvu) shouldBe true
            ClaveBancaria.isValid(valor) shouldBe true
        }
    }
})
