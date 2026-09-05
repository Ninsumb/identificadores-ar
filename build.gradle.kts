plugins {
    // Plugin de Kotlin para la JVM. Chequeá la última versión estable antes de
    // fijarla: https://kotlinlang.org/docs/releases.html
    kotlin("jvm") version "2.1.0"

    // Habilita las tareas de empaquetado y publicación de artefactos.
    // De acá salen `publishToMavenLocal` y `publish`.
    `maven-publish`
}

// Estas dos líneas más el nombre del proyecto (en settings.gradle.kts) forman
// las coordenadas del artefacto: group:name:version
group = "io.github.ninsumb"
version = "0.1.0-SNAPSHOT"

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
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // Empaqueta el componente Java estándar: el JAR compilado, el POM
            // con las dependencias declaradas, y el sources JAR.
            from(components["java"])
        }
    }
}
