package com.lebenlab.core.util;

import com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv;
import com.lebenlab.core.experimento.CsvParserResultPg1;
import com.lebenlab.core.tbmaster.PG1;
import com.lebenlab.jdbi.JdbiFactory;

import org.apache.commons.csv.CSVPrinter;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import smile.math.Random;

import static com.lebenlab.core.FilePath.appfilesPath;
import static com.lebenlab.core.FilePath.modelsDir;
import static com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv.fecha_apertura;
import static com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv.promocion_id;
import static com.lebenlab.core.experimento.CsvParserResultPg1.HeaderResultPg1.fecha_resultado;
import static com.lebenlab.core.experimento.CsvParserResultPg1.HeaderResultPg1.pg1_id;
import static com.lebenlab.core.experimento.CsvParserResultPg1.HeaderResultPg1.uds_pg1;
import static com.lebenlab.core.experimento.SqlQuery.participantes_pk;
import static com.lebenlab.core.tbmaster.PG1.randomInstance;
import static com.lebenlab.core.util.DataTestSimulation.cleanSimTables;
import static com.lebenlab.core.util.RandomUtil.randomLong30;
import static com.lebenlab.core.util.DataTestExperiment.alter_seq_particip;
import static com.lebenlab.core.util.DataTestExperiment.alter_seq_promo;
import static com.lebenlab.core.util.WebConnTestUtils.upLoadDir;
import static com.lebenlab.csv.CsvConstant.delimiter;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.lang.String.valueOf;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.newDirectoryStream;
import static java.time.LocalDate.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.Stream.of;
import static org.apache.commons.csv.CSVFormat.RFC4180;

/**
 * User: pedro@didekin
 * Date: 02/04/2020
 * Time: 11:52
 */
public class FileTestUtil {

    public static final Path downloadDir = appfilesPath.resolve("downloads");
    public static final Path csvAperturas_2 = upLoadDir.resolve("mail_aperturas_2.csv");
    public static final Path csvComunicaciones_6 = upLoadDir.resolve("comunicaciones_6.csv");
    public static final Path csvParticipantes_11577 = upLoadDir.resolve("participantes_11577.csv");
    public static final Path csvParticipantes_3 = upLoadDir.resolve("participantes_3.csv");
    public static final Path csvParticipantes_150 = upLoadDir.resolve("participantes_150.csv");
    public static final Path csvParticipantes_1000 = upLoadDir.resolve("participantes_1000.csv");
    public static final Path csvParticipantes_empty = upLoadDir.resolve("participantes_empty.csv");
    public static final Path csvVentas_2 = upLoadDir.resolve("ventas_2.csv");

    private static final Random rnd = new Random(1117);


    public static void cleanTablesFiles() throws IOException
    {
        cleanSimTables();
        jdbiFactory.getJdbi().withHandle(handle -> handle.execute(alter_seq_particip));
        for (Path file : newDirectoryStream(downloadDir)) {
            deleteIfExists(file);
        }
        for (Path file : newDirectoryStream(modelsDir)) {
            deleteIfExists(file);
        }
    }

    // =====================================================================

    @NotNull
    static String dateForFileVtsTest(int rangeDayBegin, int rangeDayEnd, @SuppressWarnings("SameParameterValue") String month)
    {
        final String vtaDate;
        int[] daysArr = range(rangeDayBegin, rangeDayEnd).toArray();
        final var day = daysArr[rnd.nextInt(daysArr.length)];
        vtaDate = day < 10 ? month + "-0" + day : month + "-" + day;
        return vtaDate;
    }

    /**
     * participante_id;pg1_id;uds_pg1;fecha_resultado
     * 1;11;27;2019-12-31
     * 5;8;3;2019-01-02
     */
    public static void writeVentasFileCsv(Path csvVentasPath, EnumSet<PG1> rangePg1s)
    {
        List<VtasFileRecordWrapper> results = JdbiFactory.jdbiFactory.getJdbi().withHandle(
                handle -> handle.select(participantes_pk.query)
                        .mapTo(Long.class)
                        .list().stream()
                        // 5 vtas por participante.
                        .flatMap(participId -> of(
                                new VtasFileRecordWrapper(participId, rangePg1s, now().minusDays(26L)),
                                new VtasFileRecordWrapper(participId, rangePg1s, now().minusDays(20L)),
                                new VtasFileRecordWrapper(participId, rangePg1s, now().minusDays(14L)),
                                new VtasFileRecordWrapper(participId, rangePg1s, now().minusDays(8L)),
                                new VtasFileRecordWrapper(participId, rangePg1s, now().minusDays(2L))
                                )
                        ).collect(toList()));

        try (CSVPrinter printer =
                     new CSVPrinter(new FileWriter(csvVentasPath.toString()), RFC4180.withDelimiter(delimiter.toString().charAt(0)))) {
            printer.printRecord(CsvParserResultPg1.HeaderResultPg1.participante_id.name(), pg1_id.name(), uds_pg1.name(), fecha_resultado.name());
            for (VtasFileRecordWrapper resultW : results) {
                printer.printRecord(valueOf(resultW.participanteId), valueOf(resultW.pg1Id), valueOf(resultW.udsPg1), resultW.vtaDate);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // TODO: pasar como parámetros códigos de promoción para que sean más de 1 y 2.
    public static void writeMailAperturasCsv(Path csvAperturasPath, Predicate<Long> filterPred)
    {
        List<AperturaRecordWrapper> aperturas = jdbiFactory.getJdbi().withHandle(
                h -> {
                    h.execute(alter_seq_promo);
                    return h.select(participantes_pk.query)
                            .mapTo(Long.class)
                            .list()
                            .stream()
                            .filter(filterPred)
                            .map(AperturaRecordWrapper::new)
                            .collect(toList());
                }
        );

        try (CSVPrinter printer =
                     new CSVPrinter(new FileWriter(csvAperturasPath.toString()), RFC4180.withDelimiter(delimiter.toString().charAt(0)))) {
            printer.printRecord(HeaderAperturaCsv.participante_id.name(), promocion_id.name(), fecha_apertura.name());
            for (AperturaRecordWrapper wrapper : aperturas) {
                printer.printRecord(valueOf(wrapper.participanteId), valueOf(wrapper.promoId), wrapper.aperturaDate);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ======================  Static classes ==================

    static class VtasFileRecordWrapper {

        final long participanteId;
        final int pg1Id;
        final int udsPg1;
        final String vtaDate;

        VtasFileRecordWrapper(long participanteId, EnumSet<PG1> rangePg1s, LocalDate dateVts)
        {
            this.participanteId = participanteId;
            pg1Id = randomInstance(rnd, rangePg1s).idPg1;
            udsPg1 = rnd.nextInt(100) + 1;
            vtaDate = dateVts.toString();
        }
    }

    static class AperturaRecordWrapper {

        final long participanteId;
        final long promoId;
        final String aperturaDate;


        AperturaRecordWrapper(long participanteId)
        {
            this.participanteId = participanteId;
            promoId = participanteId % 2 == 0 ? 1L : 2L;
            aperturaDate = now().minusDays(randomLong30()).toString();
        }
    }
}
