package io.github.ninsumb.identificadores

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.util.Locale

/**
 * Property-based testing para alias bancario.
 *
 * Sin dígito verificador no hay una propiedad aritmética que probar como en
 * Cuit o ClaveBancaria. Lo que hay que probar es lo que sí varía acá: que la
 * canonización a minúsculas no cambia la identidad, que es estable frente al
 * locale de la JVM, y que ningún string arbitrario se cuela con caracteres o
 * longitudes fuera de lo admitido.
 */
class AliasBancarioPropertyTest : StringSpec({

    /** Cualquier carácter admitido en un alias: `[A-Za-z0-9.-]`. */
    val caracterDeAlias: Arb<Char> =
        Arb.of(('0'..'9').toList() + ('a'..'z') + ('A'..'Z') + listOf('.', '-'))

    /**
     * Cualquier alias de forma válida: entre 6 y 20 caracteres admitidos.
     * Ninguno lleva espacios, así que `trim()` no altera su longitud y
     * `original` coincide con el texto generado.
     */
    val aliasValido: Arb<String> =
        Arb.list(caracterDeAlias, 6..20).map { it.joinToString("") }

    /**
     * Locales con reglas de bajado a minúsculas propias. `tr` y `az` mapean la
     * `I` ASCII a `ı` (U+0131); si el código usara `lowercase()` a secas en vez
     * de `lowercase(Locale.ROOT)`, la forma canónica cambiaría bajo estos.
     */
    val localesConCasingPropio = listOf(
        Locale.ROOT,
        Locale.ENGLISH,
        Locale("tr", "TR"),
        Locale("az", "AZ"),
        Locale("lt", "LT"),
    )

    // ---------------------------------------------------------------

    "todo alias de 6 a 20 caracteres admitidos es válido" {
        checkAll(aliasValido) { alias ->
            AliasBancario.isValid(alias) shouldBe true
        }
    }

    "valor siempre queda en minúsculas y dentro del charset" {
        checkAll(aliasValido) { alias ->
            val valor = AliasBancario.parse(alias).valor
            valor shouldBe valor.lowercase(Locale.ROOT)
            valor.all { it in '0'..'9' || it in 'a'..'z' || it == '.' || it == '-' } shouldBe true
        }
    }

    "valor siempre tiene entre 6 y 20 caracteres" {
        checkAll(aliasValido) { alias ->
            (AliasBancario.parse(alias).valor.length in 6..20) shouldBe true
        }
    }

    /**
     * La propiedad que justifica canonizar a minúsculas en el parse: cambiar
     * la capitalización no puede cambiar de qué alias se trata. Ver
     * `docs/decisiones/0007-alias-bancario-validacion-de-forma.md`.
     */
    "cambiar la capitalización no cambia la identidad" {
        checkAll(aliasValido) { alias ->
            val base = AliasBancario.parse(alias)
            AliasBancario.parse(alias.uppercase(Locale.ROOT)) shouldBe base
            AliasBancario.parse(alias.lowercase(Locale.ROOT)) shouldBe base
        }
    }

    /**
     * La forma canónica no puede depender del locale por defecto de la JVM
     * (bug clásico con `lowercase()` sin argumento en una JVM turca). El
     * `finally` restaura el locale para no contaminar el resto de la suite.
     */
    "la forma canónica no depende del Locale por defecto de la JVM" {
        val previo = Locale.getDefault()
        try {
            checkAll(aliasValido) { alias ->
                val esperado = alias.trim().lowercase(Locale.ROOT)
                for (locale in localesConCasingPropio) {
                    Locale.setDefault(locale)
                    AliasBancario.parse(alias).valor shouldBe esperado
                }
            }
        } finally {
            Locale.setDefault(previo)
        }
    }

    "original preserva el texto ingresado sin canonizar" {
        checkAll(aliasValido) { alias ->
            AliasBancario.parse(alias).original shouldBe alias
        }
    }

    "parsear el valor canónico de un alias da el mismo alias" {
        checkAll(aliasValido) { alias ->
            val a = AliasBancario.parse(alias)
            AliasBancario.parse(a.valor) shouldBe a
        }
    }

    "parsear el resultado de formateado() da el mismo alias" {
        checkAll(aliasValido) { alias ->
            val a = AliasBancario.parse(alias)
            AliasBancario.parse(a.formateado()) shouldBe a
        }
    }

    "formateado() siempre coincide con valor" {
        checkAll(aliasValido) { alias ->
            val a = AliasBancario.parse(alias)
            a.formateado() shouldBe a.valor
        }
    }

    "recortar espacios de los extremos no cambia el alias" {
        checkAll(aliasValido) { alias ->
            AliasBancario.parse("  $alias  ") shouldBe AliasBancario.parse(alias)
        }
    }

    "isValid y parseOrNull siempre coinciden" {
        checkAll(Arb.string(0..30)) { candidato ->
            AliasBancario.isValid(candidato) shouldBe (AliasBancario.parseOrNull(candidato) != null)
        }
    }

    "ningún string arbitrario produce un alias fuera de forma" {
        checkAll(Arb.string(0..30)) { s ->
            val alias = AliasBancario.parseOrNull(s)
            if (alias != null) {
                (alias.valor.length in 6..20) shouldBe true
                alias.valor.all {
                    it in '0'..'9' || it in 'a'..'z' || it == '.' || it == '-'
                } shouldBe true
            }
        }
    }
})
