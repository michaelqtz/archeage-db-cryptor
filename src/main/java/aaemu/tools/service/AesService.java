package aaemu.tools.service;

import java.nio.ByteBuffer;

import aaemu.tools.config.AesStepConfig;

/**
 * @author Shannon
 */
public interface AesService {

    void setDecryptKey(byte[] key) throws Exception;

    void setEncryptKey(byte[] key) throws Exception;

    void decrypt(AesStepConfig step, ByteBuffer buffer, int offset, int skipLength) throws Exception;

    void encrypt(AesStepConfig step, ByteBuffer buffer, int offset, int skipLength) throws Exception;
}
