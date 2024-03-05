package com.lebenlab.core;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.tbmaster.ConceptoTaller;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.mediocom.DataTestMedioCom;
import com.lebenlab.core.util.DataTestExperiment;

import org.junit.After;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lebenlab.ProcessArgException.error_build_promocion;
import static com.lebenlab.ProcessArgException.error_pg1s;
import static com.lebenlab.core.Promocion.FieldLabel.cod_promo;
import static com.lebenlab.core.Promocion.FieldLabel.conceptos;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_fin;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_inicio;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo_id;
import static com.lebenlab.core.Promocion.FieldLabel.mercados;
import static com.lebenlab.core.Promocion.FieldLabel.pg1s;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.NA;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.idToInstance;
import static com.lebenlab.core.tbmaster.ConceptoTaller.AD_Talleres;
import static com.lebenlab.core.tbmaster.ConceptoTaller.AllTrucks;
import static com.lebenlab.core.tbmaster.ConceptoTaller.AutoCrew;
import static com.lebenlab.core.tbmaster.ConceptoTaller.Autotaller;
import static com.lebenlab.core.tbmaster.ConceptoTaller.BDC_BDS;
import static com.lebenlab.core.tbmaster.ConceptoTaller.Bosch_Car_Service;
import static com.lebenlab.core.tbmaster.ConceptoTaller.CGA_Car_Service;
import static com.lebenlab.core.tbmaster.ConceptoTaller.Cecauto;
import static com.lebenlab.core.tbmaster.ConceptoTaller.Confortauto;
import static com.lebenlab.core.tbmaster.ConceptoTaller.EuroTaller;
import static com.lebenlab.core.tbmaster.ConceptoTaller.Euro_Repar;
import static com.lebenlab.core.tbmaster.ConceptoTaller.Euromaster;
import static com.lebenlab.core.tbmaster.ConceptoTaller.Otros;
import static com.lebenlab.core.tbmaster.ConceptoTaller.Profesional_Plus;
import static com.lebenlab.core.tbmaster.Mercado.AN;
import static com.lebenlab.core.tbmaster.Mercado.ES;
import static com.lebenlab.core.tbmaster.Mercado.PT;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.tbmaster.PG1.PG1_2;
import static com.lebenlab.core.tbmaster.PG1.PG1_3;
import static com.lebenlab.core.tbmaster.PG1.PG1_4;
import static com.lebenlab.core.tbmaster.PG1.PG1_6;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.gson.GsonUtil.objectFromJsonStr;
import static com.lebenlab.gson.GsonUtil.objectToJsonStr;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;


/**
 * User: pedro@didekin.es
 * Date: 15/10/2019
 * Time: 12:02
 */
public class PromocionTest {

    @After
    public void cleanDb()
    {
        DataTestMedioCom.cleanMedCommTables();
    }

    @Test
    public void testToGson_1()
    {
        Promocion promoToJson = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).build();
        // El constructor inicializa a 0 promoId. Quito inicial y final '{' '}': el resto de promocionStr está contenido en el json
        // del nuevo objeto.
        final var jsonStr = objectToJsonStr(promoToJson);
        System.out.println(jsonStr);
        assertThat(jsonStr).contains(DataTestExperiment.promocion1Str.substring(2, DataTestExperiment.promocion1Str.length() - 1));

        Promocion promoFromJson = objectFromJsonStr(DataTestExperiment.promocion1Str, Promocion.class);
        assertThat(promoFromJson.mercados).containsOnly(ES.id);
        assertThat(promoFromJson.conceptos).containsExactlyInAnyOrder(AD_Talleres.conceptoId, EuroTaller.conceptoId);
        assertThat(promoFromJson.pg1s).containsExactlyInAnyOrder(PG1_1.idPg1, PG1_2.idPg1, PG1_3.idPg1);

        assertThat(promoFromJson.codPromo).isEqualTo(promoToJson.codPromo);
        assertThat(promoFromJson.fechaInicio).isEqualTo(promoToJson.fechaInicio);
        assertThat(promoFromJson.fechaFin).isEqualTo(promoToJson.fechaFin);
        assertThat(promoFromJson.incentivo).isEqualTo(promoToJson.incentivo);
        assertThat(promoFromJson.promoMedioComunica).usingRecursiveComparison().isEqualTo(promoToJson.promoMedioComunica);
    }

    @Test
    public void testToGson_2()
    {
        // String sin idPromo.
        Promocion promoFromJson = objectFromJsonStr(DataTestExperiment.promocion2Str, Promocion.class);
        assertThat(promoFromJson.idPromo).isEqualTo(0);
        // Son iguales, salvo por la presencia de idPromo, inicializado a 0.
        final var jsonStr = objectToJsonStr(promoFromJson);
        assertThat(jsonStr).contains(DataTestExperiment.promocion2Str);
    }

    @Test
    public void test_Build()
    {
        Promocion.PromoBuilder builder = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).fechaInicio(LocalDate.now().plusDays(60));
        // Duración negativa: fechaFin < fechaInicio.
        assertThatExceptionOfType(ProcessArgException.class).isThrownBy(builder::build).withMessage(error_build_promocion);
    }

    @Test
    public void test_builderForForms_1()
    {
        Map<String, List<String>> formParams = new HashMap<>();
        formParams.put(cod_promo.name(), singletonList("codPromo1"));
        formParams.put(fecha_inicio.name(), singletonList("23-04-2020"));
        formParams.put(fecha_fin.name(), singletonList("22-05-2020"));
        formParams.put(mercados.name(), asList("1", "3"));
        formParams.put(conceptos.name(), asList("4", "12"));
        formParams.put(pg1s.name(), asList("5", "11"));
        formParams.put(incentivo_id.name(), singletonList("7"));
        formParams.put(medio_id.name(), singletonList("1"));
        formParams.put(promo_medio_text.name(), singletonList(""));

        DataTestExperiment.runScript("insert into word_class_prob (word, text_class, prior_prob) " +
                " VALUES ('w1', 1, 0.25), ('w1', 2, 0.1), ('w1', 3, 0.9);");
        Promocion promoOut = new Promocion.PromoBuilder(formParams).build();

        assertThat(promoOut.codPromo).isEqualTo("codPromo1");
        assertThat(promoOut.fechaInicio.toString()).isEqualTo("2020-04-23");
        assertThat(promoOut.fechaFin.toString()).isEqualTo("2020-05-22");
        assertThat(promoOut.mercados).containsExactly(1, 3);
        assertThat(promoOut.conceptos).containsExactly(4, 12);
        assertThat(promoOut.pg1s).containsExactly(5, 11);
        assertThat(promoOut.incentivo).isEqualTo(7);
        assertThat(promoOut.promoMedioComunica).usingRecursiveComparison()
                .isEqualTo(new PromoMedioComunica.PromoMedComBuilder().medioId(DataTestMedioCom.medioOne).textMsg(NA.name()).build());
    }

    @Test
    public void test_builderForForms_2()
    {
        Map<String, List<String>> formParams = new HashMap<>();
        formParams.put(cod_promo.name(), singletonList("codPromo1"));
        formParams.put(fecha_inicio.name(), singletonList("23-04-2020"));
        formParams.put(fecha_fin.name(), singletonList("22-05-2020"));
        formParams.put(mercados.name(), asList("1", "3"));
        formParams.put(conceptos.name(), asList("4", "12"));
        formParams.put(pg1s.name(), asList("5", "11"));
        formParams.put(incentivo_id.name(), singletonList("7"));
        // Incluyo text para el medio, que cambio de 1 a 2.
        formParams.put(medio_id.name(), singletonList("2"));
        formParams.put(promo_medio_text.name(), singletonList("text test"));

        DataTestExperiment.runScript("insert into word_class_prob (word, text_class, prior_prob) " +
                " VALUES ('text', 1, 0.25), ('text', 2, 0.1), ('text', 3, 0.9);");
        Promocion promoOut = new Promocion.PromoBuilder(formParams).build();

        assertThat(promoOut.promoMedioComunica.textMsg).isEqualTo("text test");
        // 3 class has the greater probability. idIstance.size is the number of text classes.
        assertThat(promoOut.promoMedioComunica.codTextClass).isGreaterThanOrEqualTo(3).isLessThan(idToInstance.size());
    }

    @Test
    public void test_GetDuracionDias()
    {
        assertThat(DataTestExperiment.promocion1.getDuracionDias()).isEqualTo(32);
        Promocion promoNewDate = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).fechaFin(DataTestExperiment.promocion1.fechaInicio).build();
        assertThat(promoNewDate.getDuracionDias()).isEqualTo(1);
    }

    @Test
    public void test_GetQuarter()
    {
        Promocion promoIn = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).fechaInicio(parse("2019-02-11")).build();
        assertThat(promoIn.getQuarter()).isEqualTo(1);
        promoIn = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).fechaInicio(parse("2019-09-11")).build();
        assertThat(promoIn.getQuarter()).isEqualTo(3);
    }

    @Test
    public void test_GetMercadosArr()
    {
        assertThat(DataTestExperiment.promocion1.getMercadosArr()).containsOnly(ES.id);
        // Caso null.
        Promocion promocionNull = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).mercados(null).build();
        assertThat(promocionNull.getMercadosArr()).containsExactlyInAnyOrder(ES.id, PT.id, AN.id);
        assertThat(promocionNull.mercados).containsExactlyInAnyOrder(Arrays.stream(Mercado.values()).map(mercado -> mercado.id).toArray(Integer[]::new));
    }

    @Test
    public void test_GetConceptosArr()
    {
        assertThat(DataTestExperiment.promocion1.getConceptosArr()).containsOnly(AD_Talleres.getCodConceptoId(), EuroTaller.getCodConceptoId());

        // Caso null.
        Promocion promocionNull = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).conceptos(null).build();
        assertThat(promocionNull.getConceptosArr()).containsExactlyInAnyOrder(
                AD_Talleres.getCodConceptoId(),
                AllTrucks.getCodConceptoId(),
                AutoCrew.getCodConceptoId(),
                Autotaller.getCodConceptoId(),
                BDC_BDS.getCodConceptoId(),
                Bosch_Car_Service.getCodConceptoId(),
                CGA_Car_Service.getCodConceptoId(),
                Confortauto.getCodConceptoId(),
                Euro_Repar.getCodConceptoId(),
                Euromaster.getCodConceptoId(),
                EuroTaller.getCodConceptoId(),
                Cecauto.getCodConceptoId(),
                Profesional_Plus.getCodConceptoId(),
                Otros.getCodConceptoId());

        assertThat(promocionNull.conceptos).containsExactlyInAnyOrder(Arrays.stream(ConceptoTaller.values()).map(value -> value.conceptoId).toArray(Integer[]::new));
    }

    @Test
    public void test_GetPg1sArr()
    {
        assertThat(DataTestExperiment.promocion1.getPg1IdsArr()).containsOnly(PG1_1.idPg1, PG1_2.idPg1, PG1_3.idPg1);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void test_GetPg1sArrPlus3()
    {
        assertThat(
                catchThrowable(() -> new Promocion.PromoBuilder()
                        .copyPromo(DataTestExperiment.promocion1)
                        .pg1s(asList(PG1_4.idPg1, PG1_3.idPg1, PG1_2.idPg1, PG1_6.idPg1)))
        ).isInstanceOf(ProcessArgException.class).hasMessage(error_pg1s);

        // Case null.
        assertThat(
                catchThrowable(() -> new Promocion.PromoBuilder()
                        .copyPromo(DataTestExperiment.promocion1)
                        .pg1s(null))
        ).isInstanceOf(ProcessArgException.class).hasMessage(error_pg1s);
    }

    @Test
    public void test_FechaInicio()
    {
        Promocion promoIn = new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).fechaInicio(parse("2019-02-11")).build();
        assertThat(promoIn.fechaInicio.withDayOfMonth(1).toString()).isEqualTo("2019-02-01");
    }

    @Test
    public void test_GetPg1IdsZeroPadded()
    {
        Integer[] pg1s = DataTestExperiment.promocion1.pg1s.toArray(Integer[]::new);
        assertThat(DataTestExperiment.promocion1.getPg1IdsZeroPadded()).containsExactly(pg1s[0], pg1s[1], pg1s[2]);
        assertThat(new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).pg1s(asList(pg1s[0], pg1s[1])).build().getPg1IdsZeroPadded())
                .containsExactly(pg1s[0], pg1s[1], 0);
        assertThat(new Promocion.PromoBuilder().copyPromo(DataTestExperiment.promocion1).pg1s(singletonList(pg1s[0])).build().getPg1IdsZeroPadded())
                .containsExactly(pg1s[0], 0, 0);
    }
}

