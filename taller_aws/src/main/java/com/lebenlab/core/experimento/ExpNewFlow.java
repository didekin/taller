package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteOutCsv;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteSample;
import com.lebenlab.core.mediocom.SmsDao;

import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.lebenlab.AsyncUtil.executor;
import static com.lebenlab.ProcessArgException.error_num_muestras_promociones;
import static com.lebenlab.core.experimento.ExperimentoDao.getSamples;
import static com.lebenlab.core.experimento.ExperimentoDao.insertPromoParticipProducto;
import static com.lebenlab.core.experimento.ExperimentoDao.insertPromoParticipante;
import static com.lebenlab.core.experimento.ExperimentoDao.txPromosExperiment;
import static com.lebenlab.core.experimento.ExperimentoDao.updatePromoParticipProducto;
import static com.lebenlab.core.experimento.ExperimentoDao.updatePromoParticipante;
import static com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteOutCsv.headerCsv;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.insertPromoParticipMedio;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.insertPromoParticipMedioHist;
import static com.lebenlab.core.mediocom.MedioComunicacion.promosOfflineMedio;
import static com.lebenlab.core.mediocom.MedioComunicacion.promosOnlineMedio;
import static com.lebenlab.csv.CsvConstant.newLine;
import static com.lebenlab.csv.CsvConstant.point_csv;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.joining;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 22/05/2021
 * Time: 11:55
 */
public final class ExpNewFlow {

    private static final Logger logger = getLogger(ExpNewFlow.class);

    public static final BiFunction<List<List<ParticipanteSample>>, List<Promocion>, Optional<? extends InputStream>> medioComFlow =
            (samplesList, promosIn) -> {
                logger.info("==========  medioComFlow function  ==========");
                runAsync(
                        () -> SmsDao.of(
                                samplesList.subList(0, promosOnlineMedio(promosIn).size()),
                                promosOnlineMedio(promosIn)
                        ).smsSend(),
                        executor(1)
                );
                return zipSamplesFile(
                        samplesList.subList(2 - promosOfflineMedio(promosIn).size(), 2),
                        promosOfflineMedio(promosIn)
                );
            };

    public static final BiFunction<Optional<InputStream>, Experimento, Optional<? extends InputStream>> newExperimentFlow =
            (inputStream, experimento) -> supplyAsync(() -> inputStream.map(ParticipanteDao::insertParticipantes).orElse(0), executor(1))
                    .thenApply(inserted -> getSamples(experimento.getPromos()))
                    .thenCombine(
                            supplyAsync(() -> txPromosExperiment(experimento), executor(1)),
                            (samplesList, promos) -> {
                                // Ejecuto en nuevo hilo.
                                allPromoParticipAsync(samplesList, promos);
                                // Continúo en hilo de ejecución.
                                return medioComFlow.apply(samplesList, promos);
                            }
                    ).join();


    // ===============================  Off-line promotions =============================

    public static Optional<ByteArrayInputStream> zipSamplesFile(List<List<ParticipanteSample>> samplesList, List<Promocion> promosIn)
    {
        logger.debug("zipSamplesFile()");

        if (samplesList.size() != promosIn.size()) {
            logger.error("zipSamplesFile(): error samplesList.size() != promosIn.size()");
            throw new ProcessArgException(error_num_muestras_promociones);
        }
        if (samplesList.isEmpty()) {
            return empty();
        }

        ByteArrayOutputStream byteArrayZip = new ByteArrayOutputStream(1024);
        try (ZipOutputStream zipOutput = new ZipOutputStream(byteArrayZip, UTF_8)) {
            List<ParticipanteSample> sample;
            for (int i = 0; i < promosIn.size(); ++i) {
                sample = samplesList.get(i);
                writeSampleToZipOut(zipOutput, promosIn.get(i).idPromo, sample);
            }
        } catch (Exception e) {
            throw new ProcessArgException(e.getMessage());
        }
        return of(new ByteArrayInputStream(byteArrayZip.toByteArray()));
    }

    static void writeSampleToZipOut(ZipOutputStream zipOutput, long idPromo, List<ParticipanteSample> sample) throws IOException
    {
        logger.debug("writeSampleToZipOut()");

        zipOutput.putNextEntry(new ZipEntry(idPromo + point_csv.toString()));
        zipOutput.write(writeSampleCsvStr(sample).getBytes(UTF_8));
        zipOutput.closeEntry();
    }

    /**
     * Writes a string for a csv file, withour email and tfno.
     */
    static String writeSampleCsvStr(List<ParticipanteSample> sample)
    {
        logger.debug("writeSampleCsvStr()");

        String strNoHeader = sample.stream()
                .map(particip -> new ParticipanteOutCsv(particip.participId, particip.idFiscal, particip.mercadoId, particip.provinciaId, particip.conceptoId))
                .map(ParticipanteOutCsv::toCsvRecord)
                .collect(joining(newLine.toString()));
        return new StringBuilder(strNoHeader).insert(0, headerCsv).toString();
    }

    // ===============================  SMS experiment =============================


    // ===============================  Email experiment ===========================

    // ======================================= FUTURES =======================================

    /**
     * Sample size is assumed to be always greater than 0.
     */
    static void allPromoParticipAsync(List<List<ParticipanteSample>> samplesList, List<Promocion> promosIn)
    {
        logger.info("allPromoParticipAsync()");

        final var promosSamples = Map.of(promosIn.get(0), samplesList.get(0), promosIn.get(1), samplesList.get(1)).entrySet();
        final var executor = executor(promosSamples.size() * 5);

        allOf(promosSamples.stream()
                .map(entry ->
                        insertAllPromoParticip(entry.getKey(), entry.getValue(), executor)
                                .thenRun(() -> {
                                            // Abre hilo nuevo.
                                            updateAllPromoParticip(entry.getKey(), executor);
                                            // Continúa en el hilo de ejecución: sólo depende de la inserción de participantes de la promoción.
                                            insertPromoParticipMedioHist(entry.getKey().idPromo);
                                        }
                                )
                )
                .toArray(CompletableFuture[]::new));
    }

    static CompletableFuture<Void> insertAllPromoParticip(Promocion promoIn, List<ParticipanteSample> sampleIn, ExecutorService executor)
    {
        logger.debug("insertAllPromoParticip()");
        return runAsync(() -> insertPromoParticipante(promoIn.idPromo, sampleIn), executor)
                .thenRunAsync(
                        () -> {
                            insertPromoParticipProducto(promoIn.idPromo);
                            runAsync(() -> insertPromoParticipMedio(promoIn.promoMedioComunica), executor);
                        }, executor);
    }

    static void updateAllPromoParticip(Promocion promoIn, ExecutorService executorIn)
    {
        logger.debug("updateAllPromoParticip()");
        runAsync(
                () -> {
                    updatePromoParticipante(promoIn.idPromo);
                    updatePromoParticipProducto(promoIn.idPromo);
                },
                executorIn);
    }
}
