package com.lebenlab.core;

import com.lebenlab.Jsonable;
import com.lebenlab.jwt.TokenException;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import static com.lebenlab.jwt.CryptoUtils.def_alg_symmetric_keys;
import static com.lebenlab.jwt.TkErrorCodes.invalid_token;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jose4j.jwa.AlgorithmConstraints.ConstraintType.PERMIT;
import static org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
import static org.jose4j.jwe.KeyManagementAlgorithmIdentifiers.DIRECT;

/**
 * User: pedro@didekin.es
 * Date: 23/11/2019
 * Time: 16:14
 */
public class TkConsumer implements Jsonable {

    private static final String ISS = "lebendata.com";
    private static final String AUD = "lebendata1.net";
    private static final String ALG_key = DIRECT;
    private static final String ALG_encrypt = AES_256_GCM;
    private static final String SUB = "bosch";
    static final String rawStrKey = "y/B?E(H+MbQeThWmZq4t7w!z$C&F)J@N";     // TODO: cambiar la obtención de la clave.

    static final Key dummyKey = new SecretKeySpec(rawStrKey.getBytes(UTF_8), def_alg_symmetric_keys);
    public static final TkConsumer consumerSingle = new TkConsumer();

    final JwtConsumer jwtConsumer;

    private TkConsumer()
    {
        JwtConsumerBuilder jwtBuilder = new JwtConsumerBuilder();
        // A signature on the JWT is required by default.
        jwtBuilder.setDisableRequireSignature();
        // Require that the JWT be encrypted, which is not required by default.
        jwtBuilder.setEnableRequireEncryption();
        // KEY
        jwtBuilder.setDecryptionKey(dummyKey);
        // CLAIMS
        jwtBuilder.setExpectedAudience(AUD);
        jwtBuilder.setExpectedIssuer(ISS);
        jwtBuilder.setExpectedSubject(SUB);
        // Require that the JWT contain an expiration time ("exp") claim.
        jwtBuilder.setRequireExpirationTime();
        // HEADER
        jwtBuilder.setJweAlgorithmConstraints(new AlgorithmConstraints(PERMIT, ALG_key));
        jwtBuilder.setJweContentEncryptionAlgorithmConstraints(new AlgorithmConstraints(PERMIT, ALG_encrypt));
        jwtConsumer = jwtBuilder.build();
    }

    public static Key getDummyKey()
    {
        return dummyKey;
    }

    public void checkToken(String tokenIn)
    {
        try {
            consumerSingle.jwtConsumer.processToClaims(tokenIn);
        } catch (InvalidJwtException e) {
            throw new TokenException(invalid_token, e.getMessage());
        }
    }
}


