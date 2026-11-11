param([switch]$BuildOnly, [switch]$Smoke, [string]$Render)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$classes = Join-Path $repo 'target/native/classes'
$javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$jar = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/jar.exe' } else { 'jar' }
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $repo 'src/main/java/frogger/core'),(Join-Path $repo 'src/main/java/frogger/desktop') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& $javac -encoding UTF-8 --release 17 -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
$outputJar = Join-Path $repo 'target/frogger-desktop.jar'
& $jar --create --file $outputJar --main-class frogger.desktop.Main -C $classes . -C $repo LICENSE
if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed' }
if ($BuildOnly) { Write-Output "Built $outputJar"; exit 0 }
if ($Render) { & $java '-Djava.awt.headless=true' -jar $outputJar --render $Render }
elseif ($Smoke) { & $java -jar $outputJar --smoke }
else { & $java -jar $outputJar }
exit $LASTEXITCODE
