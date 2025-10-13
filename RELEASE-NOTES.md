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