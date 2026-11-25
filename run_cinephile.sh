#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f "db.properties" ] && [ -f "db.properties.example" ]; then
  cp "db.properties.example" "db.properties"
  echo "[SETUP] Created db.properties from template. Edit db.properties with your MySQL password."
fi

if ! command -v javac >/dev/null 2>&1; then
  echo "[ERROR] Java JDK (javac) not found. Install JDK 8+ and try again."
  exit 1
fi

echo "Compiling Cinephile Java sources..."
rm -rf out
mkdir -p out
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/mysql-connector-j-8.3.0.jar" -d out @sources.txt
rm -f sources.txt

echo "Launching Cinephile Web Server on http://localhost:8080 ..."
java -cp "out:lib/mysql-connector-j-8.3.0.jar" com.cinephile.web.CinephileWebServer
