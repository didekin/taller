package com.lebenlab.core.tbmaster;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.mediocom.PromoMedioComunica;

import org.junit.Test;

import static com.lebenlab.core.mediocom.MedioComunicacion.email;
import static com.lebenlab.core.mediocom.MedioComunicacion.fromIdToInstance;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static com.lebenlab.core.mediocom.MedioComunicacion.promosOfflineMedio;
import static com.lebenlab.core.mediocom.MedioComunicacion.promosOnlineMedio;
import static com.lebenlab.core.mediocom.MedioComunicacion.sms;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 03/01/2021
 * Time: 19:48
 */
public class MedioComunicacionTest {

    @Test
    public void test_FromIdToInstance()
    {
        assertThat(fromIdToInstance(1)).isEqualTo(ninguna);
        assertThat(fromIdToInstance(2)).isEqualTo(sms);
        assertThat(fromIdToInstance(3)).isEqualTo(email);
    }

    @Test
    public void test_promosOnlineMedio()
    {
        var promoMedio1 = new PromoMedioComunica.PromoMedComBuilder().medioId(1).build();
        var promoMedio2 = new PromoMedioComunica.PromoMedComBuilder().medioId(2).build();
        var promoMedio3 = new PromoMedioComunica.PromoMedComBuilder().medioId(3).build();
        var promoA = new Promocion.PromoBuilder().copyPromo(promocion1).medio(promoMedio1).build();
        var promoB = new Promocion.PromoBuilder().copyPromo(promocion1).medio(promoMedio3).build();

        // ON-LINE
        // Medios 1 y 3
        assertThat(promosOnlineMedio(asList(promoA, promoB))).isEmpty();
        // Medios 2 y 3.
        promoA = new Promocion.PromoBuilder().copyPromo(promoA).medio(promoMedio2).build();
        assertThat(promosOnlineMedio(asList(promoA, promoB))).containsExactly(promoA);
        // Medios 2 y 2.
        promoB = new Promocion.PromoBuilder().copyPromo(promoB).medio(promoMedio2).build();
        assertThat(promosOnlineMedio(asList(promoA, promoB))).containsExactly(promoA, promoB);

        // OFF-LINE
        // Medios 2 y 2.
        assertThat(promosOfflineMedio(asList(promoA, promoB))).isEmpty();
        // Medios 2 y 3.
        promoB = new Promocion.PromoBuilder().copyPromo(promoB).medio(promoMedio3).build();
        assertThat(promosOfflineMedio(asList(promoA, promoB))).containsExactly(promoB);
    }
}