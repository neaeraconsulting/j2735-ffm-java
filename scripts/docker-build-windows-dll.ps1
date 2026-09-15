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

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

Set-Location C:\build

# Exec-form RUN does not inherit the Machine PATH set in earlier Dockerfile steps.
$llvmBin = 'C:\llvm-mingw\bin'
$env:PATH = "C:\cmake\bin;$llvmBin;C:\ninja;$env:PATH"

$crossBuildArm64 = $env:LIBRARY_SUFFIX -eq '-arm64'
$compiler = if ($crossBuildArm64) {
    Join-Path $llvmBin 'aarch64-w64-mingw32-clang.exe'
} else {
    Join-Path $llvmBin 'clang.exe'
}

if (-not (Test-Path $compiler)) {
    $available = @(Get-ChildItem $llvmBin -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name)
    throw @"
Compiler '$compiler' not found (LLVM_MINGW_ARCH=$($env:LLVM_MINGW_ARCH), LIBRARY_SUFFIX=$($env:LIBRARY_SUFFIX)).
The llvm-mingw layer may be stale. Rebuild without cache, e.g.:
  docker compose -f docker-compose-build-windows-arm64.yml build --no-cache
Available in ${llvmBin}: $($available -join ', ')
"@
}

Write-Host "Building DLL with compiler: $compiler (LLVM_MINGW_ARCH=$($env:LLVM_MINGW_ARCH), LIBRARY_SUFFIX=$($env:LIBRARY_SUFFIX))"

& cmake -G Ninja "-DCMAKE_C_COMPILER=$compiler" -DCMAKE_BUILD_TYPE=Release .
if ($LASTEXITCODE -ne 0) {
    throw "cmake configure failed with exit code $LASTEXITCODE"
}

& cmake --build . --verbose
if ($LASTEXITCODE -ne 0) {
    throw "cmake build failed with exit code $LASTEXITCODE"
}

New-Item -ItemType Directory -Force out | Out-Null
Copy-Item libasnapplication.dll "out\asnapplication$($env:LIBRARY_SUFFIX).dll"
