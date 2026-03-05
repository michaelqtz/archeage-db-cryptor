package aaemu.tools.service.impl;

import static aaemu.tools.util.ByteUtils.copyArray;
import static aaemu.tools.util.ByteUtils.copyData;
import static aaemu.tools.util.ByteUtils.writeData;

import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import aaemu.tools.config.AesStepConfig;
import aaemu.tools.service.AesService;

/**
 * @author Shannon
 */
public class AesServiceImpl implements AesService {
    private Cipher cipher;

    @Override
    public void setDecryptKey(byte[] key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
    }

    @Override
    public void setEncryptKey(byte[] key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
    }

    @Override
    public void decrypt(AesStepConfig step, ByteBuffer buffer, int offset, int skipLength) throws Exception {
        setDecryptKey(step.getAesKey());
        buffer.position(offset);

        byte[] iv = step.getIv();
        final byte[] currentBlock = new byte[16];
        final int endOffset = buffer.capacity() - 15 - skipLength;

        while (buffer.position() < endOffset) {
            copyData(buffer, currentBlock);

            byte[] decryptedBlock = decrypt(currentBlock, iv);

            writeData(buffer, decryptedBlock);

            iv = decryptedBlock;
        }
    }

    private byte[] decrypt(byte[] bytes, byte[] iv) throws Exception {
        xor(bytes, iv);

        return cipher.doFinal(bytes);
    }

    @Override
    public void encrypt(AesStepConfig step, ByteBuffer buffer, int offset, int skipLength) throws Exception {
        setEncryptKey(step.getAesKey());
        buffer.position(offset);

        byte[] iv = step.getIv();
        final byte[] currentBlock = new byte[16];
        byte[] nextIv;
        byte[] encryptedBlock;
        final int endOffset = buffer.capacity() - 15 - skipLength;

        while (buffer.position() < endOffset) {
            copyData(buffer, currentBlock);

            nextIv = copyArray(currentBlock);

            encryptedBlock = encrypt(currentBlock, iv);

            writeData(buffer, encryptedBlock);

            iv = copyArray(nextIv);
        }
    }

    private byte[] encrypt(byte[] bytes, byte[] iv) throws Exception {
        byte[] result = cipher.doFinal(bytes);
        xor(result, iv);

        return result;
    }

    private void xor(byte[] bytes, byte[] key) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ key[i]);
        }
    }
}
