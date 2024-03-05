package com.lebenlab.core.experimento;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 20/05/2021
 * Time: 19:46
 */
public class ParticipanteAbsTest {

    @Test
    public void test_ToCsvRecord()
    {
        ParticipanteAbs.ParticipanteOutCsv promoParticip =
                new ParticipanteAbs.ParticipanteOutCsv(4, "H98895J", "2", "101", "1");
        assertThat(promoParticip.toCsvRecord()).isEqualTo("4;H98895J;2;101;1");
    }

}