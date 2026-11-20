# Genera el ejecutable de Windows (carpeta dist\ChiquiChess con ChiquiChess.exe y su propio Java adentro).
# Requisitos: JDK 21 (con jpackage) y Maven en el PATH, o pasar la ruta de Maven con -Maven.
# Uso:  .\empaquetar.ps1
param([string]$Maven = "mvn")

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

& $Maven -q -DskipTests clean package
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion" }

$jar = "TPOAjedrez-1.0-SNAPSHOT-completo.jar"
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory dist\entrada | Out-Null
Copy-Item "target\$jar" dist\entrada\

jpackage --type app-image --name ChiquiChess --dest dist `
    --input dist\entrada --main-jar $jar --main-class adapters.ui.GraphicalMain `
    --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,java.naming,jdk.unsupported `
    --app-version 1.0
if ($LASTEXITCODE -ne 0) { throw "Fallo jpackage" }

Remove-Item -Recurse -Force dist\entrada
Write-Host "Listo: dist\ChiquiChess\ChiquiChess.exe"
