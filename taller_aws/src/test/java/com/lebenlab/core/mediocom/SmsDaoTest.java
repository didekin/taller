package com.lebenlab.core.mediocom;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.Promocion.PromoBuilder;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteSample;
import com.lebenlab.core.experimento.PromoParticipante;
import com.lebenlab.core.mediocom.PromoMedioComunica.PromoMedComBuilder;
import com.lebenlab.core.mediocom.SmsDao.RecipientsInPromo;

import org.junit.After;
import org.junit.Test;

import java.util.List;

import static com.lebenlab.ProcessArgException.error_in_participante_samples;
import static com.lebenlab.ProcessArgException.error_num_muestras_promociones;
import static com.lebenlab.ProcessArgException.error_promo_medio_sms;
import static com.lebenlab.ProcessArgException.error_sinch_sending_sms;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.promoParticipanteMedio;
import static com.lebenlab.core.mediocom.MedioComunicacion.email;
import static com.lebenlab.core.mediocom.MedioComunicacion.sms;
import static com.lebenlab.core.mediocom.SmsDao.RecipientsInPromo.recipientPromo;
import static com.lebenlab.core.mediocom.SmsDao.insertPromoMedioSms;
import static com.lebenlab.core.mediocom.SmsDao.mergeWithTfno;
import static com.lebenlab.core.mediocom.SmsDao.promoMedioSms;
import static com.lebenlab.core.mediocom.SmsDao.promoParticipantesTfnos;
import static com.lebenlab.core.mediocom.SmsDao.smsSendSample;
import static com.lebenlab.core.mediocom.SmsDao.updatePromoSmsDelivered;
import static com.lebenlab.core.mediocom.SmsDao.updatePromosSmsDelivered;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.core.util.DataTestExperiment.promo_1A;
import static com.lebenlab.core.util.DataTestExperiment.runScript;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/**
 * User: pedro@didekin
 * Date: 16/06/2021
 * Time: 19:36
 */
public class SmsDaoTest {

    final static Promocion promo1 = new PromoBuilder().copyPromo(promo_1A).medio(new PromoMedComBuilder().medioId(sms.id).build()).idPromo(11L).build();
    final static Promocion promo2 = new PromoBuilder().copyPromo(promo_1A).medio(new PromoMedComBuilder().medioId(sms.id).build()).idPromo(22L).build();
    final static ParticipanteSample particip1 = new ParticipanteSample(2L, "H98895J", null, "34600100000", "2", "101", "1", 1);
    final static ParticipanteSample particip2 = new ParticipanteSample(1L, "B12345X", null, "34719159100", "1", "12", "7", 2);

    @After
    public void tearDown()
    {
        cleanExpTables();
    }

    @Test
    public void test_Of()
    {
        // Case: empty samples list.
        assertThatThrownBy(() -> SmsDao.of(of(), asList(promo1, promo2))).hasMessage(error_in_participante_samples);
        // Case: lists with different sizes.
        assertThatThrownBy(() -> SmsDao.of(of(singletonList(particip1)), asList(promo1, promo2))).hasMessage(error_num_muestras_promociones);
        // Case: lists with equal size (2).
        final var recipientsPromo = SmsDao.of(asList(singletonList(particip1), singletonList(particip2)), asList(promo2, promo1)).recipientsPromoList;
        assertThat(recipientsPromo).hasSize(2).flatExtracting("tfnos").containsExactly("34600100000", "34719159100");
        assertThat(recipientsPromo).flatExtracting("participantIds").containsExactly(2L, 1L);
        assertThat(recipientsPromo).extracting("promo").containsExactly(promo2, promo1);
    }

    // =========================== Envío ===========================

    @Test
    public void test_SmsSend()
    {
        // CASO: una lista con una sola muestra sin participantes. SINCH devuelve mensaje "no destination".
        assertThatThrownBy(() -> SmsDao.of(singletonList(of()), singletonList(promo1)).smsSend()).hasMessageContaining(error_sinch_sending_sms);
    }

    @Test
    public void test_SmsSendSample()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id) VALUES (" + promo1.idPromo + "," + promo1.promoMedioComunica.medioId + ")");

        final var recipPro = new RecipientsInPromo(singletonList(particip1), promo1);
        smsSendSample(recipPro);
        await().atMost(10L, SECONDS).until(() -> promoMedioSms().size() > 0);
        final var promSmsBD = promoMedioSms();
        assertThat(promSmsBD).hasSize(1).extracting("promoId", "medioId").containsExactly(tuple(promo1.idPromo, promo1.promoMedioComunica.medioId));
        assertThat(promSmsBD.get(0).batchId).hasSizeBetween(10, 100);
    }

    // =========================== Base de datos ===========================

    @Test
    public void test_InsertPromoMedioSms()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_mediocomunicacion (promo_id, medio_id) VALUES (" + promo1.idPromo + "," + promo1.promoMedioComunica.medioId + ")");
        // Run
        insertPromoMedioSms("batchId00001", promo1);
        assertThat(promoMedioSms()).hasSize(1).extracting("promoId", "medioId", "batchId")
                .containsExactly(tuple(promo1.idPromo, promo1.promoMedioComunica.medioId, "batchId00001"));
    }

    @Test
    public void test_updatePromoSmsDelivered_1()
    {
        final var wrongPromo = new PromoBuilder().copyPromo(promo_1A).medio(new PromoMedComBuilder().medioId(email.id).build()).build();
        assertThatThrownBy(() -> updatePromoSmsDelivered(wrongPromo)).hasMessage(error_promo_medio_sms);
    }

    @Test
    public void test_updatePromoSmsDelivered_2()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO participante (participante_id, id_fiscal, provincia_id, tfno, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES (111, 'B12345X', 12, '34615201910', 7, '2004-12-31', '2020-06-01:12:32:11')," +
                "       (112, 'H42385V', 13, NULL, 4, '2004-12-29', '2020-07-01:12:32:11');");
        runScript("INSERT INTO promo_participante (promo_id, participante_id) VALUES (11, 111), (11, 112);");
        runScript("INSERT INTO promo_medio_sms (promo_id, batch_id) VALUES (11, '01F8392Q6C3VNM0B54E87T107P');");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id) VALUES (11, 111, 2);");
        // RUN
        updatePromoSmsDelivered(promo1);
        //Checks
        await().atMost(10L, SECONDS).until(() -> promoParticipanteMedio().size() > 0);
        assertThat(promoParticipanteMedio()).hasSize(1).extracting("promoId", "participanteId", "medioId", "recibidoMsg", "aperturaMsg")
                .containsExactly(tuple(11L, 111L, 2, TRUE, FALSE));
    }

    @Test
    public void test_updatePromosSmsDelivered()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO participante (participante_id, id_fiscal, provincia_id, tfno, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES (111, 'B12345X', 12, '34615201910', 7, '2004-12-31', '2020-06-01:12:32:11');");
        runScript("INSERT INTO promo_participante (promo_id, participante_id) VALUES (11, 111);");
        runScript("INSERT INTO promo_medio_sms (promo_id, batch_id) VALUES (11, '01F8392Q6C3VNM0B54E87T107P');");
        runScript("INSERT INTO promo_participante_medio (promo_id, participante_id, medio_id) VALUES (11, 111, 2);");
        // RUN - Checks
        assertThat(updatePromosSmsDelivered(asList(promo1, promo1))).isEqualTo(2);
    }

    // =========================== Utilities ===========================

    @Test
    public void test_batchIdPromo()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO promo_medio_sms (promo_id, batch_id) VALUES (11, 'batch_id_0001');");
        assertThat(SmsDao.batchIdPromo(11L)).isEqualTo("batch_id_0001");
    }

    @Test
    public void test_mergeWithTfno_1()
    {
        final var participantes = asList(
                new ParticipanteSample(111L, "34123121314"),
                new ParticipanteSample(112L, "987989799"),
                new ParticipanteSample(221L, "221234567"),
                new ParticipanteSample(222L, "222344567")
        );
        final var tfnos = asList("987989799", "221234567");
        assertThat(mergeWithTfno.apply(tfnos, participantes))
                .extracting("promoId", "participanteId")
                .containsExactly(tuple(0L, 221L), tuple(0L, 112L));
    }

    @Test
    public void test_mergeWithTfno_2()
    {
        final var participantes = singletonList(new ParticipanteSample(221L, "221234567"));
        final var tfnos = asList("987989799", "221234567");
        assertThat(mergeWithTfno.apply(tfnos, participantes))
                .extracting("promoId", "participanteId")
                .containsExactly(tuple(0L, 221L));
    }

    @Test
    public void test_mergeWithTfno_3()
    {
        final List<ParticipanteSample> participantes = emptyList();
        final var tfnos = asList("987989799", "221234567");
        assertThat(mergeWithTfno.apply(tfnos, participantes)).hasSize(0);
    }

    @Test
    public void test_mergeWithTfno_4()
    {
        final var participantes = singletonList(new ParticipanteSample(221L, "221234567"));
        final List<String> tfnos = emptyList();
        assertThat(mergeWithTfno.apply(tfnos, participantes)).hasSize(0);
    }

    @Test
    public void test_promoParticipTfno_1()
    {
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO participante (participante_id, id_fiscal, provincia_id, tfno, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES (111, 'B12345X', 12, '34615201811', 7, '2004-12-31', '2020-06-01:12:32:11');");
        runScript("INSERT INTO promo_participante (promo_id, participante_id) VALUES (11, 111), (12, 122);");

        assertThat(promoParticipantesTfnos(11L))
                .extracting("participId", "tfno")
                .containsExactly(tuple(111L, "34615201811"));
    }

    @Test
    public void test_promoParticipTfno_2()
    {
        // CASE: one participant with NULL telephone.
        runScript("SET FOREIGN_KEY_CHECKS = 0;");
        runScript("INSERT INTO participante (participante_id, id_fiscal, provincia_id, tfno, concepto_id, fecha_registro, fecha_modificacion) " +
                "VALUES (111, 'B12345X', 12, '34615201811', 7, '2004-12-31', '2020-06-01:12:32:11')," +
                "       (112, 'H42385V', 13, NULL, 4, '2004-12-29', '2020-07-01:12:32:11');");
        runScript("INSERT INTO promo_participante (promo_id, participante_id) VALUES (11, 111), (11, 112);");

        assertThat(promoParticipantesTfnos(11L))
                .extracting("participId", "tfno")
                .containsExactly(tuple(111L, "34615201811"));
    }

    @Test
    public void test_reciipientPromo()
    {
        assertThat(recipientPromo("11_112345")).isEqualTo(new PromoParticipante(11L, 112345L));
    }

    @Test
    public void test_substitutions()

    {
        final var recipientsPro = new RecipientsInPromo(asList(particip1, particip2), promo2);
        final var substitutions = recipientsPro.substitutions();
        final var subst_1 = valueOf(promo2.idPromo).concat("_").concat(valueOf(particip1.participId));
        final var subst_2 = valueOf(promo2.idPromo).concat("_").concat(valueOf(particip2.participId));
        System.out.println(subst_1);
        System.out.println(subst_2);
        assertThat(substitutions).extractingByKeys(particip1.tfno, particip2.tfno).containsExactly(
                "/open/sms/22_2",
                "/open/sms/22_1"
        );
    }
}