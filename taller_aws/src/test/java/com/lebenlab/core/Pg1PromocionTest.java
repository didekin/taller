package com.lebenlab.core;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Pg1Promocion.Pg1PromoBuilder;
import com.lebenlab.core.util.DataTestExperiment;

import org.junit.Test;

import static com.lebenlab.ProcessArgException.error_simulation_producto;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * User: pedro@didekin
 * Date: 01/04/2020
 * Time: 14:10
 */
public class Pg1PromocionTest {

    @Test
    public void test_build()
    {
        Promocion promoIn = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).pg1s(asList(2, 3)).build();
        Pg1PromoBuilder builder = new Pg1PromoBuilder().promo(promoIn);
        assertThat(catchThrowable(builder::build)).isInstanceOf(ProcessArgException.class)
                .hasMessage(error_simulation_producto);
    }

}