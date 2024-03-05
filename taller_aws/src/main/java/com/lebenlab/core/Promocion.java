package com.lebenlab.core;

import com.lebenlab.BeanBuilder;
import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;
import com.lebenlab.core.experimento.PromoVariante;
import com.lebenlab.core.mediocom.MedioComunicacion;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.mediocom.TextClassifier;
import com.lebenlab.core.tbmaster.ConceptoTaller;
import com.lebenlab.core.tbmaster.Incentivo;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.tbmaster.PG1;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.lebenlab.DataPatterns.COD_PROMO;
import static com.lebenlab.ProcessArgException.error_build_promocion;
import static com.lebenlab.ProcessArgException.error_conceptos;
import static com.lebenlab.ProcessArgException.error_incentivo;
import static com.lebenlab.ProcessArgException.error_medio;
import static com.lebenlab.ProcessArgException.error_mercados;
import static com.lebenlab.ProcessArgException.error_pg1s;
import static com.lebenlab.core.Promocion.FieldLabel.cod_promo;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_fin;
import static com.lebenlab.core.Promocion.FieldLabel.fecha_inicio;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo_id;
import static com.lebenlab.core.Promocion.FieldLabel.mercados;
import static com.lebenlab.core.Promocion.FieldLabel.promo_id;
import static java.lang.Integer.parseInt;
import static java.time.LocalDate.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.IsoFields.QUARTER_OF_YEAR;
import static java.util.Arrays.stream;
import static java.util.List.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * User: pedro@didekin.es
 * Date: 12/10/2019
 * Time: 15:58
 */
public final class Promocion implements Jsonable {

    // Nullable.
    public final long idPromo;
    public final String codPromo;
    public final LocalDate fechaInicio;
    public final LocalDate fechaFin;
    public final List<Integer> mercados;
    public final List<Integer> conceptos;
    public final List<Integer> pg1s;
    public final int incentivo;
    public final PromoMedioComunica promoMedioComunica;
    public final long experimentoId;

    private Promocion(PromoBuilder builder)
    {
        idPromo = builder.idPromo;
        codPromo = builder.codPromo;
        fechaInicio = builder.fechaInicio;
        fechaFin = builder.fechaFin;
        mercados = builder.mercados;
        conceptos = builder.conceptos;
        pg1s = builder.pg1s;
        incentivo = builder.incentivo;
        promoMedioComunica = builder.promoMedioComunica;
        experimentoId = builder.experimentoId;
    }

    // ======================= Instance methods ============================

    public long getIdPromo()
    {
        return idPromo;
    }

    public String getCodPromo()
    {
        return codPromo;
    }

    public LocalDate getFechaInicio()
    {
        return fechaInicio;
    }

    public LocalDate getFechaFin()
    {
        return fechaFin;
    }

    @NotNull
    public int[] getConceptosArr()
    {
        return conceptos.stream().mapToInt(value -> value).toArray();
    }

    @SuppressWarnings({"unused", "used in velocity template"})
    public List<ConceptoTaller> conceptosEnum()
    {
        return conceptos.stream().map(ConceptoTaller::fromIntToConcepto).collect(toList());
    }

    public int getDuracionDias()
    {
        return (int) DAYS.between(fechaInicio, fechaFin) + 1;
    }

    public int getQuarter()
    {
        return fechaInicio.get(QUARTER_OF_YEAR);
    }

    @SuppressWarnings({"unused", "used in veloctiy template"})
    public String getIncentivoNombre()
    {
        return Incentivo.fromIntToIncentivo(incentivo).name().trim();
    }

    @NotNull
    public int[] getMercadosArr()
    {
        return mercados.stream().mapToInt(value -> value).toArray();
    }

    @SuppressWarnings({"unused", "used in veloctiy template"})
    public List<Mercado> mercadosEnum()
    {
        return mercados.stream().map(Mercado::fromIntToMercado).collect(toList());
    }

    @NotNull
    public int[] getPg1IdsArr()
    {
        return pg1s.stream().mapToInt(value -> value).toArray();
    }

    public List<Integer> getPg1IdsZeroPadded()
    {
        List<Integer> newList = new ArrayList<>(pg1s);
        if (pg1s.size() < 3) {
            newList.addAll(of(0));
        }
        if (pg1s.size() < 2) {
            newList.addAll(of(0));
        }
        return newList;
    }

    // =============================== Facade methods =================================

    public static final RowMapper<Promocion> shortMapper = (rs, ctx) -> new PromoBuilder()
            .idPromo(rs.getInt(promo_id.name()))
            .codPromo(rs.getString(cod_promo.name()))
            .fechaInicio(rs.getDate(fecha_inicio.name()).toLocalDate())
            .fechaFin(rs.getDate(fecha_fin.name()).toLocalDate())
            .buildForSummary();

    public PromoVariante asVariante()
    {
        return new PromoVariante.VarianteBuilder(codPromo, pg1s, incentivo, promoMedioComunica).build();
    }

    public PromoVariante asVarianteSummary()
    {
        return new PromoVariante.VarianteBuilder(codPromo, incentivo, promoMedioComunica).buildForSummary();
    }

    /**
     * Literals to be used for templating JDBI queries and form param names.
     */
    public enum FieldLabel {
        cod_promo,
        concepto_id,
        conceptos,  // form param for a combo.
        duracion_promo,
        fecha_inicio,
        fecha_fin,
        incentivo_id,
        incentivo, // form param for a combo.
        // form param for a combo.
        mercado_id,
        mercados, // form param for a combo.
        pg1s, // form param for a combo 3-selections.
        pg1_id,
        pg1_id_with_1,
        pg1_id_with_2,
        pg1_ids_list,
        promo_id,
        promosId,
        quarter_promo,
    }

    // ============================= Builder ==================================

    public final static class PromoBuilder implements BeanBuilder<Promocion> {

        private long experimentoId;
        private long idPromo;
        private String codPromo;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private List<Integer> mercados;
        private List<Integer> conceptos;
        private List<Integer> pg1s;
        private int incentivo;
        private PromoMedioComunica promoMedioComunica;

        public PromoBuilder()
        {
        }

        public PromoBuilder(PromoVariante varianteIn)
        {
            codPromo(varianteIn.codPromo);
            incentivo(varianteIn.incentivo);
            pg1s(varianteIn.pg1s);
            medio(varianteIn.promoMedioComunica);
        }

        /**
         * Constructor for html forms.
         */
        public PromoBuilder(Map<String, List<String>> paramsMap)
        {
            codPromo(paramsMap.get(cod_promo.name()).get(0));
            fechaInicio(parse(paramsMap.get(fecha_inicio.name()).get(0), ofPattern("dd-MM-yyyy")));
            fechaFin(parse(paramsMap.get(fecha_fin.name()).get(0), ofPattern("dd-MM-yyyy")));
            mercados(paramsMap.get(FieldLabel.mercados.name()).stream().mapToInt(Integer::parseInt).boxed().collect(toList()));
            conceptos(paramsMap.get(FieldLabel.conceptos.name()).stream().mapToInt(Integer::parseInt).boxed().collect(toList()));
            pg1s(paramsMap.get(FieldLabel.pg1s.name()).stream().mapToInt(Integer::parseInt).boxed().collect(toList()));
            incentivo(parseInt(paramsMap.get(incentivo_id.name()).get(0)));
            medio(paramsMap);
        }

        public PromoBuilder copyPromo(Promocion promoIn)
        {
            if (promoIn.experimentoId != 0) {
                experimentoId(promoIn.experimentoId);
            }
            if (promoIn.idPromo != 0) {
                idPromo(promoIn.idPromo);
            }
            if (promoIn.codPromo != null && !promoIn.codPromo.isEmpty()) {
                codPromo(promoIn.codPromo);
            }
            if (promoIn.fechaInicio != null) {
                fechaInicio(promoIn.fechaInicio);
            }
            if (promoIn.fechaFin != null) {
                fechaFin(promoIn.fechaFin);
            }
            if (promoIn.incentivo > 0) {
                incentivo(promoIn.incentivo);
            }
            if (promoIn.conceptos != null && !promoIn.conceptos.isEmpty()) {
                conceptos(promoIn.conceptos);
            }
            if (promoIn.promoMedioComunica != null) {
                medio(promoIn.promoMedioComunica);
            }
            if (promoIn.mercados != null && !promoIn.mercados.isEmpty()) {
                mercados(promoIn.mercados);
            }
            if (promoIn.pg1s != null && !promoIn.pg1s.isEmpty()) {
                pg1s(promoIn.pg1s);
            }
            return this;
        }

        public PromoBuilder codPromo(String codPromoIn)
        {
            if (COD_PROMO.isPatternOk(codPromoIn)) {
                codPromo = codPromoIn;
                return this;
            }
            throw new ProcessArgException(error_build_promocion + ": " + codPromoIn);
        }

        public PromoBuilder conceptos(List<Integer> conceptosIn)
        {
            if (conceptosIn == null || conceptosIn.isEmpty()) {
                conceptos = stream(ConceptoTaller.values()).map(c -> c.conceptoId).collect(toUnmodifiableList());
                return this;
            }
            if (ConceptoTaller.checkConceptosIn(conceptosIn)) {
                conceptos = conceptosIn;
                return this;
            }
            throw new ProcessArgException(error_conceptos);
        }

        public PromoBuilder experimentoId(long experimentoIdIn)
        {
            experimentoId = experimentoIdIn;
            return this;
        }

        public PromoBuilder fechaInicio(LocalDate inicio)
        {
            fechaInicio = inicio;
            return this;
        }

        public PromoBuilder fechaFin(LocalDate fin)
        {
            fechaFin = fin;
            return this;
        }

        public PromoBuilder idPromo(long idPromoIn)
        {
            idPromo = idPromoIn;
            return this;
        }

        public PromoBuilder incentivo(int incentivoIn)
        {
            if (incentivoIn <= 0) {
                throw new ProcessArgException(error_incentivo + incentivoIn);
            }
            incentivo = incentivoIn;
            return this;
        }

        public PromoBuilder medio(Map<String, List<String>> paramsMap)
        {
            final var medioId = parseInt(paramsMap.get(MedioComunicacion.MedioLabels.medio_id.name()).get(0));
            final var medioText = paramsMap.get(MedioComunicacion.MedioLabels.promo_medio_text.name()).get(0);
            final var textClass = TextClassifier.textClassDao.classifyText(medioText);
            if (medioId <= 0) {
                throw new ProcessArgException(error_medio + medioId);
            }
            promoMedioComunica = new PromoMedioComunica.PromoMedComBuilder()
                    .medioId(medioId)
                    .textMsg(medioText)
                    .msgClassifier(textClass.codigoNum)
                    .build();
            return this;
        }

        public PromoBuilder medio(PromoMedioComunica promoMedioComunica)
        {
            this.promoMedioComunica = new PromoMedioComunica.PromoMedComBuilder().copy(promoMedioComunica).build();
            return this;
        }

        public PromoBuilder mercados(List<Integer> mercadosIn)
        {
            if (mercadosIn == null || mercadosIn.isEmpty()) {
                mercados = stream(Mercado.values()).map(mercado -> mercado.id).collect(toUnmodifiableList());
                return this;
            }
            if (Mercado.checkMercadosIn(mercadosIn)) {
                mercados = mercadosIn;
                return this;
            }
            throw new ProcessArgException(error_mercados);
        }

        public PromoBuilder pg1s(List<Integer> pg1sIn)
        {
            if (pg1sIn != null && pg1sIn.size() <= 3 && PG1.checkPg1sIn(pg1sIn)) {
                pg1s = pg1sIn;
                return this;
            }
            throw new ProcessArgException(error_pg1s);
        }

        @Override
        public Promocion build()
        {
            Promocion promocion = new Promocion(this);
            if (promocion.fechaInicio == null
                    || promocion.fechaFin == null
                    || promocion.getDuracionDias() <= 0
                    || promocion.codPromo == null
                    || promocion.mercados == null
                    || promocion.conceptos == null
                    || promocion.pg1s == null
                    || promocion.incentivo <= 0
                    || promocion.promoMedioComunica == null) {
                throw new ProcessArgException(error_build_promocion);
            }
            return promocion;
        }

        public Promocion buildForSummary()
        {
            Promocion promocion = new Promocion(this);
            if (promocion.fechaInicio == null
                    || promocion.fechaFin == null
                    || promocion.getDuracionDias() <= 0
                    || promocion.codPromo == null) {
                throw new ProcessArgException(error_build_promocion);
            }
            return promocion;
        }
    }
}
