package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.Experimento.ExperimentPromoFull;
import com.lebenlab.core.experimento.Experimento.ExperimentoBuilder;
import com.lebenlab.core.experimento.PromoVariante.VarianteBuilder;
import com.lebenlab.core.mediocom.PromoMedioComunica;

import org.junit.After;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lebenlab.ProcessArgException.error_pg1s;
import static com.lebenlab.ProcessArgException.experimento_wrongly_initialized;
import static com.lebenlab.ProcessArgException.result_experiment_wrongly_initialized;
import static com.lebenlab.core.Promocion.FieldLabel.cod_promo;
import static com.lebenlab.core.Promocion.FieldLabel.conceptos;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_fin;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_inicio;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo_id;
import static com.lebenlab.core.Promocion.FieldLabel.mercados;
import static com.lebenlab.core.Promocion.FieldLabel.pg1s;
import static com.lebenlab.core.experimento.Experimento.ExperimentPromoFull.asPromosList;
import static com.lebenlab.core.experimento.Experimento.FieldLabel.exp_nombre;
import static com.lebenlab.core.experimento.Experimento.extractPg1sToExperiment;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.cod_variante;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.incentivo_id_variante;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.pg1s_variante;
import static com.lebenlab.core.mediocom.DataTestMedioCom.cleanMedCommTables;
import static com.lebenlab.core.mediocom.DataTestMedioCom.medioOne;
import static com.lebenlab.core.mediocom.DataTestMedioCom.medioThree;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id_variante;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text_variante;
import static com.lebenlab.core.tbmaster.ConceptoTaller.AD_Talleres;
import static com.lebenlab.core.tbmaster.ConceptoTaller.CGA_Car_Service;
import static com.lebenlab.core.tbmaster.Incentivo.otros;
import static com.lebenlab.core.tbmaster.Incentivo.tarjeta_regalo;
import static com.lebenlab.core.tbmaster.Mercado.ES;
import static com.lebenlab.core.tbmaster.Mercado.PT;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static com.lebenlab.core.tbmaster.PG1.PG1_2;
import static com.lebenlab.core.tbmaster.PG1.PG1_3;
import static com.lebenlab.core.util.DataTestExperiment.expForJson;
import static com.lebenlab.core.util.DataTestExperiment.experimento1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static com.lebenlab.gson.GsonUtil.objectFromJsonStr;
import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

/**
 * User: pedro@didekin.es
 * Date: 09/02/2020
 * Time: 13:15
 */
public class ExperimentoTest {

    @After
    public void cleanDb()
    {
        cleanMedCommTables();
    }

    @Test
    public void test_FromGson()
    {
        final var experimento = objectFromJsonStr(expForJson, Experimento.class);
        // Los constructores de Experimento y Promocion añaden e incializan los id's a 0.
        assertThat(experimento.experimentoId).isEqualTo(0);
        assertThat(experimento.promocion.idPromo).isEqualTo(0);
    }

    @Test
    public void test_builder()
    {
        assertThatCode(() -> new ExperimentoBuilder(experimento1).build()).doesNotThrowAnyException();
        assertThatCode(() -> new Experimento.ExperimentoBuilder(experimento1).nombre("ok_ññ").build()).doesNotThrowAnyException();

        // No PG1s in common in promo and variante.
        Promocion promoIn = new Promocion.PromoBuilder().copyPromo(experimento1.promocion).pg1s(asList(1, 2)).build();
        PromoVariante varianteIn =
                new VarianteBuilder(experimento1.variante.codPromo, asList(3, 4), experimento1.variante.incentivo, experimento1.variante.promoMedioComunica).build();
        assertThat(catchThrowable(() -> new Experimento.ExperimentoBuilder(experimento1).promocion(promoIn).variante(varianteIn).build()))
                .isInstanceOf(ProcessArgException.class).hasMessage(experimento_wrongly_initialized + error_pg1s);

        // Wrong name.
        assertThat(catchThrowable(() -> new Experimento.ExperimentoBuilder(experimento1).nombre("wrong_name_?").build()))
                .isInstanceOf(ProcessArgException.class).hasMessage(experimento_wrongly_initialized);
    }

    @Test
    public void test_getPromos()
    {
        final var promosIn = experimento1.getPromos();
        assertThat(promosIn.get(0).experimentoId).isEqualTo(promosIn.get(1).experimentoId);
        assertThat(promosIn.get(0).conceptos).isEqualTo(promosIn.get(1).conceptos);
        assertThat(promosIn.get(0).codPromo).isEqualTo(experimento1.getPromocion().codPromo);
        assertThat(promosIn.get(1).codPromo).isEqualTo(experimento1.getVariante().codPromo);
    }

    @Test
    public void test_formBuilder()
    {
        Map<String, List<String>> formParams = new HashMap<>();
        formParams.put(exp_nombre.name(), singletonList("experimento1"));
        formParams.put(cod_promo.name(), singletonList("codPromo1"));
        formParams.put(fecha_inicio.name(), singletonList("23-04-2020"));
        formParams.put(fecha_fin.name(), singletonList("22-05-2020"));
        formParams.put(mercados.name(), asList("1", "3"));
        formParams.put(conceptos.name(), asList("4", "12"));
        formParams.put(pg1s.name(), asList("5", "11"));
        formParams.put(incentivo_id.name(), singletonList("7"));
        formParams.put(medio_id.name(), singletonList("2"));
        formParams.put(promo_medio_text.name(), singletonList(null));
        formParams.put(cod_variante.name(), singletonList("codVariante1"));
        formParams.put(pg1s_variante.name(), singletonList("11"));
        formParams.put(incentivo_id_variante.name(), singletonList("4"));
        formParams.put(medio_id_variante.name(), singletonList("1"));
        formParams.put(promo_medio_text_variante.name(), singletonList(null));

        // Inserto diccionario para la clasificación de los mensajes.
        runScript("insert into word_class_prob (word, text_class, prior_prob) " +
                " VALUES ('w1', 1, 0.25), ('w1', 2, 0.1), ('w1', 3, 0.9);");
        Experimento exp = new ExperimentoBuilder(formParams).build();

        assertThat(exp.nombre).isEqualTo("experimento1");
        assertThat(exp.promocion.codPromo).isEqualTo("codPromo1");
        assertThat(exp.variante.codPromo).isEqualTo("codVariante1");
        assertThat(exp.promocion.promoMedioComunica).usingRecursiveComparison().isEqualTo(new PromoMedioComunica.PromoMedComBuilder().medioId(2).build());
        assertThat(exp.variante.promoMedioComunica).usingRecursiveComparison().isEqualTo(new PromoMedioComunica.PromoMedComBuilder().medioId(1).build());
    }

    @Test  // ExperimentPromosFull.class
    public void test_asPromosList()
    {
        // Dos registros para una promoción.
        ExperimentPromoFull exPromoFull_1_1 = new ExperimentPromoFull(1L, "codPr_1", parse("2020-01-30"),
                parse("2020-01-31"), tarjeta_regalo.incentivoId, new PromoMedioComunica.PromoMedComBuilder().medioId(medioOne).build(), AD_Talleres.conceptoId, ES.id, PG1_2.idPg1);
        ExperimentPromoFull exPromoFull_1_2 = new ExperimentPromoFull(1L, "codPr_1", parse("2020-01-30"),
                parse("2020-01-31"), tarjeta_regalo.incentivoId, new PromoMedioComunica.PromoMedComBuilder().medioId(medioOne).build(), AD_Talleres.conceptoId, PT.id, PG1_3.idPg1);
        // Dos registros para la otra promoción (variante).
        ExperimentPromoFull exPromoFull_2_1 = new ExperimentPromoFull(2L, "codPr_2", parse("2020-01-30"),
                parse("2020-01-31"), otros.incentivoId, new PromoMedioComunica.PromoMedComBuilder().medioId(medioThree).build(), AD_Talleres.conceptoId, ES.id, PG1_2.idPg1);
        ExperimentPromoFull exPromoFull_2_2 = new ExperimentPromoFull(2L, "codPr_2", parse("2020-01-30"),
                parse("2020-01-31"), otros.incentivoId, new PromoMedioComunica.PromoMedComBuilder().medioId(medioThree).build(), CGA_Car_Service.conceptoId, ES.id, PG1_2.idPg1);

        List<ExperimentPromoFull> listExp1 = new ArrayList<>(2);
        List<ExperimentPromoFull> listExp2 = asList(exPromoFull_1_1, exPromoFull_1_2, exPromoFull_2_1, exPromoFull_2_2);

        // Lista vacía.
        final var experimentId = 11L;
        assertThatThrownBy(() -> asPromosList(listExp1, experimentId)).isInstanceOf(ProcessArgException.class)
                .hasMessage(result_experiment_wrongly_initialized + "no hay promos para experimento 11");

        // Lista OK con 2 registros resultado de agregar los cuatro originales.
        final var actualList = asPromosList(listExp2, experimentId);
        assertThat(actualList).hasSize(2);
        // Expected results.
        Promocion promoA = new Promocion.PromoBuilder()
                .experimentoId(experimentId)
                .idPromo(1L)
                .codPromo("codPr_1")
                .fechaInicio(LocalDate.parse("2020-01-30"))
                .fechaFin(LocalDate.parse("2020-01-31"))
                .incentivo(tarjeta_regalo.incentivoId)
                .medio(new PromoMedioComunica.PromoMedComBuilder().medioId(medioOne).build())
                .conceptos(singletonList(AD_Talleres.conceptoId))
                .mercados(asList(ES.id, PT.id))
                .pg1s(asList(PG1_2.idPg1, PG1_3.idPg1))
                .build();
        Promocion promoB = new Promocion.PromoBuilder()
                .experimentoId(experimentId)
                .idPromo(2L)
                .codPromo("codPr_2")
                .fechaInicio(LocalDate.parse("2020-01-30"))
                .fechaFin(LocalDate.parse("2020-01-31"))
                .incentivo(otros.incentivoId)
                .medio(new PromoMedioComunica.PromoMedComBuilder().medioId(medioThree).build())
                .conceptos(asList(AD_Talleres.conceptoId, CGA_Car_Service.conceptoId))
                .mercados(singletonList(ES.id))
                .pg1s(singletonList(PG1_2.idPg1))
                .build();
        // Checks
        assertThat(actualList).extracting("idPromo", "codPromo", "fechaInicio", "fechaFin", "experimentoId")
                .containsExactly(
                        tuple(promoA.idPromo, promoA.codPromo, promoA.fechaInicio, promoA.fechaFin, promoA.experimentoId),
                        tuple(promoB.idPromo, promoB.codPromo, promoB.fechaInicio, promoB.fechaFin, promoB.experimentoId)
                );
        assertThat(actualList).extracting("mercados", "conceptos", "pg1s", "incentivo", "promoMedioComunica.medioId", "promoMedioComunica.textMsg")
                .containsExactly(
                        tuple(promoA.mercados, promoA.conceptos, promoA.pg1s, promoA.incentivo, promoA.promoMedioComunica.medioId, promoA.promoMedioComunica.textMsg),
                        tuple(promoB.mercados, promoB.conceptos, promoB.pg1s, promoB.incentivo, promoB.promoMedioComunica.medioId, promoA.promoMedioComunica.textMsg)
                );
    }

    @Test
    public void test_ExtractPg1sToExperiment()
    {
        List<Integer> pg1A = singletonList(PG1_1.idPg1);
        List<Integer> pg1B = singletonList(PG1_1.idPg1);
        assertThat(extractPg1sToExperiment(pg1A, pg1B)).containsOnly(PG1_1.idPg1);

        pg1B = asList(PG1_1.idPg1, PG1_2.idPg1);
        assertThat(extractPg1sToExperiment(pg1A, pg1B)).containsExactlyInAnyOrder(PG1_1.idPg1);

        final List<Integer> pg1B_2 = singletonList(PG1_2.idPg1);
        assertThatThrownBy(() -> extractPg1sToExperiment(pg1A, pg1B_2))
                .isInstanceOf(ProcessArgException.class).hasMessage(experimento_wrongly_initialized + error_pg1s);
    }

    @Test
    public void test_longComparison()
    {
        List<LongWrapper> longs = asList(new LongWrapper(3L), new LongWrapper(5L), new LongWrapper(1L), new LongWrapper(4L));
        longs.sort(Comparator.comparingLong(value -> value.id));
        assertThat(longs).usingRecursiveComparison().isEqualTo(asList(new LongWrapper(1L), new LongWrapper(3L), new LongWrapper(4L), new LongWrapper(5L)));
    }

    static class LongWrapper {
        final long id;

        LongWrapper(long id)
        {
            this.id = id;
        }
    }
}