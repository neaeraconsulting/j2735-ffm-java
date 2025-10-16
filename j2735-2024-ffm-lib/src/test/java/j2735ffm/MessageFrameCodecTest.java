/*
   Copyright 2025 Neaera Consulting LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package j2735ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class MessageFrameCodecTest {

  static MessageFrameCodec codec;
  final static HexFormat hexFormat = HexFormat.of();


  static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  @BeforeAll
  public static void setup() {

    String libResource = isWindows() ? "j2735ffm/asnapplication.dll" : "j2735ffm/libasnapplication.so";
    URL url = MessageFrameCodecTest.class.getClassLoader().getResource(libResource);
    log.info("Loading library {}", libResource);

    if (url == null) {
      throw new RuntimeException("libasnapplication not found");
    }
    try {
      Path libPath = Paths.get(url.toURI());
      codec = new MessageFrameCodec(262144L, 8192L, 256L, libPath);
      log.info("Created codec");
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
    String hex = hexFormat.formatHex(uper);
    log.info("hex: {}", hex);
  }

  @ParameterizedTest
  @CsvSource({
      "001338000817a780000089680500204642b342b34802021a15a955a940181190acd0acd20100868555c555c00104342aae2aae002821a155715570",
      "001480AD562FA8400039E8E717090F9665FE1BACC37FFFFFFFF0003BBAFDFA1FA1007FFF8000000000020214C1C100417FFFFFFE824E100A3FFFFFFFE8942102047FFFFFFE922A1026A40143FFE95D610423405D7FFEA75610322C0599FFEADFA10391C06B5FFEB7E6103CB40A03FFED2121033BC08ADFFED9A6102E8408E5FFEDE2E102BDC0885FFEDF0A1000BC019BFFF7F321FFFFC005DFFFC55A1FFFFFFFFFFFFDD1A100407FFFFFFFE1A2FFFE0000",
      "001f85fe7591fd9e4f4354455420535452124c16b4fa4e724f43ef73d2f04fd7ad0ff09260b5a7d2a6249ba8936ffa29bac57176dadf5389a4000a2001c3a03dc06ba5067b8404632ae6a0503389c06966bc120c5900f771c1c6306a9a6bf319901630a0f2403124536d3e8476d9af77a7bf218901103a881122806195dcc32733526fe545fa217f819306eff7fdceaafa0230c87f355e98cb516360080a0002f348c72a3f584b11e4be99002ad4a0d12394d435cfda9fe80267e176eb96a2ff07260b5a7f4b9d40747f5ffe014b670db5da84587b200050400f55bfce9e9bc9d913782889646ff0126933eacd08beff7f5447df8cf48052da9a01005a0001bc2fa39c11fc2e7046450f0952d4dfe024ddb71fd2d3b502b9b2551e1d3f8b210e3c4000b00020c2dd46d113a3f21710e9416b92d5bfc14982d60dc94824a8e5c5095c1007d4e362e82720800270a04182240bda99c42bb6d94ff5f9a52a3014cd051a29843d4489d71d4150ae35b7533039e58186a0608ac0fc05f4bf24a4e851d3d0a801880f339e17e05ca32567a49b73b932580eecfc46064078a2e0120158f22817d1fb85eee4e66a4dfca8bf442fcca15810716140920643215dc607f829305ac1b92904951cb8a12b8200fa9c6c5d04e410004c0a0671380d2cd782418b201eee3838c60d534d7e633202c6141e4806248a6da7d08edb35eef4f7e431202207510224500c32bb9864e66a4dfca8bf442ff05260b5837252092a397142570401f538d8ba09c820009976b50fc6379b168326f9bdfe0a4c16b06e4a4125472e284ae0803ea71b1741390400132ed63059cde2d064df37c448db82d49305ad3e949305ad3e88bf939305ad3010933071674e428880448e75d46d038a29245b0b778f7001044f43ef73d2f04fd7ad0ff09260b5a7d2a6249ba8936ffa29bac57176dadf5389a4000a2001c3a03dc06ba5067b8404632ae6a0503389c06966bc120c5900f771c1c6306a9a6bf319901630a0f2403124536d3e8476d9af77a7bf218901103a881122806195dcc32733526fe545fa217f819306eff7fdceaafa0230c87f355e98cb516360080a0002f348c72a3f584b11e4be99002ad4a0d12394d435cfda9fe80267e176eb96a2ff07260b5a7f4b9d40747f5ffe014b670db5da84587b200050400f55bfce9e9bc9d913782889646ff0126933eacd08beff7f5447df8cf48052da9a01005a0001bc2fa39c11fc2e7046450f0952d4dfe024ddb71fd2d3b502b9b2551e1d3f8b210e3c4000b00020c2dd46d113a3f21710e9416b92d5bfc14982d60dc94824a8e5c5095c1007d4e362e82720800270a04182240bda99c42bb6d94ff5f9a52a3014cd051a29843d4489d71d4150ae35b7533039e58186a0608ac0fc05f4bf24a4e851d3d0a801880f339e17e05ca32567a49b73b932580eecfc46064078a2e0120158f22817d1fb85eee4e66a4dfca8bf442fcca15810716140920643215dc607f829305ac1b92904951cb8a12b8200fa9c6c5d04e410004c0a0671380d2cd782418b201eee3838c60d534d7e633202c6141e4806248a6da7d08edb35eef4f7e431202207510224500c32bb9864e66a4dfca8bf442ff05260b5837252092a397142570401f538d8ba09c820009976b50fc6379b168326f9bdfe0a4c16b06e4a4125472e284ae0803ea71b1741390400132ed63059cde2d064df37c4401b705a049305ad3e9024982d69f445fc9c982d69808499838b3a7214440224738ea3681c514922d85bbc7b8008227a1f7b9e97827ebd607f8093499f566845f7fbfaa23efc67a40296d4d00802d0000de17d1ce08fe173823228784a96a711136e0b524c16b4fa524c16b4fa22fe4e4c16b4c0424cc1c59d390a22011239df51b40e28a4916c2dde3dc004113d0fbdcf4bc13f5eb03fc049a4cfab3422fbfdfd511f7e33d2014b6a680401680006f0be8e7047f0b9c119143c254b538899b705a9260b5a7d29260b5a7d117f27260b5a60212660e2ce9c851100891cefa8da07145248b616ef1ee002089e87dee7a5e09faf581fe024d267d59a117dfefea88fbf19e900a5b5340200b40003785f473823f85ce08c8a1e12a5a9c450db82d49305ad3e949305ad3e88bf939305ad3010933071674e4288804480",
      "001d697125b7da9aa31b97b0fb3ec9148495a40fed6be3446a4e5b70ec1a2752b710fbfaf5ac086673396a81677da981b16a6b4905771f424e51683a70adb0afd713837a81719aed3fa8d4e347ef1b40e42d024585c757787442477e73341aae24982d69f40e4c16b4c428d8",
      "001e817b65e539dc93b843af683249404f9e0fc6b04fd122cf2ce89941dc1ab4d3d288394b59be74b1c04cfdee07bc9868311d2c1caa51f03dc764f993d0d511779e9ef22be1121c093e1af96b1d141a1ba967c329e47cf884b8beb3268e790f72270ca44c2519740d31d85f3a0e91a6bca5145e560e920d281085568f931b7067cc9e86a88b8f5957847ac6b4fa9d1b07fc2cd2e6a91f327a1aa229d5e21e478318d630a67bd8ffd0ce05537cf12267f5df5fc2794ff0804001a150fe00a679ce1c7934b4a6891be64f435445f9206bc5ff8e09b516244086367d14aef993d0d51175f310f233515cb0082fa6f907e861f0be64f435445412a1bcae64260fe3aa2470ea9ccf666f993d0d5114a9c22f0ba4406960b0cc40a0bd244e285bce95385017cefcbbac8d3070a7819776806012dd1dd66d719a089f3d143c9b843e001e4439710aacb223e510b9397770640941fcec2f9fc6a4cba57da12f160011a4d7c2fc0f7f10429d2be7409299a6cd9053c256161af6572d28cf07d9bdc620",
      "0013838f65227f02487f07260b5a7ea4eea3b4400807cf548e3480e0f54afae04c14982d4a1183eefde4e4ae9d30fca80a906f32c28fceb1e486cd9fd93c2d459f18a78f3a18c1f2c7f371b0869045b061dc316d4783f3b1b5a8f02373272f5e4d3e43e28bc8d949e323784c3cb28a2c7f70c56eee2f4c349774c980780a932ee3ec7f57bc8c675aa86a1b255d837884388e1a0f23ba161109e05acd8ed8bc3de03dd7365fa25734fa22150f8d614a293c26a19a3af78506832764f1cfe2a53485e496f79e7bde2224dd7f9d839305ad3cd064f9065af84baa7369921efa9bc97ca6e6f578263bd7e14f01875bfef1e605c0a61783c42ec211d7b05260b548466f818f8f900ab51f108a63e8bc424d4912e79ddc45ed9ac9f1e46193c721453ead68310790031a7494cbf5a8b10a542d320ae0548f279541bd9236f1a4d7ec61de6626cc170b17d122a1aa087b217a403f281e27a60cacab1fd15ac7922179cb33c5c5a91e48125135a3c66ee3e5887909a8e5290f145811f925e06104656f03c644f887279bda2bcdb919bc3cc8d28adbccaa6d8be7bcc10a12ccb808721905d68b035663074e9f03703207e124c16b4fa6140d1ef05200008b6986c920383d52beb81305260b5d8460f8a4e9b40095c0d52cba5013c7cb9b735b79c1f97a7e0f319b50c13997ec7246d5c011a8493a7c0c8f3ba3957c09e21bc1374bbceb140ea4f78f0dbb3d70f3a078dbdbd8bf03a48c22b47c8673ca04a0f14ab3b6a1193eb224d4de5e8cd1ee4d67c4f3a8e4eec39e66e82ffcf3cd91254fee63fbf4f21de15d4b47781ca29bc55a1a06cc78a68a243c2f18ae8212c1e4aa4a9a968bd38661b55fbde1077ad13b07260b5a7284c1f8cbc40ec0f4460cb216c44795698e88dcf19275e0569e16c133e12bcfd74b729c79dfab729a6c7f7196629e37797fd615dda0795880e21f0cdf66275b50336d6d59e0edb37999087309ef13b682aed5e3b2c8770d3c2349bfe3a278810ad60bf7a555c5496f790de20678b60a4c16a306d3f3aa0846120af3929130033795e74cd582f374d78b4b5e001433d1bbc1fbc890dd69fa4cab02194332aff100de93cc1f5deaad78163974b54f0e288cb03593ef948282513c01d3a800e60f3a001120d58be1dd4d5dc82aceb6a218a86f1f2a2d7fc9e537a3d34b3c63c95b17b784a019a9048f33964016f6f361756ff66f2e368e15bef3440fe99e6f5f3f646e7e4f5d18685fa6f5293727ccef6748b4a5c6021e86fc0041f13640f20340c13a5b0180",
      "001285837f2ac48d34380f049305ad3e982bb8ebcf8974c490985c69a3e0100912330bc58f3533b990adbd037eb2124c16b4fa42a081c0800008041efec8569c5c4491d8b4d863f49400890ce792b14bc6ff5e002873ac3d5aa6c50b973f29f90c29305acda1016000010170154469aff68b45f7e48be5608177c00425d8c8fbfb68025f181010922d087fa42018137623cf27abe4f8ff44fc561c982d69a22040e00000200095e931d65dc2bdb5b2ff8000d7f312fe5bb233fecd00181d8b72fd5666bf880206838e1f8949c5e97bac74bf4f81271ca0088000100026933647627b7e3782695a36a1a5e0f3fc2384019987e820410146899bb2b9c8fce800d34d56c97d76a64048083008be9f0e24011c1c982d69dbbf57e0b62693aa763039d84818313a4f101640406d4aed322bb441e0007ef4124c16b4fa651021040000080005ea2e4a0aa0d03f8e0087600f9370fc096bf464017e0c40d0919e29ff2a004f45b99a260904ff8e8019574b68285d9db7a9a5c312e0e4c16b4f63c0c9c76922de6a591e6ca9552d5d1a0e0b05a80bf3507260b5a6f240210800020020f1ba08077b3a2c37d8dff94b856aa493e4e4014d73f24697ffdafaf000fa5007d1ff3694feb8401dd7d138c51bd09fff100515361ccc71a2bbfa400554a33f5261933535528fc46049b8210101000042eddebe9234a98e872d5707c8c8039cca6280846565393fb4c6f11c0c983186ed3d4d68d887a3a227ba63e13020b3a44f8dcd9061fa48d2f05896411fb0049305ad3e8c04800020042e61db0e822708d17b5cc17e090024b4262ae38e8dff9b800f21e1402362efb7ce08034422a6c171d9f4241fb84093c480411000040030a6ce962ac8e060609210080154c41890612aa41eb513e30101c915ee9567cd0cfc3802402fea75e0f837be34041da19a435a063d4faf020405db37524e767bf480813783e9b8a7f698098fc3a1c982d69c59008041000b8f0e27e0183f6a695f845fd30029f92784022161d7eea0037f758d85fbe1b3f3a200e21d14fa12a43b9a8b37ed40e4c16b4d20802c4000020b8a3abbd8f6e653e7b52345f35020bca212dd11678a7db404349362bc0a6578ff0a002eb36c2134f61cab4c5063737e66024aca0100400800022699bc08da9d05f6d010ce20806a21af287c184033a8196e252a851f00080c3230b8797ce5f21adb1d709260b5a7d2541d25f8bd4586171382d50c158ece4dea2a76905ede08fd7a049345001c000002010be7db90135b7a45e64f372bce43f370fc8c0234c980f6dc33b43f4d0809702284c96cbfd0ef7ed9124c16b4fa0c120040200400109db1337c4aeff3f80a38e7248ebe9ff1f7dee484bc8f09b1fce004541b0ab498ea047fd404112d1dbdb39fbfe920d8a5564cfd9024982d69f4224401c0000022eb54640890c2a9c5e869fc1000b7e8867684a0da9f6d0049f9530cc79194f7e0c0439d7b0edd166c8bf1e002bda3d1a060f73bbbf9c33bd7e3f064c1c7410082002002e6528e72d4db8c0d2a321fe4c042b17bb989675071fdd002af233e3bf44e41fd5808333d8bc0e1b70fbf80008ef14fcb1e1994432fd7a2597e220a4c16a8c810708004005c5a6bb5f210b855fe8b4fc10804ec9b04de1fb81ff4a009eb32f765940434fa320059241f0b0a1126bf3640113d5bb33dd0f8bf9540074e6feaef682c8660089aefb23819307db654afaf50c85e57ab623e9531e761ffc815a907b6c77b31fee662b080bf2509260b5a7d34100424001000005a384e20f21454147a7d740837dcf0d32e4b203fa1400a3645cbdc49eddfd5c00728ea08d34b4443ffd100fc07edaf0a42aeff3d002e71e5f865bcf90ceda7c539fa1829305ab52004d000008176938786aa95cf5cfa48bffa0818322c5b8ea2fc7fb5100607509864fe8b0bfda011647065e12cc9b71faf009205260b5019304124c16b4fa0089a070e0c30d1010830",
      "00205d7fffc37feafd3d0d511524beaba433e42540c03bf2c4840c497e676a0e4c556c35f6fffec9289090c5dd7ca7d9149826ffed175fb22fb658eb94d1b12c3510d0200ff8dbff3fcd4abfdd4b1a38dae01c267ccc19408652aa0200421b31",
      "001c3172361c5e0ffff3e134666f7da521a3116a2199e8bae98ef90ccc6d4f5da29145c14e805287affa90113d0d511550014f43",
      "00293fee4f4354451fe189d73d8db3487a47e59668527c20a5ee4b3eb1ad79e017f38b87cebd5ff6885eb08ddfc05a5bd727e18c7bccc1edac3cf96d7e7d0afc9e48",
      "002182B72F00878190A929E86A88B1BFD51931DD5E3826BFFB8B343AFB6741300FE5C32BA3BBDE67193178F6B7A4C55D7F92AC0F0491F9F41C7FF5C0EAAC660564EC7395A3852EF80497084BD802D70CEE59F40525C774EA036AC088DCD87C1F53E812C941115332BD8AD799163C0A9C81C982D69E83F981552BFD669E5A5F2D16C74FA63E918F8F0F604A90F3A1C12E23BBAD93CB049305AD3E920B77A512C50E4CA153321E342E7B46E1E301A8A4E59014982D4A049305AD3E880A4C16A881C14BB827A1AA228030783694F4354452130736F1056E8D0514C69E86A88BA29495BE95B96477A30E24A8B850AE4165F37B57429305AA82946C0941100F20C350AD1F10EFBC12F596922EAF9F5EE842DA5CB6A3207E642A0903C5B0D861EC9F40E00D471F4F4B0459E06117072B5EC72EDD6E8A402049001556AA6ADA64EBB41999C5CFA6548032950FA47C8D7026AC31E9E66DAADAA6D910CD5064C14484DC189C9FC95A23DF37185E92AD35592B8E0E5EFD26B49962279A2B778687051070CE2BA22974CB3B0110112EADFF2FCBC29B8D28C75BF56B3AB2B823CFE3F939D142D716370A601AC78E4A5FBCF10807A140AB40F08C4344158FB9404A4FC4F5DF57B57F128327A46AD6AD1BBBBF0FF8413612022A903960AA847A131787C4361D860C2B5BD507FAF45ECD0BAF9DC89D2817EC92DDCFDD89C28532C350AD3DEA25C2100228D4347F980931D2B8DFE2BADCA94D615CA0CBEBB2A091E58BE2B51215BE98AE235E1515E348C6CA2AF567FF1CB6449305AD3E988951821392E876D923D4C0E367116255FE6BBB512CBBCD0D5A0FEA65F650A52B5E633EA40E8458E51282F562201A529BB46DA51247A862897C9696F1FB10774216AB8E3CF15D715DB2E514268A9DA38D309EF0FA0C539C893308E63BD5AFBA44436666541EA275627FC4448A5CC2AFBCB2A88869C4D09800949BB79F0573CD5AA7DE4F46A0"
  })
  public void uperToXer(final String uper) {
    // Normalize case
    String xer = codec.uperToXer(HexFormat.of().parseHex(uper));
    assertThat("xer is null", xer, notNullValue());
    log.info("xer: {}", xer);
    byte[] roundTripUper = codec.xerToUper(xer);
    String roundTripUperHex = hexFormat.formatHex(roundTripUper);
    log.info("round trip uper: {}", roundTripUper);
    assertThat("round trip hex differs", roundTripUperHex, equalToIgnoringCase(uper));
  }

  @ParameterizedTest
  @MethodSource("convertData")
  public void testConvertGeneral(final String pdu, final String inputHex, final String expectXer) {
    byte[] inputBytes = hexFormat.parseHex(inputHex);
    byte[] result = codec.convertGeneral(inputBytes, pdu, "uper", "xer");
    assertThat("result is null", result, notNullValue());
    if (expectXer != null) {
      String xer = new String(result, StandardCharsets.UTF_8);
      assertThat(xer, equalTo(expectXer));
    }
  }

  @Test
  public void invalidPduError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, "BadPDU", "uper", "xer");
        }
    );
  }

  @Test
  public void invalidInputEncodingError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, VEHICLE_EVENT_FLAGS_PDU, "badinputencoding", "xer");
        }
    );
  }

  @Test
  public void invalidOutputEncodingError() {
    byte[] inputBytes = hexFormat.parseHex(VEHICLE_EVENT_FLAGS_UPER);
    assertThrows(
        RuntimeException.class,
        () -> {
          codec.convertGeneral(inputBytes, VEHICLE_EVENT_FLAGS_PDU, "uper", "badoutputencoding");
        }
    );
  }

  private static Stream<Arguments> convertData() {
    return Stream.of(
        Arguments.of(VEHICLE_EVENT_FLAGS_PDU, VEHICLE_EVENT_FLAGS_UPER, VEHICLE_EVENT_FLAGS_XER),
        Arguments.of(SSM_PDU, SSM_UPER, null)
    );
  }

  private static final String VEHICLE_EVENT_FLAGS_UPER = "8740FE";
  private static final String VEHICLE_EVENT_FLAGS_XER = "<VehicleEventFlags>10000001111111</VehicleEventFlags>";
  private static final String VEHICLE_EVENT_FLAGS_PDU = "VehicleEventFlags";

  private static final String SSM_UPER = "65e539dc93b843af683249404f9e0fc6b04fd122cf2ce89941dc1ab4d3d288394b59be74b1c04cfdee07bc9868311d2c1caa51f03dc764f993d0d511779e9ef22be1121c093e1af96b1d141a1ba967c329e47cf884b8beb3268e790f72270ca44c2519740d31d85f3a0e91a6bca5145e560e920d281085568f931b7067cc9e86a88b8f5957847ac6b4fa9d1b07fc2cd2e6a91f327a1aa229d5e21e478318d630a67bd8ffd0ce05537cf12267f5df5fc2794ff0804001a150fe00a679ce1c7934b4a6891be64f435445f9206bc5ff8e09b516244086367d14aef993d0d51175f310f233515cb0082fa6f907e861f0be64f435445412a1bcae64260fe3aa2470ea9ccf666f993d0d5114a9c22f0ba4406960b0cc40a0bd244e285bce95385017cefcbbac8d3070a7819776806012dd1dd66d719a089f3d143c9b843e001e4439710aacb223e510b9397770640941fcec2f9fc6a4cba57da12f160011a4d7c2fc0f7f10429d2be7409299a6cd9053c256161af6572d28cf07d9bdc620";
  private static final String SSM_PDU = "SignalStatusMessage";

}
