package aaemu.tools.service.impl;

import static aaemu.tools.util.ByteUtils.copyArray;
import static aaemu.tools.util.ByteUtils.copyData;
import static aaemu.tools.util.ByteUtils.readData;
import static aaemu.tools.util.ByteUtils.writeData;
import static aaemu.tools.util.ConstantsUtils.DB_SQLITE;
import static aaemu.tools.util.ConstantsUtils.DB_ZIP;
import static aaemu.tools.util.HexUtils.toBigInt;
import static aaemu.tools.util.HexUtils.toByteArray;
import static aaemu.tools.util.HexUtils.toHex;
import static aaemu.tools.util.ZipUtils.overwriteZipHeader;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import aaemu.tools.config.AesStepConfig;
import aaemu.tools.config.ConfigProperties;
import aaemu.tools.config.RsaStepConfig;
import aaemu.tools.enums.CipherVersion;
import aaemu.tools.service.AesService;
import aaemu.tools.service.CryptoService;
import aaemu.tools.service.FileService;
import lombok.RequiredArgsConstructor;

/**
 * @author Shannon
 */
@RequiredArgsConstructor
public class CryptoServiceImpl implements CryptoService {
    private final FileService fileService;
    private final AesService aesService;

    @Override
    public ByteBuffer decrypt(ConfigProperties properties) throws Exception {
        CipherVersion cipherVersion = properties.getCipherVersion();
        ByteBuffer buffer = fileService.readFile(DB_SQLITE);

        switch (cipherVersion) {
            case _2 -> decryptV2(properties, buffer);
            case _3 -> decryptV3(properties, buffer);
            case _4 -> decryptV4(properties, buffer);
        }

        return buffer;
    }

    @Override
    public ByteBuffer encrypt(ConfigProperties properties) throws Exception {
        CipherVersion cipherVersion = properties.getCipherVersion();
        ByteBuffer buffer = fileService.readFile(DB_ZIP);

        switch (cipherVersion) {
            case _2 -> encryptV2(properties, buffer);
            case _3 -> encryptV3(properties, buffer);
            case _4 -> encryptV4(properties, buffer);
        }

        return buffer;
    }

    private void decryptV2(ConfigProperties properties, ByteBuffer buffer) throws Exception {
        decryptAesFirstStage(properties, buffer);
        decryptAesSecondStage(properties, buffer, 0);
    }

    private void encryptV2(ConfigProperties properties, ByteBuffer buffer) throws Exception {
        encryptAesFirstStage(properties, buffer, 0);
        encryptAesSecondStage(properties, buffer);
    }

    private void decryptV3(ConfigProperties properties, ByteBuffer buffer) throws Exception {
        int totalCipherDataLength = properties.getRsa().getTotalCipherDataLength();

        decryptAesFirstStage(properties, buffer);
        decryptRsa(properties, buffer);
        decryptAesSecondStage(properties, buffer, totalCipherDataLength);

        overwriteZipHeader(buffer);
    }

    private void encryptV3(ConfigProperties properties, ByteBuffer buffer) {
        // TODO
    }

    private void decryptV4(ConfigProperties properties, ByteBuffer buffer) throws Exception {
        int totalCipherDataLength = properties.getRsa().getTotalCipherDataLength();

        decryptAesFirstStage(properties, buffer);
        swapBlocks(properties, buffer);
        decryptAesSecondStage(properties, buffer, totalCipherDataLength);
    }

    private void encryptV4(ConfigProperties properties, ByteBuffer buffer) throws Exception {
        int totalCipherDataLength = properties.getRsa().getTotalCipherDataLength();

        encryptAesFirstStage(properties, buffer, totalCipherDataLength);
        swapBlocks(properties, buffer);
        encryptAesSecondStage(properties, buffer);
    }

    private void decryptRsa(ConfigProperties properties, ByteBuffer buffer) {
        RsaStepConfig rsa = properties.getRsa();

        List<byte[]> rsaBlocks = readRsaBlocks(rsa, buffer);
        List<byte[]> decryptedBlocks = decryptRsaBlocks(properties, rsaBlocks);
        List<byte[]> originalBlocks = new ArrayList<>(decryptedBlocks.size());

        List<Integer> blocksOffsets = rsa.getBlocksOffsets();

        for (int i = 0; i < decryptedBlocks.size(); i++) {  // Write decrypted data to offset
            byte[] block = decryptedBlocks.get(i);
            buffer.position(blocksOffsets.get(i));

            byte[] originalBlock = new byte[block.length];
            copyData(buffer, originalBlock);
            originalBlocks.add(originalBlock);

            writeData(buffer, block);
        }

        int lengthDiff = rsa.getCLength() - rsa.getMLength();
        int offset = buffer.capacity() - rsa.getTotalCipherDataLength();

        for (byte[] originalBlock : originalBlocks) {       // Write original data instead of encrypted data
            buffer.position(offset);

            writeData(buffer, originalBlock);

            offset = buffer.position() + lengthDiff;
        }
    }

    private void swapBlocks(ConfigProperties properties, ByteBuffer buffer) {
        RsaStepConfig rsa = properties.getRsa();

        List<byte[]> rsaBlocks = readRsaBlocks(rsa, buffer);
        List<byte[]> originalBlocks = new ArrayList<>(rsaBlocks.size());

        int lengthDiff = rsa.getCLength() - rsa.getMLength();
        List<Integer> blocksOffsets = rsa.getBlocksOffsets();

        for (int i = 0; i < rsaBlocks.size(); i++) {        // Write decrypted data to offset
            byte[] block = rsaBlocks.get(i);
            buffer.position(blocksOffsets.get(i));

            byte[] decryptedBlock = Arrays.copyOf(block, block.length - lengthDiff);

            byte[] originalBlock = new byte[block.length - lengthDiff];
            copyData(buffer, originalBlock);
            originalBlocks.add(originalBlock);

            writeData(buffer, decryptedBlock);
        }

        int offset = buffer.capacity() - rsa.getTotalCipherDataLength();

        for (byte[] originalBlock : originalBlocks) {       // Write original data instead of encrypted data
            buffer.position(offset);

            writeData(buffer, originalBlock);

            offset = buffer.position() + lengthDiff;
        }
    }

    public void decryptAesFirstStage(ConfigProperties properties, ByteBuffer buffer) throws Exception {
        AesStepConfig step = properties.getAesFirstStage();
        int length = buffer.capacity();
        int offset = calculateOffset(length);

        aesService.decrypt(step, buffer, offset, 0);
    }

    public void decryptAesSecondStage(ConfigProperties properties, ByteBuffer buffer, int skipLength) throws Exception {
        AesStepConfig step = properties.getAesSecondStage();
        int offset = 0;

        aesService.decrypt(step, buffer, offset, skipLength);
    }

    public void encryptAesFirstStage(ConfigProperties properties, ByteBuffer buffer, int skipLength) throws Exception {
        AesStepConfig step = properties.getAesSecondStage();
        int offset = 0;

        aesService.encrypt(step, buffer, offset, skipLength);
    }

    public void encryptAesSecondStage(ConfigProperties properties, ByteBuffer buffer) throws Exception {
        AesStepConfig step = properties.getAesFirstStage();
        int length = buffer.capacity();
        int offset = calculateOffset(length);

        aesService.encrypt(step, buffer, offset, 0);
    }

    private static List<byte[]> readRsaBlocks(RsaStepConfig rsa, ByteBuffer buffer) {
        int cipherBlockSize = rsa.getCLength();
        int parts = rsa.getParts();
        int totalCipherSize = rsa.getTotalCipherDataLength();

        int offset = buffer.capacity() - totalCipherSize;
        buffer.position(offset);

        byte[] currentBlock = new byte[cipherBlockSize];
        List<byte[]> ciphers = new ArrayList<>(parts);

        for (int i = 0; i < parts; i++) {
            readData(buffer, currentBlock);

            ciphers.add(copyArray(currentBlock));
        }

        return ciphers;
    }

    private static List<byte[]> decryptRsaBlocks(ConfigProperties properties, List<byte[]> blocks) {
        RsaStepConfig rsa = properties.getRsa();

        List<byte[]> decryptedBytes = new ArrayList<>(blocks.size());

        for (byte[] cipherBytes : blocks) {
            BigInteger c = toBigInt(cipherBytes);
            BigInteger d = rsa.getD();
            BigInteger n = rsa.getN();

            BigInteger m = c.modPow(d, n);

            String mHex = toHex(m);
            byte[] bytes = toByteArray(mHex);

            decryptedBytes.add(bytes);
        }

        return decryptedBytes;
    }

    private static int calculateOffset(int length) {
        return 16 - (length % 16);
    }
}
