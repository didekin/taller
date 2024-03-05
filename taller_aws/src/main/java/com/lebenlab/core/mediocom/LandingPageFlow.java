package com.lebenlab.core.mediocom;

import org.apache.logging.log4j.Logger;

import static com.lebenlab.AsyncUtil.executor;
import static com.lebenlab.core.experimento.ExperimentoDao.promocionById;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.updateApertura;
import static com.lebenlab.core.mediocom.SmsDao.RecipientsInPromo.recipientPromo;
import static com.lebenlab.core.mediocom.SmsDao.participanteTfno;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 21/06/2021
 * Time: 13:42
 */
public final class LandingPageFlow {

    private static final Logger logger = getLogger(LandingPageFlow.class);

    static final String paragraphPromo = "Mensaje personal en la promoci&oacute;n ";
    static final String paragraphTfno = " para el tel&eacute;fono ";

    public static String smsPageFlow(String substitutionId)
    {
        logger.info("smsPageFlow()");

        final var promoId = recipientPromo(substitutionId).promoId;
        final var participanteId = recipientPromo(substitutionId).participanteId;

        runAsync(() -> updateApertura(promoId, participanteId), executor(1));

        return supplyAsync(
                () -> promocionById(recipientPromo(substitutionId).promoId),
                executor(1)
        ).thenCombine(
                supplyAsync(
                        () -> participanteTfno(recipientPromo(substitutionId).participanteId),
                        executor(1)),
                (promo, participSample) -> paragraphPromo.concat(promo.codPromo).concat(paragraphTfno).concat(participSample.tfno)
        ).join();
    }
}
