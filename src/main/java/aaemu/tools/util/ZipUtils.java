package aaemu.tools.util;

import static aaemu.tools.util.ByteUtils.copyData;
import static aaemu.tools.util.ByteUtils.writeData;
import static aaemu.tools.util.ConstantsUtils.GAME0_NAME_LFH;
import static aaemu.tools.util.ConstantsUtils.ZIP_BITFLAG;
import static aaemu.tools.util.ConstantsUtils.ZIP_COMPRESSION_METHOD;
import static aaemu.tools.util.ConstantsUtils.ZIP_HEADER;
import static aaemu.tools.util.ConstantsUtils.ZIP_OVERWRITE_HEADER_SIZE;
import static aaemu.tools.util.ConstantsUtils.ZIP_VERSION;

import java.nio.ByteBuffer;
import java.util.Arrays;

import lombok.experimental.UtilityClass;

/**
 * @author Shannon
 */
@UtilityClass
public class ZipUtils {

    public static void overwriteZipHeader(ByteBuffer buffer) {
        if (isValidZipHeader(buffer)) {
            return;
        }

        ByteBuffer zipHeaderBuffer = ByteBuffer.allocate(ZIP_OVERWRITE_HEADER_SIZE);
        zipHeaderBuffer.put(ZIP_HEADER);
        zipHeaderBuffer.put(ZIP_VERSION);
        zipHeaderBuffer.put(ZIP_BITFLAG);
        zipHeaderBuffer.put(ZIP_COMPRESSION_METHOD);

        writeData(buffer, zipHeaderBuffer.array());

        System.out.println("ZIP header overwritten");
    }

    public static boolean isValidZipHeader(ByteBuffer buffer) {
        buffer.position(0);
        byte[] header = new byte[ZIP_HEADER.length];
        copyData(buffer, header);

        return Arrays.equals(header, ZIP_HEADER);
    }

    public static boolean isValidZip(ByteBuffer buffer) {
        if (buffer.capacity() <= 30 + GAME0_NAME_LFH.length) {
            return false;
        }
        if (!isValidZipHeader(buffer)) {
            return false;
        }

        buffer.position(30);
        byte[] copy = new byte[GAME0_NAME_LFH.length];
        copyData(buffer, copy);

        return Arrays.equals(copy, GAME0_NAME_LFH);
    }
}
