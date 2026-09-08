# v3.0.0

Introduces the generic, thread-safe `Asn1Codec` API with XER, UPER, OER, and COER
conversion. `MessageFrameCodec` remains as a deprecated adapter.

Adds signed `Ieee1609Dot2Data` COER support and direct
`AdvisorySituationData` UPER support, including the SEMI 16-region maximum.
Malformed, truncated, constraint, unknown-PDU, output-limit, and native-loading
errors now have stable categories and Java operation context.

Targets Java 25 and publishes `usdot.jpo.ode:j2735-2024-ffm-lib:3.0.0` plus an
Ubuntu Noble `linux-x86_64` classifier JAR. Artifact manifests record native ABI
version `1` and `asn1_codec` commit
`3615085e993f44bacb810a1ac3671977057e0b82`.

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
