package com.lebenlab.core.mediocom;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.experimento.Experimento;

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static com.lebenlab.ProcessArgException.error_promos_in_exp;
import static com.lebenlab.core.mediocom.AperturasFileDao.aperturasFileDao;
import static com.lebenlab.core.mediocom.AperturasFileDao.currentPromosToUpdate;
import static com.lebenlab.core.mediocom.DataTestMedioCom.upCsvAperturas;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.promoParticipanteMedio;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * User: pedro@didekin
 * Date: 22/12/2020
 * Time: 16:59
 */
public class AperturasFileDaoTest {

    @After
    public void clean()
    {
        cleanExpTables();
    }

    //.............. Fichero de aperturas .....................

    @Test
    public void test_handleFileAperturas()
    {
        dataTestMedCom();
        final var updatedRec = aperturasFileDao.handleFileAperturas(new ByteArrayInputStream(upCsvAperturas().getBytes(UTF_8)));
        // Checks.
        assertThat(updatedRec).isEqualTo(2);
        assertThat(promoParticipanteMedio()).extracting("promoId", "participanteId", "aperturaMsg")
                .containsExactlyInAnyOrder(tuple(11L, 1111L, TRUE), tuple(12L, 2222L, TRUE));
    }

    @Test
    public void test_parseAperturasFile()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");

        final var date_1 = now().minusDays(1L);
        final var date_2 = now().plusDays(1L);

        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'cod_pr_1_A','" + date_1 + "','" + date_2 + "', 1)," +
                "        (12, 'cod_pr_1_B','" + date_1 + "','" + date_2 + "', 1);");
        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id)" +
                "VALUES (11, 3), (12, 2);");

        final var promosApertura = aperturasFileDao.parseAperturasFile(new ByteArrayInputStream(upCsvAperturas().getBytes(UTF_8)));
        assertThat(promosApertura).extracting("promoId", "participanteId")
                .containsExactlyInAnyOrder(tuple(11L, 1111L), tuple(12L, 2222L));
    }

    // =========================== Static members ===========================

    @Test
    public void test_currentPromosToUpdate_1()
    {
        final var date_1 = now().minusDays(1L);
        final var date_2 = now().plusDays(1L);

        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'cod_pr_1_A','" + date_1 + "','" + date_2 + "', 1)," +
                "        (12, 'cod_pr_1_B','" + date_1 + "','" + date_2 + "', 1);");  // medio_id wrong.
        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id)" +
                "VALUES (11, 3), (12, 1);");

        assertThat(currentPromosToUpdate()).containsExactly(11L);

        runScript("UPDATE promo_mediocomunicacion SET medio_id = 2 WHERE promo_id = 12;");
        assertThat(currentPromosToUpdate()).containsExactly(11L, 12L);
    }

    @Test
    public void test_currentPromosToUpdate_2()
    {
        final var date_1 = now().minusDays(35L);
        final var date_2 = now().minusDays(29L);
        final var date_3 = now().minusDays(31L); // para obtener fecha_fin < NOW() - 30

        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO  promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'cod_pr_1_A','" + date_1 + "','" + date_2 + "', 1)," +
                "        (21, 'cod_pr_2_A','" + date_1 + "','" + date_3 + "', 2);");  // fecha_fin wrong.
        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id)" +
                "VALUES (11, 3), (21, 3);");

        assertThat(currentPromosToUpdate()).containsExactly(11L);
    }

    // ===============================  Static utilities ===============================

    public static void dataTestMedCom()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");

        final var date_1 = now().minusDays(1L);
        final var date_2 = now().plusDays(1L);

        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'cod_pr_1_A','" + date_1 + "','" + date_2 + "', 1)," +
                "        (12, 'cod_pr_1_B','" + date_1 + "','" + date_2 + "', 1);");
        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id)" +
                "VALUES (11, 3), (12, 2);");
        runScript("INSERT INTO promo_participante (promo_id, participante_id, concepto_id, provincia_id, dias_registro)" +
                "VALUES (11, 1111, 1, 12, 111), (12, 2222, 2, 22, 2222);");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id)" +
                "VALUES (11, 1111, 3), (12, 2222, 2);");

        assertThat(promoParticipanteMedio()).extracting("promoId", "participanteId", "aperturaMsg")
                .containsExactlyInAnyOrder(tuple(11L, 1111L, FALSE), tuple(12L, 2222L, FALSE));
    }
}