package com.lebenlab.core.tbmaster;


import org.junit.Test;

import smile.math.Random;

import static com.lebenlab.core.tbmaster.ConceptoTaller.Bosch_Car_Service;
import static com.lebenlab.core.tbmaster.ConceptoTaller.allConceptos;
import static com.lebenlab.core.tbmaster.ConceptoTaller.randomInstance;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * User: pedro@didekin.es
 * Date: 24/01/2020
 * Time: 13:01
 */
public class ConceptoTallerTest {

    @Test
    public void test_Name()
    {
        assertThat(Bosch_Car_Service.name()).isEqualTo(Bosch_Car_Service.toString()).isEqualTo("Bosch_Car_Service");
    }

    @Test
    public void test_maxConceptoId()
    {
        assertThat(ConceptoTaller.maxConceptoId).isEqualTo(14);
    }

    @Test
    public void test_RandomInstance()
    {
        Random rnd = new Random(11);
        for (int i = 0; i < 1000; i++) {
            assertThat(randomInstance(rnd)).isIn(allConceptos);
        }
    }
}