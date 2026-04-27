#!/bin/sh

##############################################################################
# Gradle wrapper script
##############################################################################

DEFAULT_GRADLE_VERSION="8.4"
GRADLE_VERSION="${GRADLE_VERSION:-$DEFAULT_GRADLE_VERSION}"

APP_BASE_NAME=$(basename "$0" .sh)
APP_HOME=$(dirname "$0")

exec "$APP_HOME/gradle/bin/gradle" "$@"