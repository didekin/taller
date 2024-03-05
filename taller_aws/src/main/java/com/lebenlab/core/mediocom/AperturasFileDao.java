package com.lebenlab.core.mediocom;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.experimento.PromoParticipante;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import static com.lebenlab.ProcessArgException.file_error_reading_msg;
import static com.lebenlab.core.experimento.CsvParserApertura.csvRecordToPromoParticip;
import static com.lebenlab.core.experimento.CsvParserApertura.csvRecords;
import static com.lebenlab.core.mediocom.MedioComunicaDaoIf.updateAperturas;
import static com.lebenlab.core.mediocom.SqlComunicacion.promos_to_update_aperturas;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 21/12/2020
 * Time: 15:36
 */
public enum AperturasFileDao implements MedioComunicaDaoIf {

    aperturasFileDao;

    public int handleFileAperturas(InputStream uploadedFile)
    {
        logger.info("handleFileAperturas()");
        final var aperturaMsgCsv = parseAperturasFile(uploadedFile);
        return updateAperturas(aperturaMsgCsv);
    }

    List<PromoParticipante> parseAperturasFile(InputStream uploadedFile)
    {
        logger.info(this.getClass() + ".parseAperturasFile()");
        final var currentPromosToUpdate = currentPromosToUpdate();
        try {
            return csvRecords(uploadedFile)
                    .stream()
                    .map(csvRecordToPromoParticip)
                    .filter(promoParticip -> isInPromosToUpdate.test(promoParticip.promoId, currentPromosToUpdate))
                    .collect(toList());
        } catch (IOException e) {
            logger.warn(this.getClass() + ".parseAperturasFile(): {}", e.getMessage());
            throw new ProcessArgException(file_error_reading_msg);
        }
    }

    // =========================== Static members ===========================

    public static final double NO_MEDIO_IN_PROMO = -1d;

    static final Logger logger = getLogger(AperturasFileDao.class);

    public static final BiPredicate<Long, Set<Long>> isInPromosToUpdate = (promoId, promosId) -> promosId.contains(promoId);

    /**
     * @return promoIds of promos with fecha_inicio older than today and fecha_fin more recent than 30 days past today.
     */
    static Set<Long> currentPromosToUpdate()
    {
        logger.info("currentPromosToUpdate()");
        return jdbiFactory.getJdbi().withHandle(
                handle -> handle.select(promos_to_update_aperturas.statement)
                        .mapTo(Long.class)
                        .collect(toSet())
        );
    }
}
