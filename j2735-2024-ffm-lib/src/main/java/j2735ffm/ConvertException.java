package j2735ffm;

/**
 * Checked exception for errors returned from the native library
 */
@Deprecated(forRemoval = false, since = "3.0.0")
public class ConvertException extends Exception{

  public ConvertException(String message) {
    super(message);
  }

}
