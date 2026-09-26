package j2735ffm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;


@Slf4j
public abstract class BaseCodecTest {

  protected static final long TEXT_BUFFER_SIZE = 262144L;
  protected static final long BINARY_BUFFER_SIZE = 8192L;
  protected static final long ERROR_BUFFER_SIZE = 256L;

  protected final static HexFormat hexFormat = HexFormat.of();

  protected static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  protected static Path getLibPath() {
    Path libPath = LibraryDetector.findLibraryFromResource("j2735ffm", "asnapplication");
    log.info("Loading library {}", libPath);

    if (libPath == null) {
      throw new RuntimeException("libasnapplication not found");
    }
    return libPath;
  }

  protected static byte[] hexNoWs(String hex) {
    return hexFormat.parseHex(hex.replaceAll("\\s", ""));
  }

  protected static String loadResource(String name) {
    try {
      return IOUtils.resourceToString("/j2735ffm/" + name, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
