package j2735ffm;

import lombok.Getter;

/**
 * Encodings supported for conversion by the native library
 */
@Getter
public enum AsnEncoding {
  /** ASN.1 Unaligned Packed Encoding Rules - binary */
  UPER("uper", true, true),
  /** ASN.1 XML Encoding Rules - text */
  XER("xer", true, false),
  JER("jer", true, false),
  OER("oer", true, true),
  /** Unrecognized encoding */
  INVALID("invalid", false, false);

  /**
   * Name of the encoding as used by the native library
   * @return Name of the encoding as used by the native library
   */
  private final String name;
  /**
   * Whether this encoding is supported for conversion
   * @return Whether this encoding is supported for conversion
   */
  private final boolean supported;
  /**
   * Whether this encoding is binary rather than text
   * @return Whether this encoding is binary rather than text
   */
  private final boolean binary;

  AsnEncoding(String name, boolean supported, boolean binary) {
    this.name = name;
    this.supported = supported;
    this.binary = binary;
  }

  /**
   * Look up an encoding by its native library name
   * @param name The encoding name, e.g. "uper" or "xer"
   * @return The matching encoding
   * @throws IllegalArgumentException if no encoding matches the given name
   */
  public static AsnEncoding fromName(String name) {
    for (AsnEncoding encoding : AsnEncoding.values()) {
      if (encoding.getName().equalsIgnoreCase(name)) {
        return encoding;
      }
    }
    throw new IllegalArgumentException("Unknown encoding: " + name);
  }
}
