# Aethelon - install the two mods we do NOT embed (license-restricted).
#
# These projects do not allow redistribution of their mods, so Aethelon does
# not bundle them. This script downloads them from the official Modrinth CDN
# and verifies their SHA-512 hashes before copying them into your mods folder.
#
# Usage:
#   .\scripts\install-mods.ps1                  # installs to %APPDATA%\.minecraft\mods
#   .\scripts\install-mods.ps1 -ModsDir "C:\path\to\mods"
#   .\scripts\install-mods.ps1 -Dev             # installs to .\run\mods (dev client)

param(
    [string]$ModsDir = "$env:APPDATA\.minecraft\mods",
    [switch]$Dev
)

$ErrorActionPreference = "Stop"

if ($Dev) {
    $ModsDir = Join-Path $PSScriptRoot "..\run\mods"
}

$mods = @(
    @{
        Name   = "Sodium"
        File   = "sodium-fabric-0.8.14+mc1.21.11.jar"
        Url    = "https://cdn.modrinth.com/data/AANobbMI/versions/rkdTcxoT/sodium-fabric-0.8.14%2Bmc1.21.11.jar"
        Sha512 = "04c43f9e8534b87a52c42ffd51b0e344d4ef92dc9cc52da33d13af6a18bf74b05380d2027f1e0db982698d0aff274c42741c1e21b313b0cd75ed3af005fc98d9"
    },
    @{
        Name   = "EntityCulling"
        File   = "entityculling-fabric-1.10.5-mc1.21.11.jar"
        Url    = "https://cdn.modrinth.com/data/NNAgCjsB/versions/sP0vNbeN/entityculling-fabric-1.10.5-mc1.21.11.jar"
        Sha512 = "67c4fb10bec6dba3368e65cd57f1bb53b9ca4172b2dc391c3d524f2c6e88c8782d8ff5f9634d549fbf7590296d18042cca84b12f3a8035a12f2ab555420df5a9"
    }
)

if (-not (Test-Path -LiteralPath $ModsDir)) {
    New-Item -ItemType Directory -Path $ModsDir -Force | Out-Null
}

$tmp = Join-Path $env:TEMP "aethelon-install"
New-Item -ItemType Directory -Path $tmp -Force | Out-Null

foreach ($m in $mods) {
    $dest = Join-Path $ModsDir $m.File
    $dl = Join-Path $tmp $m.File

    Write-Host "[aethelon] Installing $($m.Name) ..."

    if (-not (Test-Path -LiteralPath $dl)) {
        Invoke-WebRequest -Uri $m.Url -OutFile $dl -UserAgent "aethelon/0.1.0 (install script)"
    }

    $hash = (Get-FileHash -LiteralPath $dl -Algorithm SHA512).Hash.ToLower()
    if ($hash -ne $m.Sha512) {
        Remove-Item -LiteralPath $dl -Force
        throw "[aethelon] SHA-512 mismatch for $($m.Name): expected $($m.Sha512), got $hash"
    }

    Copy-Item -LiteralPath $dl -Destination $dest -Force
    Write-Host "[aethelon] OK: $dest"
}

Write-Host ""
Write-Host "[aethelon] Done. Sodium and EntityCulling are in:"
Write-Host "           $ModsDir"
Write-Host "           (copy them next to your other mods / the Aethelon jar, then restart the game.)"