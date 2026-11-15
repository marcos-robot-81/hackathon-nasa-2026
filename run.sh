#!/usr/bin/env bash
set -e

mkdir -p build
javac -encoding UTF-8 -d build $(find src -name "*.java")
exec java -Xms8m -Xmx24m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -cp build main "$@"
