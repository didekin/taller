package com.lebenlab.core.mediocom;

import com.lebenlab.Jsonable;
import com.lebenlab.core.Promocion;

import org.jdbi.v3.core.mapper.RowMapper;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import smile.math.Random;

import static java.util.EnumSet.allOf;
import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.of;
import static java.util.Objects.hash;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * User: pedro@didekin
 * Date: 11/12/2020
 * Time: 17:53
 */
public enum MedioComunicacion {

    ninguna(1),
    sms(2),
    email(3),
    ;

    public final int id;

    MedioComunicacion(int medioId)
    {
        this.id = medioId;
    }

    // =========================== Static members ===========================

    static final EnumSet<MedioComunicacion> onLineMedios = of(sms);
    static final EnumSet<MedioComunicacion> offLineMedios = complementOf(onLineMedios);

    public static final RowMapper<MedioComunicaJson> mapper = (rs, ctx) ->
            new MedioComunicaJson(rs.getInt("medio_id"), rs.getString("nombre"));

    static final Map<Integer, MedioComunicacion> idToInstance =
            allOf(MedioComunicacion.class).stream().collect(toMap(medio -> medio.id, medio -> medio));

    public static MedioComunicacion fromIdToInstance(int idIn)
    {
        return idToInstance.get(idIn);
    }

    public static MedioComunicacion randomInstance(Random rnd)
    {
        return values()[rnd.nextInt(allOf(MedioComunicacion.class).size())];
    }

    public static List<Promocion> promosOnlineMedio(List<Promocion> promos)
    {
        return promos.stream()
                .filter(promoIn -> onLineMedios.contains(fromIdToInstance(promoIn.promoMedioComunica.medioId)))
                .collect(toList());
    }

    public static List<Promocion> promosOfflineMedio(List<Promocion> promos)
    {
        return promos.stream()
                .filter(promoIn -> offLineMedios.contains(fromIdToInstance(promoIn.promoMedioComunica.medioId)))
                .collect(toList());
    }

    // =========================== Nested classes ===========================

    public enum MedioLabels {
        aperturas,
        percent_aperturas,
        NA, // default identifier for promociones without text message.
        medio_promo,
        medio_variante,
        medio_id,
        msg_classification,
        medios_comunicacion,
        promo_medio_text,
        medio_id_variante,
        promo_medio_text_variante,
        ratio_aperturas,
        sms_custom_page_txt
    }

    public static class MedioComunicaJson implements Jsonable {
        public final int medioId;
        public final String nombre;

        public MedioComunicaJson(int id, String nombre)
        {
            this.medioId = id;
            this.nombre = nombre;
        }

        @SuppressWarnings({"unused", "velocity"})
        public int getMedioId()
        {
            return medioId;
        }

        public String getNombre()
        {
            return nombre;
        }
    }

    /**
     * For queries.
     */
    public final static class PromoParticipanteMedio {

        public final long promoId;
        public final long participanteId;
        public final int medioId;
        public final boolean recibidoMsg;
        public final boolean aperturaMsg;
        public final LocalDate fechaUpdate;

        public PromoParticipanteMedio(long promoId, long participanteId, int medioId, boolean recibidoMsg, boolean aperturaMsg, LocalDate fechaUpdate)
        {
            this.promoId = promoId;
            this.participanteId = participanteId;
            this.medioId = medioId;
            this.recibidoMsg = recibidoMsg;
            this.aperturaMsg = aperturaMsg;
            this.fechaUpdate = fechaUpdate;
        }
    }

    public final static class PromoMedioSms {

        public final long promoId;
        public final int medioId;
        public final String batchId;

        public PromoMedioSms(long promoId, int medioId, String batchId)
        {
            this.promoId = promoId;
            this.medioId = medioId;
            this.batchId = batchId;
        }

        @Override
        public int hashCode()
        {
            return hash(promoId, medioId, batchId);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof PromoMedioSms) {
                return promoId == ((PromoMedioSms) obj).promoId
                        && medioId == ((PromoMedioSms) obj).medioId
                        && batchId.equals(((PromoMedioSms) obj).batchId);
            }
            return false;
        }
    }
}
