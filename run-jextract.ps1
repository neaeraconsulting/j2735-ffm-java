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

$ErrorActionPreference = "Stop"
Set-PSDebug -Trace 1

# Copy the native library out to the shared volume
Copy-Item C:\build\out\* C:\build-lib\

# Copy the generated C files to the shared volume. -Force merges into an
# already-populated destination instead of erroring (this folder is likely
# already populated from a prior Linux build).
New-Item -ItemType Directory -Force C:\generated-files | Out-Null
Copy-Item -Recurse -Force C:\build\generated-files\* C:\generated-files\

# Copy the generated Java code to the shared volume (separate folder from the
# Linux output -- these bindings are Windows-specific and must not overwrite it)
New-Item -ItemType Directory -Force C:\generated-jextract-windows | Out-Null
Copy-Item -Recurse -Force C:\build\java-src\* C:\generated-jextract-windows\

# Keep the container running
while ($true) { Start-Sleep -Seconds 3600 }
