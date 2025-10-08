package j2735_2024_MessageFrame;

import java.lang.foreign.*;

/**
 * Non-functional stub to make IDE's happy.
 * Sources files in the package are overwritten by the jextract output in Docker.
 */
public class MessageFrame_h {

    MessageFrame_h() {
    }

    private static final int ATS_CANONICAL_XER = (int)13L;
    public static int ATS_CANONICAL_XER() {
        return ATS_CANONICAL_XER;
    }


    private static final int ATS_UNALIGNED_BASIC_PER = (int)8L;
    public static int ATS_UNALIGNED_BASIC_PER() {
        return ATS_UNALIGNED_BASIC_PER;
    }


    public static MemorySegment asn_encode_to_buffer(SegmentAllocator allocator, MemorySegment opt_codec_parameters, int x1, MemorySegment type_to_encode, MemorySegment structure_to_encode, MemorySegment buffer, long buffer_size) {
        return null;
    }


    public static MemorySegment asn_decode(SegmentAllocator allocator, MemorySegment opt_codec_parameters, int x1, MemorySegment type_to_decode, MemorySegment structure_ptr, MemorySegment buffer, long size) {
        return null;
    }


    public static int asn_fprint(MemorySegment stream, MemorySegment td, MemorySegment struct_ptr) {
        return 0;
    }



    public static MemorySegment stdout() {
        return null;
    }


    public static MemorySegment asn_DEF_MessageFrame() {
        return null;
    }
}

