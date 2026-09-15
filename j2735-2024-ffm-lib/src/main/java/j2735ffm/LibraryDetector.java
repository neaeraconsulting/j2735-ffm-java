/*
   Copyright 2025 Neaera Consulting LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package j2735ffm;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for detecting the current architecture and finding the appropriate native library.
 */
@Slf4j
public class LibraryDetector {

    private LibraryDetector() {
        // Utility class
    }

    /**
     * Detects the current system architecture.
     * @return Architecture string (amd64, arm64, etc.)
     */
    public static String detectArchitecture() {
        String osArch = System.getProperty("os.arch").toLowerCase();

        // Normalize architecture names
        if (osArch.contains("amd64") || osArch.contains("x86_64") || osArch.equals("x64")) {
            return "amd64";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            return "arm64";
        } else if (osArch.contains("arm")) {
            return "arm";
        }

        return osArch;
    }

    /**
     * Detects the operating system.
     * @return OS string (linux, windows, macos)
     */
    public static String detectOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "windows";
        } else if (osName.contains("mac")) {
            return "macos";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return "linux";
        }
        return osName;
    }

    /**
     * Gets the library filename for the current platform.
     * Linux and Windows are supported; other operating systems return null.
     * @param baseName Base name of the library (e.g., "asnapplication")
     * @return Library filename, or null if the OS is not supported
     */
    public static String getLibraryFilename(String baseName) {
        String os = detectOS();
        String arch = detectArchitecture();

        if (os.equals("windows")) {
            return baseName + ".dll";
        }
        if (os.equals("linux")) {
            if (arch.equals("arm64")) {
                return "lib" + baseName + "-arm64.so";
            }
            return "lib" + baseName + ".so";
        }
        return null;
    }

    /**
     * Finds the native library in a directory, trying architecture-specific paths first.
     * @param baseDirectory Base directory to search
     * @param libraryName Base name of the library (e.g., "asnapplication")
     * @return Path to the library, or null if not found
     */
    public static Path findLibrary(Path baseDirectory, String libraryName) {
        String filename = libraryFilenameIfSupported(libraryName);
        if (filename == null) {
            return null;
        }
        String os = detectOS();
        String arch = detectArchitecture();

        Path found = existingFile(
            baseDirectory.resolve(os + "-" + arch).resolve(filename),
            "architecture-specific directory");
        if (found != null) {
            return found;
        }

        found = existingFile(baseDirectory.resolve(filename), "root directory");
        if (found != null) {
            return found;
        }

        // Try without architecture suffix for Linux (backward compatibility)
        if (os.equals("linux") && !arch.equals("amd64")) {
            found = existingFile(baseDirectory.resolve("lib" + libraryName + ".so"), null);
            if (found != null) {
                log.warn("Using fallback library path (may be wrong architecture): {}", found);
                return found;
            }
        }

        log.error("Library not found in {}", baseDirectory);
        return null;
    }

    /**
     * Finds the native library from a resource path (for use in JARs).
     * @param resourceBasePath Base resource path (e.g., "j2735ffm")
     * @param libraryName Base name of the library (e.g., "asnapplication")
     * @return Path to the library, or null if not found
     */
    public static Path findLibraryFromResource(String resourceBasePath, String libraryName) {
        String filename = libraryFilenameIfSupported(libraryName);
        if (filename == null) {
            return null;
        }
        String os = detectOS();
        String arch = detectArchitecture();

        Path found = resourceAsPath(resourceBasePath + "/" + os + "-" + arch + "/" + filename);
        if (found != null) {
            return found;
        }

        found = resourceAsPath(resourceBasePath + "/" + filename);
        if (found != null) {
            return found;
        }

        log.error("Library resource not found: {}/{}", resourceBasePath, filename);
        return null;
    }

    private static String libraryFilenameIfSupported(String libraryName) {
        String os = detectOS();
        if (os.equals("macos")) {
            log.error("macOS is not supported");
            return null;
        }
        if (!os.equals("linux") && !os.equals("windows")) {
            log.error("Unsupported operating system: {}", os);
            return null;
        }
        return getLibraryFilename(libraryName);
    }

    private static Path existingFile(Path path, String locationDescription) {
        if (!Files.exists(path)) {
            return null;
        }
        if (locationDescription != null) {
            log.info("Found library in {}: {}", locationDescription, path);
        }
        return path;
    }

    private static Path resourceAsPath(String resourcePath) {
        URL url = LibraryDetector.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            return null;
        }
        try {
            Path path = Paths.get(url.toURI());
            log.info("Found library resource: {}", resourcePath);
            return path;
        } catch (Exception e) {
            log.warn("Error converting resource URL to path: {}", e.getMessage());
            return null;
        }
    }
}
