package com.lebenlab.core.experimento;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteNoPk;
import com.lebenlab.core.util.DataTestExperiment;
import com.lebenlab.core.util.FileTestUtil;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.getHeaderStr;
import static com.lebenlab.core.experimento.ParticipanteDao.countParticipByPromo;
import static com.lebenlab.core.experimento.ParticipanteDao.countParticipantes;
import static com.lebenlab.core.experimento.ParticipanteDao.deleteParticipByOldDate;
import static com.lebenlab.core.experimento.ParticipanteDao.deleteParticipByOldDateAsync;
import static com.lebenlab.core.experimento.ParticipanteDao.insertParticipantes;
import static com.lebenlab.core.experimento.ParticipanteDao.participantes;
import static com.lebenlab.core.experimento.SqlUpdate.insert_participante;
import static com.lebenlab.core.experimento.SqlUpdate.particip_delete_all;
import static com.lebenlab.core.util.DataTestExperiment.insert_particip_test;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.DataTestExperiment.upCsvParticip;
import static com.lebenlab.core.util.DataTestExperiment.upCsvParticipFull;
import static com.lebenlab.csv.CsvConstant.newLine;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static java.time.LocalDate.parse;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/**
 * User: pedro@didekin.es
 * Date: 22/01/2020
 * Time: 16:00
 */
public class ParticipanteDaoTest {

    private final Jdbi jdbi = jdbiFactory.getJdbi();

    @After
    public void clean()
    {
        jdbi.withHandle(handle -> handle.execute(particip_delete_all.statement));
    }

    @Test
    public void test_CountParticipantes()
    {
        DataTestExperiment.runScript(" INSERT INTO  participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES " +
                "(1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'), " +      // mercado-concepto (1,7)
                "(3,'C98345Z', 101, 2, '2019-01-04', '2020-06-01:12:32:11'), " +     // (2,2)
                "(2,'H98895J', 101, 1, '2019-01-04', '2020-06-01:12:32:11');");
        assertThat(countParticipantes()).isEqualTo(3);
    }

    @Test
    public void test_countParticipByPromo()
    {
        DataTestExperiment.runScript(" INSERT INTO  participante (participante_id, id_fiscal, provincia_id, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES " +
                "(1,'B12345X', 12, 7, '2004-12-31', '2020-06-01:12:32:11'), " +      // mercado-concepto (1,7)
                "(3,'C98345Z', 101, 2, '2019-01-04', '2020-06-01:12:32:11'), " +     // (2,2)
                "(2,'H98895J', 101, 1, '2019-01-04', '2020-06-01:12:32:11');");
        Promocion promocion = new Promocion.PromoBuilder().copyPromo(promocion1).mercados(singletonList(2)).conceptos(asList(1, 2)).build();
        assertThat(countParticipByPromo(promocion)).isEqualTo(2);
    }

    @Test
    public void test_DeleteAllParticipante()
    {
        assertThat((int) jdbi.withHandle(handle -> handle.execute(insert_particip_test, "B12345X", 12, 7, "2004-12-31", now()))).isEqualTo(1);
        assertThat((int) jdbi.withHandle(handle -> handle.execute(particip_delete_all.statement))).isEqualTo(1);
    }

    @Test
    public void test_deleteParticipByOldDate_1()
    {
        LocalDateTime nowTime = now();
        jdbi.useHandle(
                handle -> {
                    assertThat(handle.execute(insert_particip_test, "B12345X", 12, 7, "2004-12-31", nowTime.minus(2L, SECONDS))).isEqualTo(1);
                    assertThat(handle.execute(insert_particip_test, "H98895J", 101, 1, "2018-04-01", nowTime)).isEqualTo(1);
                });
        // Borra el más antiguo de los dos.
        assertThat(deleteParticipByOldDate(participantes().get(1).fechaModificacion)).isEqualTo(1);
        assertThat(participantes()).hasSize(1).extracting("idFiscal").containsExactly("H98895J");
    }

    @Test
    public void test_deleteParticipByOldDate_2()
    {
        LocalDateTime nowTime = now();
        jdbi.useHandle(
                handle -> {
                    assertThat(handle.execute(insert_particip_test, "B12345X", 12, 7, "2004-12-31", nowTime.minus(2L, SECONDS))).isEqualTo(1);
                    assertThat(handle.execute(insert_particip_test, "H98895J", 101, 1, "2018-04-01", nowTime.minus(1L, SECONDS))).isEqualTo(1);
                });
        // Borra los dos.
        assertThat(deleteParticipByOldDate(nowTime)).isEqualTo(2);
        assertThat(participantes()).hasSize(0);
    }

    @Test
    public void test_deleteParticipByOldDate_3()
    {
        jdbi.useHandle(
                // No records.
                handle -> assertThat(deleteParticipByOldDate(now(), handle)).isEqualTo(0)
        );
    }

    @Test
    public void test_deleteParticipByOldDate_4()
    {
        // No oldTime
        jdbi.useHandle(
                handle -> assertThat(deleteParticipByOldDate(now(), handle)).isEqualTo(0)
        );
    }

    @Test
    public void test_deleteParticipByOldDateAsync()
    {
        jdbi.useHandle(h -> assertThat(h.execute(insert_particip_test, "B12345X", 12, 7, "2004-12-31", now().minus(5L, SECONDS))).isEqualTo(1));
        // Ya no existe en BD.
        deleteParticipByOldDateAsync(now());
        await().until(() -> participantes().size() == 0);
    }

    @Test
    public void test_insertDate()
    {
        LocalDateTime timeLess = now().minus(1L, SECONDS);
        LocalDateTime timeMore = now();
        assertThat(timeMore.compareTo(timeLess)).isGreaterThan(0);
    }

    @Test
    public void test_insertParticipantes_1()
    {
        // No hay duplicados en BD.
        ByteArrayInputStream byteStream = new ByteArrayInputStream(upCsvParticip.getBytes(UTF_8));
        assertThat(insertParticipantes(byteStream)).isEqualTo(2);
        final var participBd = participantes();
        assertThat(participBd).hasSize(2).extracting("idFiscal", "provinciaId", "conceptoId", "email", "tfno", "fechaRegistro")
                .containsExactly(tuple("B12345X", 12, 7, "", "", parse("2004-12-31")), tuple("C98345Z", 101, 2, "", "", parse("2019-01-02")));
    }

    @Test
    public void test_insertParticipantes_2()
    {
        // No hay duplicados en BD.
        ByteArrayInputStream byteStream = new ByteArrayInputStream(upCsvParticipFull.getBytes(UTF_8));
        assertThat(insertParticipantes(byteStream)).isEqualTo(2);
        final var participBd = participantes();
        assertThat(participBd).hasSize(2).extracting("idFiscal", "provinciaId", "conceptoId", "email", "tfno", "fechaRegistro")
                .containsExactly(tuple("B12345X", 12, 7, "mail1@lebenlab.com", "34600000100", parse("2004-12-31")), tuple("C98345Z", 101, 2, "mail2@lebenlab.com", "34600000200", parse("2019-01-02")));
    }

    @Test
    public void test_insertParticipantes_3()
    {
        // Fichero con un participante repetido y uno nuevo.
        String csvFile = getHeaderStr() + newLine +
                "B12345X;1;1;;;2004-12-31" + newLine +
                "C98345Z;101;2;;;2019-01-02";  // pk = 3.
        ByteArrayInputStream byteStream = new ByteArrayInputStream(csvFile.getBytes(UTF_8));

        LocalDateTime oldTime = now().minus(20L, SECONDS);
        jdbi.useHandle(
                handle -> {
                    // Auto-increment a 0.
                    handle.execute(DataTestExperiment.alter_seq_particip);
                    // Inserto el participante repetido en fichero con pk = 1.
                    assertThat(handle.execute(insert_particip_test, "B12345X", 12, 7, "2004-12-31", oldTime)).isEqualTo(1);
                    // Inserto el participante que no se repetirá y que se borrará, con pk = 2.
                    assertThat(handle.execute(insert_particip_test, "H98895J", 102, 1, "2018-04-01", oldTime)).isEqualTo(1);

                    assertThat(insertParticipantes(byteStream)).isEqualTo(3); // 2 for update + 1 for insert.

                    await().until(() -> participantes(handle).size() == 2);
                    assertThat(participantes(handle)).extracting("participId", "idFiscal", "provinciaId", "conceptoId", "fechaRegistro")
                            // Inno-DB auto-increment salta +1 (3+1) con los que detecta repetidos, aunque mantenga la PK.
                            .containsExactly(
                                    tuple(1L, "B12345X", 1, 1, parse("2004-12-31")),
                                    tuple(4L, "C98345Z", 101, 2, parse("2019-01-02")));
                });
    }

    @Test
    public void test_insertParticipantes_4()
    {
        // Fichero con los dos participante repetidos. Cambia las fechas de modificacion.
        String csvFile = getHeaderStr() + newLine +
                "B12345X;12;7;;;2004-12-31" + newLine +
                "H98895J;102;1;;;2018-04-01";
        ByteArrayInputStream byteStream = new ByteArrayInputStream(csvFile.getBytes(UTF_8));

        jdbi.useHandle(
                handle -> {
                    // Auto-increment a 0.
                    handle.execute(DataTestExperiment.alter_seq_particip);
                    // Inserto el participante repetido en fichero con pk = 1.
                    assertThat(handle.execute(insert_particip_test, "B12345X", 12, 7, "2004-12-31", now().minus(3L, SECONDS))).isEqualTo(1);
                    // Inserto el participante que no se repetirá y que se borrará, con pk = 2.
                    assertThat(handle.execute(insert_particip_test, "H98895J", 102, 1, "2018-04-01", now().minus(3L, SECONDS))).isEqualTo(1);
                    List<ParticipanteNoPk> participIn = participantes(handle);

                    assertThat(insertParticipantes(byteStream)).isEqualTo(4); // 2 fecha_modificacion * 2.
                    await().until(() -> participantes(handle).size() == 2);

                    final var participOut = participantes(handle);
                    assertThat(participIn.get(0).fechaModificacion).isBefore(participOut.get(0).fechaModificacion);
                    assertThat(participIn.get(1).fechaModificacion).isBefore(participOut.get(1).fechaModificacion);
                    assertThat(participOut).extracting("participId", "idFiscal", "provinciaId", "conceptoId", "fechaRegistro")
                            // Inno-DB auto-increment salta +1 (3+1) con los que detecta repetidos, aunque mantenga la PK.
                            .containsExactly(tuple(1L, "B12345X", 12, 7, parse("2004-12-31")), tuple(2L, "H98895J", 102, 1, parse("2018-04-01")));
                });
    }

    @Test
    public void test_insertParticipantes_5() throws IOException
    {
        // Long file.
        ByteArrayInputStream byteStream = new ByteArrayInputStream(readAllBytes(FileTestUtil.csvParticipantes_11577));
        assertThat(insertParticipantes(byteStream)).isEqualTo(11577);
        await().until(() -> participantes().size() == 11577);

        // 1000 registros: todos repetidos.
        ByteArrayInputStream byteStream2 = new ByteArrayInputStream(readAllBytes(FileTestUtil.csvParticipantes_1000));
        // Cada variación (fecha_modificación cuenta como variación) suma 2.
        assertThat(insertParticipantes(byteStream2)).isEqualTo(1000 * 2);
        // Todos los registros en el nuevo fichero quedan en base de datos.
        await().until(() -> participantes().size() == 1000);
    }

    @Test
    public void test_insert_singleBatch()
    {
        int[] count = jdbi.withHandle(
                handle -> {
                    PreparedBatch batch = handle.prepareBatch(insert_participante.statement);
                    return batch.bindFields(new ParticipanteNoPk(null, "B12345X", 12, 7, parse("2004-12-31"), now()))
                            .execute();
                }
        );
        assertThat(stream(count).sum()).isEqualTo(1);
    }

    @Test
    public void test_participantes()
    {
        jdbi.useHandle(
                handle -> {
                    // Auto-increment a 0.
                    handle.execute(DataTestExperiment.alter_seq_particip);
                    assertThat(handle.execute(insert_particip_test, "B12345X", 12, 7, "2004-12-31", now())).isEqualTo(1);
                    assertThat(handle.execute(insert_particip_test, "H98895J", 101, 1, "2018-04-01", now())).isEqualTo(1);
                    List<ParticipanteNoPk> particips = participantes(handle);
                    assertThat(particips).hasSize(2)
                            .extracting("participId", "idFiscal", "provinciaId", "conceptoId", "fechaRegistro")
                            .containsExactly(tuple(1L, "B12345X", 12, 7, parse("2004-12-31")), tuple(2L, "H98895J", 101, 1, parse("2018-04-01")));
                }
        );
    }
}