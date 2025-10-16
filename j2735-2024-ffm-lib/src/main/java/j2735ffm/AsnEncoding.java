package j2735ffm;

import lombok.Getter;

@Getter
public enum AsnEncoding {
  UPER("uper", true, true),
  XER("xer", true, false),
  JER("jer", false, false),
  OER("oer", false, true),
  INVALID("invalid", false, false);

  private final String name;
  private final boolean supported;
  private final boolean binary;

  AsnEncoding(String name, boolean supported, boolean binary) {
    this.name = name;
    this.supported = supported;
    this.binary = binary;
  }

  public static AsnEncoding fromName(String name) {
    for (AsnEncoding encoding : AsnEncoding.values()) {
      if (encoding.getName().equalsIgnoreCase(name)) {
        return encoding;
      }
    }
    throw new IllegalArgumentException("Unknown encoding: " + name);
  }
}
