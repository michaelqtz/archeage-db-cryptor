package aaemu.tools.service.impl;

import static aaemu.tools.util.ConstantsUtils.DB_SQLITE;
import static aaemu.tools.util.ZipUtils.isValidZip;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Scanner;

import aaemu.tools.config.ConfigProperties;
import aaemu.tools.enums.AesKeyBit;
import aaemu.tools.service.AesKeysBitService;
import aaemu.tools.service.CryptoService;
import aaemu.tools.service.FileService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;

/**
 * @author Shannon
 */
@RequiredArgsConstructor
public class AesKeysBitServiceImpl implements AesKeysBitService {
    private final FileService fileService;
    private final CryptoService cryptoService;

    @Override
    public void calculateKeysBit(ConfigProperties properties, Scanner scanner) throws Exception {
        System.out.println("\n=== Analysis of AES key bit combinations ===");
        System.out.println("┌─────────────┬─────────────┬──────────┐");
        System.out.println("│ AES-1 (bit) │ AES-2 (bit) │  Result  │");
        System.out.println("├─────────────┼─────────────┼──────────┤");

        boolean found = false;
        int firstKeyBit = 0;
        int secondKeyBit = 0;

        for (AesKeyBit first : AesKeyBit.values()) {
            for (AesKeyBit second : AesKeyBit.values()) {
                properties.getAesFirstStage().updateKey(first.getBit());
                properties.getAesSecondStage().updateKey(second.getBit());
                String statusSymbol;

                try {
                    ByteBuffer buffer = cryptoService.decrypt(properties);

                    if (isValidZip(buffer)) {
                        statusSymbol = "+";
                        found = true;
                    } else {
                        statusSymbol = "-";
                    }
                } catch (Exception exception) {
                    statusSymbol = "!";
                }

                System.out.printf("│     %3d     │     %3d     │    %s     │%n", first.getBit(), second.getBit(), statusSymbol);

                if (found) {
                    firstKeyBit = first.getBit();
                    secondKeyBit = second.getBit();

                    break;
                }
            }

            if (found) {
                break;
            }
        }

        System.out.println("└─────────────┴─────────────┴──────────┘");

        if (!found) {
            System.out.println("Bit length of AES keys hasn't been detected");
            System.out.printf("Check config constants and %s%n", DB_SQLITE);

            return;
        }
        System.out.println("\n=== Overwrite config file ===");
        System.out.print(" Enter y/n: ");

        boolean overwrite = scanner.next().equalsIgnoreCase("y");

        if (overwrite) {
            overwriteJsonConfig(properties, firstKeyBit, secondKeyBit);
        }
    }

    private void overwriteJsonConfig(ConfigProperties properties, int firstKeySize, int secondKeySize) throws IOException {
        ObjectNode jsonConfig = fileService.readJson(properties.getPath());
        ObjectNode aesStage = (ObjectNode) jsonConfig.get("aes_first_stage");
        aesStage.put("key_bit", firstKeySize);
        aesStage = (ObjectNode) jsonConfig.get("aes_second_stage");
        aesStage.put("key_bit", secondKeySize);

        fileService.writeFile(properties.getPath().toString(), jsonConfig.toPrettyString().getBytes());
    }

}
