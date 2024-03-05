package com.lebenlab.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import static java.nio.charset.Charset.forName;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getUrlEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jose4j.jwa.AlgorithmConstraints.ConstraintType.PERMIT;
import static org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
import static org.jose4j.jwe.KeyManagementAlgorithmIdentifiers.DIRECT;

/**
 * User: pedro@didekin.es
 * Date: 11/11/2019
 * Time: 14:00
 */
public class TokenLebenTest {

    // Ejemplo en Anexo I de RFC 7516
    @Test
    public void test_encrypToken_1()
    {
        Gson myGson = new GsonBuilder().create();

        String header = "{\"alg\":\"RSA-OAEP\",\"enc\":\"A256GCM\"}".trim();
        String baseUtf8Header = getUrlEncoder().withoutPadding().encodeToString(new String(header.getBytes(), forName(UTF_8.name())).getBytes());
        assertThat(baseUtf8Header).isEqualTo("eyJhbGciOiJSU0EtT0FFUCIsImVuYyI6IkEyNTZHQ00ifQ");
        String charsHeaderToJson = myGson.toJson(baseUtf8Header.chars().toArray());
        String arrIntExpected = "[101,121,74,104,98,71,99,105,79,105,74,83,85,48,69," +
                "116,84,48,70,70,85,67,73,115,73,109,86,117,89,121,73," +
                "54,73,107,69,121,78,84,90,72,81,48,48,105,102,81]";
        assertThat(charsHeaderToJson).isEqualTo(arrIntExpected);

        String plainText = "The true sign of intelligence is not knowledge but imagination.";
        String textCharsToJson = myGson.toJson(plainText.chars().toArray());
        arrIntExpected = "[84,104,101,32,116,114,117,101,32,115,105,103,110,32," +
                "111,102,32,105,110,116,101,108,108,105,103,101,110,99," +
                "101,32,105,115,32,110,111,116,32,107,110,111,119,108," +
                "101,100,103,101,32,98,117,116,32,105,109,97,103,105," +
                "110,97,116,105,111,110,46]";
        assertThat(textCharsToJson).isEqualTo(arrIntExpected);
    }

    @Test
    public void test_decrypToken_1() throws JoseException
    {

        String originalPlainText = "{\"iss\":\"lebendata.com\",\"iat\":\"1300819000\",\"exp\":\"1300819000\",\"sub\":\"bosch\",\"aud\":\"lebendata1.net\"}";

        String token =
                "eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIiwidHlwIjoiand0In0" +
                        "." +
                        ".ywErjYDkuV8RnpjP" +
                        ".vBVswvV0iMKx588Ckla6C5VBmhBJhmc0zj5bkVYUnHvq937D9FBfz24isnwFYHt0MAztMtTOitIWTibEH4t45O6kNe6pq1Qyfq1Eum9U9SuTHQVmoYBJsDoNdDrIsuCuDRY" +
                        ".lqeOQrotN6RFWIcx9BcBYA";

        String keyRaw = "y/B?E(H+MbQeThWmZq4t7w!z$C&F)J@N";

        byte[] keyBytes = keyRaw.getBytes(UTF_8);
        String key = getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String jwkJson = "{\"kty\":\"oct\",\"k\":" + "\"" + key + "\"" + "}";

        JsonWebEncryption receiverJwe = new JsonWebEncryption();
        receiverJwe.setAlgorithmConstraints(new AlgorithmConstraints(PERMIT, DIRECT));
        receiverJwe.setContentEncryptionAlgorithmConstraints(new AlgorithmConstraints(PERMIT, AES_256_GCM));
        // Utilizamos compact serialization
        receiverJwe.setCompactSerialization(token);
        receiverJwe.setKey(JsonWebKey.Factory.newJwk(jwkJson).getKey());

        String plaintext = receiverJwe.getPlaintextString();
        assertThat(plaintext).isEqualTo(originalPlainText);
    }
}
