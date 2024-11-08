package j2735ffm;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static j2735_2024_MessageFrame.MessageFrame_h.*;
import j2735_2024_MessageFrame.*;

public class MessageFrameCodec {

    public static byte[] xerToUper(String xer) {
        byte[] xmlBytes = xer.getBytes(StandardCharsets.UTF_8);
        ByteBuffer bb = ByteBuffer.wrap(xmlBytes);
        MemorySegment heapXml = MemorySegment.ofBuffer(bb);

        int outputBufferSize = 16384;
        byte[] outputArray = new byte[outputBufferSize];
        MemorySegment heapOutput = MemorySegment.ofArray(outputArray);

        try (var arena = Arena.ofConfined()) {
            MemorySegment optCodecParameters = asn_codec_ctx_t.allocate(arena);
            asn_codec_ctx_t.max_stack_size(optCodecParameters, 2048);
            MemorySegment typeToDecode = asn_DEF_MessageFrame();

            // The result Message Frame
            MemorySegment messageFrame = MessageFrame_t.allocate(arena);

            // Pointer to the result Message Frame
            MemorySegment structurePtr = arena.allocate(8);
            structurePtr.set(ValueLayout.JAVA_LONG, 0, messageFrame.address());

            long bufferSize = 65536;
            MemorySegment buffer = arena.allocate(bufferSize);
            buffer.copyFrom(heapXml);
            MemorySegment er = asn_decode(arena, optCodecParameters, ATS_BASIC_XER(), typeToDecode, structurePtr,
                    buffer, bufferSize);
            long retCode = asn_dec_rval_t.code(er);
            long consumed = asn_dec_rval_t.consumed(er);
            System.out.println("Ret code: " + retCode + ", Consumed: " + consumed);

//            long messagePointer = structurePtr.get(ValueLayout.JAVA_LONG, 0);
//            System.out.println("MessageFrame pointer: " + messagePointer);

            long messageId = MessageFrame_t.messageId(messageFrame);
            System.out.println("Message ID: " + messageId);

            int printResult = asn_fprint(stdout(), typeToDecode, messageFrame);
            System.out.println("Print result: " + printResult);


            MemorySegment outputBuffer = arena.allocate(bufferSize);

            MemorySegment erEnc = asn_encode_to_buffer(arena, optCodecParameters, ATS_UNALIGNED_BASIC_PER(),
                    typeToDecode, messageFrame, outputBuffer, outputBufferSize);
            long encoded = asn_enc_rval_t.encoded(erEnc);
            if (encoded > -1) {
                System.out.printf("Encoded %s bytes", encoded);
                heapOutput.copyFrom(outputBuffer);
                return Arrays.copyOfRange(outputArray, 0, (int)encoded);
            } else {
                System.out.println("Error");
                throw new RuntimeException("Error encoding");
                // Check the error info
                // fprintf(stderr, ”Cannot encode %s: %s\n”, er.failed_typeି >name, strerror(errno))
                // Need c function to expose errno macro?
            }

        }
    }

    public static byte[] jerToUper(String jer) {
        return null;
    }

    public static String uperToXer(byte[] uper) {
        return null;
    }

    public static String uperToJer(byte[] uper) {
        return null;
    }

    public static String xerToJer(String xer) {
        return null;
    }

    public static String jerToXer(String jer) {
        return null;
    }
}
