package j2735ffm;

import java.nio.file.Path;

public class Ieee1609Dot2DataCodec extends GeneralCodec {

  public final static String IEEE1609_DOT2_DATA_PDU = "Ieee1609Dot2Data";

  /**
   * Constructor.  Configures the library and loads the underlying native library
   * @param textBufferSize - Size of the input or output buffer for text encodings (XER)
   * @param oerBufferSize - Size of the input or output buffer for OER binary encoding.
   * @param libraryPath - Absolute or relative path to the native library, e.g. "/usr/lib/libasnapplication.so"
   */
  public Ieee1609Dot2DataCodec(long textBufferSize, long oerBufferSize, long errorBufferSize,
      Path libraryPath) {
    super(textBufferSize, oerBufferSize, errorBufferSize, libraryPath);
  }

  /**
   * Convert an XER encoded Ieee1609Dot2Data to OER
   * @param xer The XER encoded Ieee1609Dot2Data
   * @return Byte array with the OER encoding
   */
  public byte[] xerToOer(String xer) {
    return super.xerToOer(IEEE1609_DOT2_DATA_PDU, xer);
  }

  /**
   * Convert an OER encoded Ieee1609Dot2Data to XER
   * @param oer The OER encoded Ieee1609Dot2Data
   * @return XER encoded result
   */
  public String oerToXer(byte[] oer) {
    return super.oerToXer(IEEE1609_DOT2_DATA_PDU, oer);
  }

}
