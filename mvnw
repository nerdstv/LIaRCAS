#!/bin/sh
# Maven Wrapper script for Unix/macOS
# Downloads Maven if needed and runs it using the project's wrapper config

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -f "$MAVEN_WRAPPER_PROPERTIES" ]; then
    MAVEN_DOWNLOAD_URL=$(grep 'distributionUrl' "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2- | tr -d '\r')
fi

MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.9"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven..."
    mkdir -p "$MAVEN_HOME"
    curl -fsSL "$MAVEN_DOWNLOAD_URL" -o "$MAVEN_HOME/maven.zip"
    unzip -qo "$MAVEN_HOME/maven.zip" -d "$MAVEN_HOME"
    rm "$MAVEN_HOME/maven.zip"
fi

MAVEN_EXEC=$(find "$MAVEN_HOME" -name "mvn" -path "*/bin/mvn" | head -1)
exec "$MAVEN_EXEC" "$@"
