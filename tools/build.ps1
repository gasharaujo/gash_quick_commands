param([switch]$Offline, [switch]$Install)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
function Find-Jdk([int]$Major) {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    foreach ($root in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft", "$env:USERPROFILE\.jdks", "$env:USERPROFILE\.gradle\jdks")) {
        if (Test-Path -LiteralPath $root) { $candidates += Get-ChildItem -Directory -LiteralPath $root | Select-Object -ExpandProperty FullName }
    }
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        $release = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe')) -and (Test-Path -LiteralPath $release)) {
            if ((Get-Content -Raw -LiteralPath $release) -match ('JAVA_VERSION="' + $Major + '(\.|"|-)')) { return $candidate }
        }
    }
    throw "JDK $Major not found. Install it or configure JAVA_HOME."
}
$matrix = @(
    @{ Module = 'forge-1.20.1'; Java = 17; Instance = '20.1' },
    @{ Module = 'neoforge-1.21.1'; Java = 21; Instance = '21' },
    @{ Module = 'neoforge-26.1.2'; Java = 25; Instance = '26.1.2' }
)
$originalJava = $env:JAVA_HOME
try {
    $artifacts = Join-Path $projectRoot 'artifacts'
    New-Item -ItemType Directory -Path $artifacts -Force | Out-Null
    foreach ($target in $matrix) {
        $env:JAVA_HOME = Find-Jdk $target.Java
        $module = Join-Path $projectRoot $target.Module
        Push-Location $module
        try {
            $arguments = @('build', '--console=plain')
            if ($Offline) { $arguments += '--offline' }
            & '.\gradlew.bat' @arguments
            if ($LASTEXITCODE -ne 0) { throw "Build failed: $($target.Module)" }
        } finally { Pop-Location }
        $jar = Get-ChildItem -LiteralPath (Join-Path $module 'build/libs') -File -Filter '*.jar' |
            Where-Object { $_.Name -notmatch '-sources\.jar$' } | Select-Object -First 1
        Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $artifacts $jar.Name) -Force
        if ($Install) {
            $mods = Join-Path $env:USERPROFILE "curseforge/minecraft/Instances/$($target.Instance)/mods"
            if (-not (Test-Path -LiteralPath $mods)) { throw "Test instance not found: $mods" }
            foreach ($old in (Get-ChildItem -LiteralPath $mods -Filter 'gash_quick_commands-*.jar' -File)) {
                if ($old.Name -ne $jar.Name) {
                    Move-Item -LiteralPath $old.FullName -Destination ($old.FullName + '.disabled-' + [DateTimeOffset]::Now.ToUnixTimeMilliseconds())
                }
            }
            Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $mods $jar.Name) -Force
        }
    }
    $hashes = Get-ChildItem -LiteralPath $artifacts -Filter '*.jar' -File | Get-FileHash -Algorithm SHA256 |
        ForEach-Object { "$($_.Hash.ToLowerInvariant())  $([IO.Path]::GetFileName($_.Path))" }
    [IO.File]::WriteAllLines((Join-Path $artifacts 'SHA256SUMS.txt'), [string[]]$hashes)
} finally { $env:JAVA_HOME = $originalJava }
