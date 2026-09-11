#!/usr/bin/env bash
#
# Gate local antes de subir a Maven Central: verifica que lo que quedó en el
# repositorio Maven local (después de `./gradlew publishToMavenLocal`) esté
# completo y correctamente firmado. No sube nada, no toca la red: solo lee
# `~/.m2/repository`. Es la contraparte, para Central, del smoke test externo
# que ya se usa para JitPack.
#
# Uso:
#   ./gradlew publishToMavenLocal -Pversion=<version>
#   ./scripts/verificar-artefactos-central.sh <version>
#
# Ejemplo:
#   ./scripts/verificar-artefactos-central.sh 1.0.0
#
# Maven Central exige firma GPG en cada artefacto de la publicación, no solo
# en los jars: acá se cuentan cuatro -el jar principal, el de fuentes, el de
# javadoc, y el POM-, ocho archivos en total con sus .asc. (Si contás nada más
# los tres jars, son seis; el POM firmado es el cuarto par.)

set -euo pipefail

version="${1:?Uso: $0 <version>}"

group_path="io/github/ninsumb"
artifact="identificadores-ar"
repo="${HOME}/.m2/repository/${group_path}/${artifact}/${version}"

if [ ! -d "$repo" ]; then
    echo "No existe $repo" >&2
    echo "Corré primero: ./gradlew publishToMavenLocal -Pversion=${version}" >&2
    exit 1
fi

base="${repo}/${artifact}-${version}"
payloads=(
    "${base}.jar"
    "${base}-sources.jar"
    "${base}-javadoc.jar"
    "${base}.pom"
)

fallo=0

echo "Verificando artefactos de ${artifact}:${version} en $repo"
echo

for payload in "${payloads[@]}"; do
    nombre="$(basename "$payload")"
    firma="${payload}.asc"

    if [ ! -f "$payload" ]; then
        echo "FALTA ARTEFACTO  $nombre"
        fallo=1
        continue
    fi

    if [ ! -f "$firma" ]; then
        echo "FALTA FIRMA      $nombre"
        fallo=1
        continue
    fi

    salida_gpg="$(mktemp)"
    if gpg --verify "$firma" "$payload" >"$salida_gpg" 2>&1; then
        # gpg --verify escribe el resultado en stderr (capturado arriba); la
        # línea "Good signature from..." es la que importa para eyeballear
        # que la clave usada es la esperada, no cualquier clave válida.
        firmante="$(grep -m1 'Good signature from' "$salida_gpg" || echo "(sin detalle)")"
        echo "OK               $nombre -- $firmante"
    else
        echo "FIRMA INVALIDA   $nombre"
        sed 's/^/    /' "$salida_gpg" >&2
        fallo=1
    fi
    rm -f "$salida_gpg"
done

echo

if [ "$fallo" -ne 0 ]; then
    echo "Verificación FALLIDA: no subas este bundle a Central." >&2
    exit 1
fi

echo "Verificación OK: ${#payloads[@]} artefactos (jar, sources, javadoc, pom), firmas válidas."
echo "Revisá igual el listado de archivos en la UI del Central Portal antes de apretar Publish (publishingType = USER_MANAGED)."
