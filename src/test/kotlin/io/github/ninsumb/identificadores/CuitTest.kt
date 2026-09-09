package io.github.ninsumb.identificadores

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Casos concretos de validación de CUIT.
 *
 * Los CUIT usados acá están verificados a mano contra la tabla de pesos.
 * Ver `docs/decisiones/0004-casos-limite-modulo-11.md`.
 */
class CuitTest : StringSpec({

    // ---------------------------------------------------------------
    // Casos válidos
    // ---------------------------------------------------------------

    "acepta un CUIT válido de persona física" {
        // suma = 148, resto = 5, DV = 11 - 5 = 6
        Cuit.isValid("20-12345678-6") shouldBe true
    }

    "acepta un CUIT válido de persona jurídica" {
        // suma = 17, resto = 6, DV = 11 - 6 = 5
        Cuit.isValid("30-00000001-5") shouldBe true
    }

    "acepta un CUIT con prefijo desconocido pero verificador correcto" {
        // suma = 83, resto = 6, DV = 5
        Cuit.isValid("99-00000001-5") shouldBe true
    }

    // ---------------------------------------------------------------
    // Casos límite del módulo 11
    // ---------------------------------------------------------------

    "resto 0 produce dígito verificador 0" {
        // 2000000006 -> suma = 22, resto = 0, DV = 0
        Cuit.calcularDigitoVerificador("2000000006") shouldBe 0
        Cuit.isValid("20-00000006-0") shouldBe true
    }

    "resto 1 no tiene dígito verificador posible" {
        // 2000000001 -> suma = 12, resto = 1
        Cuit.calcularDigitoVerificador("2000000001").shouldBeNull()
    }

    "un cuerpo con resto 1 se rechaza con cualquier dígito verificador" {
        for (dv in 0..9) {
            Cuit.isValid("20-00000001-$dv") shouldBe false
        }
    }

    "el mismo número con prefijo 23 sí es válido" {
        // 2300000001 -> suma = 24, resto = 2, DV = 9
        Cuit.isValid("23-00000001-9") shouldBe true
    }

    "segundo caso verificado: prefijo 27 con resto 1 pasa a 23" {
        // 2700000012 -> suma = 45, resto = 1  (sin DV posible)
        Cuit.calcularDigitoVerificador("2700000012").shouldBeNull()
        // 2300000012 -> suma = 29, resto = 7, DV = 4
        Cuit.isValid("23-00000012-4") shouldBe true
    }

    // ---------------------------------------------------------------
    // Rechazos
    // ---------------------------------------------------------------

    "rechaza un dígito verificador incorrecto" {
        Cuit.isValid("20-12345678-7") shouldBe false
    }

    "rechaza longitudes distintas de 11" {
        Cuit.isValid("2012345678") shouldBe false     // 10
        Cuit.isValid("201234567861") shouldBe false   // 12
        Cuit.isValid("") shouldBe false
    }

    "rechaza caracteres no numéricos" {
        Cuit.isValid("20-1234567A-6") shouldBe false
        Cuit.isValid("hola mundo!!") shouldBe false
    }

    // ---------------------------------------------------------------
    // Solo dígitos ASCII: regresión, ver docs/decisiones/0004-...
    // Char.isDigit()/Char.digitToInt() de Kotlin aceptan cualquier dígito
    // Unicode de la categoría Nd, no solo 0-9. "20-12345678-6" con cada
    // dígito reescrito en otro sistema numérico tiene que rechazarse igual
    // que si tuviera letras.
    // ---------------------------------------------------------------

    "rechaza dígitos arábigo-índico aunque el valor numérico cierre" {
        // "20-12345678-6" con cada dígito ASCII reemplazado por su
        // equivalente arábigo-índico (٠-٩, U+0660 a U+0669).
        Cuit.isValid("٢٠-١٢٣٤٥٦٧٨-٦") shouldBe false
    }

    "rechaza dígitos devanagari aunque el valor numérico cierre" {
        // "20-12345678-6" con cada dígito ASCII reemplazado por su
        // equivalente devanagari (०-९, U+0966 a U+096F).
        Cuit.isValid("२०-१२३४५६७८-६") shouldBe false
    }

    "parse rechaza dígitos arábigo-índico" {
        Cuit.parseOrNull("٢٠-١٢٣٤٥٦٧٨-٦").shouldBeNull()
        shouldThrow<IllegalArgumentException> {
            Cuit.parse("٢٠-١٢٣٤٥٦٧٨-٦")
        }
    }

    "parse rechaza dígitos devanagari" {
        Cuit.parseOrNull("२०-१२३४५६७८-६").shouldBeNull()
        shouldThrow<IllegalArgumentException> {
            Cuit.parse("२०-१२३४५६७८-६")
        }
    }

    "calcularDigitoVerificador rechaza cuerpos con dígitos arábigo-índico" {
        // "2012345678" con cada dígito ASCII reemplazado por su equivalente
        // arábigo-índico.
        shouldThrow<IllegalArgumentException> {
            Cuit.calcularDigitoVerificador("٢٠١٢٣٤٥٦٧٨")
        }
    }

    "calcularDigitoVerificador rechaza cuerpos con dígitos devanagari" {
        // "2012345678" con cada dígito ASCII reemplazado por su equivalente
        // devanagari.
        shouldThrow<IllegalArgumentException> {
            Cuit.calcularDigitoVerificador("२०१२३४५६७८")
        }
    }

    "parseOrNull devuelve null en vez de tirar" {
        Cuit.parseOrNull("20-12345678-7").shouldBeNull()
    }

    "parse tira IllegalArgumentException" {
        shouldThrow<IllegalArgumentException> {
            Cuit.parse("20-12345678-7")
        }
    }

    // ---------------------------------------------------------------
    // Normalización de entrada
    // ---------------------------------------------------------------

    "acepta el mismo CUIT con distintos separadores" {
        val esperado = Cuit.parse("20123456786")

        Cuit.parse("20-12345678-6") shouldBe esperado
        Cuit.parse("20.12345678.6") shouldBe esperado
        Cuit.parse("20 12345678 6") shouldBe esperado
        Cuit.parse("  20-12345678-6  ") shouldBe esperado
    }

    // ---------------------------------------------------------------
    // Componentes
    // ---------------------------------------------------------------

    "expone los componentes del CUIT" {
        val cuit = Cuit.parse("20-12345678-6")

        cuit.prefijo shouldBe "20"
        cuit.numero shouldBe "12345678"
        cuit.digitoVerificador shouldBe 6
        cuit.valor shouldBe "20123456786"
    }

    "formatea con guiones" {
        Cuit.parse("20123456786").formateado() shouldBe "20-12345678-6"
    }

    // ---------------------------------------------------------------
    // Tipo de persona
    // ---------------------------------------------------------------

    "infiere persona física" {
        Cuit.parse("20-12345678-6").tipoPersona shouldBe TipoPersona.FISICA
        Cuit.parse("23-00000001-9").tipoPersona shouldBe TipoPersona.FISICA

        // 24-12345678: suma = 164, resto = 10, DV = 11 - 10 = 1
        Cuit.parse("24-12345678-1").tipoPersona shouldBe TipoPersona.FISICA

        // 27-12345678: suma = 176, resto = 0, DV = 0
        Cuit.parse("27-12345678-0").tipoPersona shouldBe TipoPersona.FISICA
    }

    "infiere persona jurídica" {
        Cuit.parse("30-00000001-5").tipoPersona shouldBe TipoPersona.JURIDICA

        // 33-00000001: suma = 29, resto = 7, DV = 11 - 7 = 4
        Cuit.parse("33-00000001-4").tipoPersona shouldBe TipoPersona.JURIDICA

        // 34-00000001: suma = 33, resto = 0, DV = 0
        Cuit.parse("34-00000001-0").tipoPersona shouldBe TipoPersona.JURIDICA
    }

    "un prefijo desconocido no invalida, solo queda sin clasificar" {
        val cuit = Cuit.parse("99-00000001-5")

        cuit.shouldNotBeNull()
        cuit.tipoPersona shouldBe TipoPersona.DESCONOCIDO
    }

    // ---------------------------------------------------------------
    // Igualdad
    // ---------------------------------------------------------------

    "dos CUIT con el mismo valor son iguales" {
        val a = Cuit.parse("20-12345678-6")
        val b = Cuit.parse("20123456786")

        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
    }

    "CUIT distintos no son iguales" {
        Cuit.parse("20-12345678-6") shouldNotBe Cuit.parse("30-00000001-5")
    }

    "un Cuit no es igual a null" {
        val cuit: Any = Cuit.parse("20-12345678-6")
        cuit shouldNotBe null
    }

    "un Cuit no es igual a un valor de otro tipo" {
        val cuit: Any = Cuit.parse("20-12345678-6")
        cuit shouldNotBe "20123456786"
    }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    "toString devuelve los 11 dígitos sin separadores" {
        Cuit.parse("20-12345678-6").toString() shouldBe "20123456786"
    }

    // ---------------------------------------------------------------
    // Precondiciones internas
    // ---------------------------------------------------------------

    "calcularDigitoVerificador exige exactamente 10 dígitos" {
        shouldThrow<IllegalArgumentException> {
            Cuit.calcularDigitoVerificador("123")
        }
        shouldThrow<IllegalArgumentException> {
            Cuit.calcularDigitoVerificador("123456789012")
        }
    }

    "calcularDigitoVerificador exige que sean dígitos" {
        shouldThrow<IllegalArgumentException> {
            Cuit.calcularDigitoVerificador("20123456AB")
        }
    }
})
