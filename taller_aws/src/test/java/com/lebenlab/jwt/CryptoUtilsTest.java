package com.lebenlab.jwt;

import org.junit.Test;

import java.security.Key;
import java.security.NoSuchAlgorithmException;

import static com.lebenlab.jwt.CryptoUtils.def_alg_symmetric_keys;
import static com.lebenlab.jwt.CryptoUtils.default_key_size;
import static com.lebenlab.jwt.CryptoUtils.getBinaryStr;
import static com.lebenlab.jwt.CryptoUtils.getNewSymmetricKey;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 15/11/2019
 * Time: 10:58
 */
public class CryptoUtilsTest {

    @Test
    public void test_GetBinaryStr()
    {
        String binaryStr = getBinaryStr("hola pedro");
        assertThat(binaryStr.length()).isEqualTo("hola pedro".length() * 8);
        System.out.println(binaryStr);
    }

    @Test
    public void test_GetNewSymmetricKey_1() throws NoSuchAlgorithmException
    {
        Key symmetricKey = getNewSymmetricKey(def_alg_symmetric_keys, default_key_size);
        assertThat(symmetricKey.getEncoded().length).isEqualTo(default_key_size / 8); // length is in bytes.
        assertThat(symmetricKey.getAlgorithm()).isEqualTo(def_alg_symmetric_keys);
    }
}