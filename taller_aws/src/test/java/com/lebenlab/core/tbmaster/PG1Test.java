package com.lebenlab.core.tbmaster;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import smile.math.Random;

import static com.lebenlab.core.tbmaster.PG1.PG1_0;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.tbmaster.PG1.PG1_10;
import static com.lebenlab.core.tbmaster.PG1.PG1_11;
import static com.lebenlab.core.tbmaster.PG1.PG1_17;
import static com.lebenlab.core.tbmaster.PG1.PG1_5;
import static com.lebenlab.core.tbmaster.PG1.PG1_6;
import static com.lebenlab.core.tbmaster.PG1.checkPg1sIn;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static com.lebenlab.core.tbmaster.PG1.randomInstance;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.range;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 03/11/2019
 * Time: 14:22
 */
public class PG1Test {

    @Test
    public void test_GetNombreCode()
    {
        assertThat(PG1_6.name()).isEqualTo("PG1_6");
        assertThat(PG1_11.name()).isEqualTo("PG1_11");
    }

    @Test
    public void test_checkPg1sIn()
    {
        assertThat(checkPg1sIn(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 17))).isTrue();
        assertThat(checkPg1sIn(singletonList(9))).isFalse();
        assertThat(checkPg1sIn(singletonList(16))).isFalse();
    }

    @Test
    public void test_RandomInstance_1()
    {
        List<PG1> pg1Rnd = new ArrayList<>(10000);
        Random rnd = new Random(11);
        for (int i = 0; i < 1000; i++) {
            pg1Rnd.add(randomInstance(rnd));
        }
        Assertions.assertThat(pg1Rnd).containsAll(allOf(PG1.class)).containsOnly(allOf(PG1.class).toArray(PG1[]::new));
    }

    @Test
    public void test_RandomInstance_2()
    {
        List<PG1> pg1Rnd = new ArrayList<>(10000);
        Random rnd = new Random(11);
        for (int i = 0; i < 1000; i++) {
            pg1Rnd.add(randomInstance(rnd, range(PG1_1, PG1_5)));
        }
        Assertions.assertThat(pg1Rnd).containsAll(range(PG1_1, PG1_5)).containsOnly(range(PG1_1, PG1_5).toArray(PG1[]::new));
    }

    @Test
    public void test_removeRandomInstance()
    {
        List<PG1> pg1Rnd = new ArrayList<>(10000);
        Random rnd = new Random(11);
        for (int i = 0; i < 1000; i++) {
            pg1Rnd.add(randomInstance(PG1_11, rnd));  // remove pg1_11.
        }
        Assertions.assertThat(pg1Rnd).doesNotContain(PG1_11)
                .containsOnly(allOf(PG1.class).stream().filter(pg1 -> pg1 != PG1_11).toArray(PG1[]::new));
    }

    @Test
    public void test_permutationsPromo()
    {
        assertThat(PG1.permutationsPromo(singletonList(10)))
                .hasSize(1).containsKey(fromIntPg1(10)).containsValue(asList(PG1_0, PG1_0));
        assertThat(PG1.permutationsPromo(asList(10, 11)))
                .hasSize(2)
                .containsKeys(fromIntPg1(10), fromIntPg1(11))
                .containsValues(asList(PG1_11, PG1_0), asList(PG1_10, PG1_0));
        assertThat(PG1.permutationsPromo(asList(10, 11, 17)))
                .hasSize(3)
                .containsKeys(fromIntPg1(10), fromIntPg1(11), fromIntPg1(17))
                .containsValues(asList(PG1_11, PG1_17), asList(PG1_10, PG1_17), asList(PG1_10, PG1_11));
    }
}