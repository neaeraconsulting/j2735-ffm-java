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

$arch = $env:LLVM_MINGW_ARCH
if ([string]::IsNullOrWhiteSpace($arch)) {
    throw "LLVM_MINGW_ARCH is not set. Pass build-arg LLVM_MINGW_ARCH (x86_64 or aarch64)."
}

Write-Host "Installing Windows toolchain for LLVM_MINGW_ARCH=$arch"

Invoke-WebRequest -Uri "https://github.com/Kitware/CMake/releases/download/v$($env:CMAKE_VERSION)/cmake-$($env:CMAKE_VERSION)-windows-x86_64.zip" -OutFile cmake.zip
Expand-Archive cmake.zip -DestinationPath C:\
Rename-Item "C:\cmake-$($env:CMAKE_VERSION)-windows-x86_64" C:\cmake

Invoke-WebRequest -Uri "https://github.com/mstorsjo/llvm-mingw/releases/download/$($env:LLVM_MINGW_VERSION)/llvm-mingw-$($env:LLVM_MINGW_VERSION)-ucrt-$arch.zip" -OutFile llvm-mingw.zip
Expand-Archive llvm-mingw.zip -DestinationPath C:\
Rename-Item "C:\llvm-mingw-$($env:LLVM_MINGW_VERSION)-ucrt-$arch" C:\llvm-mingw

Invoke-WebRequest -Uri "https://github.com/ninja-build/ninja/releases/download/v$($env:NINJA_VERSION)/ninja-win.zip" -OutFile ninja.zip
Expand-Archive ninja.zip -DestinationPath C:\ninja
Remove-Item cmake.zip, llvm-mingw.zip, ninja.zip

$llvmBin = 'C:\llvm-mingw\bin'
$crossBuildArm64 = $env:LIBRARY_SUFFIX -eq '-arm64'
$expectedCompiler = if ($crossBuildArm64) {
    Join-Path $llvmBin 'aarch64-w64-mingw32-clang.exe'
} else {
    Join-Path $llvmBin 'clang.exe'
}

if (-not (Test-Path $expectedCompiler)) {
    $available = @(Get-ChildItem $llvmBin -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name)
    throw "Expected compiler '$expectedCompiler' not found after installing llvm-mingw host arch '$arch' (LIBRARY_SUFFIX=$($env:LIBRARY_SUFFIX)). Available in ${llvmBin}: $($available -join ', ')"
}

[Environment]::SetEnvironmentVariable(
    'PATH',
    "C:\cmake\bin;$llvmBin;C:\ninja;" + [Environment]::GetEnvironmentVariable('PATH', 'Machine'),
    'Machine'
)

Write-Host "Toolchain ready: $expectedCompiler"
