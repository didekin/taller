package com.lebenlab.core.mediocom;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.mediocom.TextClassifier.TextClassEnum;

import org.junit.Test;

import java.util.ArrayList;

import smile.math.Random;

import static com.lebenlab.ProcessArgException.error_build_promomedcom;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.NA;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.idToInstance;
import static com.lebenlab.gson.GsonUtil.objectFromJsonStr;
import static java.util.EnumSet.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * User: pedro@didekin
 * Date: 15/01/2021
 * Time: 14:07
 */
public class PromoMedioComunicaTest {

    @Test
    public void testToGson_1()
    {
        final var promoComStr = "{\"promoId\":0,\"medioId\":3,\"msgClassifier\":0,\"textMsg\":\"NA\"}";
        final var jsonObj = objectFromJsonStr(promoComStr, PromoMedioComunica.class);
        assertThat(jsonObj.medioId).isEqualTo(3);
        assertThat(jsonObj).usingRecursiveComparison().isEqualTo(
                new PromoMedioComunica.PromoMedComBuilder().promoId(0L).medioId(3).msgClassifier(0).textMsg(NA.name()).build()
        );
    }

    @Test
    public void test_sizeEnumClassifier()
    {
        assertThat(idToInstance.size()).isEqualTo(allOf(TextClassEnum.class).size());
    }

    @Test
    public void test_inconsistent()
    {
        assertThatThrownBy(() -> new PromoMedioComunica.PromoMedComBuilder().medioId(DataTestMedioCom.medioOne).textMsg("hola").build())
                .isInstanceOf(ProcessArgException.class)
                .hasMessage(error_build_promomedcom);
    }

    @Test
    public void test_lengthTextMsg()
    {
        final var testStr500 = "abcde".repeat(100);
        assertThat(testStr500).hasSize(500);
        assertThatNoException().isThrownBy(() -> new PromoMedioComunica.PromoMedComBuilder().textMsg(testStr500));
        assertThatExceptionOfType(ProcessArgException.class).isThrownBy(() -> new PromoMedioComunica.PromoMedComBuilder().textMsg(testStr500 + "a"));
    }

    @Test
    public void test_randInstance()
    {
        Random rnd = new Random(1131);
        final var listPromoMedCom = new ArrayList<PromoMedioComunica>();
        for (int i = 0; i < 100; i++) {
            listPromoMedCom.add(DataTestMedioCom.randInstance(rnd));
        }
        for (PromoMedioComunica instance : listPromoMedCom) {
            if (instance.medioId == 1) {
                assertThat(instance.textMsg).isEqualTo(NA.name());
            } else {
                assertThat(instance.textMsg).contains(DataTestMedioCom.text_rnd_msg);
                assertThat(instance.codTextClass).isGreaterThan(0).isLessThan(allOf(TextClassEnum.class).size());
            }
        }
    }
}