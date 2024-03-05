package com.lebenlab;

import org.junit.Test;

import static com.lebenlab.HttpConstant.valueForWwwAuthHeader;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 23/11/2019
 * Time: 13:48
 */
public class HttpConstantTest {

    @Test
    public void test_DoStringForWwwAuth()
    {
        assertThat(valueForWwwAuthHeader()).isEqualTo("Bearer realm=userbosch@lebendata1");
    }
}