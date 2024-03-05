package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteNoPk;

import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.PreparedBatch;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static com.lebenlab.ProcessArgException.file_error_reading_msg;
import static com.lebenlab.core.experimento.CsvParserParticipantes.parser;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.conceptoIds;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.fecha_modificacion;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.mercadoIds;
import static com.lebenlab.core.experimento.ParticipanteAbs.ParticipanteNoPk.mapper;
import static com.lebenlab.core.experimento.SqlUpdate.insert_participante;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.stream;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin.es
 * Date: 22/01/2020
 * Time: 15:49
 */
public final class ParticipanteDao {

    private static final Logger logger = getLogger(ParticipanteDao.class);

    public static int countParticipantes()
    {
        try {
            return jdbiFactory.getJdbi().withHandle(
                    handle -> handle.select(SqlQuery.participante_all_count.query)
                            .mapTo(Integer.class).one());
        } catch (JdbiException e) {
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    public static int countParticipByPromo(Promocion promoIn)
    {
        try {
            return jdbiFactory.getJdbi().withHandle(
                    handle -> handle.select(SqlQuery.participante_count_by_promo.query)
                            .bindList(mercadoIds.name(), promoIn.mercados)
                            .bindList(conceptoIds.name(), promoIn.conceptos)
                            .mapTo(Integer.class).one()
            );
        } catch (JdbiException e) {
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    public static void deleteParticipByOldDateAsync(LocalDateTime oldTime)
    {
        logger.debug("deleteParticipByOldDateAsync()");
        runAsync(() -> deleteParticipByOldDate(oldTime));
    }

    static int deleteParticipByOldDate(LocalDateTime oldTime)
    {
        return jdbiFactory.getJdbi().withHandle(handle -> deleteParticipByOldDate(oldTime, handle));
    }

    public static int deleteParticipByOldDate(LocalDateTime oldTime, Handle handle)
    {
        try {
            int recordsDeleted = handle.createUpdate(SqlUpdate.participante_delete_by_time.statement)
                    .bind(fecha_modificacion.name(), oldTime)
                    .execute();
            logger.info("deleteParticipByOldDate(); records deleted = {}", recordsDeleted);
            return recordsDeleted;
        } catch (JdbiException e) {
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    /**
     * It inserts records, updating the duplicates with their previous pk. After the update, it deletes the records either not
     * newly inserted or not updated (duplicates). An instance of LocalDateTime is updated as fecha_modificacion in all records in file.
     * Records with previous fecha_modificacion are deleted.
     * <p>
     * A null is passed in participante_id to ease the ON DUPLICATE KEY UPDATE clause: null forbids two UNIQUE fields
     * duplication (participante_id, id_fiscal) in records already in database.
     *
     * @return number of insertions from uploaded file: updated counts 2, newly inserted counts 1.
     */
    public static int insertParticipantes(InputStream inputStream)
    {
        List<ParticipanteNoPk> participantes;
        final var fechaModif = now().truncatedTo(SECONDS);
        final LocalDateTime oldTime = fechaModif.minus(1L, SECONDS);
        try {
            participantes = parser.readCsvParticipantes(inputStream, fechaModif);
            logger.info("insertParticipantes(): {} registros en fichero", participantes.size());
        } catch (IOException e) {
            logger.error("insertParticipantes(): error leyendo fichero csv.");
            throw new ProcessArgException(file_error_reading_msg + e.getMessage());
        }
        try {
            return jdbiFactory.getJdbi().withHandle(
                    handle -> {
                        PreparedBatch batch = handle.prepareBatch(insert_participante.statement);
                        for (ParticipanteNoPk participante : participantes) {
                            batch.bindFields(participante).add();
                        }
                        int insertedRecords = stream(batch.execute()).sum();
                        logger.info("insertParticipantes(): {} registros insertados en BD", insertedRecords);
                        deleteParticipByOldDateAsync(oldTime);
                        return insertedRecords;
                    }
            );
        } catch (JdbiException e) {
            logger.error("insertParticipantes(): " + e.getMessage());
            throw new ProcessArgException(error_jdbi_statement + e.getMessage());
        }
    }

    static List<ParticipanteNoPk> participantes()
    {
        return jdbiFactory.getJdbi().withHandle(ParticipanteDao::participantes);
    }

    static List<ParticipanteNoPk> participantes(Handle handle)
    {
        return handle.select(SqlQuery.participantes.query).map(mapper).list();
    }
}
