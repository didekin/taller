package com.lebenlab.core.experimento;

import org.jdbi.v3.core.mapper.RowMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.concepto_id;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.fecha_modificacion;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.fecha_registro;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.id_fiscal;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.mercado_id;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.participante_id;
import static com.lebenlab.core.experimento.ParticipanteAbs.FieldLabel.provincia_id;
import static com.lebenlab.csv.CsvConstant.delimiter;
import static com.lebenlab.csv.CsvConstant.newLine;
import static java.lang.String.join;
import static java.lang.String.valueOf;

/**
 * User: pedro@didekin
 * Date: 21/03/2020
 * Time: 16:47
 */
public class ParticipanteAbs {

    public final String idFiscal;
    public final Long participId;

    public ParticipanteAbs(String idFiscal, Long participId)
    {
        this.idFiscal = idFiscal;
        this.participId = participId;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ParticipanteAbs) {
            ParticipanteAbs objIn = (ParticipanteAbs) obj;
            return objIn.idFiscal.equalsIgnoreCase(idFiscal);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(idFiscal);
    }

    // ============================= INNER CLASSES ================================

    public static final class ParticipanteSample extends ParticipanteAbs {
        public final String email;
        public final String tfno;
        public final String mercadoId;
        public final String provinciaId;
        public final String conceptoId;
        public final int rowNumber;


        public ParticipanteSample(long participId, String tfnoIn)
        {
            super(null, participId);
            tfno = tfnoIn;
            email = null;
            mercadoId = null;
            provinciaId = null;
            conceptoId = null;
            rowNumber = 0;
        }

        public ParticipanteSample(Long participId, String idFiscal, String mercadoId, String provinciaId, String conceptoId)
        {
            super(idFiscal, participId);
            email = null;
            tfno = null;
            this.mercadoId = mercadoId;
            this.provinciaId = provinciaId;
            this.conceptoId = conceptoId;
            rowNumber = 0;
        }

        public ParticipanteSample(long participId, String idFiscal, String emailIn, String tfnoIn, String mercadoId, String provinciaId, String conceptoId, int rowNumber)
        {
            super(idFiscal, participId);
            email = emailIn;
            tfno = tfnoIn;
            this.mercadoId = mercadoId;
            this.provinciaId = provinciaId;
            this.conceptoId = conceptoId;
            this.rowNumber = rowNumber;
        }

        public static final RowMapper<ParticipanteSample> mapper = (rs, ctx) -> new ParticipanteSample(
                rs.getLong(participante_id.name()),
                rs.getString(id_fiscal.name()),
                rs.getString("email"),
                rs.getString("tfno"),
                valueOf(rs.getInt(mercado_id.name())),
                valueOf(rs.getInt(provincia_id.name())),
                valueOf(rs.getInt(concepto_id.name())),
                rs.getInt("row_num")
        );

        public String getTfno()
        {
            return tfno;
        }
    }

    public static final class ParticipanteOutCsv extends ParticipanteAbs {

        static final String headerCsv =
                join(delimiter.toString(), participante_id.name(), id_fiscal.name(),
                        mercado_id.name(), provincia_id.name(), concepto_id.name()) + newLine;

        public final String mercadoId;
        public final String provinciaId;
        public final String conceptoId;

        public ParticipanteOutCsv(long participId, String idFiscal, String mercadoId, String provinciaId, String conceptoId)
        {
            super(idFiscal, participId);
            this.provinciaId = provinciaId;
            this.conceptoId = conceptoId;
            this.mercadoId = mercadoId;
        }

        String toCsvRecord()
        {
            return join(delimiter.toString(), valueOf(participId), idFiscal, mercadoId, provinciaId, conceptoId);
        }
    }

    // ....................................................................

    public static final class ParticipanteNoPk extends ParticipanteAbs {

        public static final RowMapper<ParticipanteNoPk> mapper = (rs, ctx) -> new ParticipanteNoPk(
                rs.getLong(participante_id.name()),
                rs.getString(id_fiscal.name()),
                rs.getInt(provincia_id.name()),
                rs.getInt(concepto_id.name()),
                rs.getString(FieldLabel.email.name()),
                rs.getString(FieldLabel.tfno.name()),
                rs.getDate(fecha_registro.name()).toLocalDate(),
                rs.getTimestamp(fecha_modificacion.name()).toLocalDateTime()
        );

        public final int provinciaId;
        public final int conceptoId;
        public final String email;
        public final String tfno;
        public final LocalDate fechaRegistro;
        public final LocalDateTime fechaModificacion;

        public ParticipanteNoPk(Long participId, String idFiscal, int provinciaId, int conceptoId,
                                LocalDate fechaRegistro, LocalDateTime fechaModificacion)
        {
            super(idFiscal, participId);
            this.provinciaId = provinciaId;
            this.conceptoId = conceptoId;
            email = null;
            tfno = null;
            this.fechaRegistro = fechaRegistro;
            this.fechaModificacion = fechaModificacion;
        }

        public ParticipanteNoPk(Long participId, String idFiscal, int provinciaId, int conceptoId, String email, String tfno, LocalDate fechaRegistro, LocalDateTime fechaModificacion)
        {
            super(idFiscal, participId);
            this.provinciaId = provinciaId;
            this.conceptoId = conceptoId;
            this.email = email;
            this.tfno = tfno;
            this.fechaRegistro = fechaRegistro;
            this.fechaModificacion = fechaModificacion;
        }
    }

    // ....................................................................

    public enum FieldLabel {

        participante_id,
        id_fiscal,
        mercado_id,
        mercadoIds,
        provincia_id,
        concepto_id,
        conceptoIds,
        email,
        tfno,
        fecha_registro,
        fecha_modificacion,
        dias_registro,
    }
}
