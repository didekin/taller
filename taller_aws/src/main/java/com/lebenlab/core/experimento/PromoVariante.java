package com.lebenlab.core.experimento;

import com.lebenlab.BeanBuilder;
import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.mediocom.PromoMedioComunica;

import java.util.List;
import java.util.Map;

import static com.lebenlab.DataPatterns.COD_PROMO;
import static com.lebenlab.ProcessArgException.error_build_promocion;
import static com.lebenlab.ProcessArgException.error_build_variante;
import static com.lebenlab.ProcessArgException.error_incentivo;
import static com.lebenlab.ProcessArgException.error_medio;
import static com.lebenlab.ProcessArgException.error_pg1s;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.cod_variante;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.incentivo_id_variante;
import static com.lebenlab.core.experimento.PromoVariante.FieldLabel.pg1s_variante;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id_variante;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.promo_medio_text_variante;
import static com.lebenlab.core.tbmaster.Incentivo.fromIntToIncentivo;
import static com.lebenlab.core.tbmaster.PG1.checkPg1sIn;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;


/**
 * User: pedro@didekin.es
 * Date: 06/02/2020
 * Time: 15:50
 */
public class PromoVariante implements Jsonable {

    public final String codPromo;
    public final List<Integer> pg1s;
    public final int incentivo;
    public final PromoMedioComunica promoMedioComunica;

    public PromoVariante(VarianteBuilder builder)
    {
        codPromo = builder.codPromo;
        pg1s = builder.pg1s;
        incentivo = builder.incentivo;
        promoMedioComunica = builder.promoMedioComunica;
    }

    public String getCodPromo()
    {
        return codPromo;
    }

    @SuppressWarnings({"unused", "velocity"})
    public String getIncentivoNombre()
    {
        return fromIntToIncentivo(incentivo).name().trim();
    }

    // =============================== Facade methods and classes =================================

    public Promocion asPromocion(Promocion promocionA)
    {
        return new Promocion.PromoBuilder(this)
                .fechaInicio(promocionA.fechaInicio)
                .fechaFin(promocionA.fechaFin)
                .mercados(promocionA.mercados)
                .conceptos(promocionA.conceptos)
                .build();
    }

    public enum FieldLabel {
        cod_variante,
        pg1s_variante,
        incentivo_id_variante,
    }

    // =============================== Builder =================================

    public static final class VarianteBuilder implements BeanBuilder<PromoVariante> {

        private String codPromo;
        private List<Integer> pg1s;
        private int incentivo;
        public PromoMedioComunica promoMedioComunica;

        public VarianteBuilder(String codPromo, int incentivo, PromoMedioComunica promoMedioIn)
        {
            codPromo(codPromo).incentivo(incentivo).medio(promoMedioIn);
        }

        public VarianteBuilder(String codPromo, List<Integer> pg1s, int incentivo, PromoMedioComunica promoMedioIn)
        {
            codPromo(codPromo).pg1s(pg1s).incentivo(incentivo).medio(promoMedioIn);
        }

        public VarianteBuilder(Map<String, List<String>> expParams)
        {
            codPromo(expParams.get(cod_variante.name()).get(0));
            pg1s(expParams.get(pg1s_variante.name()).stream().mapToInt(Integer::parseInt).boxed().collect(toList()));
            incentivo(parseInt(expParams.get(incentivo_id_variante.name()).get(0)));
            medio(expParams);
        }

        VarianteBuilder codPromo(String codPromoIn)
        {
            if (COD_PROMO.isPatternOk(codPromoIn)) {
                codPromo = codPromoIn;
                return this;
            }
            throw new ProcessArgException(error_build_promocion + ": " + codPromoIn);
        }

        VarianteBuilder pg1s(List<Integer> pg1sIn)
        {
            if (pg1sIn != null && pg1sIn.size() <= 3 && checkPg1sIn(pg1sIn)) {
                pg1s = pg1sIn;
                return this;
            }
            throw new ProcessArgException(error_pg1s);
        }

        VarianteBuilder incentivo(int incentivoIn)
        {
            if (incentivoIn <= 0) {
                throw new ProcessArgException(error_incentivo + incentivoIn);
            }
            incentivo = incentivoIn;
            return this;
        }

        @SuppressWarnings({"UnusedReturnValue"})
        VarianteBuilder medio(Map<String, List<String>> paramsMap)
        {
            final var medioVarianteText = paramsMap.get(promo_medio_text_variante.name()).get(0);
            final var medioVarianteId = parseInt(paramsMap.get(medio_id_variante.name()).get(0));
            if (medioVarianteId <= 0) {
                throw new ProcessArgException(error_medio + medioVarianteId);
            }
            promoMedioComunica = new PromoMedioComunica.PromoMedComBuilder()
                    .medioId(medioVarianteId)
                    .textMsg(medioVarianteText)
                    .build();
            return this;
        }

        public VarianteBuilder medio(PromoMedioComunica promoMedioComunica)
        {
            this.promoMedioComunica = promoMedioComunica;
            return this;
        }

        @Override
        public PromoVariante build()
        {
            PromoVariante variante = new PromoVariante(this);
            if (codPromo == null || incentivo <= 0 || promoMedioComunica == null || pg1s == null || pg1s.isEmpty()) {
                throw new ProcessArgException(error_build_variante);
            }
            return variante;
        }

        public PromoVariante buildForSummary()
        {
            PromoVariante variante = new PromoVariante(this);
            if (codPromo == null) {
                throw new ProcessArgException(error_build_variante);
            }
            return variante;
        }
    }
}
