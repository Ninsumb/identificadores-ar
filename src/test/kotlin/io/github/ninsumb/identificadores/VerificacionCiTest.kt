package io.github.ninsumb.identificadores

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Test de descarte. Existe solo para comprobar que el CI se pone en rojo
 * cuando un test falla. Se elimina junto con la rama `ci/verificar-rojo`;
 * no debe llegar nunca a `main`.
 */
class VerificacionCiTest : StringSpec({
    "FALLA A PROPOSITO: el CI tiene que detectar este test roto" {
        AliasBancario.parse("mi.alias").valor shouldBe "valor-deliberadamente-incorrecto"
    }
})
