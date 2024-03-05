package com.lebenlab.core.mediocom;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.PromoParticipante;

import org.junit.After;
import org.junit.Test;

import static com.lebenlab.core.mediocom.AperturasFileDao.NO_MEDIO_IN_PROMO;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.aperturasInExperimento;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.eventsComunicaInPromo;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.insertPromoParticipMedio;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.insertPromoParticipMedioHist;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.promoParticipanteMedio;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.recibidosInExperimento;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.updateApertura;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.updateAperturas;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.updateRecibidos;
import static com.lebenlab.core.mediocom.SqlComunicacion.promo_aperturas_resultado;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * User: pedro@didekin
 * Date: 01/06/2021
 * Time: 17:50
 */
public class MedioComunicaDaoIfTest {

    @After
    public void clean()
    {
        cleanExpTables();
    }

    //............................ Inserts/updates ..............................

    @Test
    public void test_insertPromoParticipMedio()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante (promo_id, participante_id) VALUES (11, 2), (11, 3);");
        assertThat(insertPromoParticipMedio(new PromoMedioComunica.PromoMedComBuilder().promoId(11L).medioId(1).build()))
                .isEqualTo(2);
        assertThat(promoParticipanteMedio())
                .extracting("promoId", "participanteId", "recibidoMsg", "aperturaMsg")
                .containsExactly(
                        tuple(11L, 2L, FALSE, FALSE),
                        tuple(11L, 3L, FALSE, FALSE));
    }

    //.............. Aperturas en promos .....................

    @Test
    public void test_aperturasInExperimento_1()
    {
        Promocion promo1 = new Promocion.PromoBuilder().copyPromo(promocion1).medio(new PromoMedioComunica.PromoMedComBuilder().medioId(1).build()).build();
        Promocion promo3 = new Promocion.PromoBuilder().copyPromo(promocion1).medio(new PromoMedioComunica.PromoMedComBuilder().medioId(3).build()).build();
        // Sin datos en BD.
        final var promosIn = aperturasInExperimento(asList(promo1, promo3));
        assertThat(promosIn).containsExactly(NO_MEDIO_IN_PROMO, NO_MEDIO_IN_PROMO);
    }

    @Test
    public void test_aperturasInExperimento_2()
    {
        // Hay datos en BD para las dos promociones, pero la 1ª tiene medio == ninguno.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg)" +
                "VALUES (11, 111, 1, FALSE, FALSE), (12, 221, 2, TRUE, TRUE), (12, 222, 2, TRUE, FALSE);");

        Promocion promo1 = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(11L).medio(new PromoMedioComunica.PromoMedComBuilder().medioId(1).build()).build();
        Promocion promo2 = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(12L).medio(new PromoMedioComunica.PromoMedComBuilder().medioId(2).build()).build();
        final var promosIn = aperturasInExperimento(asList(promo1, promo2));

        assertThat(promosIn).containsExactly(NO_MEDIO_IN_PROMO, .5);
    }

    @Test
    public void test_updateApertura()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id)  " +
                "VALUES (11, 111, 2);");
        assertThat(updateApertura(11L, 111L)).isEqualTo(1);
        assertThat(promoParticipanteMedio()).extracting("promoId", "participanteId", "medioId", "recibidoMsg", "aperturaMsg")
                .containsExactlyInAnyOrder(tuple(11L, 111L, 2, TRUE, TRUE));
    }

    @Test
    public void test_updateAperturas()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id)  " +
                "VALUES (11, 111, 2), (11, 112, 2);");

        assertThat(updateAperturas(singletonList(new PromoParticipante(11L, 111L))))
                .isEqualTo(1);
        assertThat(promoParticipanteMedio()).extracting("promoId", "participanteId", "recibidoMsg", "aperturaMsg")
                .containsExactlyInAnyOrder(tuple(11L, 111L, TRUE, TRUE), tuple(11L, 112L, FALSE, FALSE));

    }

    //.............. Recibidos en promos .....................

    @Test
    public void test_recibidosInExperimento()
    {
        // Hay datos en BD para las dos promociones, pero la 1ª tiene medio == ninguno.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg)" +
                "VALUES (11, 111, 1, FALSE, FALSE), (12, 221, 2, TRUE, FALSE), (12, 222, 2, FALSE, FALSE);");

        Promocion promo1 = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(11L).medio(new PromoMedioComunica.PromoMedComBuilder().medioId(1).build()).build();
        Promocion promo2 = new Promocion.PromoBuilder().copyPromo(promocion1).idPromo(12L).medio(new PromoMedioComunica.PromoMedComBuilder().medioId(2).build()).build();
        final var promosIn = recibidosInExperimento(asList(promo1, promo2));

        assertThat(promosIn).containsExactly(NO_MEDIO_IN_PROMO, .5);
    }

    @Test
    public void test_updateRecibidos_1()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id)  " +
                "VALUES (11, 111, 2), (11, 112, 2);");

        assertThat(updateRecibidos(singletonList(new PromoParticipante(11L, 112L))))
                .isEqualTo(1);
        assertThat(promoParticipanteMedio()).extracting("promoId", "participanteId", "recibidoMsg", "aperturaMsg")
                .containsExactlyInAnyOrder(tuple(11L, 111L, FALSE, FALSE), tuple(11L, 112L, TRUE, FALSE));
    }

    @Test
    public void test_updateRecibidos_2()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id)  " +
                "VALUES (11, 111, 2), (11, 112, 2);");

        assertThat(updateRecibidos(emptyList())).isEqualTo(0);
    }

    // ======================== Aperturas históricas para predicciones ========================

    @Test
    public void test_insertPromoParticipMedioHist_1()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg)" +
                " VALUES (11, 111, 2, 1, 1), (12, 111, 2, 0, 0);" +
                " INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'cod_pr_1_A', '2019-01-01', '2019-01-10', 1), (12, 'cod_pr_1_B', '2019-01-11', '2019-01-28', 2);");
        // Inserta registro: hay campañas previas con aperturas en el mismo medio.
        assertThat(insertPromoParticipMedioHist(12L)).isEqualTo(1);
        assertThat(eventsComunicaInPromo(11L, promo_aperturas_resultado)).isEqualTo(1);
    }

    @Test
    public void test_insertPromoParticipMedioHist_2()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg)" +
                " VALUES (11, 111, 2, 1, 1), (12, 111, 3, 0, 0);" +
                " INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'cod_pr_1_A', '2019-01-01', '2019-01-10', 1), (12, 'cod_pr_1_B', '2019-01-11', '2019-01-28', 2);");
        // Inserta registro: hay campañas previas con aperturas, pero en un medio distinto.
        assertThat(insertPromoParticipMedioHist(12L)).isEqualTo(0);
    }

    @Test
    public void test_insertPromoParticipMedioHist_3()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg)" +
                " VALUES (11, 111, 2, 1, 1), (12, 222, 2, 0, 0);" +
                " INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'cod_pr_1_A', '2019-01-01', '2019-01-10', 1), (12, 'cod_pr_1_B', '2019-01-11', '2019-01-28', 2);");
        // No hay promociones previas: no inserta registros.
        assertThat(insertPromoParticipMedioHist(11L)).isEqualTo(0);
        // Sólo promociones previas sin participantes en común: no inserta ningún registro.
        assertThat(insertPromoParticipMedioHist(12L)).isEqualTo(0);
    }

    @Test
    public void test_insertPromoParticipMedioHist_4()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id, recibido_msg, apertura_msg)" +
                " VALUES (11, 111, 2, 1, 0), (12, 111, 2, 1, 0);" +
                " INSERT INTO promo (promo_id, cod_promo, fecha_inicio, fecha_fin, experimento_id)" +
                " VALUES (11, 'cod_pr_1_A', '2019-01-01', '2019-01-10', 1), (12, 'cod_pr_1_B', '2019-01-11', '2019-01-28', 2);");
        // Inserta registro con ratio a 0: hay campañas previas sin aperturas en el mismo medio.
        assertThat(insertPromoParticipMedioHist(12L)).isEqualTo(1);
        assertThat(eventsComunicaInPromo(11L, promo_aperturas_resultado)).isEqualTo(0);
    }

    // ======================== Utilidades ========================
}