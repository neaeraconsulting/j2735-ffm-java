package j2735ffm;

/**
 * Checked exception for errors returned from the native library
 */
public class ConvertException extends Exception{

  /**
   * Constructor
   * @param message - Error message from the native library
   */
  public ConvertException(String message) {
    super(message);
  }

}
