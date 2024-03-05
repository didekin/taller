package com.lebenlab.core.experimento;

import static java.util.Objects.hash;

/**
 * User: pedro@didekin
 * Date: 17/06/2021
 * Time: 16:20
 */
public class PromoParticipante {

    public final long promoId;
    public final long participanteId;
    public final int conceptoId;
    public final int provinciaId;
    public final int diasRegistro;

    /**
     * Constructor for updates in BD.
     */
    public PromoParticipante(long promoId, long participanteId)
    {
        this.promoId = promoId;
        this.participanteId = participanteId;
        conceptoId = 0;
        provinciaId = 0;
        diasRegistro = 0;
    }

    PromoParticipante(long promoId, long participanteId, int conceptoId, int provinciaId, int diasRegistro)
    {
        this.promoId = promoId;
        this.participanteId = participanteId;
        this.conceptoId = conceptoId;
        this.provinciaId = provinciaId;
        this.diasRegistro = diasRegistro;
    }



    @Override
    public int hashCode()
    {
        return hash(promoId, participanteId);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof PromoParticipante) {
            PromoParticipante objectIn = (PromoParticipante) obj;
            return promoId == objectIn.promoId
                    && participanteId == objectIn.participanteId;
        }
        return false;
    }
}
