package com.lebenlab.core.experimento;

import java.util.function.Function;

import static com.lebenlab.AsyncUtil.executor;
import static com.lebenlab.core.experimento.ExperimentoDao.getExperimentoById;
import static com.lebenlab.core.experimento.ResultadoPg1Dao.promosExp;
import static com.lebenlab.core.experimento.ResultadoPg1Dao.resultPg1Dao;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.recibidosInExperimento;
import static com.lebenlab.core.mediocom.SmsDao.updatePromosSmsDelivered;
import static java.lang.Long.parseLong;
import static java.util.concurrent.CompletableFuture.supplyAsync;

/**
 * User: pedro@didekin
 * Date: 19/06/2021
 * Time: 16:40
 */
public final class ExpStatisticsFlow {

    public static final Function<String, ResultadoExp> statisticsFlow =
            expIdStr -> supplyAsync(() -> promosExp(expIdStr), executor(1))
                    .thenComposeAsync(
                            promos ->
                                    supplyAsync(() -> updatePromosSmsDelivered(promos), executor(1))
                                            .thenCombineAsync(supplyAsync(() -> resultPg1Dao.statisticsExperiment(promos), executor(1)),
                                                    (updated, resultExpBuilder) ->
                                                            resultExpBuilder
                                                                    .experimento(getExperimentoById(parseLong(expIdStr)))
                                                                    .recibidosPercent(recibidosInExperimento(promos))
                                                                    .build()
                                            )
                            , executor(1))
                    .join();
}
