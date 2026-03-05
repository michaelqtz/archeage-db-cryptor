package aaemu.tools.service;

import java.util.Scanner;

import aaemu.tools.config.ConfigProperties;

/**
 * @author Shannon
 */
public interface AesKeysBitService {

    void calculateKeysBit(ConfigProperties properties, Scanner scanner) throws Exception;
}
