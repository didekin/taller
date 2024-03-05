package com.lebenlab.jwt;

import org.junit.Test;

import static com.lebenlab.jwt.TkErrorCodes.insufficient_scope;
import static com.lebenlab.jwt.TkErrorCodes.invalid_token;
import static com.lebenlab.jwt.TkErrorCodes.no_token_in_header;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 23/11/2019
 * Time: 14:41
 */
public class TkErrorCodesTest {

    @Test
    public void test_ValueForWwwAuthAttr()
    {
        assertThat(invalid_token.valueForWwwAuthAttr())
                .isEqualTo("Bearer realm=userbosch@lebendata1," +
                        "error=\"invalid_token\"," +
                        "error_description=\"Token con insuficientes privilegios o fechas invalidas\"");
        System.out.println(invalid_token.valueForWwwAuthAttr());

        assertThat(no_token_in_header.valueForWwwAuthAttr())
                .isEqualTo("Bearer realm=userbosch@lebendata1");
        System.out.println(no_token_in_header.valueForWwwAuthAttr());

        assertThat(insufficient_scope.valueForWwwAuthAttr())
                .isEqualTo("Bearer realm=userbosch@lebendata1," +
                        "error=\"insufficient_scope\"," +
                        "error_description=\"Token con insuficientes privilegios\"");
        System.out.println(insufficient_scope.valueForWwwAuthAttr());
    }
}