package com.lebenlab.core.mediocom;

import com.lebenlab.BeanBuilder;
import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;

import static com.lebenlab.ProcessArgException.error_build_promomedcom;
import static com.lebenlab.ProcessArgException.error_build_textcomunicacion;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.NA;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static java.util.Objects.hash;

/**
 * User: pedro@didekin
 * Date: 15/01/2021
 * Time: 13:59
 */
public class PromoMedioComunica implements Jsonable {

    public final long promoId;
    public final int medioId;
    public final int codTextClass;
    public final String textMsg;

    private PromoMedioComunica(PromoMedComBuilder builder)
    {
        promoId = builder.promoId;
        medioId = builder.medioId;
        codTextClass = builder.codTextClass;
        textMsg = builder.textMsg;
    }

    // ============================= Builder ==================================

    public final static class PromoMedComBuilder implements BeanBuilder<PromoMedioComunica> {

        private long promoId;
        private int medioId;
        private int codTextClass;
        private String textMsg;

        public PromoMedComBuilder()
        {
            textMsg = NA.name();
            codTextClass = TextClassifier.TextClassEnum.NA.codigoNum;
        }

        public PromoMedComBuilder promoId(long idIn)
        {
            if (idIn > 0) {
                promoId = idIn;
            }
            return this;
        }

        public PromoMedComBuilder medioId(int idIn)
        {
            if (idIn >= 1) {
                medioId = idIn;
            }
            return this;
        }

        public PromoMedComBuilder msgClassifier(int classifierIn)
        {
            if (classifierIn > 0) {
                codTextClass = classifierIn;
            }
            return this;
        }

        public PromoMedComBuilder textMsg(String msgIn)
        {
            if (msgIn != null && !msgIn.trim().isEmpty()) {
                if (msgIn.length() > 500) {
                    throw new ProcessArgException(error_build_textcomunicacion);
                }
                textMsg = msgIn;
            }
            return this;
        }

        public PromoMedComBuilder copy(PromoMedioComunica promoMedComIn)
        {
            return this.promoId(promoMedComIn.promoId)
                    .medioId(promoMedComIn.medioId)
                    .msgClassifier(promoMedComIn.codTextClass)
                    .textMsg(promoMedComIn.textMsg);
        }

        @Override
        public PromoMedioComunica build()
        {
            PromoMedioComunica promoMedCom = new PromoMedioComunica(this);
            final var inconsistenMedTxt = (promoMedCom.medioId == ninguna.id) && !promoMedCom.textMsg.equals(NA.name());
            if (promoMedCom.medioId <= 0 || inconsistenMedTxt) {
                throw new ProcessArgException(error_build_promomedcom);
            }
            return promoMedCom;
        }

        @Override
        public int hashCode()
        {
            return hash(promoId, medioId, codTextClass, textMsg);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof PromoMedioComunica) {
                PromoMedioComunica objIn = (PromoMedioComunica) obj;
                return promoId == objIn.promoId
                        && medioId == objIn.medioId
                        && codTextClass == objIn.codTextClass
                        && textMsg.equals(objIn.textMsg);
            }
            return false;
        }
    }

    public static class PromoMedComFacade implements Jsonable {

        public final long promoId;
        public final String textMsg;
        public final String medioName;

        public PromoMedComFacade(long promoId, String textMsg, String medioName)
        {
            this.promoId = promoId;
            this.textMsg = textMsg;
            this.medioName = medioName;
        }

        public PromoMedComFacade(PromoMedioComunica medioComunicaIn)
        {
            promoId = medioComunicaIn.promoId;
            textMsg = medioComunicaIn.textMsg;
            medioName = MedioComunicacion.fromIdToInstance(medioComunicaIn.medioId).name();
        }

        @SuppressWarnings({"unused", "used in velocity"})
        public String getTextMsg()
        {
            return textMsg;
        }

        @SuppressWarnings({"unused", "used in velocity"})
        public String getMedioName()
        {
            return medioName;
        }
    }
}
