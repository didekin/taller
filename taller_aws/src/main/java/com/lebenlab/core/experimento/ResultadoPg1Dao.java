package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.CsvParserResultPg1.ResultPg1;
import com.lebenlab.core.experimento.ResultadoExp.PG1ResultByPromoWithAvg;
import com.lebenlab.core.experimento.ResultadoExp.Pg1ResultExp;
import com.lebenlab.core.experimento.ResultadoExp.Pg1ResultExpBuilder;
import com.lebenlab.core.experimento.ResultadoExp.ResultadoExpBuilder;
import com.lebenlab.core.simulacion.ModelForSimulationDao;
import com.lebenlab.core.simulacion.ModelRowDf;
import com.lebenlab.core.simulacion.RndForestSmile;
import com.lebenlab.core.tbmaster.PG1;

import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import smile.stat.hypothesis.TTest;

import static com.lebenlab.AsyncUtil.executor;
import static com.lebenlab.DataPatterns.EXPERIMENTO_ID;
import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static com.lebenlab.ProcessArgException.experimento_wrongly_initialized;
import static com.lebenlab.ProcessArgException.file_error_reading_msg;
import static com.lebenlab.ProcessArgException.num_promos_by_pg1_wrong;
import static com.lebenlab.ProcessArgException.result_experiment_more_3_pg1s;
import static com.lebenlab.ProcessArgException.result_experiment_no_pg1s;
import static com.lebenlab.ProcessArgException.result_experiment_wrongly_initialized;
import static com.lebenlab.ProcessArgException.tTest_error_generic;
import static com.lebenlab.core.Promocion.FieldLabel.promo_id;
import static com.lebenlab.core.experimento.CsvParserResultPg1.parser;
import static com.lebenlab.core.experimento.Experimento.extractPg1sToExperiment;
import static com.lebenlab.core.experimento.ExperimentoDao.promosByExperiment;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.participante_id;
import static com.lebenlab.core.experimento.ResultadoPg1Dao.PromoPg1ParticipResult.mapper;
import static com.lebenlab.core.experimento.SqlQuery.particip_by_promo_pg1;
import static com.lebenlab.core.experimento.SqlUpdate.del_results;
import static com.lebenlab.core.experimento.SqlUpdate.insert_resultado_pg1;
import static com.lebenlab.core.experimento.SqlUpdate.update_promo_days_on;
import static com.lebenlab.core.experimento.SqlUpdate.update_promo_particip_pg1_act;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.aperturasInExperimento;
import static com.lebenlab.core.simulacion.ModelForSimulationDao.modelSimulateDao;
import static com.lebenlab.core.simulacion.RndForestSmile.rndForestSmile;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static com.lebenlab.smile.Smiler.doTtest;
import static java.lang.Long.parseLong;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparingLong;
import static java.util.Map.copyOf;
import static java.util.Objects.hash;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 26/02/2020
 * Time: 17:15
 * <p>
 * Transformación resultado --> respuesta de una promoción-participante:
 * - Un resultado sólo computará como respuesta si se dan las dos siguientes condiciones simultáneamente:
 * -- la fecha está dentro del intervalo fecha_inicial - fecha_final de un experimento (las promociones
 * de un experimento tienen la misma fecha).
 * -- el PG1 está incluido en los pg1s de una de las dos promociones del experimento que cumple las dos condiciones anteriores.
 */
public class ResultadoPg1Dao {

    private static final Logger logger = getLogger(ResultadoPg1Dao.class);
    public static final int minimum_for_tTest = 25;
    public static final ResultadoPg1Dao resultPg1Dao = new ResultadoPg1Dao(modelSimulateDao);

    private final ModelForSimulationDao modelSimDao;
    private final RndForestSmile randomForest;

    private ResultadoPg1Dao(ModelForSimulationDao modelSimDao)
    {
        this.modelSimDao = modelSimDao;
        randomForest = rndForestSmile(modelSimDao);
    }

    // ============================ Handle fichero de ventas =========================

    /**
     * Delete table records before updating it.
     */
    public int handleVentasFile(InputStream fileContentResults)
    {
        logger.info("handleVentasFile()");
        // TODO. Mejora: antes de borrar, cargar registros. Si dos registros son iguales, no actualizarlo. Borrar, después de cargar, todos
        // los registros con fecha de carga anterior a la fecha de la última que se acaba de hacer.
        deleteAllResults();
        int insertedResults = insertResults(fileContentResults);
        if (insertedResults > 0) {
            handleVentasAsync(modelSimDao::rowsForModelDf);
        }
        return insertedResults;
    }

    public int insertResults(InputStream fileContentResults)
    {
        logger.info("insertResults()");
        try {
            List<ResultPg1> results = parser.readCsvResultPg1(fileContentResults);
            logger.info("insertResults(); after parsing file of results");
            return jdbiFactory.getJdbi().withHandle(
                    handle -> {
                        PreparedBatch batch = handle.prepareBatch(insert_resultado_pg1.statement);
                        for (ResultPg1 resultPg1 : results) {
                            batch.bindFields(resultPg1).add();
                        }
                        return stream(batch.execute()).sum();
                    }
            );
        } catch (IOException e) {
            logger.warn("insertResults(): {}", e.getMessage());
            throw new ProcessArgException(file_error_reading_msg);
        }
    }

    public int deleteAllResults()
    {
        logger.info("deleteAllResults()");
        try {
            return jdbiFactory.getJdbi().withHandle(handle -> handle.execute(del_results.statement));
        } catch (JdbiException e) {
            logger.error("deleteAllResults(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    // ...................  (asynchronous) ...................

    public void handleVentasAsync(Function<Integer, List<ModelRowDf>> modelRowsDfFromProdId)
    {
        logger.debug("handleVentasAsync()");
        ExecutorService executorIn = executor(2);
        runAsync(this::updatePromoDaysOn, executorIn)
                .thenRun(this::updatePromoParticipPg1Act)
                .thenRunAsync(() -> randomForest.upDateModel(modelRowsDfFromProdId), executorIn);
    }

    int updatePromoDaysOn()
    {
        try {
            int updatedRec = jdbiFactory.getJdbi()
                    .withHandle(h -> h.createUpdate(update_promo_days_on.statement).execute());
            logger.info("updatePromoDaysOn(); updated records = {}", updatedRec);
            return updatedRec;
        } catch (Exception e) {
            logger.error("updatePromoDaysOn(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * Updates promo_participantes with the results of a promo. If the same sales records are upload in
     * different (consecutively uploaded) files,
     * sales records are accumulated twice in the balance of the participante.
     */
    int updatePromoParticipPg1Act()
    {
        try {
            var updatedRec = jdbiFactory.getJdbi()
                    .withHandle(h -> h.createUpdate(update_promo_particip_pg1_act.statement).execute());
            logger.info("updatePromoParticipPg1Act(); updated records = {}", updatedRec);
            return updatedRec;
        } catch (JdbiException e) {
            logger.error("updatePromoParticipPg1Act(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    // ============================ Handle statistics experimento =========================

    /**
     * @param resultsByPg1Promo: Map<pg1Id, Map<promoId, List<vtaMediaDiariaPg1>>, with a max of 3 pg1s and 2 promos for each one, and a item in the
     *                           List<vtaMediaDiariaPg1> for each participant in the promo.
     * @return Map<PG1, Pg1ResultExp> where Pg1ResultExp contains a list of 2 items, one for the promo and another for the variant of the experiment,
     * and the result of T-test.
     */
    @NotNull
    Map<PG1, Pg1ResultExp> resultsByPg1(Map<Integer, Map<Long, List<Double>>> resultsByPg1Promo)
    {
        logger.info("resultsByPg1()");
        checkInvariantsMapResults(resultsByPg1Promo);

        final var pg1ResultMap = new HashMap<PG1, Pg1ResultExp>(resultsByPg1Promo.keySet().size());

        resultsByPg1Promo.forEach(
                (pg1Id, mapPromoVtaMediaDiariaPg1) ->
                {
                    // Lista con los resultados de las dos promociones de un mismo PG1.
                    List<PG1ResultByPromoWithAvg> resultByPromos = new ArrayList<>(2);
                    // Para cada promoción, construímos una instancia de PG1ResultByPromoWithAvg.
                    mapPromoVtaMediaDiariaPg1.forEach(
                            (promoId, listVtaMediaDiariaPg1) -> resultByPromos.add(
                                    new PG1ResultByPromoWithAvg(
                                            promoId,
                                            listVtaMediaDiariaPg1.stream().mapToDouble(value -> value).sum(),  // suma de la venta media diaria de los participantes.
                                            listVtaMediaDiariaPg1.size() // nº participantes.
                                    )
                            )
                    );
                    resultByPromos.sort(comparingLong(o -> o.promoId));
                    pg1ResultMap.put(fromIntPg1(pg1Id), new Pg1ResultExpBuilder().resultByPromos(resultByPromos).tTest(tTest(mapPromoVtaMediaDiariaPg1)).build());
                }
        );
        return copyOf(pg1ResultMap);
    }

    /**
     * @param promosExp: a list with the two promos related to an experiment.
     * @return a list of results (venta media diaria esperada y venta media diaria real por participante)
     * ordered by pg1, promo and participant. For participants with no results, a zero result is returned.
     */
    List<PromoPg1ParticipResult> resultsByPromoPg1Particip(List<Promocion> promosExp)
    {
        try {
            final var promosExpId = promosExp.stream().mapToLong(pr -> pr.idPromo).distinct().boxed().collect(toList());
            final var pg1sExpId = extractPg1sToExperiment(promosExp.get(0).pg1s, promosExp.get(1).pg1s);
            List<PromoPg1ParticipResult> resWithinDates = jdbiFactory.getJdbi().withHandle(
                    h -> h.createQuery(particip_by_promo_pg1.query)
                            .bindList("promoIds", promosExpId)
                            .bindList("pg1Ids", pg1sExpId)
                            .map(mapper)
                            .collect(toList())
            );
            logger.info("resultsByPromoPg1Particip(); participantes: {}", resWithinDates.size());
            return resWithinDates;
        } catch (JdbiException e) {
            logger.error("resultsByPromoPg1Particip(): {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * @return Map<pg1Id, Map < promoId, List < vtaMediaDiariaPg1>>, with a max of 3 pg1s and 2 promos for each one, and a item in the
     * List<vtaMediaDiariaPg1> for each participant.
     */
    Map<Integer, Map<Long, List<Double>>> resultsByPromoPg1(List<Promocion> promosInExperiment)
    {
        return resultsByPromoPg1Particip(promosInExperiment)
                .stream()
                .collect(
                        groupingBy(re -> re.pg1Id,
                                groupingBy(res -> res.promoId,
                                        mapping(proPg1PartResult -> proPg1PartResult.vtaMediaDiariaPg1, toList())
                                )

                        )
                );
    }

    public ResultadoExpBuilder statisticsExperiment(List<Promocion> promosIn)
    {
        logger.info("statisticsExperiment()");

        final var resultsByPg1Promo = resultsByPromoPg1(promosIn);
        final var resultsByPg1 = resultsByPg1(resultsByPg1Promo);
        return new ResultadoExpBuilder(promosIn)
                .aperturasPercent(aperturasInExperimento(promosIn))
                .resultsPg1(resultsByPg1);
    }

    // ============================= Static utilities ==============================

    /**
     * Invariants: Map<pg1Id, Map<promoId, List<cantidad>>. Invariant: keys pg1Id <= 3; keys promoId = 2.
     */
    static void checkInvariantsMapResults(Map<Integer, Map<Long, List<Double>>> resultsByPg1Promo)
    {
        logger.info("checkInvariantsMapResults()");
        int pg1sInMap = resultsByPg1Promo.keySet().size();
        if (pg1sInMap == 0) {
            throw new ProcessArgException(result_experiment_wrongly_initialized + result_experiment_no_pg1s);
        }
        if (pg1sInMap > 3) {
            throw new ProcessArgException(result_experiment_wrongly_initialized + result_experiment_more_3_pg1s);
        }

        resultsByPg1Promo.forEach(
                (pg1Id, mapPromoResults) -> {
                    if (mapPromoResults.keySet().size() != 2) {
                        throw new ProcessArgException(result_experiment_wrongly_initialized + num_promos_by_pg1_wrong);
                    }
                });
    }

    static void checkTtestInvariants(Long[] promosInPg1)
    {
        logger.debug("checkTtestInvariants(promosIn)");
        if (promosInPg1.length != 2) {
            logger.error("checkTtestInvariants(), error: resultados con número promociones ≠ 2.");
            throw new ProcessArgException(result_experiment_wrongly_initialized + num_promos_by_pg1_wrong);
        }
    }

    static boolean checkTtestInvariants(double[] cantidadesPromoA, double[] cantidadesPromoB)
    {
        logger.debug("checkTtestInvariants(cantidades)");
        if (cantidadesPromoA.length < minimum_for_tTest || cantidadesPromoB.length < minimum_for_tTest) {
            logger.warn("tTest(): resultados < {}", minimum_for_tTest);
            return false;
        }
        if (stream(cantidadesPromoA).sum() == 0 && stream(cantidadesPromoB).sum() == 0) {
            logger.warn("tTest(): resultados == 0");
            return false;
        }
        return true;
    }

    public static List<Promocion> promosExp(String expIdStr)
    {
        logger.info("promosExp()");

        if (!EXPERIMENTO_ID.isPatternOk(expIdStr)) {
            throw new ProcessArgException(experimento_wrongly_initialized + ": " + expIdStr);
        }

        final var experimentoId = parseLong(expIdStr);
        var listPromos = promosByExperiment(experimentoId);
        listPromos.sort(comparingLong(o -> o.idPromo));
        return unmodifiableList(listPromos);
    }

    /**
     * @param mapPromoCantidades: map associated to each PG1 with results in the experiment. The map has two keys (promoA and
     *                            promoB) and to each of them is associated a list with the quantities computed for the PG1 in
     *                            that promo.
     * @return a Smile Ttest instance.
     */
    static TTest tTest(Map<Long, List<Double>> mapPromoCantidades)
    {
        logger.info("tTest()");
        // Extraigo los promoId de cada promoción en el map.
        Long[] promosInPg1 = mapPromoCantidades.keySet().toArray(Long[]::new);
        checkTtestInvariants(promosInPg1);
        // Extraigo un array de resultados (cantidades) para cada promoción.
        double[] cantidadesA = mapPromoCantidades.get(promosInPg1[0]).stream().mapToDouble(value -> value).toArray();
        double[] cantidadesB = mapPromoCantidades.get(promosInPg1[1]).stream().mapToDouble(value -> value).toArray();
        try {
            if (!checkTtestInvariants(cantidadesA, cantidadesB)) {
                return null;
            }
            return doTtest(cantidadesA, cantidadesB, true);
        } catch (Error | Exception e) {
            logger.error("tTest() error/exception: {}", e.getMessage());
            throw new ProcessArgException(tTest_error_generic + e.getMessage());
        }
    }

    static List<Integer> sortPg1s(Promocion promoExpIn)
    {
        logger.debug("sortPg1s()");
        promoExpIn.pg1s.sort(Integer::compareTo);
        return promoExpIn.pg1s;
    }

    // ============================= Mappers ==============================

    /**
     * Wrapper class for the results of one participant in a PG1 common to both promotions in an experiment.
     * Two instances are equal is they share promoId, pg1Id and participId.
     */
    static final class PromoPg1ParticipResult {

        static final RowMapper<PromoPg1ParticipResult> mapper = (rs, ctx) -> new PromoPg1ParticipResult(
                rs.getLong(promo_id.name()),
                rs.getInt("pg1_id"),
                rs.getLong(participante_id.name()),
                rs.getDouble("vta_media_diaria_pg1")
        );

        /**
         * Identificador de una de las promociones en el experimento.
         */
        public final long promoId;
        /**
         * Pg1 al que se refiere el nº de envíos.
         */
        public final int pg1Id;
        /**
         * Participante para el que se recoge el nº de envíos hecho en la promoción.
         */
        public final long participId;
        /**
         * Venta media diaria, hasta el momento de la consulta, hecha por un participante en un pg1 en promoción.
         */
        public final double vtaMediaDiariaPg1;

        public PromoPg1ParticipResult(long promo_id, int pg1_id, long particip_id, double vtaMediaDiariaPg1)
        {
            promoId = promo_id;
            pg1Id = pg1_id;
            participId = particip_id;
            this.vtaMediaDiariaPg1 = vtaMediaDiariaPg1;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof PromoPg1ParticipResult) {
                PromoPg1ParticipResult objectIn = (PromoPg1ParticipResult) obj;
                return participId == objectIn.participId
                        && pg1Id == objectIn.pg1Id
                        && promoId == objectIn.promoId;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return hash(pg1Id, promoId, participId);
        }
    }
}
