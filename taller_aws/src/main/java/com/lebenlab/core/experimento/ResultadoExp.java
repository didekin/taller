package com.lebenlab.core.experimento;

import com.lebenlab.BeanBuilder;
import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.tbmaster.PG1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import smile.stat.hypothesis.TTest;

import static com.lebenlab.DataPatterns.getDecimalNumberStr;
import static com.lebenlab.ProcessArgException.result_experiment_no_pg1s;
import static com.lebenlab.ProcessArgException.result_experiment_wrong_participantes;
import static com.lebenlab.ProcessArgException.result_experiment_wrongly_initialized;
import static com.lebenlab.core.mediocom.AperturasFileDao.NO_MEDIO_IN_PROMO;
import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.HALF_UP;
import static java.util.stream.Collectors.toList;

/**
 * User: pedro@didekin
 * Date: 27/02/2020
 * Time: 17:48
 */
public class ResultadoExp implements Jsonable {

    public final Experimento experimento;
    public final Promocion promocion;
    public final PromoVariante variante;
    /**
     * Results by PG1 in common in promo and variante.
     */
    public final Map<PG1, Pg1ResultExp> resultsPg1;
    /**
     * A decimal number in the interval [0,1] equal to number of aperturas divided by number of participantes in the promotion.
     * No control is made about about the fact that fecha_apertura should be in the interval [fecha_inicio, fecha_fin] of the promotion.
     */
    public final List<Double> aperturasPercent;

    public final List<Double> recibidosPercent;

    private ResultadoExp(ResultadoExpBuilder builderIn)
    {
        experimento = builderIn.experimento;
        promocion = builderIn.promocion;
        variante = builderIn.variante;
        resultsPg1 = builderIn.resultsPg1;
        aperturasPercent = builderIn.aperturasPercent;
        recibidosPercent = builderIn.recibidosPercent;
    }

    public Experimento getExperimento()
    {
        return experimento;
    }

    public Promocion getPromocion()
    {
        return promocion;
    }

    public PromoVariante getVariante()
    {
        return variante;
    }

    @SuppressWarnings("unused")
    public Map<PG1, Pg1ResultExp> getResultsPg1()
    {
        return resultsPg1;
    }

    @SuppressWarnings("unused")
    public List<Double> getAperturasPercent()
    {
        return aperturasPercent;
    }

    @SuppressWarnings("unused")
    public List<Double> getRecibidosPercent()
    {
        return recibidosPercent;
    }

    public List<String> getAperturasPercentStr()
    {
        return getPercentStr(aperturasPercent);
    }

    public List<String> getRecibidosPercentStr()
    {
        return getPercentStr(recibidosPercent);
    }

    static List<String> getPercentStr(List<Double> percentsIn)
    {
        return percentsIn.stream().map(value -> {
            if (value == NO_MEDIO_IN_PROMO) {
                return "NA";
            } else {
                return getDecimalNumberStr(value * 100, 1);
            }

        }).collect(toList());
    }

    /**
     * @return a list with two records: number of participantes in promocion, number of participantes in variante.
     */
    public List<Integer> getNumParticipantes()
    {
        PG1 pg1First = resultsPg1.keySet().stream().findFirst().orElseThrow(() -> new ProcessArgException(result_experiment_no_pg1s));
        return Arrays.asList(resultsPg1.get(pg1First).resultByPromos.get(0).participantes, resultsPg1.get(pg1First).resultByPromos.get(1).participantes);
    }

    // =============================  Builder  ===========================

    public static final class ResultadoExpBuilder implements BeanBuilder<ResultadoExp> {

        private Promocion promocion;
        private PromoVariante variante;
        private Experimento experimento;
        private Map<PG1, Pg1ResultExp> resultsPg1;
        private List<Double> aperturasPercent;
        private List<Double> recibidosPercent;

        public ResultadoExpBuilder()
        {
        }

        public ResultadoExpBuilder(List<Promocion> promosIn)
        {
            this();
            if (promosIn.size() != 2) {
                throw new ProcessArgException(result_experiment_wrongly_initialized + "Nº promociones no es 2");
            }
            promocion(promosIn.get(0));
            variante(promosIn.get(1).asVariante());
        }

        public ResultadoExpBuilder experimento(Experimento experimentoIn)
        {
            if (experimentoIn != null) {
                experimento = experimentoIn;
                return this;
            }
            throw new ProcessArgException(result_experiment_wrongly_initialized + "experimento es nulo.");
        }

        public ResultadoExpBuilder promocion(Promocion promoIn)
        {
            if (promoIn != null) {
                promocion = promoIn;
                return this;
            }
            throw new ProcessArgException(result_experiment_wrongly_initialized + "promoción es nula.");
        }

        public ResultadoExpBuilder variante(PromoVariante varianteIn)
        {
            if (varianteIn != null) {
                variante = varianteIn;
                return this;
            }
            throw new ProcessArgException(result_experiment_wrongly_initialized + "variante es nula.");
        }

        public ResultadoExpBuilder resultsPg1(Map<PG1, Pg1ResultExp> resultsPg1In)
        {
            if (resultsPg1In != null && resultsPg1In.size() > 0) {
                resultsPg1 = resultsPg1In;
                return this;
            }
            throw new ProcessArgException(result_experiment_wrongly_initialized + "lista resultados PG1 vacía o nula");
        }

        public ResultadoExpBuilder aperturasPercent(List<Double> aperturasPercentsIn)
        {
            aperturasPercent = aperturasPercentsIn;
            return this;
        }

        public ResultadoExpBuilder recibidosPercent(List<Double> recibidosPercentIn)
        {
            recibidosPercent = recibidosPercentIn;
            return this;
        }

        @Override
        public ResultadoExp build()
        {
            checkInvariants();
            ResultadoExp resultadoExp = new ResultadoExp(this);
            if (resultadoExp.experimento != null
                    && resultadoExp.promocion != null
                    && resultadoExp.variante != null
                    && resultadoExp.resultsPg1 != null
                    && resultadoExp.aperturasPercent != null
                    && resultadoExp.recibidosPercent != null
            ) {
                return resultadoExp;
            }
            throw new ProcessArgException(result_experiment_wrongly_initialized);
        }

        ResultadoExp buildForTest()
        {
            checkInvariants();
            ResultadoExp resultadoExp = new ResultadoExp(this);
            if (resultadoExp.promocion != null
                    && resultadoExp.variante != null
                    && resultadoExp.resultsPg1 != null
                    && resultadoExp.aperturasPercent != null
            ) {
                return resultadoExp;
            }
            throw new ProcessArgException(result_experiment_wrongly_initialized);
        }

        /**
         * Check that the number de participants is the same across all the PG1s for the promocion and similarly for the variante.
         */
        boolean checkParticipants()
        {
            List<Integer> particPromo = new ArrayList<>(resultsPg1.size());
            List<Integer> particVariante = new ArrayList<>(resultsPg1.size());
            resultsPg1.forEach(
                    (pg1, pg1ResultExp) ->
                    {
                        // Number of participants in promocion for different PG1s.
                        particPromo.add(pg1ResultExp.getResultByPromos().get(0).getParticipantes());
                        // Number of participants in variante for different PG1s.
                        particVariante.add(pg1ResultExp.getResultByPromos().get(1).getParticipantes());
                    }
            );
            final var numPromo = particPromo.stream().distinct().count();
            final var numVariante = particVariante.stream().distinct().count();
            if (!(numPromo == numVariante && numPromo == 1)) {
                throw new ProcessArgException(result_experiment_wrong_participantes);
            }
            return true;
        }

        boolean checkInvariants()
        {
            final var pg1s = resultsPg1.keySet();
            boolean isOk = false;
            for (PG1 pg1 : pg1s) {
                isOk = resultsPg1.get(pg1).resultByPromos.get(0).promoId == promocion.idPromo
                        // variante id different from zero.
                        && (resultsPg1.get(pg1).resultByPromos.get(1).promoId > 0)
                        && checkParticipants();
            }
            return isOk;
        }
    }

    // =============================  Auxiliary classess  ===========================

    /**
     * Container class for the results of each PG1 in an experiment.
     */
    public static final class Pg1ResultExp implements Jsonable {

        /**
         * A list with two elements for the same PG1: one for each of the promotions (promo/variant) in the experiment.
         */
        public final List<PG1ResultByPromoWithAvg> resultByPromos;
        /**
         * t-statistic for the results of one PG1 of those included in the experiment.
         */
        public final double tTestPvalue;

        private Pg1ResultExp(Pg1ResultExpBuilder builder)
        {
            resultByPromos = builder.resultByPromos;
            tTestPvalue = builder.tTestPvalue;
        }

        public List<PG1ResultByPromoWithAvg> getResultByPromos()
        {
            return resultByPromos;
        }

        @SuppressWarnings("unused")
        public double gettTestPvalue()
        {
            return tTestPvalue;
        }

        @SuppressWarnings("unused")
        public String gettTestPvalueStr()
        {
            return getDecimalNumberStr(tTestPvalue, 4);
        }
    }

    public final static class PG1ResultByPromoWithAvg {

        public final long promoId;
        /**
         * Media resultante de sumar las venta media diaria de cada participante y dividir por el número de participantes.
         */
        public final double mediaVtaMediaDiariaParticip;
        public final int participantes;

        public PG1ResultByPromoWithAvg(long promoId, double sumaVtaMediaDiariaParticip, int participantes)
        {
            this.promoId = promoId;
            this.participantes = participantes;
            mediaVtaMediaDiariaParticip = sumaVtaMediaDiariaParticip / participantes;
        }

        @SuppressWarnings("unused")
        public double mediaVtaMediaDiariaParticip()
        {
            return mediaVtaMediaDiariaParticip;
        }

        public String mediaVtaMediaDiariaParticipStr()
        {
            return getDecimalNumberStr(mediaVtaMediaDiariaParticip, 2);
        }

        public int getParticipantes()
        {
            return participantes;
        }
    }

    // =============================  Auxiliary classes builders  ===========================

    public static final class Pg1ResultExpBuilder implements BeanBuilder<Pg1ResultExp> {

        static final int intNullValue = -1;
        static final double default_tTest_pvalue = 1717d;
        private List<PG1ResultByPromoWithAvg> resultByPromos;
        private double tTestPvalue = default_tTest_pvalue;

        public Pg1ResultExpBuilder()
        {
        }

        public Pg1ResultExpBuilder resultByPromos(List<PG1ResultByPromoWithAvg> listIn)
        {
            if (listIn != null && listIn.size() == 2) {
                resultByPromos = listIn;
                return this;
            }
            throw new ProcessArgException(result_experiment_wrongly_initialized + "nº de resultados erróneo");
        }

        public Pg1ResultExpBuilder tTest(TTest tTestIn)
        {
            // Round up because p is evaluated as near 0 as possible.
            tTestPvalue = (tTestIn != null) ? valueOf(tTestIn.pvalue).setScale(3, HALF_UP).doubleValue() : intNullValue;
            return this;
        }

        @Override
        public Pg1ResultExp build()
        {
            Pg1ResultExp pg1Result = new Pg1ResultExp(this);
            if (pg1Result.resultByPromos == null || pg1Result.tTestPvalue == default_tTest_pvalue) {
                throw new ProcessArgException(result_experiment_wrongly_initialized + "resultados o tTest no válidos");
            }
            return pg1Result;
        }
    }
}
