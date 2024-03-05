package com.lebenlab.core.mediocom;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteSample;
import com.lebenlab.core.experimento.PromoParticipante;
import com.lebenlab.core.mediocom.MedioComunicacion.PromoMedioSms;

import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.JdbiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import static com.lebenlab.AsyncUtil.executor;
import static com.lebenlab.DataPatterns.TFNO_MOVIL;
import static com.lebenlab.ProcessArgException.error_in_participante_samples;
import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static com.lebenlab.ProcessArgException.error_num_muestras_promociones;
import static com.lebenlab.ProcessArgException.error_promo_medio_sms;
import static com.lebenlab.core.UrlPath.smsPath;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.updateEventsComunicaMedio;
import static com.lebenlab.core.mediocom.MedioComunicacion.sms;
import static com.lebenlab.core.mediocom.SqlComunicacion.insert_promo_medio_sms;
import static com.lebenlab.core.mediocom.SqlComunicacion.promo_batchid_sms;
import static com.lebenlab.core.mediocom.SqlComunicacion.promo_medio_sms;
import static com.lebenlab.core.mediocom.SqlComunicacion.promo_participante_tfno;
import static com.lebenlab.core.mediocom.SqlComunicacion.promo_participantes_tfnos;
import static com.lebenlab.core.mediocom.SqlComunicacion.update_promo_particip_recibido;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static com.lebenlab.sinch.SinchFacade.sendSms;
import static com.lebenlab.sinch.SinchFacade.tfnosDevilered;
import static java.lang.Long.parseLong;
import static java.lang.String.valueOf;
import static java.util.Comparator.comparing;
import static java.util.List.copyOf;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 09/06/2021
 * Time: 17:53
 */
public class SmsDao implements MedioComunicaDaoIf {

    private static final Logger logger = getLogger(SmsDao.class);

    final List<RecipientsInPromo> recipientsPromoList;

    private SmsDao(List<List<ParticipanteSample>> samplesList, List<Promocion> promosIn)
    {
        logger.debug("SmsDao()");

        if (samplesList.isEmpty()) {
            logger.error("SmsDao(): error samplesList.isEmpty");
            throw new ProcessArgException(error_in_participante_samples);
        }
        if (samplesList.size() != promosIn.size()) {
            logger.error("SmsDao(): error samplesList.size() != promosIn.size()");
            throw new ProcessArgException(error_num_muestras_promociones);
        }

        List<RecipientsInPromo> tempList = new ArrayList<>(promosIn.size());
        for (int i = 0; i < samplesList.size(); i++) {
            tempList.add(new RecipientsInPromo(samplesList.get(i), promosIn.get(i)));
        }
        recipientsPromoList = copyOf(tempList);
    }

    public static SmsDao of(List<List<ParticipanteSample>> samplesList, List<Promocion> promosIn)
    {
        logger.debug("of()");
        return new SmsDao(samplesList, promosIn);
    }

    // =========================== Envío ===========================

    public void smsSend()
    {
        logger.info("smsSend(), num samples = {}", recipientsPromoList.size());
        for (RecipientsInPromo recipPromo : recipientsPromoList) {
            smsSendSample(recipPromo);
        }
    }

    static void smsSendSample(RecipientsInPromo recipientsPromo)
    {
        logger.debug("smsSendSample()");

        CompletableFuture<String> smsSendFuture = supplyAsync(
                () -> sendSms(
                        recipientsPromo.promo.promoMedioComunica.textMsg,
                        recipientsPromo::substitutions,
                        recipientsPromo.tfnosArr()
                ), executor(1)
        );
        insertPromoMedioSms(smsSendFuture.join(), recipientsPromo.promo);
    }

    // =========================== Base de datos ===========================

    static void insertPromoMedioSms(String batchIdStr, Promocion promo)
    {
        logger.info("insertPromoMedioSms()");
        try {
            jdbiFactory.getJdbi().withHandle(
                    h -> h.createUpdate(insert_promo_medio_sms.statement)
                            .bind("promo_id", promo.idPromo)
                            .bind("batch_id", batchIdStr)
                            .execute()
            );
        } catch (JdbiException e) {
            logger.error("insertPromoMedioSms(); JDBIExcepction: {}", e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    public static int updatePromosSmsDelivered(List<Promocion> promosIn)
    {
        logger.debug("updatePromosSmsDelivered()");
        final var futuresOut = promosIn.stream()
                .filter(promo -> promo.promoMedioComunica.medioId == sms.id)
                .map(
                        promoIn -> supplyAsync(() -> updatePromoSmsDelivered(promoIn), executor(1))
                ).collect(toList());
        return futuresOut.stream().mapToInt(CompletableFuture::join).sum();
    }

    /**
     * Updates messages received (with TRUE) by participants in a promotion.
     */
    static int updatePromoSmsDelivered(Promocion promoIn)
    {
        logger.debug("updatePromoSmsDelivered() entering ...");

        if (promoIn.promoMedioComunica.medioId != sms.id) {
            logger.error("updatePromoSmsDelivered() : medio comunicación no es SMS");
            throw new ProcessArgException(error_promo_medio_sms);
        }

        final List<PromoParticipante> promoParticipantes =
                supplyAsync(
                        () -> tfnosDevilered(batchIdPromo(promoIn.idPromo)),
                        executor(1)
                ).thenCombineAsync(
                        supplyAsync(
                                () -> promoParticipantesTfnos(promoIn.idPromo),
                                executor(1)
                        ), mergeWithTfno
                ).join();

        final var promoParticipToBd = promoParticipantes.stream()
                .map(proPart -> new PromoParticipante(promoIn.idPromo, proPart.participanteId))
                .collect(toList());

        return updateEventsComunicaMedio(promoParticipToBd, update_promo_particip_recibido);
        // NOTA: optimización: sólo actualizar cuando consulta en los 5 primeros días desde la fecha de envío.
    }

    // =========================== Utilities ===========================

    static String batchIdPromo(long promoId)
    {
        logger.debug("batchIdPromo()");
        return jdbiInstance.withHandle(
                h -> h.select(promo_batchid_sms.statement)
                        .bind("promo_id", promoId)
                        .mapTo(String.class)
                        .one()
        );
    }

    /**
     * A function whose inputs are a list of distinct telephone numbers and a list of distinct participants. It returns a list of
     * participants in the promo who received its SMS message.
     */
    public static final BiFunction<List<String>, List<ParticipanteSample>, List<PromoParticipante>> mergeWithTfno =
            (tfnoList, participantList) ->
            {
                if (tfnoList.isEmpty() || participantList.isEmpty()) {
                    return Collections.emptyList();
                }

                tfnoList.sort(String::compareTo);
                participantList.sort(comparing(ParticipanteSample::getTfno));
                final List<PromoParticipante> promoParticipantes = new ArrayList<>();
                int j = 0;
                tfnosLoop:
                for (String tfno : tfnoList) {
                    while (!tfno.equals(participantList.get(j).tfno)) {
                        j++;
                        if (j >= participantList.size()) {
                            break tfnosLoop;
                        }
                    }
                    promoParticipantes.add(new PromoParticipante(0, participantList.get(j).participId));
                }
                return promoParticipantes;
            };

    static List<ParticipanteSample> promoParticipantesTfnos(long promoIdIn)
    {
        logger.debug("promoParticipantesTfnos()");
        return jdbiInstance.withHandle(
                h -> h.select(promo_participantes_tfnos.statement)
                        .bind("promo_id", promoIdIn)
                        .map(
                                (rs, ctx) -> new ParticipanteSample(
                                        rs.getLong("participante_id"),
                                        rs.getString("tfno")
                                )
                        )
                        .list()
        );
    }

    public static ParticipanteSample participanteTfno(long participanteId)
    {
        logger.info("promoParticipantesTfnos()");
        return jdbiInstance.withHandle(
                h -> h.select(promo_participante_tfno.statement)
                        .bind("participante_id", participanteId)
                        .map(
                                (rs, ctx) -> new ParticipanteSample(
                                        rs.getLong("participante_id"),
                                        rs.getString("tfno")
                                )
                        )
                        .one()
        );
    }

    /**
     * Utility mainly for tests.
     */
    public static List<PromoMedioSms> promoMedioSms()
    {
        return jdbiInstance.withHandle(
                h -> h.select(promo_medio_sms.statement)
                        .map((r, ctx) -> new PromoMedioSms(
                                        r.getLong("promo_id"),
                                        r.getShort("medio_id"),
                                        r.getString("batch_id")
                                )
                        ).list());
    }

    static class RecipientsInPromo {

        public final List<String> tfnos;
        public final List<Long> participantIds;
        public final Promocion promo;

        RecipientsInPromo(List<ParticipanteSample> participants, Promocion promo)
        {
            logger.debug("RecipientsInPromo()");

            List<String> tempTfno = new ArrayList<>(participants.size());
            List<Long> tempIds = new ArrayList<>(participants.size());

            for (ParticipanteSample participant : participants) {
                if (TFNO_MOVIL.isPatternOk(participant.tfno)) {
                    tempTfno.add(participant.tfno);
                    tempIds.add(participant.participId);
                }
            }

            tfnos = copyOf(tempTfno);
            participantIds = copyOf(tempIds);
            this.promo = promo;
        }

        String[] tfnosArr()
        {
            logger.debug("tfnosArr()");
            return tfnos.toArray(String[]::new);
        }


        Map<String, String> substitutions()
        {
            logger.debug("substitutions()");

            @SuppressWarnings("unchecked") final Map.Entry<String, String>[] arrEntries = new Map.Entry[participantIds.size()];
            for (int i = 0; i < participantIds.size(); i++) {
                arrEntries[i] = entry(
                        tfnos.get(i),
                        smsPath.fullPath
                                .concat("/")
                                .concat(valueOf(promo.idPromo))
                                .concat("_")
                                .concat(valueOf(participantIds.get(i)))
                );
            }
            return ofEntries(arrEntries);
        }

        public static PromoParticipante recipientPromo(String substitutionId)
        {
            logger.debug("recipientPromo()");
            final var parts = substitutionId.split("_");
            return new PromoParticipante(parseLong(parts[0]), parseLong(parts[1]));
        }
    }
}
