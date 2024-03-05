package com.lebenlab.jdbi;

import org.junit.Test;

import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 24/01/2020
 * Time: 12:38
 */
public class JdbiFactoryTest {

    @Test
    public void test_GetJdbi()
    {
        assertThat(jdbiFactory.getJdbi()).isNotNull();
    }
}