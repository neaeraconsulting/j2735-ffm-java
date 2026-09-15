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

[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [string]$GeneratedFilesDir
)

$ErrorActionPreference = "Stop"

if (-not $GeneratedFilesDir) {
    $GeneratedFilesDir = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")) "generated-files\2024"
} else {
    $GeneratedFilesDir = Resolve-Path $GeneratedFilesDir
}

if (-not (Test-Path $GeneratedFilesDir)) {
    throw "Generated C sources not found at '$GeneratedFilesDir'. Run a Linux Docker build first to populate generated-files/2024."
}

Write-Host "Preparing generated C sources for Windows build in $GeneratedFilesDir"

$filesToRemove = @(
    "GeneralizedTime.h", "GeneralizedTime.c", "GeneralizedTime_ber.c",
    "GeneralizedTime_print.c", "GeneralizedTime_rfill.c", "GeneralizedTime_xer.c",
    "Period.c", "Period.h",
    "AggregatedSingleTariffClassSession.c", "AggregatedSingleTariffClassSession.h",
    "DetectedChargeObject.c", "DetectedChargeObject.h",
    "converter-example.c"
)

foreach ($file in $filesToRemove) {
    $path = Join-Path $GeneratedFilesDir $file
    if (Test-Path $path) {
        Remove-Item -Force $path
        Write-Host "  removed $file"
    }
}

$pduPath = Join-Path $GeneratedFilesDir "pdu_collection.c"
if (-not (Test-Path $pduPath)) {
    throw "Missing pdu_collection.c at $pduPath"
}

(Get-Content $pduPath) `
    -replace 'extern struct asn_TYPE_descriptor_s asn_DEF_Period;', '// extern struct asn_TYPE_descriptor_s asn_DEF_Period;' `
    -replace 'extern struct asn_TYPE_descriptor_s asn_DEF_AggregatedSingleTariffClassSession;', '// extern struct asn_TYPE_descriptor_s asn_DEF_AggregatedSingleTariffClassSession;' `
    -replace 'extern struct asn_TYPE_descriptor_s asn_DEF_DetectedChargeObject;', '// extern struct asn_TYPE_descriptor_s asn_DEF_DetectedChargeObject;' `
    -replace '&asn_DEF_Period,', '// &asn_DEF_Period,' `
    -replace '&asn_DEF_AggregatedSingleTariffClassSession,', '// &asn_DEF_AggregatedSingleTariffClassSession,' `
    -replace '&asn_DEF_DetectedChargeObject,', '// &asn_DEF_DetectedChargeObject,' `
    | Set-Content $pduPath

Write-Host "Windows C source preparation complete."
