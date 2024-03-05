package com.lebenlab.jwt;

import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;

import static com.lebenlab.DataPatterns.jwt_with_direct_key;
import static com.lebenlab.HttpConstant.auth_bearer_scheme;
import static com.lebenlab.HttpConstant.auth_request_header;
import static com.lebenlab.core.TkConsumer.getDummyKey;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
import static org.jose4j.jwe.KeyManagementAlgorithmIdentifiers.DIRECT;
import static org.jose4j.jwt.JwtClaims.parse;
import static org.jose4j.jwt.NumericDate.fromSeconds;

/**
 * User: pedro@didekin.es
 * Date: 25/11/2019
 * Time: 17:37
 */
public class JwtTestUtil {

    private static final String claimsStr = "{\"iss\":\"lebendata.com\"," +
            "\"sub\":\"bosch\"," +
            "\"aud\":\"lebendata1.net\"" +
            "}";

    public static String doSerialToken(JwtClaims claims) throws JoseException
    {
        JsonWebEncryption encrypter = new JsonWebEncryption();
        encrypter.setPayload(claims.toJson());
        encrypter.setAlgorithmHeaderValue(DIRECT);
        encrypter.setEncryptionMethodHeaderParameter(AES_256_GCM);
        encrypter.setKey(getDummyKey());
        String compactJwt = encrypter.getCompactSerialization();
        // Token se produce correctamente.
        assertThat(jwt_with_direct_key.isPatternOk(compactJwt)).isTrue();
        return compactJwt;
    }

    public static String[] doHeaderToken(JwtClaims claims) throws JoseException
    {
        return new String[]{auth_request_header.toString(), auth_bearer_scheme.toString() + " " + doSerialToken(claims)};
    }

    public static JwtClaims doBasicClaims() throws InvalidJwtException
    {
        return parse(JwtTestUtil.claimsStr);
    }

    public static JwtClaims putFechasOk(JwtClaims claims)
    {
        // Fechas OK.
        NumericDate expDate = fromSeconds(now().plusSeconds(1000).getEpochSecond());
        NumericDate iapDate = fromSeconds(expDate.getValue() - 10000L);
        assertThat(expDate.isOnOrAfter(iapDate)).isTrue();
        claims.setExpirationTime(expDate);
        claims.setIssuedAt(iapDate);
        return claims;
    }

    @SuppressWarnings("unused")
    public static JwtClaims putFechasWrong(JwtClaims claims)
    {
        // Fechas más antiguas que fecha actual.
        NumericDate expDate = fromSeconds(now().minusSeconds(1000).getEpochSecond());
        NumericDate iapDate = fromSeconds(expDate.getValue() - 10000L);
        assertThat(expDate.isOnOrAfter(iapDate)).isTrue();
        assertThat(iapDate.isOnOrAfter(fromSeconds(now().getEpochSecond()))).isFalse();
        claims.setExpirationTime(expDate);
        claims.setIssuedAt(iapDate);
        return claims;
    }

}
