package com.lebenlab.core;

import com.lebenlab.core.util.WebConnTestUtils;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import kong.unirest.HttpResponse;

import static com.lebenlab.HttpConstant.www_authenticate_resp_header;
import static com.lebenlab.jwt.JwtTestUtil.doBasicClaims;
import static com.lebenlab.jwt.JwtTestUtil.doHeaderToken;
import static com.lebenlab.jwt.JwtTestUtil.putFechasOk;
import static com.lebenlab.jwt.TkErrorCodes.invalid_token;
import static com.lebenlab.core.AuthHeaderCtrler.getTokenFromHeader;
import static com.lebenlab.core.UrlPath.simulacionPath;
import static com.lebenlab.core.UrlPath.login;
import static kong.unirest.Unirest.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 21/11/2019
 * Time: 18:20
 */
public class AuthHeaderCtrlerTest {

    @ClassRule
    public static final ExternalResource resource = WebConnTestUtils.initJavalinInTest();

    static void checkInvalidTk(HttpResponse<String> response)
    {
        assertThat(response.getStatus()).isEqualTo(invalid_token.statusCode());
        assertThat(response.getHeaders().get(www_authenticate_resp_header.toString()))
                .containsExactly(invalid_token.valueForWwwAuthAttr());
    }

    @Test
    public void test_GetTokenFromHeader()
    {
        String headerTest = " Bearer mF_9.B5f-4.1JqM";
        assertThat(getTokenFromHeader(headerTest)).isEqualTo("mF_9.B5f-4.1JqM");

        headerTest = "Bearer mF_9.B5f-4.1JqM ";
        assertThat(getTokenFromHeader(headerTest)).isEqualTo("mF_9.B5f-4.1JqM");

        headerTest = "Bearer mF_9.B5f-4.1JqM";
        assertThat(getTokenFromHeader(headerTest)).isEqualTo("mF_9.B5f-4.1JqM");
    }

    /*@Test
    public void test_NoToken()
    {
        HttpResponse<String> response = put(getLocalHttp(simulacionPath))
                .header("accept", "application/json")
                .body(promocion1)
                .asString();

        assertThat(response.getStatus()).isEqualTo(no_token_in_header.statusCode());
        assertThat(response.getBody()).isEqualTo(no_token_in_header.description());
        assertThat(response.getHeaders().get(www_authenticate_resp_header.toString()))
                .containsExactly(no_token_in_header.valueForWwwAuthAttr());
    }*/   // TODO: descomentar cuando haya login

    @Test
    public void test_validToken() throws InvalidJwtException, JoseException
    {
        String[] tokenHeader = doHeaderToken(putFechasOk(doBasicClaims()));
        HttpResponse<String> response = get(WebConnTestUtils.getLocalHttp(simulacionPath))
                .header(tokenHeader[0], tokenHeader[1])
                .header("accept", "application/json")
                .asString();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeaders().containsKey(www_authenticate_resp_header.toString())).isFalse();
    }

    /*@Test
    public void test_invalidToken() throws InvalidJwtException, JoseException
    {
        JwtClaims claims = doBasicClaims();
        // Con sub erróneo.
        claims.setSubject("vodafone");

        String[] tokenHeader = doHeaderToken(putFechasOk(claims));
        HttpResponse<String> response =
                post(getLocalHttp(simulacionPath))
                        .header(tokenHeader[0], tokenHeader[1])
                        .header("accept", "application/json")
                        .body(promocion1).asString();
        checkInvalidTk(response);
    }*/    // TODO: descomentar cuando haya login

    @Test
    public void test_welcome()
    {
        HttpResponse<String> response = WebConnTestUtils.doJsonGet(WebConnTestUtils.getLocalHttp(login)).asString();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}