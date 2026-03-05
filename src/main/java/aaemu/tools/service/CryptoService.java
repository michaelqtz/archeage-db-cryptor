package aaemu.tools.service;

import java.nio.ByteBuffer;

import aaemu.tools.config.ConfigProperties;

/**
 * @author Shannon
 */
public interface CryptoService {

    ByteBuffer decrypt(ConfigProperties properties) throws Exception;

    ByteBuffer encrypt(ConfigProperties properties) throws Exception;
}
