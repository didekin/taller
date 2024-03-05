package com.lebenlab.core.mediocom;

import org.junit.After;
import org.junit.Test;

import static com.lebenlab.core.mediocom.LandingPageFlow.smsPageFlow;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.promoParticipanteMedio;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * User: pedro@didekin
 * Date: 21/06/2021
 * Time: 15:24
 */
public class LandingPageFlowTest {

    @After
    public void tearDown()
    {
        cleanExpTables();
    }

    @Test
    public void test_smsPageFlow()
    {
        dataTestSmsPageFlow();
        assertThat(smsPageFlow("11_111")).contains(sms_personal_message);
        assertThat(promoParticipanteMedio()).extracting("promoId", "participanteId", "medioId", "recibidoMsg", "aperturaMsg")
                .containsExactlyInAnyOrder(tuple(11L, 111L, 2, TRUE, TRUE));
    }

    // =================  Utilities ===============

    public static final String sms_personal_message = "fono 34615201811";

    public static void dataTestSmsPageFlow()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'promo11', '2020-01-05', '2020-01-20', 1);");
        runScript("INSERT INTO participante (participante_id, id_fiscal, provincia_id, tfno, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES (111, 'B12345X', 12, '34615201811', 7, '2004-12-31', '2020-06-01:12:32:11')");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id)  " +
                "VALUES (11, 111, 2);");
    }
}