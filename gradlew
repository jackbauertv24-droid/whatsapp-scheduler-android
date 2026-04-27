#!/bin/sh

# Use the bundled Gradle
exec "$(dirname "$0")/gradle-8.4/bin/gradle" "$@"