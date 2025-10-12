package j2735ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
public class MessageFrameCodecTest {

  static MessageFrameCodec codec;

  @BeforeAll
  public static void setup() {
    URL url = MessageFrameCodecTest.class.getClassLoader().getResource("j2735ffm/libasnapplication.so");
    if (url == null) {
      throw new RuntimeException("libasnapplication.so not found");
    }
    try {
      Path libPath = Paths.get(url.toURI());
      codec = new MessageFrameCodec(262144L, 8192L, libPath);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

  }

  @Test
  public void testLibraryLoaded() {
    assertThat(codec, notNullValue());
    log.info("Library loaded");
  }

  @Test
  public void testXerToUper() {
    byte[] uper = codec.xerToUper("<MessageFrame><messageId>19</messageId><value><SPAT><intersections><IntersectionState><id><id>12111</id></id><revision>0</revision><status>0000000000000000</status><timeStamp>35176</timeStamp><states><MovementState><signalGroup>2</signalGroup><state-time-speed><MovementEvent><eventState><protected-Movement-Allowed/></eventState><timing><minEndTime>22120</minEndTime><maxEndTime>22121</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>4</signalGroup><state-time-speed><MovementEvent><eventState><stop-And-Remain/></eventState><timing><minEndTime>22181</minEndTime><maxEndTime>22181</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>6</signalGroup><state-time-speed><MovementEvent><eventState><protected-Movement-Allowed/></eventState><timing><minEndTime>22120</minEndTime><maxEndTime>22121</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>8</signalGroup><state-time-speed><MovementEvent><eventState><stop-And-Remain/></eventState><timing><minEndTime>21852</minEndTime><maxEndTime>21852</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>1</signalGroup><state-time-speed><MovementEvent><eventState><stop-And-Remain/></eventState><timing><minEndTime>21852</minEndTime><maxEndTime>21852</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>5</signalGroup><state-time-speed><MovementEvent><eventState><stop-And-Remain/></eventState><timing><minEndTime>21852</minEndTime><maxEndTime>21852</maxEndTime></timing></MovementEvent></state-time-speed></MovementState></states></IntersectionState></intersections></SPAT></value></MessageFrame>");
    assertThat(uper, notNullValue());
    String hex = HexFormat.of().formatHex(uper);
    log.info("hex: {}", hex);
  }

  @Test
  public void uperToXer() {
    String xer = codec.uperToXer(HexFormat.of().parseHex("001338000817a780000089680500204642b342b34802021a15a955a940181190acd0acd20100868555c555c00104342aae2aae002821a155715570"));
    assertThat(xer, notNullValue());
    log.info("xer: {}", xer);
  }
}
