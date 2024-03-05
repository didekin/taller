package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.PromoVariante.VarianteBuilder;
import com.lebenlab.core.experimento.ResultadoExp.PG1ResultByPromoWithAvg;
import com.lebenlab.core.experimento.ResultadoExp.Pg1ResultExp;
import com.lebenlab.core.experimento.ResultadoExp.Pg1ResultExpBuilder;
import com.lebenlab.core.experimento.ResultadoExp.ResultadoExpBuilder;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.tbmaster.PG1;
import com.lebenlab.core.mediocom.AperturasFileDao;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static com.lebenlab.ProcessArgException.result_experiment_wrong_participantes;
import static com.lebenlab.ProcessArgException.result_experiment_wrongly_initialized;
import static com.lebenlab.core.mediocom.AperturasFileDao.NO_MEDIO_IN_PROMO;
import static com.lebenlab.core.util.DataTestExperiment.getTest;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * User: pedro@didekin
 * Date: 04/03/2020
 * Time: 10:52
 */
public class ResultadoExpTest {

    @Test
    public void test_ResultadoExpBuilder_Constructor()
    {
        // More than two promos in an experiment.
        assertThatThrownBy(() -> new ResultadoExpBuilder(asList(promocion1, promocion1, promocion1)))
                .isInstanceOf(ProcessArgException.class)
                .hasMessage(result_experiment_wrongly_initialized + "Nº promociones no es 2");
    }

    @Test  // Pg1ResultBuilder
    public void test_Pg1ResultExpBuilder_1()
    {
        List<PG1ResultByPromoWithAvg> pg1Results = asList(
                new PG1ResultByPromoWithAvg(1L, 112, 1112),
                new PG1ResultByPromoWithAvg(2L, 111, 1111),
                new PG1ResultByPromoWithAvg(3L, 110, 1110));

        // More than two results by PG1.
        assertThatThrownBy(() -> new Pg1ResultExpBuilder().resultByPromos(pg1Results))
                .isInstanceOf(ProcessArgException.class)
                .hasMessage(result_experiment_wrongly_initialized + "nº de resultados erróneo");

        // tTest not passed to builder.
        assertThatThrownBy(() -> new Pg1ResultExpBuilder().resultByPromos(
                asList(
                        new PG1ResultByPromoWithAvg(1L, 112, 1112),
                        new PG1ResultByPromoWithAvg(2L, 111, 1111)
                )).build())
                .isInstanceOf(ProcessArgException.class)
                .hasMessage(result_experiment_wrongly_initialized + "resultados o tTest no válidos");
    }

    @Test
    public void test_Pg1ResultExpBuilder_2()
    {
        // tTest == null
        assertThat(new Pg1ResultExpBuilder().resultByPromos(
                asList(
                        new PG1ResultByPromoWithAvg(1L, 112, 1112),
                        new PG1ResultByPromoWithAvg(2L, 111, 1111)
                ))
                .tTest(null)
                .build().tTestPvalue).isEqualTo(Pg1ResultExpBuilder.intNullValue);
    }

    @Test
    public void test_PG1ResultByPromoWithAvg_getMediaUdsStr()
    {
        assertThat(new PG1ResultByPromoWithAvg(1L, 2345.67, 100).mediaVtaMediaDiariaParticipStr()).isEqualTo("23,46");
        assertThat(new PG1ResultByPromoWithAvg(2L, 0.56, 10).mediaVtaMediaDiariaParticipStr()).isEqualTo("0,06");
    }

    @Test
    public void test_ResultExpBuilder_checkInvariants()
    {
        Promocion promoIn_Ok = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(11L).build();
        Promocion promoIn_Wrong = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(99L).build();
        List<PG1ResultByPromoWithAvg> pg1Results =
                asList(new PG1ResultByPromoWithAvg(promoIn_Ok.idPromo, 111, 1111),
                        new PG1ResultByPromoWithAvg(22L, 222, 2222));
        var pg1ResultExp = new Pg1ResultExpBuilder().resultByPromos(pg1Results).tTest(getTest()).build();
        var resultsPG1 = new HashMap<PG1, Pg1ResultExp>();
        resultsPG1.put(PG1.fromIntPg1(promoIn_Ok.pg1s.get(0)), pg1ResultExp);

        // promoId no es congruente.
        assertThat(new ResultadoExpBuilder().resultsPg1(resultsPG1).promocion(promoIn_Wrong).checkInvariants()).isFalse();

        // promoId and variante id son válidos.
        assertThat(new ResultadoExpBuilder().resultsPg1(resultsPG1).promocion(promoIn_Ok).checkInvariants()).isTrue();

        // variante id inválida == 0.
        pg1Results =
                asList(new PG1ResultByPromoWithAvg(promoIn_Ok.idPromo, 111, 1111),
                        new PG1ResultByPromoWithAvg(0L, 222, 2222));
        pg1ResultExp = new Pg1ResultExpBuilder().resultByPromos(pg1Results).tTest(getTest()).build();
        resultsPG1.put(PG1.fromIntPg1(promoIn_Ok.pg1s.get(0)), pg1ResultExp);
        assertThat(new ResultadoExpBuilder().resultsPg1(resultsPG1).promocion(promoIn_Ok).checkInvariants()).isFalse();
    }

    @Test
    public void test_participantesAperturas()
    {
        var promoIn = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(1L).build();
        var experimento = new Experimento.ExperimentoBuilder().nombre("experimento1").experimentoId(1L).buildMinimal();
        List<PG1ResultByPromoWithAvg> pg1Results1 = asList(
                new PG1ResultByPromoWithAvg(1L, 112, 1112),
                new PG1ResultByPromoWithAvg(2L, 111, 1111)
        );
        var pg1ResultExp1 = new Pg1ResultExpBuilder().resultByPromos(pg1Results1).tTest(getTest()).build();
        var resultsPG1 = new HashMap<PG1, Pg1ResultExp>();
        resultsPG1.put(PG1.fromIntPg1(2), pg1ResultExp1);
        final var resultadoExp = new ResultadoExpBuilder().experimento(experimento)
                .resultsPg1(resultsPG1)
                .promocion(promoIn)
                .variante(new VarianteBuilder("variante", 11, new PromoMedioComunica.PromoMedComBuilder().medioId(1).build())
                        .buildForSummary())
                .aperturasPercent(asList(NO_MEDIO_IN_PROMO, .1234))
                .recibidosPercent(asList(NO_MEDIO_IN_PROMO, .345))
                .build();
        List<Integer> participantesNum = resultadoExp.getNumParticipantes();
        assertThat(participantesNum).containsExactly(1112, 1111);
        assertThat(resultadoExp.getAperturasPercentStr()).containsExactly("NA", "12,3");
        assertThat(resultadoExp.getRecibidosPercentStr()).containsExactly("NA", "34,5");
    }

    @Test
    public void test_checkParticipants_1()
    {
        Promocion promoIn = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(1L).build();
        List<PG1ResultByPromoWithAvg> pg1Results1 = asList(
                new PG1ResultByPromoWithAvg(1L, 112, 1112),
                new PG1ResultByPromoWithAvg(2L, 111, 1111)
        );
        List<PG1ResultByPromoWithAvg> pg1Results2 = asList(
                // Número de participantes diferente en promoción respecto al anterior PG1.
                new PG1ResultByPromoWithAvg(1L, 211, 1000),
                new PG1ResultByPromoWithAvg(2L, 212, 1111)
        );

        var pg1ResultExp1 = new Pg1ResultExpBuilder().resultByPromos(pg1Results1).tTest(getTest()).build();
        var pg1ResultExp2 = new Pg1ResultExpBuilder().resultByPromos(pg1Results2).tTest(getTest()).build();
        var resultsPG1 = new HashMap<PG1, Pg1ResultExp>();
        resultsPG1.put(PG1.fromIntPg1(2), pg1ResultExp1);
        resultsPG1.put(PG1.fromIntPg1(3), pg1ResultExp2);

        assertThatThrownBy(() -> new ResultadoExpBuilder().resultsPg1(resultsPG1).promocion(promoIn).checkParticipants())
                .isInstanceOf(ProcessArgException.class)
                .hasMessage(result_experiment_wrong_participantes);
    }

    @Test
    public void test_checkParticipants_2()
    {
        Promocion promoIn = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(1L).build();
        List<PG1ResultByPromoWithAvg> pg1Results1 = asList(
                new PG1ResultByPromoWithAvg(1L, 112, 1112),
                new PG1ResultByPromoWithAvg(2L, 111, 1111)
        );
        List<PG1ResultByPromoWithAvg> pg1Results2 = asList(
                new PG1ResultByPromoWithAvg(1L, 211, 1112),
                new PG1ResultByPromoWithAvg(2L, 212, 1111)
        );

        var pg1ResultExp1 = new Pg1ResultExpBuilder().resultByPromos(pg1Results1).tTest(getTest()).build();
        var pg1ResultExp2 = new Pg1ResultExpBuilder().resultByPromos(pg1Results2).tTest(getTest()).build();
        var resultsPG1 = new HashMap<PG1, Pg1ResultExp>();
        resultsPG1.put(PG1.fromIntPg1(2), pg1ResultExp1);
        resultsPG1.put(PG1.fromIntPg1(3), pg1ResultExp2);

        assertThat(new ResultadoExpBuilder().resultsPg1(resultsPG1).promocion(promoIn).checkParticipants()).isTrue();
        assertThat(new ResultadoExpBuilder().resultsPg1(resultsPG1).promocion(promoIn).checkInvariants()).isTrue();
    }
}