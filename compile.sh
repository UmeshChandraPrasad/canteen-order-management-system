#!/usr/bin/env bash
# Compile the Canteen Order Management System (Linux / macOS).
# Requires: Java 17+ (javac on PATH) and MySQL Connector/J in lib/.
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -cp "lib/mysql-connector-j.jar" \
      -d out $(find src -name "*.java")
echo "Compiled OK -> out/"
