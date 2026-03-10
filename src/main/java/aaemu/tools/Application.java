package aaemu.tools;

import static aaemu.tools.enums.CipherMode.DECRYPT;
import static aaemu.tools.enums.CipherMode.ENCRYPT;
import static aaemu.tools.enums.CipherVersion._2;
import static aaemu.tools.enums.CipherVersion._4;
import static aaemu.tools.util.ConstantsUtils.DB_SQLITE_NEW;
import static aaemu.tools.util.ConstantsUtils.DB_ZIP;
import static aaemu.tools.util.HexUtils.toHex;
import static aaemu.tools.util.ZipUtils.isValidZip;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import aaemu.tools.config.AesStepConfig;
import aaemu.tools.config.ConfigProperties;
import aaemu.tools.config.RsaStepConfig;
import aaemu.tools.enums.CipherMode;
import aaemu.tools.enums.CipherVersion;
import aaemu.tools.service.AesKeysBitService;
import aaemu.tools.service.AesService;
import aaemu.tools.service.CryptoService;
import aaemu.tools.service.FileService;
import aaemu.tools.service.SqliteService;
import aaemu.tools.service.impl.AesKeysBitServiceImpl;
import aaemu.tools.service.impl.AesServiceImpl;
import aaemu.tools.service.impl.CryptoServiceImpl;
import aaemu.tools.service.impl.FileServiceImpl;
import aaemu.tools.service.impl.SqliteServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * @author Shannon
 */
public class Application {
    private static final ObjectMapper objectMapper = JsonMapper.builder().build();
    private static final FileService fileService = new FileServiceImpl(objectMapper);
    private static final AesService aesService = new AesServiceImpl();
    private static final CryptoService cryptoService = new CryptoServiceImpl(fileService, aesService);
    private static final AesKeysBitService aesKeysBitService = new AesKeysBitServiceImpl(fileService, cryptoService);
    private static final RsaKeyFormater rsaKeyFormater = new RsaKeyFormater(fileService);
    private static final SqliteService SQLITE_SERVICE = new SqliteServiceImpl(fileService);

    public static void main(String[] args) throws IOException {
        try {
            printFunctionSelection();

            Scanner scanner = new Scanner(System.in);
            int selectedFunction = scanner.nextInt();

            switch (selectedFunction) {
                case 1 -> cipher(scanner);
                case 2 -> calculateKeysBit(scanner);
                case 3 -> rsaKeyFormater.formatRsaKeys(4);
                case 4 -> SQLITE_SERVICE.createSchema();
            }

            System.out.println("\nPress any to exit");

            System.in.read(new byte[2]);
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("Press any to exit");

            System.in.read(new byte[2]);
            System.exit(0);
        }
    }

    private static void cipher(Scanner scanner) throws Exception {
        List<ConfigProperties> configProperties = fileService.readConfigPropertiesList();

        if (configProperties.isEmpty()) {
            System.out.println("No config properties found");

            return;
        }

        printConfigsInfo(configProperties);
        ConfigProperties properties = selectConfig(configProperties, scanner);
        CipherMode cipherMode = selectCipherMode(properties, scanner);

        if (cipherMode.isDecrypt()) {
            properties.setCipherMode(DECRYPT);
        } else {
            properties.setCipherMode(ENCRYPT);
        }

        start(properties);
    }

    private static void start(ConfigProperties properties) throws Exception {
        printConfigInfo(properties);
        printAesKeysInfo(properties);
        printRsaKeysInfo(properties);

        if (properties.getCipherMode().isDecrypt()) {
            ByteBuffer buffer = cryptoService.decrypt(properties);

            if (isValidZip(buffer)) {
                fileService.writeFile(DB_ZIP, buffer);
            } else {
                System.err.println("Invalid config or db file");
            }
        } else {
            ByteBuffer buffer = cryptoService.encrypt(properties);
            fileService.writeFile(DB_SQLITE_NEW, buffer);
        }
    }

    private static void calculateKeysBit(Scanner scanner) throws Exception {
        List<ConfigProperties> configProperties = fileService.readConfigPropertiesList();

        if (configProperties.isEmpty()) {
            System.out.println("No config properties found");

            return;
        }

        printConfigsInfo(configProperties);
        ConfigProperties properties = selectConfig(configProperties, scanner);
        properties.setCipherMode(DECRYPT);

        aesKeysBitService.calculateKeysBit(properties, scanner);
    }

    private static void printFunctionSelection() {
        System.out.println("=== Select function ===");
        System.out.println("┌───┬────────────────────────┐");
        System.out.println("│ № │         Name           │");
        System.out.println("├───┼────────────────────────┤");
        System.out.println("│ 1 │ Cipher                 │");
        System.out.println("│ 2 │ Calculate AES keys bit │");
        System.out.println("│ 3 │ Format RSA keys        │");
        System.out.println("│ 4 │ Create DB schema       │");
        System.out.println("└───┴────────────────────────┘");
        System.out.print(" Enter №: ");
    }

    private static void printConfigsInfo(List<ConfigProperties> configProperties) {
        System.out.printf("%n=== Select version ===%n");
        System.out.println("┌─────┬─────────────────────────┐");
        System.out.println("│  №  │          Name           │");
        System.out.println("├─────┼─────────────────────────┤");

        for (int i = 0; i < configProperties.size(); i++) {
            ConfigProperties config = configProperties.get(i);

            String str = "│ %2d  │ %s v%s".formatted(i + 1, config.getProvider(), config.getVersion());

            System.out.printf(str + " ".repeat(32 - str.length()) + "│%n");
        }

        System.out.println("└─────┴─────────────────────────┘");
    }

    private static ConfigProperties selectConfig(List<ConfigProperties> configProperties, Scanner scanner) {
        System.out.print(" Enter №: ");

        int selectedNumber = scanner.nextInt() - 1;

        return configProperties.get(selectedNumber);
    }

    private static CipherMode selectCipherMode(ConfigProperties properties, Scanner scanner) throws Exception {
        System.out.printf("%n=== Select cipher mode ===%n");
        System.out.println("┌─────┬─────────┐");
        System.out.println("│  №  │  Mode   │");
        System.out.println("├─────┼─────────┤");
        System.out.println("│  1  │ DECRYPT │");

        CipherVersion cipherVersion = properties.getCipherVersion();
        boolean canEncrypt = false;

        if (cipherVersion.equals(_2) || cipherVersion.equals(_4)) {
            canEncrypt = true;

            System.out.println("│  2  │ ENCRYPT │");
        }

        System.out.println("└─────┴─────────┘");
        System.out.print(" Enter №: ");

        int selectedNumber = scanner.nextInt();

        if (canEncrypt) {
            return switch (selectedNumber) {
                case 1 -> DECRYPT;
                case 2 -> ENCRYPT;
                default -> throw new Exception("Invalid cipher mode");
            };
        }

        if (1 == selectedNumber) {
            return DECRYPT;
        } else {
            throw new Exception("Invalid cipher mode");
        }
    }

    private static void printConfigInfo(ConfigProperties properties) {
        String provider = properties.getProvider();
        String version = properties.getVersion();
        CipherMode cipherMode = properties.getCipherMode();

        System.out.printf("%n=== %s v%s (%s) ===%n", provider, version, cipherMode);
    }

    private static void printAesKeysInfo(ConfigProperties properties) {
        System.out.printf("%n==== AES ====%n");

        AesStepConfig aesStep = properties.getAesFirstStage();
        int keyBit = aesStep.getKeyBit();
        String key = toHex(aesStep.getAesKey());
        String iv = toHex(aesStep.getIv());

        printAesInfo(keyBit, key, iv);

        aesStep = properties.getAesSecondStage();
        keyBit = aesStep.getKeyBit();
        key = toHex(aesStep.getAesKey());
        iv = toHex(aesStep.getIv());

        printAesInfo(keyBit, key, iv);
    }

    private static void printAesInfo(int bit, String key, String iv) {
        System.out.println("┌────────────┬──────────────────────────────────────────────────────────────────┐");
        System.out.println("│    Type    │                              Value                               │");
        System.out.println("├────────────┼──────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Key " + "(%3d)".formatted(bit) + " │ " + key + " ".repeat(65 - key.length()) + "│");
        System.out.println("│  IV        │ " + iv + " ".repeat(65 - iv.length()) + "│");
        System.out.printf("└────────────┴──────────────────────────────────────────────────────────────────┘%n");
    }

    private static void printRsaKeysInfo(ConfigProperties properties) {
        RsaStepConfig rsaStep = properties.getRsa();

        if (Objects.isNull(rsaStep)) {
            return;
        }

        System.out.printf("%n==== RSA ====%n");

        BigInteger privateExponent = rsaStep.getD();
        BigInteger modulus = rsaStep.getN();
        String parts = "%d".formatted(rsaStep.getParts());
        String cLength = "%3d".formatted(rsaStep.getCLength());
        String mLength = "%3d".formatted(rsaStep.getMLength());

        System.out.println("┌────────────┬──────────────────────────────────────────────────────────────────┐");
        System.out.println("│    Type    │                              Value                               │");
        System.out.println("├────────────┼──────────────────────────────────────────────────────────────────┤");
        System.out.println("│ d          │ " + privateExponent);
        System.out.println("│ n          │ " + modulus);
        System.out.println("│ Parts      │ " + parts + " ".repeat(65 - parts.length()) + "│");
        System.out.println("│ c length   │ " + cLength + " ".repeat(65 - cLength.length()) + "│");
        System.out.println("│ m length   │ " + mLength + " ".repeat(65 - mLength.length()) + "│");
        System.out.println("└────────────┴──────────────────────────────────────────────────────────────────┘");
    }
}
