package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteSample;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.tbmaster.PG1;

import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static com.lebenlab.ProcessArgException.experimento_overlapping;
import static com.lebenlab.core.Promocion.FieldLabel.promo_id;
import static com.lebenlab.core.Promocion.shortMapper;
import static com.lebenlab.core.experimento.Experimento.ExperimentPromoFull.asPromosList;
import static com.lebenlab.core.experimento.Experimento.ExperimentPromoFull.mapper;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.conceptoIds;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.mercadoIds;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.participante_id;
import static com.lebenlab.core.experimento.SqlQuery.experimento_samples;
import static com.lebenlab.core.experimento.SqlQuery.promos_by_experimentoId;
import static com.lebenlab.core.experimento.SqlUpdate.del_experiment;
import static com.lebenlab.core.experimento.SqlUpdate.insert_experimento;
import static com.lebenlab.core.experimento.SqlUpdate.insert_promo;
import static com.lebenlab.core.experimento.SqlUpdate.insert_promo_conceptos;
import static com.lebenlab.core.experimento.SqlUpdate.insert_promo_incentivo;
import static com.lebenlab.core.experimento.SqlUpdate.insert_promo_mercados;
import static com.lebenlab.core.experimento.SqlUpdate.insert_promo_particip_pg1;
import static com.lebenlab.core.experimento.SqlUpdate.insert_promo_participante;
import static com.lebenlab.core.experimento.SqlUpdate.insert_promo_pg1s;
import static com.lebenlab.core.experimento.SqlUpdate.update_promo_particip_pg1_prev;
import static com.lebenlab.core.experimento.SqlUpdate.update_promo_participante;
import static com.lebenlab.core.mediocom.SqlComunicacion.insert_promo_mediocomunicacion;
import static com.lebenlab.core.tbmaster.ConceptoTaller.values;
import static com.lebenlab.core.tbmaster.PG1.permutationsPromo;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingLong;
import static java.util.List.copyOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;

/**
 * User: pedro@didekin.es
 * Date: 06/02/2020
 * Time: 16:55
 */
public final class ExperimentoDao {

    private static final Logger logger = getLogger(ExperimentoDao.class);

    // ===========================  Experimento síncrono ========================

    static List<List<ParticipanteSample>> getSamples(List<Promocion> promocionesIn)
    {
        logger.debug("getSamples()");
        if (promocionesIn.size() != 2) {
            throw new ProcessArgException(ProcessArgException.error_promos_in_exp);
        }

        try {
            final var singleList = jdbiFactory.getJdbi().withHandle(h -> h.createQuery(experimento_samples.query)
                    .bindList(mercadoIds.name(), promocionesIn.get(0).mercados)
                    .bindList(conceptoIds.name(), promocionesIn.get(0).conceptos)
                    .map(ParticipanteSample.mapper)
                    .list());
            final var sample2 = supplyAsync(() -> sample(singleList, 1));
            return asList(sample(singleList, 0), sample2.join());
        } catch (Exception e) {
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    static List<ParticipanteSample> sample(List<ParticipanteSample> singleList, int modulo)
    {
        logger.debug("sample(), sample{}", modulo);
        return singleList.stream().filter(particip -> particip.rowNumber % 2 == modulo)
                .collect(toList());
    }

    // =====================  Experimento síncrono: inserción componentes experimento ===================

    static boolean isOverlapConceptos(List<Integer> conceptosIn, Handle h)
    {
        logger.debug("isOverlapConceptos()");
        if (conceptosIn.size() == values().length) {
            return true;
        }
        try {
            return !(
                    h.select(SqlQuery.experiments_same_conceptos.query)
                            .bindList(conceptoIds.name(), conceptosIn)
                            .mapTo(Long.class)
                            .list()
                            .isEmpty()
            );
        } catch (Exception e) {
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    static boolean isOverlapDates(LocalDate fechaInicio, LocalDate fechaFin, Handle h)
    {
        logger.debug("isOverlapDates()");
        try {
            return !(
                    h.createQuery(SqlQuery.experiments_same_dates.query)
                            .bind("fechaInicio", fechaInicio)
                            .bind("fechaFin", fechaFin)
                            .mapTo(Long.class)
                            .list()
                            .isEmpty()
            );
        } catch (Exception e) {
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    static boolean isOverlapExperiment(Experimento expIn)
    {
        logger.debug("isOverlapExperiment()");
        try (Handle h = jdbiFactory.getJdbi().open()) {
            return isOverlapDates(expIn.promocion.fechaInicio, expIn.promocion.fechaFin, h)
                    && isOverlapProductos(expIn.pg1sInExperiment(), h)
                    && isOverlapConceptos(expIn.promocion.conceptos, h)
                    && isOverlapMercados(expIn.promocion.mercados, h);
        }
    }

    static boolean isOverlapMercados(List<Integer> mercadosIn, Handle h)
    {
        logger.debug("isOverlapMercados()");
        if (mercadosIn.size() == Mercado.values().length) {
            return true;
        }
        try {
            return !(
                    h.select(SqlQuery.experiments_same_mercados.query)
                            .bindList(mercadoIds.name(), mercadosIn)
                            .mapTo(Long.class)
                            .list()
                            .isEmpty()
            );
        } catch (Exception e) {
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * @param pg1sInPromosExp : PG1s either in promocion or in variante of the experiment.
     */
    static boolean isOverlapProductos(Set<Integer> pg1sInPromosExp, Handle h)
    {
        logger.debug("isOverlapProductos()");

        // Descontamos PG1_0. Si se incluyen todos los PG1s, siempre hay overlap.
        if (pg1sInPromosExp.size() == (PG1.values().length - 1)) {
            return true;
        }
        try {
            return !(
                    h.select(SqlQuery.experiments_same_pg1s.query)
                            .bindList("pg1Ids", pg1sInPromosExp)
                            .mapTo(Integer.class)
                            .list()
                            .isEmpty()
            );
        } catch (Exception e) {
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    static long txInsertExperimento(String experimentoNombre, Handle h) throws JdbiException
    {
        logger.info("txInsertExperimento()");
        // For now, it is assumed that one experiment has only two promos associated.
        return h.createUpdate(insert_experimento.statement)
                .bind("nombre", experimentoNombre)
                .executeAndReturnGeneratedKeys()
                .mapTo(Integer.class)
                .one();
    }

    static public long txInsertPromo(Promocion promoIn, Handle h) throws JdbiException
    {
        logger.info("txInsertPromo()");
        return h.createUpdate(insert_promo.statement)
                .bindFields(promoIn)
                .executeAndReturnGeneratedKeys()
                .mapTo(Integer.class)
                .first();
    }

    static int txInsertPromoConceptos(Promocion promoIn, Handle h) throws JdbiException
    {
        logger.info("txInsertPromoConceptos()");
        PreparedBatch batch = h.prepareBatch(insert_promo_conceptos.statement);
        for (int conceptoId : promoIn.conceptos) {
            batch.bind("idPromo", promoIn.idPromo).bind("cod", conceptoId).add();
        }
        return stream(batch.execute()).sum();
    }

    static int txInsertPromoIncentivo(Promocion promoIn, Handle h) throws JdbiException
    {
        logger.info("txInsertPromoIncentivo()");
        return h.createUpdate(insert_promo_incentivo.statement)
                .bind("idPromo", promoIn.idPromo)
                .bind("incentivo", promoIn.incentivo)
                .execute();
    }

    static PromoMedioComunica txInsertPromoMedioCom(Promocion promoIn, Handle h)
    {
        logger.info("txInsertPromoMedioCom()");
        final var promoMedIn = new PromoMedioComunica.PromoMedComBuilder()
                .copy(promoIn.promoMedioComunica)
                .promoId(promoIn.idPromo)
                .build();
        int recordsPromoMedio = h.createUpdate(insert_promo_mediocomunicacion.statement)
                .bindFields(promoMedIn)
                .execute();
        if (recordsPromoMedio == 1) {
            return promoMedIn;
        }
        throw new ProcessArgException(error_jdbi_statement + "Inserción incorrrecta de PromoMedioComunicacion");
    }

    static int txInsertPromoMercados(Promocion promoIn, Handle h) throws JdbiException
    {
        logger.info("txInsertPromoMercados()");
        PreparedBatch batch = h.prepareBatch(insert_promo_mercados.statement);
        for (int idMercado : promoIn.mercados) {
            batch.bind("idPromo", promoIn.idPromo).bind("idMercado", idMercado).add();
        }
        return stream(batch.execute()).sum();
    }

    static int txInsertPromoProd(Promocion promoIn, Handle h) throws JdbiException
    {
        logger.info("txInsertPromoProd()");
        PreparedBatch batch = h.prepareBatch(insert_promo_pg1s.statement);
        final var mapPg1s = permutationsPromo(promoIn.pg1s);
        mapPg1s.forEach(
                (pg1_result, pg1s_with) -> batch
                        .bind("idPromo", promoIn.idPromo)
                        .bind("idPg1", pg1_result.idPg1)
                        .bind("idPg1_with_1", pg1s_with.get(0).idPg1)
                        .bind("idPg1_with_2", pg1s_with.get(1).idPg1)
                        .add()
        );
        return stream(batch.execute()).sum();
    }

    static public List<Promocion> txPromosExperiment(Experimento experimentoIn)
    {
        logger.info("txPromosExperiment()");
        if (isOverlapExperiment(experimentoIn)) {
            logger.error("txPromosExperiment(): experiments overlapping");
            throw new ProcessArgException(experimento_overlapping + experimentoIn.nombre);
        }

        List<Promocion> promosIn = new ArrayList<>(2);
        promosIn.add(experimentoIn.promocion);
        promosIn.add(experimentoIn.variante.asPromocion(experimentoIn.promocion));

        try (Handle handle = jdbiFactory.getJdbi().open()) {
            return handle.inTransaction(
                    READ_COMMITTED,
                    h -> {
                        long experimentPk = txInsertExperimento(experimentoIn.nombre, h);
                        Promocion promoPk;
                        PromoMedioComunica proMedComPk;
                        List<Promocion> pksPromos = new ArrayList<>(promosIn.size());
                        for (Promocion promoIn : promosIn) {
                            promoPk = new Promocion.PromoBuilder().copyPromo(promoIn).experimentoId(experimentPk).build();
                            promoPk = new Promocion.PromoBuilder()
                                    .copyPromo(promoPk)
                                    .idPromo(txInsertPromo(promoPk, h))
                                    .build();
                            txInsertPromoConceptos(promoPk, h);
                            txInsertPromoIncentivo(promoPk, h);
                            proMedComPk = txInsertPromoMedioCom(promoPk, h);
                            txInsertPromoMercados(promoPk, h);
                            txInsertPromoProd(promoPk, h);
                            pksPromos.add(new Promocion.PromoBuilder().copyPromo(promoPk).medio(proMedComPk).build());
                        }
                        pksPromos.sort(comparingLong(o -> o.idPromo));
                        return copyOf(pksPromos);
                    }
            );
        } catch (Exception e) {
            logger.error("txPromosExperiment(): {}", e.getCause().getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    // ===========================  Experimento asíncrono: inserción participantes ===========================

    /**
     * @param promoIdIn : the promo whose participantes are to be inserted.
     * @param sample    :  the list of participantes to be inserted.
     * @return number of records inserted in promo_participante.
     */
    static int insertPromoParticipante(long promoIdIn, List<ParticipanteSample> sample)
    {
        logger.info("insertPromoParticipante(), entering ...: sample size {}", sample.size());
        try {
            int insertedRec = jdbiFactory.getJdbi().withHandle(
                    handle -> {
                        PreparedBatch batch1 = handle.prepareBatch(insert_promo_participante.statement);
                        for (ParticipanteSample participante : sample) {
                            batch1.bind("promoId", promoIdIn)
                                    .bind("participId", participante.participId)
                                    .add();
                        }
                        return stream(batch1.execute()).sum();
                    }
            );
            logger.info("insertPromoParticipante(); insertedRec = {}", insertedRec);
            return insertedRec;
        } catch (JdbiException e) {
            logger.error("insertPromoParticipante(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * It updates promo_participante with 'concepto', 'provincia' and 'dias_registro'.
     *
     * @param promoId: the promo whose participant data are updated in the data base.
     * @return number of records updated.
     */
    static int updatePromoParticipante(long promoId)
    {
        logger.info("updatePromoParticipante(); entering ...");
        try {
            var updatedParticip = jdbiFactory.getJdbi().withHandle(
                    h -> h.createUpdate(update_promo_participante.statement)
                            .bind("promo_id", promoId)
                            .execute()
            );
            logger.info("updatePromoParticipante(); updated = {}", updatedParticip);
            return updatedParticip;
        } catch (JdbiException e) {
            logger.error("updatePromoParticipante(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * It inserts the primary keys of the table promo_participante_pg1.
     *
     * @return number of records inserted.
     */
    static int insertPromoParticipProducto(long idPromo)
    {
        logger.info("insertPromoParticipProducto(); entering ...");
        try {
            int insertedRec = jdbiFactory.getJdbi().withHandle(
                    h -> h.createUpdate(insert_promo_particip_pg1.statement)
                            .bind("promo_id", idPromo)
                            .execute()
            );
            logger.info("insertPromoParticipProducto(); inserted records = {}", insertedRec);
            return insertedRec;
        } catch (JdbiException e) {
            logger.error("insertPromoParticipProducto(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * Updates promo_participante_pg1 with results of previous promos to the 'record' promo in this table.
     */
    static int updatePromoParticipProducto(long idPromo)
    {
        logger.info("updatePromoParticipProducto(); entering ...");
        try {
            final var updatedRec = jdbiFactory.getJdbi()
                    .withHandle(h -> h.createUpdate(update_promo_particip_pg1_prev.statement)
                            .bind("promo_id", idPromo)
                            .execute());
            logger.info("updatePromoParticipProducto(); updated = {}", updatedRec);
            return updatedRec;
        } catch (JdbiException e) {
            logger.error("updatePromoParticipProducto(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    // =========================  Plots  ==========================

    public static double[][] prPartPg1ForClusterPlot(long promoId, int pg1Id)
    {
        List<PromoParticipantePg1> dataPromo = promoParticipantesPg1(promoId, pg1Id);
        double[][] arrPr1 = new double[dataPromo.size()][];
        int i = 0;
        for (PromoParticipantePg1 pr_part_pg1 : dataPromo) {
            arrPr1[i++] = new double[]{
                    pr_part_pg1.vtaMediaDiariaPg1Exp,
                    pr_part_pg1.vtaMediaDiariaPg1};
        }
        return arrPr1;
    }

    public static List<PromoParticipantePg1> promoParticipantesPg1(long promoId, int pg1Id)
    {
        try {
            List<PromoParticipantePg1> listRecords = jdbiFactory.getJdbi().withHandle(
                    h -> h.createQuery(SqlQuery.particip_by_promo_pg1.query)
                            .bindList("promoIds", singletonList(promoId))
                            .bindList("pg1Ids", singletonList(pg1Id))
                            .map(PromoParticipantePg1.mapper)
                            .collect(toList())
            );
            logger.info("promoParticipantesPg1(); participantes: {}", listRecords.size());
            return listRecords;
        } catch (JdbiException e) {
            logger.error("promoParticipantesPg1(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    // ============================= Utilities ==============================

    public static int deleteExperiment(long experimentoId)
    {
        logger.info("deleteExperiment(): {}", experimentoId);
        return jdbiFactory.getJdbi().withHandle(
                handle -> handle.execute(del_experiment.statement, experimentoId)
        );
    }

    public static Experimento getExperimentoById(long experimentoId)
    {
        logger.debug("getExperimentoById(): {}", experimentoId);
        return jdbiFactory.getJdbi().withHandle(
                handle -> handle.select(SqlQuery.experiment_byId.query, experimentoId)
                        .map(
                                (r, columnNumber, ctx) -> new Experimento.ExperimentoBuilder()
                                        .nombre(r.getString("nombre"))
                                        .experimentoId(r.getLong("experimento_id"))
                                        .buildMinimal()
                        )
                        .one()
        );
    }

    public static List<Experimento> getExperimentos()
    {
        logger.info("getExperimentos()");
        return jdbiFactory.getJdbi().withHandle(
                handle -> handle.createQuery(SqlQuery.experimentos.query)
                        .map(Experimento.ExperimentPromoShort.mapper)
                        .list()
                        .stream()
                        .collect(groupingBy(Experimento.ExperimentPromoShort::getExperimentId, toList()))
                        .values()
                        .stream()
                        .map(list -> new Experimento.ExperimentoBuilder(list).buildForSummary())
                        .collect(toList())
        );
    }

    /**
     * Mainly for tests.
     */
    public static Promocion insertOnePromo(Handle h, Promocion promoIn, String expNombre)
    {
        Promocion promoPk =
                new Promocion.PromoBuilder().copyPromo(promoIn)
                        .experimentoId(txInsertExperimento(expNombre, h))
                        .build();
        final var idPromo = txInsertPromo(promoPk, h);
        promoPk = new Promocion.PromoBuilder()
                .copyPromo(promoPk)
                .medio(new PromoMedioComunica.PromoMedComBuilder().copy(promoPk.promoMedioComunica).promoId(idPromo).build())
                .idPromo(idPromo).build();
        return promoPk;
    }

    /**
     * Mainly for tests.
     */
    public static Promocion promocionById(long promoIdIn)
    {
        logger.info("promocionById()");
        return jdbiFactory.getJdbi().withHandle(
                handle -> handle.select(SqlQuery.promo_byId.query, promoIdIn)
                        .map(shortMapper)
                        .one()
        );
    }

    public static List<Promocion> promosByExperiment(long experimentoId)
    {
        logger.info("promosByExperiment()");
        try {
            return asPromosList(
                    jdbiFactory.getJdbi().withHandle(
                            h -> h.select(promos_by_experimentoId.query, experimentoId).map(mapper).list()
                    ),
                    experimentoId
            );
        } catch (JdbiException e) {
            logger.warn("promosByExperiment(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * Mainly for tests.
     */
    static List<Map<String, Object>> promoDaysOn()
    {
        return jdbiFactory.getJdbi().withHandle(
                h -> h.select(SqlQuery.promos_days_on.query)
                        .mapToMap()
                        .list()
        );
    }

    /**
     * Mainly for tests.
     */
    static List<PromoParticipante> promoParticipantes(long promoId)
    {
        return jdbiFactory.getJdbi().withHandle(
                h -> h.select(SqlQuery.participante_by_promo.query, promoId)
                        .map((r, ctx) -> new PromoParticipante(
                                        r.getLong("promo_id"),
                                        r.getLong("participante_id"),
                                        r.getInt("concepto_id"),
                                        r.getInt("provincia_id"),
                                        r.getInt("dias_registro")
                                )
                        )
                        .list());
    }

    /**
     * Mainly for tests.
     */
    static List<PromoParticipantePg1> promoParticipantesPg1(long promoId)
    {
        return jdbiFactory.getJdbi().withHandle(
                h -> h.select(SqlQuery.participantes_pg1_by_promo.query, promoId)
                        .map(PromoParticipantePg1.mapper)
                        .list());
    }

    // ====================  Static classes ==================

    public static class PromoParticipantePg1 {

        public final long promoId;
        public final long participanteId;
        public final int pg1Id;
        public final double vtaMediaDiariaPg1Exp;
        public final double vtaMediaDiariaPg1;

        PromoParticipantePg1(long promoId, long participanteId, int pg1Id, double vtaMediaDiariaPg1Exp, double vtaMediaDiariaPg1)
        {
            this.promoId = promoId;
            this.participanteId = participanteId;
            this.pg1Id = pg1Id;
            this.vtaMediaDiariaPg1Exp = vtaMediaDiariaPg1Exp;
            this.vtaMediaDiariaPg1 = vtaMediaDiariaPg1;
        }

        static final RowMapper<PromoParticipantePg1> mapper = (rs, ctx) -> new PromoParticipantePg1(
                rs.getLong(promo_id.name()),
                rs.getLong(participante_id.name()),
                rs.getInt("pg1_id"),
                rs.getDouble("vta_media_diaria_pg1_exp"),
                rs.getDouble("vta_media_diaria_pg1")
        );
    }
}