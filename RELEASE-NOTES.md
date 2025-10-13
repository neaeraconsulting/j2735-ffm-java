# v2.0.0

Refactor to use the existing C codec from asn1_codec, with a new "convert_bytes" C function.

Includes Linux library and Windows shared libraries.

Only UPER and XER are supported to match the current version of asn1_codec exactly.

Linux library:
[libasnapplication.so](https://github.com/neaeraconsulting/j2735-ffm-java/blob/main/lib/libasnapplication.so)

Windows library:
[asnapplication.dll](https://github.com/neaeraconsulting/j2735-ffm-java/blob/main/lib/asnapplication.dll)


**Full Changelog**: https://github.com/neaeraconsulting/j2735-ffm-java/compare/v1.0.5...v2.0.0