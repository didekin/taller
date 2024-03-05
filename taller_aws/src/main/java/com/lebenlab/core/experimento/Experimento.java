package com.lebenlab.core.experimento;

import com.lebenlab.BeanBuilder;
import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.mediocom.PromoMedioComunica;

import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.lebenlab.DataPatterns.EXPERIMENTO;
import static com.lebenlab.ProcessArgException.error_pg1s;
import static com.lebenlab.ProcessArgException.error_promos_in_exp;
import static com.lebenlab.ProcessArgException.experimento_wrongly_initialized;
import static com.lebenlab.ProcessArgException.result_experiment_wrongly_initialized;
import static com.lebenlab.core.Promocion.FieldLabel.cod_promo;
import static com.lebenlab.core.Promocion.FieldLabel.concepto_id;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_fin;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_inicio;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo_id;
import static com.lebenlab.core.Promocion.FieldLabel.mercado_id;
import static com.lebenlab.core.Promocion.FieldLabel.pg1_id;
import static com.lebenlab.core.Promocion.FieldLabel.promo_id;
import static com.lebenlab.core.experimento.Experimento.FieldLabel.exp_nombre;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin.es
 * Date: 06/02/2020
 * Time: 15:45
 */
public class Experimento implements Jsonable {

    private static final Logger logger = getLogger(Experimento.class);

    public final long experimentoId;
    public final Promocion promocion;
    public final PromoVariante variante;
    public final String nombre;

    private Experimento(ExperimentoBuilder builder)
    {
        experimentoId = builder.experimentoId;
        promocion = builder.promocion;
        variante = builder.variante;
        nombre = builder.nombre;
    }

    public long getExperimentoId()
    {
        return experimentoId;
    }

    public Promocion getPromocion()
    {
        return promocion;
    }

    public PromoVariante getVariante()
    {
        return variante;
    }

    public List<Promocion> getPromos()
    {
        return List.of(promocion, variante.asPromocion(promocion));
    }

    public String getNombre()
    {
        return nombre;
    }

    @NotNull
    public List<Integer> pg1sToExperiment()
    {
        return extractPg1sToExperiment(promocion.pg1s, variante.pg1s);
    }

    public static List<Integer> extractPg1sToExperiment(List<Integer> pg1sA, List<Integer> pg1sB)
    {
        List<Integer> copyPg1s = new ArrayList<>(min(pg1sA.size(), pg1sB.size()));
        copyPg1s.addAll(pg1sA);
        copyPg1s.retainAll(pg1sB);
        if (copyPg1s.isEmpty()) {
            throw new ProcessArgException(experimento_wrongly_initialized + error_pg1s);
        }
        return copyPg1s;
    }

    @NotNull
    public static Set<Integer> pg1sInExperiment(List<Integer> pg1sA, List<Integer> pg1sB)
    {
        Set<Integer> copyPg1s = new HashSet<>(max(pg1sA.size(), pg1sB.size()));
        copyPg1s.addAll(pg1sA);
        copyPg1s.addAll(pg1sB);
        return copyPg1s;
    }

    @NotNull
    public Set<Integer> pg1sInExperiment()
    {
        return pg1sInExperiment(promocion.pg1s, variante.pg1s);
    }

    // ======================== Builder ======================

    public final static class ExperimentoBuilder implements BeanBuilder<Experimento> {

        private long experimentoId;
        private Promocion promocion;
        private PromoVariante variante;
        private String nombre;

        public ExperimentoBuilder()
        {
        }

        /**
         * Builder for a lists of experiments.
         *
         * @param experimentsPromo is assumed to have only two elements.
         */
        public ExperimentoBuilder(List<ExperimentPromoShort> experimentsPromo)
        {
            experimentoId(experimentsPromo.get(0).experimentId);
            nombre(experimentsPromo.get(0).experimentName);
            promocion(new Promocion.PromoBuilder()
                    .copyPromo(experimentsPromo.get(0).promocion)
                    .experimentoId(experimentsPromo.get(0).experimentId)
                    .buildForSummary());
            variante(new Promocion.PromoBuilder()
                    .copyPromo(experimentsPromo.get(1).promocion)
                    .experimentoId(experimentsPromo.get(0).experimentId)
                    .buildForSummary().asVarianteSummary());
        }

        /**
         * Builder for html forms.
         */
        public ExperimentoBuilder(Map<String, List<String>> expParams)
        {
            nombre(expParams.get(exp_nombre.name()).get(0));
            promocion(new Promocion.PromoBuilder(expParams).build());
            variante(new PromoVariante.VarianteBuilder(expParams).build());
        }

        /**
         * Builder for tests.
         */
        public ExperimentoBuilder(Experimento experimentoIn)
        {
            experimentoId(experimentoIn.experimentoId)
                    .promocion(experimentoIn.promocion).variante(experimentoIn.variante).nombre(experimentoIn.nombre);
        }

        @SuppressWarnings("UnusedReturnValue")
        public ExperimentoBuilder experimentoId(long id)
        {
            experimentoId = id;
            return this;
        }

        public ExperimentoBuilder promocion(Promocion promoIn)
        {
            promocion = promoIn;
            return this;
        }

        public ExperimentoBuilder variante(PromoVariante varianteIn)
        {
            variante = varianteIn;
            return this;
        }

        public ExperimentoBuilder nombre(String nombreIn)
        {
            if (EXPERIMENTO.isPatternOk(nombreIn)) {
                nombre = nombreIn;
                return this;
            }
            throw new ProcessArgException(experimento_wrongly_initialized);
        }

        @Override
        public Experimento build()
        {
            Experimento exp = new Experimento(this);
            if (exp.nombre == null
                    || exp.nombre.isEmpty()
                    || exp.promocion == null
                    || exp.variante == null
                    || exp.promocion.codPromo.equals(exp.variante.codPromo)
                    || exp.pg1sToExperiment().isEmpty()) {
                logger.error("build(): experimento mal construido.");
                throw new ProcessArgException(experimento_wrongly_initialized);
            }
            return exp;
        }

        public Experimento buildForSummary()
        {
            Experimento exp = new Experimento(this);
            if (exp.promocion == null
                    || exp.variante == null
                    || exp.nombre == null
                    || exp.nombre.isEmpty()
            ) {
                logger.error("buildForSummary(): experimento mal construido.");
                throw new ProcessArgException(experimento_wrongly_initialized);
            }
            return exp;
        }

        public Experimento buildMinimal()
        {
            Experimento exp = new Experimento(this);
            if (exp.experimentoId <= 0
                    || exp.nombre == null
                    || exp.nombre.isEmpty()
            ) {
                logger.error("buildMinimal(): experimento mal construido.");
                throw new ProcessArgException(experimento_wrongly_initialized);
            }
            return exp;
        }
    }

    // ======================== Labels ======================

    /**
     * For the promocion and variante, labels are the same as in Promocion.FieldLabels and PromoVariante.FieldLabels.
     */
    public enum FieldLabel {
        exp_nombre,
    }

    // ======================== Mappers ======================

    /**
     * Container class for a join record of experimento and promo records (short version).
     */
    public static final class ExperimentPromoShort {

        public static final RowMapper<ExperimentPromoShort> mapper = (rs, ctx) -> new ExperimentPromoShort(
                rs.getLong("experimento_id"),
                rs.getString("nombre"),
                new Promocion.PromoBuilder()
                        .idPromo(rs.getLong(promo_id.name()))
                        .codPromo(rs.getString("cod_promo"))
                        .fechaInicio(rs.getDate("fecha_inicio").toLocalDate())
                        .fechaFin(rs.getDate("fecha_fin").toLocalDate())
                        .incentivo(rs.getInt("incentivo_id"))
                        .buildForSummary()
        );

        public final long experimentId;
        public final String experimentName;
        public final Promocion promocion;

        public ExperimentPromoShort(long experimentId, String experimentName, Promocion promocion)
        {
            this.experimentId = experimentId;
            this.experimentName = experimentName;
            this.promocion = promocion;
        }

        public long getExperimentId()
        {
            return experimentId;
        }
    }

    public static final class ExperimentPromoFull {

        public static final RowMapper<ExperimentPromoFull> mapper = (rs, ctx) -> new ExperimentPromoFull(
                rs.getLong(promo_id.name()),
                rs.getString(cod_promo.name()),
                rs.getDate(fecha_inicio.name()).toLocalDate(),
                rs.getDate(fecha_fin.name()).toLocalDate(),
                rs.getInt(incentivo_id.name()),
                new PromoMedioComunica.PromoMedComBuilder()
                        .promoId(rs.getLong(promo_id.name()))
                        .medioId(rs.getInt(medio_id.name()))
                        .textMsg(rs.getString(promo_medio_text.name()))
                        .build(),
                rs.getInt(concepto_id.name()),
                rs.getInt(mercado_id.name()),
                rs.getInt(pg1_id.name())
        );
        final long promoId;
        final String codPromo;
        final LocalDate fechaInicio;
        final LocalDate fechaFin;
        final int incentivoId;
        final PromoMedioComunica promoMedioCom;
        final int conceptoId;
        final int mercadoId;
        final int pg1Id;

        public ExperimentPromoFull(long promo_id, String cod_promo, LocalDate fecha_inicio, LocalDate fecha_fin,
                                   int incentivo_id, PromoMedioComunica promoMedComIn, int concepto_id, int mercado_id, int pg1_id)
        {
            promoId = promo_id;
            codPromo = cod_promo;
            fechaInicio = fecha_inicio;
            fechaFin = fecha_fin;
            incentivoId = incentivo_id;
            promoMedioCom = new PromoMedioComunica.PromoMedComBuilder().copy(promoMedComIn).build();
            conceptoId = concepto_id;
            mercadoId = mercado_id;
            pg1Id = pg1_id;
        }

        /**
         * It groups in a list of two elements (one for each promo in the experiment) the definition of each of them.
         * Invariants: we do not check the maximum number of records for an experiment (2) at this point.
         *
         * @param recordsDb:    a list with one or more records for each of the two promos in the experiment,
         *                      with the result of a 1 -> 2n map of each of the promotion to its definition variables.
         * @param experimentId: id of the experiment.
         * @return a list with two promos of one experiment.
         */
        public static List<Promocion> asPromosList(List<ExperimentPromoFull> recordsDb, long experimentId)
        {
            if (recordsDb == null || recordsDb.size() == 0 || experimentId == 0) {
                throw new ProcessArgException(result_experiment_wrongly_initialized + "no hay promos para experimento " + experimentId);
            }

            Map<Long, List<ExperimentPromoFull>> listByPromo = recordsDb.stream().collect(groupingBy(pro -> pro.promoId, toList()));
            List<Promocion> promosOut = new ArrayList<>(2);
            listByPromo.forEach(
                    (promoId, experimentPromos) ->
                            promosOut.add(new Promocion.PromoBuilder()
                                    .experimentoId(experimentId)
                                    .idPromo(promoId)
                                    // Variables con un único valor en todos los registros de una misma promo.
                                    .codPromo(experimentPromos.get(0).codPromo)
                                    .fechaInicio(experimentPromos.get(0).fechaInicio)
                                    .fechaFin(experimentPromos.get(0).fechaFin)
                                    .incentivo(experimentPromos.get(0).incentivoId)
                                    .medio(experimentPromos.get(0).promoMedioCom)
                                    // Variables con diferentes valores en los registros de una promo.
                                    .mercados(experimentPromos.stream().mapToInt(expro -> expro.mercadoId).distinct().boxed().collect(toList()))
                                    .conceptos(experimentPromos.stream().mapToInt(expro -> expro.conceptoId).distinct().boxed().collect(toList()))
                                    .pg1s(experimentPromos.stream().mapToInt(expro -> expro.pg1Id).distinct().boxed().collect(toList())).build())

            );
            return promosOut;
        }
    }
}
