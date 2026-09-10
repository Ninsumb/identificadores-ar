package io.github.ninsumb.identificadores

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.Locale

/**
 * Casos concretos de validación de alias bancario.
 *
 * Sin dígito verificador y sin poder consultar nada, la validación es solo de
 * forma: longitud 6-20 y caracteres `[A-Za-z0-9.-]`, con canonización a
 * minúsculas. Ver `docs/decisiones/0007-alias-bancario-validacion-de-forma.md`.
 */
class AliasBancarioTest : StringSpec({

    // ---------------------------------------------------------------
    // Longitud: de 6 a 20 caracteres (texto ordenado 3.7.2.1.i)
    // ---------------------------------------------------------------

    "acepta el mínimo de 6 caracteres" {
        AliasBancario.isValid("juan.p") shouldBe true
    }

    "acepta el máximo de 20 caracteres" {
        AliasBancario.isValid("juan.perez.caja.ahor") shouldBe true // 20
    }

    "rechaza menos de 6 caracteres" {
        AliasBancario.isValid("corto") shouldBe false // 5
        AliasBancario.isValid("a.b") shouldBe false
    }

    "rechaza más de 20 caracteres" {
        AliasBancario.isValid("juan.perez.caja.ahorro") shouldBe false // 22
    }

    "rechaza la cadena vacía" {
        AliasBancario.isValid("") shouldBe false
    }

    // ---------------------------------------------------------------
    // Caracteres admitidos: letras, dígitos, punto y guion medio
    // ---------------------------------------------------------------

    "acepta letras, dígitos, punto y guion medio" {
        AliasBancario.isValid("Juan.Perez-2024") shouldBe true
        AliasBancario.isValid("MiAlias99") shouldBe true
        AliasBancario.isValid("a-b-c-d-e-f") shouldBe true
        AliasBancario.isValid("a.b.c.d.e.f") shouldBe true
    }

    "rechaza el guion bajo" {
        AliasBancario.isValid("mi_alias") shouldBe false
    }

    "rechaza espacios interiores" {
        AliasBancario.isValid("mi alias") shouldBe false
    }

    "rechaza acentos y eñe" {
        AliasBancario.isValid("aliás.mío") shouldBe false
        AliasBancario.isValid("peña.online") shouldBe false
    }

    "rechaza otros signos de puntuación" {
        AliasBancario.isValid("mi@alias") shouldBe false
        AliasBancario.isValid("mi/alias") shouldBe false
        AliasBancario.isValid("mi+alias") shouldBe false
        AliasBancario.isValid("mi.alias!") shouldBe false
    }

    "rechaza dígitos no ASCII aunque la longitud sea válida" {
        // "1234567" en arábigo-índico y en devanagari: mismo criterio que
        // Cuit, Dni y ClaveBancaria (ver Digitos.kt).
        AliasBancario.isValid("١٢٣٤٥٦٧") shouldBe false
        AliasBancario.isValid("१२३४५६७") shouldBe false
    }

    // ---------------------------------------------------------------
    // Canonización a minúsculas; original preserva la capitalización
    // ---------------------------------------------------------------

    "valor es la forma canónica en minúsculas" {
        AliasBancario.parse("Juan.Perez.Ahorro").valor shouldBe "juan.perez.ahorro"
        AliasBancario.parse("MI.ALIAS").valor shouldBe "mi.alias"
    }

    "original preserva la capitalización tal como se ingresó" {
        AliasBancario.parse("Juan.Perez.Ahorro").original shouldBe "Juan.Perez.Ahorro"
        AliasBancario.parse("MI.ALIAS").original shouldBe "MI.ALIAS"
    }

    "recorta los espacios de los extremos, en valor y en original" {
        val alias = AliasBancario.parse("  Juan-Perez  ")
        alias.valor shouldBe "juan-perez"
        alias.original shouldBe "Juan-Perez"
    }

    // ---------------------------------------------------------------
    // Locale.ROOT: el bug del locale turco
    // ---------------------------------------------------------------

    "canoniza con Locale.ROOT: en una JVM turca la I mayúscula baja a i, no a ı" {
        val previo = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))
            AliasBancario.parse("MI.ALIAS").valor shouldBe "mi.alias"
        } finally {
            Locale.setDefault(previo)
        }
    }

    // ---------------------------------------------------------------
    // Insensibilidad a mayúsculas: identidad
    // ---------------------------------------------------------------

    "dos alias que difieren solo en mayúsculas son iguales" {
        val a = AliasBancario.parse("Mi.Alias")
        val b = AliasBancario.parse("mi.alias")

        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
        a.toString() shouldBe b.toString()
        a.formateado() shouldBe b.formateado()
    }

    "dos alias que difieren solo en mayúsculas conservan cada uno su original" {
        AliasBancario.parse("Mi.Alias").original shouldBe "Mi.Alias"
        AliasBancario.parse("mi.alias").original shouldBe "mi.alias"
    }

    "alias distintos no son iguales" {
        AliasBancario.parse("juan.perez") shouldNotBe AliasBancario.parse("juana.perez")
    }

    "un alias no es igual a null ni a un valor de otro tipo" {
        val alias: Any = AliasBancario.parse("mi.alias")
        alias shouldNotBe null
        alias shouldNotBe "mi.alias"
    }

    // ---------------------------------------------------------------
    // toString y formateado(): ambos devuelven valor
    // ---------------------------------------------------------------

    "toString devuelve la forma canónica" {
        AliasBancario.parse("Juan.Perez").toString() shouldBe "juan.perez"
    }

    "formateado devuelve lo mismo que valor: un alias no tiene nada que formatear" {
        val alias = AliasBancario.parse("Juan.Perez")
        alias.formateado() shouldBe alias.valor
        alias.formateado() shouldBe "juan.perez"
    }

    // ---------------------------------------------------------------
    // Un alias con forma de CBU/CVU no se rechaza
    // (ADR 0007: la normativa no lo prohíbe)
    // ---------------------------------------------------------------

    "acepta un alias íntegramente numérico de 6 a 20 dígitos" {
        AliasBancario.isValid("123456") shouldBe true
        AliasBancario.isValid("12345678901234567890") shouldBe true // 20
    }

    "un string con forma de CBU o CVU se rechaza solo por longitud" {
        // 22 dígitos: es la forma de una CBU/CVU, pero lo que lo invalida es
        // que excede el máximo de 20, no una regla contra "parece una clave".
        AliasBancario.isValid("0110000700000000000123") shouldBe false
    }

    // ---------------------------------------------------------------
    // Separadores al inicio, al final o consecutivos: la norma es
    // silenciosa, así que se aceptan (ADR 0007, "Lo que no se pudo
    // verificar")
    // ---------------------------------------------------------------

    "acepta separadores al inicio, al final y consecutivos" {
        AliasBancario.isValid(".mi.alias") shouldBe true
        AliasBancario.isValid("mi.alias.") shouldBe true
        AliasBancario.isValid("mi..alias") shouldBe true
        AliasBancario.isValid("-mi-alias-") shouldBe true
        AliasBancario.isValid("mi.-alias") shouldBe true
    }

    // ---------------------------------------------------------------
    // parseOrNull / parse
    // ---------------------------------------------------------------

    "parseOrNull devuelve null en vez de tirar" {
        AliasBancario.parseOrNull("corto").shouldBeNull()
        AliasBancario.parseOrNull("mi_alias").shouldBeNull()
    }

    "parse tira IllegalArgumentException" {
        shouldThrow<IllegalArgumentException> { AliasBancario.parse("corto") }
        shouldThrow<IllegalArgumentException> { AliasBancario.parse("mi alias") }
    }
})
