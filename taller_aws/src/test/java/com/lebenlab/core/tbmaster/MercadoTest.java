package com.lebenlab.core.tbmaster;


import org.junit.Test;

import smile.math.Random;

import static com.lebenlab.core.tbmaster.Mercado.PT;
import static com.lebenlab.core.tbmaster.Mercado.allMercados;
import static com.lebenlab.core.tbmaster.Mercado.maxId;
import static com.lebenlab.core.tbmaster.Mercado.randomInstance;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 03/11/2019
 * Time: 14:11
 */
public class MercadoTest {

    @Test
    public void test_1()
    {
        assertThat(PT.id).isEqualTo(2);
    }

    @Test
    public void test_maxMinIds()
    {
        assertThat(maxId).isEqualTo(3);
        assertThat(Mercado.minId).isEqualTo(1);
    }

    @Test
    public void test_RandomInstance()
    {
        Random rnd = new Random(11);
        for (int i = 0; i < 1000; i++) {
            assertThat(randomInstance(rnd)).isIn(allMercados);
        }
    }
}