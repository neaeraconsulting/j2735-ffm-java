# v3.0.0

Upgrade the Java library, demo API, CI, and container images from JDK 22 to JDK 25.

Regenerate the Foreign Function and Memory API bindings with the JDK 25 version of jextract.

Java 25 is now the minimum supported build and runtime version.

Adds dockerfiles to be able to automatically build the Windows dll, and generate correct jextract wrapper code for Windows. 

# v2.0.2

Use asn1_codec submodule from usdot repository.

Linux library:
[libasnapplication.so](https://github.com/neaeraconsulting/j2735-ffm-java/blob/v2.0.2/lib/libasnapplication.so)

Windows library:
[asnapplication.dll](https://github.com/neaeraconsulting/j2735-ffm-java/blob/v2.0.2/lib/asnapplication.dll)

**Changelog**: https://github.com/neaeraconsulting/j2735-ffm-java/compare/v2.0.1...v2.0.2

# v2.0.1

Issues fixed in [internal review](https://github.com/neaeraconsulting/j2735-ffm-java/pull/2)

**Changelog**: https://github.com/neaeraconsulting/j2735-ffm-java/compare/v2.0.0...v2.0.1

# v2.0.0

Refactor to use the existing C codec from asn1_codec, with a new C API, and backwards compatible Java API, except JER support is removed as noted below.

Includes a Linux native library and adds a Windows native library.  

Adds unit tests which can be run in Windows or Linux.

Only UPER and XER are supported to match the current version of asn1_codec exactly. JER support is not included.

Adds the ability to specify the location of the native library.

Linux library:
[libasnapplication.so](https://github.com/neaeraconsulting/j2735-ffm-java/blob/v2.0.0/lib/libasnapplication.so)

Windows library:
[asnapplication.dll](https://github.com/neaeraconsulting/j2735-ffm-java/blob/v2.0.0/lib/asnapplication.dll)


**Full Changelog**: https://github.com/neaeraconsulting/j2735-ffm-java/compare/v1.0.5...v2.0.0
