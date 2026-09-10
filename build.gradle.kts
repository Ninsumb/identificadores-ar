import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    // Plugin de Kotlin para la JVM. Chequeá la última versión estable antes de
    // fijarla: https://kotlinlang.org/docs/releases.html
    kotlin("jvm") version "2.1.0"

    // Habilita las tareas de empaquetado y publicación de artefactos.
    // De acá salen `publishToMavenLocal` y `publish`.
    `maven-publish`
}

// El group más el nombre del proyecto (en settings.gradle.kts) y la versión de
// abajo forman las coordenadas del artefacto: group:name:version.
//
// El group queda en `io.github.ninsumb` porque el destino final es Maven Central
// (Fase 7) y esa es la coordenada definitiva. En JitPack (Fase 6) la coordenada
// la impone la cuenta de GitHub y es `com.github.Ninsumb:identificadores-ar`,
// sin importar lo que diga acá.
group = "io.github.ninsumb"

// La versión la fija quien publica, con -Pversion. JitPack corre
// `gradle -Pversion=<tag> … publishToMavenLocal`, así que acá entra el nombre
// del tag de Git. Sin -Pversion —build local, o `publishToMavenLocal` para
// consumir desde `mavenLocal()`— cae en un SNAPSHOT.
version = run {
    val declarada = (findProperty("version") as? String)
        ?.takeUnless { it == "unspecified" || it.isBlank() }
        ?: return@run "0.1.0-SNAPSHOT"

    // Los tags de versión de este repo llevan prefijo `v` (`v0.1.0`): es la
    // convención que asumen GitHub Releases y los generadores de changelog. La
    // coordenada Maven no lo lleva, así que se recorta el `v` inicial —solo
    // cuando lo sigue un dígito, para no tocar `main-SNAPSHOT` ni hashes de
    // commit, que JitPack también acepta como versión.
    if (declarada.length > 1 && declarada[0] == 'v' && declarada[1].isDigit()) {
        declarada.substring(1)
    } else {
        declarada
    }
}

repositories {
    // De dónde se bajan las dependencias.
    mavenCentral()
}

dependencies {
    // Ni una sola dependencia de runtime. Es deliberado, está en ALCANCE.md.
    // Todo lo de abajo es solo para tests.

    testImplementation(kotlin("test"))

    // Kotest: runner + assertions + property-based testing.
    // Chequeá la última versión: https://github.com/kotest/kotest/releases
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
}

kotlin {
    // Gradle descarga y usa el JDK 17 sin importar cuál tengas instalado.
    // Esto hace el build reproducible en cualquier máquina, incluida la de CI.
    jvmToolchain(17)

    // Modo de API explícita: el compilador exige visibilidad y tipo de retorno
    // declarados en todo lo público. Molesto al principio, indispensable en una
    // librería.
    explicitApi()

    compilerOptions {
        // Un warning que nadie mira es un error que se descubre tarde. Que
        // rompa el build es la única forma de que no se acumulen.
        allWarningsAsErrors.set(true)
    }
}

java {
    // Genera el JAR con el código fuente, además del JAR compilado.
    // Es lo que permite que un IDE ajeno muestre tu código al hacer ctrl+click.
    // Maven Central lo exige; conviene tenerlo desde el día uno.
    withSourcesJar()
}

tasks.test {
    // Kotest corre sobre la plataforma JUnit 5. Sin esta línea, los tests no
    // se ejecutan y Gradle no te avisa: simplemente reporta cero tests.
    useJUnitPlatform()

    // Que un test fallado imprima el assert en la consola, no solo
    // "> Task :test FAILED". En el CI eso evita tener que bajar el artefacto
    // del reporte para saber qué se rompió. Solo el evento "failed": los
    // tests que pasan no ensucian el log. FULL incluye el stack trace y las
    // causas encadenadas de la excepción.
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // Empaqueta el componente Java estándar: el JAR compilado, el POM
            // con las dependencias declaradas, y el sources JAR.
            //
            // Esto es todo lo que JitPack necesita (Fase 6): corre
            // `publishToMavenLocal` en su servidor y sirve lo que quede en el
            // repo local. La metadata completa del POM (licencia,
            // desarrolladores, SCM), el javadoc JAR, la firma GPG y el
            // repositorio destino son requisitos de Maven Central y van en la
            // Fase 7; ver ALCANCE.md y CLAUDE.md.
            from(components["java"])
        }
    }
}
