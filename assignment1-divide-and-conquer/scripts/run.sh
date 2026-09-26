#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p target/classes target/test-classes
java com.sun.tools.javac.Main --release 17 -encoding UTF-8 -d target/classes src/daa/*.java
java com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp target/classes -d target/test-classes tests/daa/*.java
if [[ "${1:-demo}" == "test" ]]; then
    java -Xms128m -Xmx512m -cp target/classes:target/test-classes daa.AlgorithmTests
else
    java -Xms256m -Xmx1024m -cp target/classes daa.Main "$@"
fi
