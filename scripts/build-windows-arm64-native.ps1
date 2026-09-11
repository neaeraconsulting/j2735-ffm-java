#   Copyright 2025 Neaera Consulting LLC
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

<#
.SYNOPSIS
  Build the Windows ARM64 asnapplication DLL natively on Windows on ARM (no Docker Windows containers).

.DESCRIPTION
  Downloads llvm-mingw, CMake, and Ninja to a local cache (if needed), applies the Windows
  asn1c source prep, compiles with CMake, and copies asnapplication-arm64.dll into
  j2735-2024-ffm-lib/lib.

  Prerequisites:
    - Generated C sources in generated-files/2024 (from a Linux Docker build)
    - PowerShell 5.1+

.EXAMPLE
  .\scripts\build-windows-arm64-native.ps1

.EXAMPLE
  .\scripts\build-windows-arm64-native.ps1 -CopyToTestResources
#>

[CmdletBinding()]
param(
    [switch]$SkipWindowsPrep,
    [switch]$CopyToTestResources,
    [switch]$ForceToolDownload,
    [string]$ToolsRoot = "$env:LOCALAPPDATA\j2735-build-tools",
    [string]$CMakeVersion = "4.2.3",
    [string]$LlvmMingwVersion = "20260908",
    [string]$NinjaVersion = "1.13.1"
)

$ErrorActionPreference = "Stop"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$GeneratedFilesDir = Join-Path $RepoRoot "generated-files\2024"
$LibDir = Join-Path $RepoRoot "j2735-2024-ffm-lib\lib"
$TestResourceDir = Join-Path $RepoRoot "j2735-2024-ffm-lib\src\test\resources\j2735ffm"
$BuildDir = Join-Path $RepoRoot "out\build\native-arm64"
$OutputDll = Join-Path $LibDir "asnapplication-arm64.dll"

function Get-HostCpuArch {
    switch -Regex ($env:PROCESSOR_ARCHITECTURE) {
        "^ARM64$" { return "arm64" }
        default { return "x86_64" }
    }
}

function Ensure-Directory([string]$Path) {
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
}

function Download-AndExtractZip([string]$Url, [string]$DestinationDir, [string]$ExpectedSubdir) {
    Ensure-Directory $DestinationDir
    $zipPath = Join-Path $env:TEMP ("j2735-" + [IO.Path]::GetFileName($Url))
    Write-Host "Downloading $Url"
    Invoke-WebRequest -Uri $Url -OutFile $zipPath
    Expand-Archive -Path $zipPath -DestinationPath $DestinationDir -Force
    Remove-Item $zipPath
    $extracted = Join-Path $DestinationDir $ExpectedSubdir
    if (-not (Test-Path $extracted)) {
        throw "Expected '$extracted' after extracting $Url"
    }
    return $extracted
}

function Ensure-BuildTools {
    param(
        [string]$Root,
        [string]$HostArch,
        [switch]$Force
    )

    $cmakeZipArch = if ($HostArch -eq "arm64") { "windows-arm64" } else { "windows-x86_64" }
    $ninjaZipName = if ($HostArch -eq "arm64") { "ninja-winarm64.zip" } else { "ninja-win.zip" }

    $cmakeDir = Join-Path $Root "cmake-$CMakeVersion-$cmakeZipArch"
    $llvmDir = Join-Path $Root "llvm-mingw-$LlvmMingwVersion-ucrt-aarch64"
    $ninjaDir = Join-Path $Root "ninja"

    if ($Force -or -not (Test-Path (Join-Path $cmakeDir "bin\cmake.exe"))) {
        if (Test-Path $cmakeDir) { Remove-Item -Recurse -Force $cmakeDir }
        Download-AndExtractZip `
            "https://github.com/Kitware/CMake/releases/download/v$CMakeVersion/cmake-$CMakeVersion-$cmakeZipArch.zip" `
            $Root `
            "cmake-$CMakeVersion-$cmakeZipArch" | Out-Null
    }

    if ($Force -or -not (Test-Path (Join-Path $llvmDir "bin\clang.exe"))) {
        if (Test-Path $llvmDir) { Remove-Item -Recurse -Force $llvmDir }
        Download-AndExtractZip `
            "https://github.com/mstorsjo/llvm-mingw/releases/download/$LlvmMingwVersion/llvm-mingw-$LlvmMingwVersion-ucrt-aarch64.zip" `
            $Root `
            "llvm-mingw-$LlvmMingwVersion-ucrt-aarch64" | Out-Null
    }

    if ($Force -or -not (Test-Path (Join-Path $ninjaDir "ninja.exe"))) {
        Ensure-Directory $ninjaDir
        $ninjaZip = Join-Path $env:TEMP "j2735-ninja.zip"
        Write-Host "Downloading Ninja $NinjaVersion ($ninjaZipName)"
        Invoke-WebRequest `
            "https://github.com/ninja-build/ninja/releases/download/v$NinjaVersion/$ninjaZipName" `
            -OutFile $ninjaZip
        Expand-Archive -Path $ninjaZip -DestinationPath $ninjaDir -Force
        Remove-Item $ninjaZip
        if (-not (Test-Path (Join-Path $ninjaDir "ninja.exe"))) {
            throw "ninja.exe not found after extracting $ninjaZipName"
        }
    }

    return @{
        CmakeBin = Join-Path $cmakeDir "bin"
        LlvmBin = Join-Path $llvmDir "bin"
        NinjaBin = $ninjaDir
    }
}

Write-Host "Repository root: $RepoRoot"
Write-Host "Host CPU: $(Get-HostCpuArch)"

if (-not (Test-Path $GeneratedFilesDir)) {
    throw @"
Generated C sources not found at '$GeneratedFilesDir'.

Populate them with a Linux Docker build (works natively on Windows ARM64):
  docker context use desktop-linux
  docker compose -f docker-compose-build-arm64.yml up --build -d
"@
}

if (-not $SkipWindowsPrep) {
    & (Join-Path $PSScriptRoot "prepare-for-windows.ps1") -GeneratedFilesDir $GeneratedFilesDir
}

$tools = Ensure-BuildTools -Root $ToolsRoot -HostArch (Get-HostCpuArch) -Force:$ForceToolDownload
$env:PATH = "$($tools.CmakeBin);$($tools.LlvmBin);$($tools.NinjaBin);$env:PATH"

if (Test-Path $BuildDir) {
    Remove-Item -Recurse -Force $BuildDir
}
Ensure-Directory $BuildDir
Ensure-Directory $LibDir

Push-Location $RepoRoot
try {
    $compiler = if ((Get-HostCpuArch) -eq 'arm64') { 'clang' } else { 'aarch64-w64-mingw32-clang' }
    Write-Host "Configuring CMake in $BuildDir (compiler: $compiler)"
    & cmake -G Ninja -S $RepoRoot -B $BuildDir "-DCMAKE_C_COMPILER=$compiler" -DCMAKE_BUILD_TYPE=Release
    if ($LASTEXITCODE -ne 0) { throw "cmake configure failed with exit code $LASTEXITCODE" }

    Write-Host "Building native ARM64 DLL"
    & cmake --build $BuildDir --verbose
    if ($LASTEXITCODE -ne 0) { throw "cmake build failed with exit code $LASTEXITCODE" }

    $builtDll = Join-Path $BuildDir "libasnapplication.dll"
    if (-not (Test-Path $builtDll)) {
        throw "Build succeeded but DLL not found at $builtDll"
    }

    Copy-Item -Force $builtDll $OutputDll
    Write-Host "Copied DLL to $OutputDll"

    if ($CopyToTestResources) {
        Ensure-Directory $TestResourceDir
        Copy-Item -Force $OutputDll (Join-Path $TestResourceDir "asnapplication-arm64.dll")
        Write-Host "Copied DLL to test resources"
    }
}
finally {
    Pop-Location
}

Write-Host "Done. Run unit tests with:"
Write-Host "  cd j2735-2024-ffm-lib"
Write-Host "  .\gradlew.bat clean test"
