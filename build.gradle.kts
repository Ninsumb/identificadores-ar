import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    // Plugin de Kotlin para la JVM. Chequeá la última versión estable antes de
    // fijarla: https://kotlinlang.org/docs/releases.html
    kotlin("jvm") version "2.1.0"

    // Habilita las tareas de empaquetado y publicación de artefactos.
    // De acá salen `publishToMavenLocal` y `publish`.
    `maven-publish`

    // Firma GPG de los artefactos. Plugin del core de Gradle -no es una
    // dependencia de terceros-: detecta solo las propiedades convencionales
    // `signing.keyId` / `signing.password` / `signing.secretKeyRingFile` si
    // están definidas (en `~/.gradle/gradle.properties`, nunca en el repo).
    signing

    // Dokka genera el javadoc JAR que exige Maven Central (Fase 7, issue #11)
    // a partir del KDoc que ya tiene todo miembro público. Chequeá la última
    // versión estable antes de fijarla: https://github.com/Kotlin/dokka/releases
    id("org.jetbrains.dokka") version "2.2.0"

    // Sube el bundle firmado al Central Portal usando su API nueva (el
    // Publisher API), sin tocar el `publishing {}` de abajo -JitPack lo sigue
    // necesitando tal cual-. Es la única dependencia nueva de las cuatro
    // opciones evaluadas para la Fase 7: no reemplaza `maven-publish` (a
    // diferencia de vanniktech/gradle-maven-publish-plugin) y no es una
    // capa de compatibilidad con OSSRH pensada para migrar, no para
    // arrancar de cero (a diferencia del endpoint ossrh-staging-api). Ver
    // https://github.com/GradleUp/nmcp -- chequeá la última versión antes
    // de fijarla.
    id("com.gradleup.nmcp.aggregation") version "1.6.2"
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

// El javadoc JAR que exige Maven Central (issue #11). Empaqueta el HTML de
// Dokka (`dokkaGeneratePublicationHtml`), no el formato "Javadoc" nativo de
// Dokka (plugin separado `org.jetbrains.dokka-javadoc`, tarea
// `dokkaGeneratePublicationJavadoc`): ese formato sigue en alfa en la 2.2.0,
// con bugs de conversión documentados por el propio proyecto Dokka. Empaquetar
// el HTML estable como si fuera el javadoc JAR es el patrón que ya usan
// kotlinx, Kotest y Ktor -a Maven Central no le importa el contenido interno
// del jar, solo el classifier `javadoc`- y javadoc.io sirve igual cualquier
// HTML que encuentre ahí adentro, sea Javadoc real o no. Si en una versión
// futura de Dokka el formato Javadoc deja el alfa, vale la pena reevaluar.
val javadocJar by tasks.registering(Jar::class) {
    val dokkaHtml = dokka.dokkaPublications.named("html")
    dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
    from(dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
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
            // repo local. Lo de abajo -POM completo y javadoc JAR- lo agrega
            // esta publicación sin romper eso: JitPack simplemente ignora lo
            // que no usa.
            from(components["java"])
            artifact(javadocJar)

            // POM completo: lo exige Maven Central (Fase 7), no JitPack.
            pom {
                name.set("identificadores-ar")
                description.set(
                    "Librería Kotlin/JVM para validar, parsear y formatear " +
                        "identificadores argentinos: CUIT, CUIL, CBU, CVU, DNI y alias bancario."
                )
                url.set("https://github.com/Ninsumb/identificadores-ar")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("Ninsumb")
                        name.set("Lucia Arrieta")
                        // El no-reply de GitHub que ya figura como autor del primer commit
                        // del repo, no un email personal: este campo queda público y
                        // permanente en Maven Central.
                        email.set("131300090+Ninsumb@users.noreply.github.com")
                    }
                }

                scm {
                    url.set("https://github.com/Ninsumb/identificadores-ar")
                    connection.set("scm:git:https://github.com/Ninsumb/identificadores-ar.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Ninsumb/identificadores-ar.git")
                }
            }
        }
    }
}

signing {
    // Gradle busca `signing.keyId` / `signing.password` /
    // `signing.secretKeyRingFile` como propiedades de proyecto -las tres ya
    // están en `~/.gradle/gradle.properties`, fuera del repo- sin que este
    // archivo las tenga que nombrar. Firma los tres jars (main, sources,
    // javadoc) y el POM de la publicación; no se ejecuta salvo que se pida
    // explícitamente una tarea que la necesite (`publishToMavenLocal`,
    // `publish`), así que no afecta a `./gradlew build` ni al CI.
    sign(publishing.publications["maven"])
}

nmcpAggregation {
    // Un solo módulo, pero el plugin `nmcp` simple (no `.aggregation`) marca
    // `publishAllPublicationsToCentralPortal` como deprecado a favor de este
    // -son funcionalmente lo mismo, la aggregation es la forma mantenida
    // incluso para un único proyecto-. Esto agrega las publicaciones de este
    // proyecto (el único que hay) al bundle.
    publishAllProjectsProbablyBreakingProjectIsolation()

    centralPortal {
        // `username` / `password` son el user token generado en
        // central.sonatype.com -no la cuenta-. Son `Property<String>`, así
        // que asignarles el `Provider` de la propiedad de Gradle es
        // perezoso: si `centralUsername`/`centralPassword` no están
        // definidas (CI, o cualquier máquina que no vaya a publicar), la
        // propiedad queda ausente y no rompe nada salvo que se ejecute de
        // verdad una tarea de publicación que las necesite.
        username.set(providers.gradleProperty("centralUsername"))
        password.set(providers.gradleProperty("centralPassword"))

        // No negociable: deja el deployment en estado pendiente en el
        // Central Portal. Nadie lo hace público hasta que un humano revise
        // el bundle (POM, firmas, checksums) en la UI y apriete "Publish" --
        // Central no permite deshacer eso una vez hecho.
        publishingType.set("USER_MANAGED")
    }
}
