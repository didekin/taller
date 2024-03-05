package com.lebenlab.core.tbmaster;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.tbmaster.Incentivo.IncentivoId;

import org.junit.Test;

import static com.lebenlab.core.tbmaster.Incentivo.fromIntToIncentivo;
import static com.lebenlab.core.tbmaster.Incentivo.maxIncentivoId;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * User: pedro@didekin
 * Date: 23/03/2020
 * Time: 17:46
 */
public class IncentivoTest {

    @Test
    public void test_FromIntToIncentivo()
    {
        assertThat(catchThrowable(() -> fromIntToIncentivo(11)))
                .isInstanceOf(ProcessArgException.class).hasMessage(ProcessArgException.error_incentivo + 11);

        assertThat(fromIntToIncentivo(2)).isEqualTo(Incentivo.tarjeta_regalo);
    }

    @Test
    public void test_maxIncentivoId()
    {
        assertThat(maxIncentivoId).isEqualTo(10);
    }

    @Test
    public void test_incentivoId()
    {
        assertThat(stream(Incentivo.values()).mapToInt(value -> value.incentivoId).toArray())
                .containsExactly(stream(IncentivoId.values()).mapToInt(value -> value.id).toArray());
    }
}