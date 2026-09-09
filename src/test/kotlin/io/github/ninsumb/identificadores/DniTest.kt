package io.github.ninsumb.identificadores

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Casos concretos de validación de DNI.
 *
 * Sin dígito verificador (ver `docs/decisiones/0006-dni-validacion-estructural.md`),
 * así que acá no hay ningún cálculo que verificar a mano: la validación es
 * de longitud y composición, no aritmética.
 */
class DniTest : StringSpec({

    // ---------------------------------------------------------------
    // Casos válidos y forma canónica
    // ---------------------------------------------------------------

    "acepta un DNI de 8 dígitos" {
        Dni.isValid("70000001") shouldBe true
    }

    "acepta un DNI de menos de 8 dígitos, como un documento viejo" {
        Dni.isValid("1234567") shouldBe true
    }

    "acepta un DNI de un solo dígito" {
        Dni.isValid("1") shouldBe true
    }

    "completa con ceros a la izquierda hasta 8 dígitos" {
        Dni.parse("1").valor shouldBe "00000001"
        Dni.parse("1234567").valor shouldBe "01234567"
        Dni.parse("70000001").valor shouldBe "70000001"
    }

    "un DNI escrito con y sin ceros a la izquierda es el mismo" {
        Dni.parse("1234567") shouldBe Dni.parse("01234567")
    }

    // ---------------------------------------------------------------
    // Rechazo del valor cero
    // ---------------------------------------------------------------

    "rechaza el valor cero, con cualquier cantidad de ceros" {
        Dni.isValid("0") shouldBe false
        Dni.isValid("00") shouldBe false
        Dni.isValid("0000000") shouldBe false
        Dni.isValid("00000000") shouldBe false
    }

    // ---------------------------------------------------------------
    // Longitudes y caracteres inválidos
    // ---------------------------------------------------------------

    "rechaza más de 8 dígitos" {
        Dni.isValid("123456789") shouldBe false
    }

    "rechaza la cadena vacía" {
        Dni.isValid("") shouldBe false
    }

    "rechaza caracteres no numéricos" {
        Dni.isValid("1234567A") shouldBe false
        Dni.isValid("hola") shouldBe false
    }

    // ---------------------------------------------------------------
    // Solo dígitos ASCII: mismo criterio que en Cuit y ClaveBancaria
    // ---------------------------------------------------------------

    "rechaza dígitos arábigo-índico aunque tengan la longitud correcta" {
        // "1234567" con cada dígito reescrito en arábigo-índico
        Dni.isValid("١٢٣٤٥٦٧") shouldBe false
    }

    "rechaza dígitos devanagari aunque tengan la longitud correcta" {
        // "1234567" con cada dígito reescrito en devanagari
        Dni.isValid("१२३४५६७") shouldBe false
    }

    // ---------------------------------------------------------------
    // Normalización de entrada: separadores
    // ---------------------------------------------------------------

    "acepta separadores: puntos, guiones y espacios" {
        val esperado = Dni.parse("12345678")

        Dni.parse("12.345.678") shouldBe esperado
        Dni.parse("12-345-678") shouldBe esperado
        Dni.parse("12 345 678") shouldBe esperado
        Dni.parse("  12345678  ") shouldBe esperado
    }

    // ---------------------------------------------------------------
    // Relación con Cuit: el mismo DNI da el mismo numero
    //
    // Cuit.numero ya devuelve 8 dígitos con ceros, y su KDoc dice que en
    // personas físicas coincide con el DNI. La forma canónica de Dni tiene
    // que ser la misma para que esa igualdad sea real y no solo un
    // comentario. Ver docs/decisiones/0006-dni-validacion-estructural.md.
    // ---------------------------------------------------------------

    "el valor de un Dni coincide con el numero de un Cuit de la misma persona (documento viejo)" {
        // 20-00000006-0 es un caso de test de CuitTest (resto 0, DV 0):
        // persona física con DNI 6, escrito antiguamente sin ceros.
        val cuit = Cuit.parse("20-00000006-0")
        val dni = Dni.parse("6")

        cuit.numero shouldBe dni.valor
    }

    "el valor de un Dni coincide con el numero de un Cuit de la misma persona (documento con 8 dígitos)" {
        // 20-12345678-6 es el caso de test principal de CuitTest.
        val cuit = Cuit.parse("20-12345678-6")
        val dni = Dni.parse("12345678")

        cuit.numero shouldBe dni.valor
    }

    // ---------------------------------------------------------------
    // formateado(): puntos de miles, sin los ceros de relleno
    // ---------------------------------------------------------------

    "formatea un DNI de 8 dígitos con dos puntos" {
        Dni.parse("70000001").formateado() shouldBe "70.000.001"
    }

    "formatea un DNI corto sin mostrar el cero de relleno" {
        // "01234567" es el valor canónico, pero formateado() opera sobre los
        // dígitos significativos: "1.234.567", no "01.234.567".
        Dni.parse("1234567").formateado() shouldBe "1.234.567"
    }

    "formatea un DNI de un solo dígito sin puntos" {
        Dni.parse("1").formateado() shouldBe "1"
    }

    "formatea DNI de distintas longitudes con la cantidad de puntos que corresponde" {
        Dni.parse("12").formateado() shouldBe "12"
        Dni.parse("123").formateado() shouldBe "123"
        Dni.parse("1234").formateado() shouldBe "1.234"
        Dni.parse("12345").formateado() shouldBe "12.345"
        Dni.parse("123456").formateado() shouldBe "123.456"
    }

    // ---------------------------------------------------------------
    // toString
    // ---------------------------------------------------------------

    "toString devuelve los 8 dígitos con ceros a la izquierda" {
        Dni.parse("1").toString() shouldBe "00000001"
    }

    // ---------------------------------------------------------------
    // Igualdad
    // ---------------------------------------------------------------

    "dos Dni con el mismo valor son iguales" {
        val a = Dni.parse("1234567")
        val b = Dni.parse("01234567")

        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
    }

    "Dni distintos no son iguales" {
        Dni.parse("1234567") shouldNotBe Dni.parse("7654321")
    }

    "un Dni no es igual a null ni a un valor de otro tipo" {
        val dni: Any = Dni.parse("1234567")
        dni shouldNotBe null
        dni shouldNotBe "01234567"
    }

    // ---------------------------------------------------------------
    // parseOrNull / parse
    // ---------------------------------------------------------------

    "parseOrNull devuelve null en vez de tirar" {
        Dni.parseOrNull("0").shouldBeNull()
        Dni.parseOrNull("123456789").shouldBeNull()
    }

    "parse tira IllegalArgumentException" {
        shouldThrow<IllegalArgumentException> { Dni.parse("0") }
        shouldThrow<IllegalArgumentException> { Dni.parse("123456789") }
    }
})
