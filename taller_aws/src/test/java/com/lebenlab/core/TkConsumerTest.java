package com.lebenlab.core;

import com.lebenlab.jwt.JwtTestUtil;
import com.lebenlab.jwt.TokenException;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.junit.Before;
import org.junit.Test;

import java.security.Key;

import static com.lebenlab.jwt.JwtTestUtil.doBasicClaims;
import static com.lebenlab.core.TkConsumer.consumerSingle;
import static com.lebenlab.core.TkConsumer.dummyKey;
import static com.lebenlab.core.TkConsumer.rawStrKey;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Base64.getUrlEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jose4j.jwt.NumericDate.fromSeconds;

/**
 * User: pedro@didekin.es
 * Date: 24/11/2019
 * Time: 15:44
 */
public class TkConsumerTest {

    private NumericDate expDate;
    private JwtClaims claims;

    @Before
    public void setUp() throws InvalidJwtException
    {
        claims = doBasicClaims();
        // Fechas OK.
        expDate = fromSeconds(now().plusSeconds(1000).getEpochSecond());
        NumericDate iapDate = fromSeconds(expDate.getValue() - 10000L);
        assertThat(expDate.isOnOrAfter(iapDate)).isTrue();
        claims.setExpirationTime(expDate);
        claims.setIssuedAt(iapDate);
    }

    @Test
    public void test_DoKeyFromRawStr() throws JoseException
    {
        // Comparo la construcción de jose4j con mi método.
        byte[] keyBytes = rawStrKey.getBytes(UTF_8);
        String key = getUrlEncoder().withoutPadding().encodeToString(keyBytes);
        String jwkJson = "{\"kty\":\"oct\",\"k\":" + "\"" + key + "\"" + "}";
        Key jose4jKey = JsonWebKey.Factory.newJwk(jwkJson).getKey();

        assertThat(jose4jKey).isEqualTo(dummyKey);
    }

    @Test
    public void test_CheckToken_1() throws JoseException, InvalidJwtException
    {
        // Creamos el token con fechas OK.
        String compactJwt = JwtTestUtil.doSerialToken(claims);
        // Token se desencripta y valida correctamente.
        assertThat(claims.toJson()).isEqualTo(consumerSingle.jwtConsumer.processToClaims(compactJwt).toJson());

        assertThatCode(() -> consumerSingle.checkToken(compactJwt)).doesNotThrowAnyException();
    }

    @Test
    public void test_CheckToken_2() throws JoseException
    {
        // Creamos el token sin fechas OK.
        claims.setIssuedAt(null);
        claims.setExpirationTime(null);
        String compactJwt = JwtTestUtil.doSerialToken(claims);

        assertThatThrownBy(() -> consumerSingle.checkToken(compactJwt)).isInstanceOf(TokenException.class).hasMessageContaining("No Expiration Time");
    }

    @Test
    public void test_CheckToken_3() throws JoseException
    {
        // Creamos el token sin 'sub'.
        claims.setSubject(null);
        String compactJwt = JwtTestUtil.doSerialToken(claims);

        assertThatThrownBy(() -> consumerSingle.checkToken(compactJwt)).isInstanceOf(TokenException.class).hasMessageContaining("No Subject");
    }

    @Test
    public void test_CheckToken_4() throws JoseException
    {
        // Creamos el token sin 'sub'.
        claims.setIssuer("dummyIssuer");
        String compactJwt = JwtTestUtil.doSerialToken(claims);

        assertThatThrownBy(() -> consumerSingle.checkToken(compactJwt)).isInstanceOf(TokenException.class)
                .hasMessageContaining("Issuer (iss) claim value (dummyIssuer) doesn't match expected value");
    }

    @Test
    public void test_CheckToken_5() throws JoseException
    {
        // Creamos expiración < hoy.
        expDate = fromSeconds(now().minusSeconds(1000).getEpochSecond());
        claims.setExpirationTime(expDate);
        String compactJwt = JwtTestUtil.doSerialToken(claims);

        assertThatThrownBy(() -> consumerSingle.checkToken(compactJwt)).isInstanceOf(TokenException.class)
                .hasMessageContaining("The JWT is no longer valid");
    }
}