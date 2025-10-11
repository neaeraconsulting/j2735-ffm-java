package j2735ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HexFormat;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class MessageFrameCodecTest {

  @Test
  public void testLibraryLoaded() {
    var codec = new MessageFrameCodec();
    assertThat(codec, notNullValue());
  }

  @Test
  public void testXerToUper() {
    var codec = new MessageFrameCodec();
    byte[] uper = codec.xerToUper("<MessageFrame><messageId>19</messageId><value><SPAT><intersections><IntersectionState><id><id>12111</id></id><revision>0</revision><status>0000000000000000</status><timeStamp>35176</timeStamp><states><MovementState><signalGroup>2</signalGroup><state-time-speed><MovementEvent><eventState><protected-Movement-Allowed/></eventState><timing><minEndTime>22120</minEndTime><maxEndTime>22121</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>4</signalGroup><state-time-speed><MovementEvent><eventState><stop-And-Remain/></eventState><timing><minEndTime>22181</minEndTime><maxEndTime>22181</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>6</signalGroup><state-time-speed><MovementEvent><eventState><protected-Movement-Allowed/></eventState><timing><minEndTime>22120</minEndTime><maxEndTime>22121</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>8</signalGroup><state-time-speed><MovementEvent><eventState><stop-And-Remain/></eventState><timing><minEndTime>21852</minEndTime><maxEndTime>21852</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>1</signalGroup><state-time-speed><MovementEvent><eventState><stop-And-Remain/></eventState><timing><minEndTime>21852</minEndTime><maxEndTime>21852</maxEndTime></timing></MovementEvent></state-time-speed></MovementState><MovementState><signalGroup>5</signalGroup><state-time-speed><MovementEvent><eventState><stop-And-Remain/></eventState><timing><minEndTime>21852</minEndTime><maxEndTime>21852</maxEndTime></timing></MovementEvent></state-time-speed></MovementState></states></IntersectionState></intersections></SPAT></value></MessageFrame>");
    assertThat(uper, notNullValue());
    String hex = HexFormat.of().formatHex(uper);
    log.info("hex: {}", hex);
  }
}
