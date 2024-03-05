package com.lebenlab.jwt;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.KeyGenerator.getInstance;

/**
 * User: pedro@didekin.es
 * Date: 14/11/2019
 * Time: 20:07
 */
public class CryptoUtils {

    public static final String def_alg_symmetric_keys = "AES";
    static final int default_key_size = 256;

    private CryptoUtils()
    {
    }

    static SecretKey getNewSymmetricKey(String algorithm, int keySizeInBits) throws NoSuchAlgorithmException
    {
        KeyGenerator keyGenerator = getInstance(algorithm);
        keyGenerator.init(keySizeInBits);
        return keyGenerator.generateKey();
    }

    static String getBinaryStr(String stringToBinary)
    {
        StringBuilder binary = new StringBuilder();
        for (byte b : stringToBinary.getBytes(UTF_8)) {
            int val = b;
            for (int i = 0; i < 8; i++) {
                binary.append((val & 128) == 0 ? 0 : 1);
                val <<= 1;
            }
        }
        return binary.toString();
    }
}
