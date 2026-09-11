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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for detecting the current architecture and finding the appropriate native library.
 */
@Slf4j
public class LibraryDetector {
    
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
     * @param baseName Base name of the library (e.g., "asnapplication")
     * @return Library filename
     */
    public static String getLibraryFilename(String baseName) {
        String os = detectOS();
        String arch = detectArchitecture();
        
        if (os.equals("windows")) {
            return baseName + ".dll";
        } else if (os.equals("macos")) {
            return "lib" + baseName + ".dylib";
        } else {
            // Linux
            if (arch.equals("arm64")) {
                return "lib" + baseName + "-arm64.so";
            } else {
                return "lib" + baseName + ".so";
            }
        }
    }
    
    /**
     * Finds the native library in a directory, trying architecture-specific paths first.
     * @param baseDirectory Base directory to search
     * @param libraryName Base name of the library (e.g., "asnapplication")
     * @return Path to the library, or null if not found
     */
    public static Path findLibrary(Path baseDirectory, String libraryName) {
        String os = detectOS();
        String arch = detectArchitecture();
        
        // Try architecture-specific subdirectory first (e.g., lib/linux-amd64/)
        if (os.equals("linux")) {
            Path archSpecificPath = baseDirectory.resolve("linux-" + arch)
                .resolve(getLibraryFilename(libraryName));
            if (Files.exists(archSpecificPath)) {
                log.info("Found library in architecture-specific directory: {}", archSpecificPath);
                return archSpecificPath;
            }
        }
        
        // Try root directory
        Path rootPath = baseDirectory.resolve(getLibraryFilename(libraryName));
        if (Files.exists(rootPath)) {
            log.info("Found library in root directory: {}", rootPath);
            return rootPath;
        }
        
        // Try without architecture suffix for Linux (backward compatibility)
        if (os.equals("linux") && !arch.equals("amd64")) {
            Path fallbackPath = baseDirectory.resolve("lib" + libraryName + ".so");
            if (Files.exists(fallbackPath)) {
                log.warn("Using fallback library path (may be wrong architecture): {}", fallbackPath);
                return fallbackPath;
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
        String os = detectOS();
        String arch = detectArchitecture();
        String libraryFilename = getLibraryFilename(libraryName);
        
        // Try architecture-specific resource path first
        if (os.equals("linux")) {
            String archResourcePath = resourceBasePath + "/linux-" + arch + "/" + libraryFilename;
            java.net.URL url = LibraryDetector.class.getClassLoader().getResource(archResourcePath);
            if (url != null) {
                try {
                    Path path = Paths.get(url.toURI());
                    log.info("Found library resource: {}", archResourcePath);
                    return path;
                } catch (Exception e) {
                    log.warn("Error converting resource URL to path: {}", e.getMessage());
                }
            }
        }
        
        // Try root resource path
        String rootResourcePath = resourceBasePath + "/" + libraryFilename;
        java.net.URL url = LibraryDetector.class.getClassLoader().getResource(rootResourcePath);
        if (url != null) {
            try {
                Path path = Paths.get(url.toURI());
                log.info("Found library resource: {}", rootResourcePath);
                return path;
            } catch (Exception e) {
                log.warn("Error converting resource URL to path: {}", e.getMessage());
            }
        }
        
        log.error("Library resource not found: {}", rootResourcePath);
        return null;
    }
}



