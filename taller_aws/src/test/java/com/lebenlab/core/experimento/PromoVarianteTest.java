package com.lebenlab.core.experimento;

import com.lebenlab.core.mediocom.PromoMedioComunica;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.cod_variante;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.incentivo_id_variante;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.pg1s_variante;
import static com.lebenlab.core.mediocom.DataTestMedioCom.medioThree;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id_variante;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text_variante;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 22/05/2020
 * Time: 19:03
 */
public class PromoVarianteTest {

    @Test
    public void test_FormBuilder()
    {
        Map<String, List<String>> formParams = new HashMap<>();
        formParams.put(cod_variante.name(), singletonList("codVariante1"));
        formParams.put(pg1s_variante.name(), asList("5", "11"));
        formParams.put(incentivo_id_variante.name(), singletonList("7"));
        formParams.put(medio_id_variante.name(), singletonList("3"));
        formParams.put(promo_medio_text_variante.name(), singletonList(""));

        PromoVariante varianteOut = new PromoVariante.VarianteBuilder(formParams).build();
        assertThat(varianteOut.codPromo).isEqualTo("codVariante1");
        assertThat(varianteOut.pg1s).containsExactly(5, 11);
        assertThat(varianteOut.incentivo).isEqualTo(7);
        assertThat(varianteOut.promoMedioComunica).usingRecursiveComparison()
                .isEqualTo(new PromoMedioComunica.PromoMedComBuilder().medioId(medioThree).build());
    }
}