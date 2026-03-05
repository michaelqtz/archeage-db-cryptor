package aaemu.tools.util;

import static java.util.Arrays.copyOf;

import java.nio.ByteBuffer;

import lombok.experimental.UtilityClass;

/**
 * @author Shannon
 */
@UtilityClass
public class ByteUtils {

    public static void copyData(ByteBuffer from, byte[] dst) {     // Not incremented pos
        from.get(dst);
        from.position(from.position() - dst.length);
    }

    public static void readData(ByteBuffer from, byte[] dst) {     // Incremented pos
        int length = dst.length;

        from.get(dst, 0, length);
    }

    public static void writeData(ByteBuffer buffer, byte[] src) {  // Incremented pos
        buffer.put(src, 0, src.length);
    }

    public static byte[] copyArray(byte[] currentBlock) {
        return copyOf(currentBlock, currentBlock.length);
    }
}
