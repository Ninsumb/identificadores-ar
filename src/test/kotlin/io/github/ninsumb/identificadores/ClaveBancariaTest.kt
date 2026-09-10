package io.github.ninsumb.identificadores

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Casos concretos de validación de CBU y CVU.
 *
 * Los valores usados acá están verificados a mano con el algoritmo
 * "clave 10 con ponderador 9713" (BCRA, Comunicación "A" 2622): se pondera
 * cada dígito según su distancia al último con el ciclo [3,1,7,9] aplicado de
 * derecha a izquierda, se suman los productos, y el dígito verificador es
 * `(10 - suma % 10) % 10`. Ver `docs/decisiones/0005-modulo-10-cbu-cvu.md`.
 */
class ClaveBancariaTest : StringSpec({

    // ---------------------------------------------------------------
    // Casos válidos
    // ---------------------------------------------------------------

    "acepta un CBU válido" {
        // entidad=011 sucursal=0059 -> cuerpo1=0110059, pesos[7,1,3,9,7,1,3]
        // suma = 0+1+3+0+0+5+27 = 36, resto=6, dv1=4
        // cuenta=0000000000001 -> suma=3, resto=3, dv2=7
        Cbu.isValid("0110059400000000000017") shouldBe true
        ClaveBancaria.isValid("0110059400000000000017") shouldBe true
    }

    "acepta un CVU válido" {
        // centinela=000 psp=0001 -> cuerpo1=0000001, suma=3, resto=3, dv1=7
        // cuenta=0000000000001 -> suma=3, resto=3, dv2=7
        Cvu.isValid("0000001700000000000017") shouldBe true
        ClaveBancaria.isValid("0000001700000000000017") shouldBe true
    }

    // ---------------------------------------------------------------
    // Cálculo del dígito verificador (módulo 10)
    // ---------------------------------------------------------------

    "calcula el dígito verificador del bloque 1 (7 dígitos)" {
        // 0110059: suma=36, resto=6, dv=4
        calcularDigitoVerificadorModulo10("0110059") shouldBe 4
    }

    "calcula el dígito verificador del bloque 2 (13 dígitos)" {
        // 0000000000001: suma=3, resto=3, dv=7
        calcularDigitoVerificadorModulo10("0000000000001") shouldBe 7
    }

    "resto múltiplo de 10 produce dígito verificador 0, en los dos largos" {
        // 0110002 (entidad 011, sucursal 0002): suma=10, resto=0, dv=0
        calcularDigitoVerificadorModulo10("0110002") shouldBe 0
        // 0000000000013: suma=10, resto=0, dv=0
        calcularDigitoVerificadorModulo10("0000000000013") shouldBe 0
        // clave completa con los dos dígitos verificadores en 0
        Cbu.isValid("0110002000000000000130") shouldBe true
    }

    // ---------------------------------------------------------------
    // Cada bloque falla de forma independiente
    // ---------------------------------------------------------------

    "un bloque 1 corrupto invalida la clave aunque el bloque 2 cierre" {
        // el dv1 correcto de 0110059 es 4; se reemplaza por 5
        Cbu.isValid("0110059500000000000017") shouldBe false
        // el bloque 2, aislado, sigue calculando el mismo dígito correcto
        calcularDigitoVerificadorModulo10("0000000000001") shouldBe 7
    }

    "un bloque 2 corrupto invalida la clave aunque el bloque 1 cierre" {
        // el dv2 correcto de 0000000000001 es 7; se reemplaza por 8
        Cbu.isValid("0110059400000000000018") shouldBe false
        // el bloque 1, aislado, sigue calculando el mismo dígito correcto
        calcularDigitoVerificadorModulo10("0110059") shouldBe 4
    }

    // ---------------------------------------------------------------
    // Discriminación CBU/CVU, en los dos sentidos
    // ---------------------------------------------------------------

    "Cbu rechaza un CVU válido" {
        Cbu.isValid("0000001700000000000017") shouldBe false
        Cbu.parseOrNull("0000001700000000000017").shouldBeNull()
        shouldThrow<IllegalArgumentException> { Cbu.parse("0000001700000000000017") }
    }

    "Cvu rechaza un CBU válido" {
        Cvu.isValid("0110059400000000000017") shouldBe false
        Cvu.parseOrNull("0110059400000000000017").shouldBeNull()
        shouldThrow<IllegalArgumentException> { Cvu.parse("0110059400000000000017") }
    }

    "ClaveBancaria.parse despacha al subtipo correcto" {
        ClaveBancaria.parse("0110059400000000000017").shouldBeInstanceOf<Cbu>()
        ClaveBancaria.parse("0000001700000000000017").shouldBeInstanceOf<Cvu>()
    }

    // ---------------------------------------------------------------
    // Rechazos: longitudes y caracteres inválidos
    // ---------------------------------------------------------------

    "rechaza longitudes distintas de 22" {
        ClaveBancaria.isValid("011005940000000000001") shouldBe false   // 21
        ClaveBancaria.isValid("01100594000000000000177") shouldBe false // 24
        ClaveBancaria.isValid("") shouldBe false
    }

    "rechaza caracteres no numéricos" {
        ClaveBancaria.isValid("011005940000000000001A") shouldBe false
        ClaveBancaria.isValid("hola mundo, esto no es una clave!!!!!") shouldBe false
    }

    // ---------------------------------------------------------------
    // Solo dígitos ASCII: mismo criterio que en Cuit
    // ---------------------------------------------------------------

    "rechaza dígitos arábigo-índico en un CBU aunque el valor numérico cierre" {
        // "0110059400000000000017" con cada dígito reescrito en arábigo-índico
        ClaveBancaria.isValid("٠١١٠٠٥٩٤٠٠٠٠٠٠٠٠٠٠٠٠١٧") shouldBe false
    }

    "rechaza dígitos devanagari en un CBU aunque el valor numérico cierre" {
        // "0110059400000000000017" con cada dígito reescrito en devanagari
        ClaveBancaria.isValid("०११००५९४००००००००००००१७") shouldBe false
    }

    "rechaza dígitos arábigo-índico en un CVU aunque el valor numérico cierre" {
        // "0000001700000000000017" con cada dígito reescrito en arábigo-índico
        ClaveBancaria.isValid("٠٠٠٠٠٠١٧٠٠٠٠٠٠٠٠٠٠٠٠١٧") shouldBe false
    }

    "rechaza dígitos devanagari en un CVU aunque el valor numérico cierre" {
        // "0000001700000000000017" con cada dígito reescrito en devanagari
        ClaveBancaria.isValid("००००००१७००००००००००००१७") shouldBe false
    }

    "calcularDigitoVerificadorModulo10 rechaza dígitos no ASCII" {
        shouldThrow<IllegalArgumentException> {
            calcularDigitoVerificadorModulo10("٠١١٠٠٥٩")
        }
    }

    // ---------------------------------------------------------------
    // Normalización de entrada: separadores
    // ---------------------------------------------------------------

    "acepta el mismo CBU con distintos separadores" {
        val esperado = Cbu.parse("0110059400000000000017")

        Cbu.parse("011-0059-4-0000000000001-7") shouldBe esperado
        Cbu.parse("011.0059.4.0000000000001.7") shouldBe esperado
        Cbu.parse("011 0059 4 0000000000001 7") shouldBe esperado
        Cbu.parse("  0110059400000000000017  ") shouldBe esperado
    }

    // ---------------------------------------------------------------
    // Componentes
    // ---------------------------------------------------------------

    "expone los componentes de un CBU" {
        val cbu = Cbu.parse("0110059400000000000017")

        cbu.codigoEntidad shouldBe "011"
        cbu.sucursal shouldBe "0059"
        cbu.bloque1 shouldBe "01100594"
        cbu.bloque2 shouldBe "00000000000017"
        cbu.numeroCuenta shouldBe "0000000000001"
        cbu.digitoVerificadorBloque1 shouldBe 4
        cbu.digitoVerificadorBloque2 shouldBe 7
        cbu.valor shouldBe "0110059400000000000017"
    }

    "expone los componentes de un CVU" {
        val cvu = Cvu.parse("0000001700000000000017")

        cvu.codigoPsp shouldBe "0001"
        cvu.bloque1 shouldBe "00000017"
        cvu.bloque2 shouldBe "00000000000017"
        cvu.numeroCuenta shouldBe "0000000000001"
        cvu.digitoVerificadorBloque1 shouldBe 7
        cvu.digitoVerificadorBloque2 shouldBe 7
        cvu.valor shouldBe "0000001700000000000017"
    }

    // ---------------------------------------------------------------
    // formateado(): los dos bloques separados por un espacio
    // ---------------------------------------------------------------

    "formatea un CBU con los dos bloques separados por un espacio" {
        Cbu.parse("0110059400000000000017").formateado() shouldBe "01100594 00000000000017"
    }

    "formatea un CVU con los dos bloques separados por un espacio" {
        Cvu.parse("0000001700000000000017").formateado() shouldBe "00000017 00000000000017"
    }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    "toString devuelve los 22 dígitos sin separadores" {
        Cbu.parse("0110059400000000000017").toString() shouldBe "0110059400000000000017"
        Cvu.parse("0000001700000000000017").toString() shouldBe "0000001700000000000017"
    }

    // ---------------------------------------------------------------
    // Igualdad
    // ---------------------------------------------------------------

    "dos CBU con el mismo valor son iguales" {
        val a = Cbu.parse("0110059400000000000017")
        val b = Cbu.parse("011-0059-4-0000000000001-7")

        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
    }

    "CBU distintos no son iguales" {
        // segundo CBU: entidad=072 sucursal=1234 -> suma=51, resto=1, dv1=9
        // cuenta=0000012345678 -> suma=156, resto=6, dv2=4
        Cbu.parse("0110059400000000000017") shouldNotBe Cbu.parse("0721234900000123456784")
    }

    "un Cbu no es igual a null ni a un valor de otro tipo" {
        val cbu: Any = Cbu.parse("0110059400000000000017")
        cbu shouldNotBe null
        cbu shouldNotBe "0110059400000000000017"
    }

    // ---------------------------------------------------------------
    // parseOrNull / parse
    // ---------------------------------------------------------------

    "parseOrNull devuelve null en vez de tirar" {
        Cbu.parseOrNull("0110059400000000000018").shouldBeNull() // dv2 incorrecto
        Cvu.parseOrNull("0000001700000000000018").shouldBeNull()
        ClaveBancaria.parseOrNull("0110059400000000000018").shouldBeNull()
    }

    "parse tira IllegalArgumentException" {
        shouldThrow<IllegalArgumentException> { Cbu.parse("0110059400000000000018") }
        shouldThrow<IllegalArgumentException> { Cvu.parse("0000001700000000000018") }
        shouldThrow<IllegalArgumentException> { ClaveBancaria.parse("0110059400000000000018") }
    }

    // ---------------------------------------------------------------
    // Precondiciones internas
    // ---------------------------------------------------------------

    "calcularDigitoVerificadorModulo10 exige un cuerpo no vacío" {
        shouldThrow<IllegalArgumentException> {
            calcularDigitoVerificadorModulo10("")
        }
    }

    "calcularDigitoVerificadorModulo10 exige que sean dígitos" {
        shouldThrow<IllegalArgumentException> {
            calcularDigitoVerificadorModulo10("12A")
        }
    }
})
