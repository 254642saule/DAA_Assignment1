    param([string]$Command = "demo")
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
New-Item -ItemType Directory -Force target/classes, target/test-classes | Out-Null
$sources = (Get-ChildItem src/daa/*.java).FullName
& java com.sun.tools.javac.Main --release 17 -encoding UTF-8 -d target/classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$tests = (Get-ChildItem tests/daa/*.java).FullName
& java com.sun.tools.javac.Main --release 17 -encoding UTF-8 -cp target/classes -d target/test-classes $tests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($Command -eq "test") {
    & java -Xms128m -Xmx512m -cp 'target/classes;target/test-classes' daa.AlgorithmTests
} else {
    & java -Xms256m -Xmx1024m -cp target/classes daa.Main $Command
}
exit $LASTEXITCODE
