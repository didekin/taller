package com.lebenlab.core.simulacion;

import com.lebenlab.BeanBuilder;
import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;
import com.lebenlab.core.Promocion;
import com.lebenlab.core.mediocom.MedioComunicacion;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.mediocom.TextClassifier.TextClassEnum;
import com.lebenlab.core.tbmaster.ConceptoTaller;
import com.lebenlab.core.tbmaster.Incentivo;
import com.lebenlab.core.tbmaster.Incentivo.IncentivoId;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.tbmaster.PG1;

import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static com.lebenlab.ProcessArgException.error_build_text_clasificacion;
import static com.lebenlab.ProcessArgException.error_dataframe_modelo_wrong;
import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static com.lebenlab.core.Promocion.FieldLabel.duracion_promo;
import static com.lebenlab.core.Promocion.FieldLabel.incentivo_id;
import static com.lebenlab.core.Promocion.FieldLabel.pg1_id_with_1;
import static com.lebenlab.core.Promocion.FieldLabel.pg1_id_with_2;
import static com.lebenlab.core.Promocion.FieldLabel.quarter_promo;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.medio_id;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.msg_classification;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.ratio_aperturas;
import static com.lebenlab.core.mediocom.MedioComunicacion.fromIdToInstance;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.NA;
import static com.lebenlab.core.simulacion.Quarter.fromIntToQuarter;
import static com.lebenlab.core.tbmaster.ConceptoTaller.fromIntToConcepto;
import static com.lebenlab.core.tbmaster.Incentivo.IncentivoId.fromIntToIncentivoId;
import static com.lebenlab.core.tbmaster.Incentivo.fromIntToIncentivo;
import static com.lebenlab.core.tbmaster.Mercado.fromIntToMercado;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static java.time.LocalDate.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;

/**
 * User: pedro@didekin
 * Date: 29/03/2020
 * Time: 13:37
 */
public class PredictorRowDf implements Jsonable {

    public final Mercado mercado;
    public final ConceptoTaller concepto;
    public final int diasRegistro;
    public final double vtaMediaDiariaPg1Exp;
    public final int duracionPromo;
    public final Quarter quarterPromo;
    public final IncentivoId incentivo;
    public final MedioComunicacion medioCom;
    public final TextClassEnum txtMsgClass;
    public final double ratioAperturas;
    public final PG1 pg1WithOne;
    public final PG1 pg1WithTwo;

    public PredictorRowDf(PredictorRowDfBuilder builder)
    {
        mercado = builder.mercado;
        concepto = builder.concepto;
        diasRegistro = builder.diasRegistro;
        vtaMediaDiariaPg1Exp = builder.vtaMediaDiariaPg1Exp;
        duracionPromo = builder.duracionPromo;
        quarterPromo = builder.quarterPromo;
        incentivo = fromIntToIncentivoId(builder.incentivo.incentivoId);
        medioCom = fromIdToInstance(builder.proMedCom.medioId);
        txtMsgClass = TextClassEnum.fromIdToInstance(builder.proMedCom.codTextClass);
        ratioAperturas = builder.ratioAperturas;
        pg1WithOne = builder.pg1WithOne;
        pg1WithTwo = builder.pg1WithTwo;
    }

    public Mercado getMercado()
    {
        return mercado;
    }

    public ConceptoTaller getConcepto()
    {
        return concepto;
    }

    @SuppressWarnings({"unused", "For Smile reflection"})
    public int getDiasRegistro()
    {
        return diasRegistro;
    }

    @SuppressWarnings({"unused", "For Smile reflection"})
    public double getVtaMediaDiariaPg1Exp()
    {
        return vtaMediaDiariaPg1Exp;
    }

    @SuppressWarnings({"unused", "For Smile reflection"})
    public int getDuracionPromo()
    {
        return duracionPromo;
    }

    @SuppressWarnings({"unused", "For Smile reflection"})
    public Quarter getQuarterPromo()
    {
        return quarterPromo;
    }

    public IncentivoId getIncentivo()
    {
        return incentivo;
    }

    public MedioComunicacion getMedioCom()
    {
        return medioCom;
    }

    @SuppressWarnings({"unused", "For Smile reflection"})
    public TextClassEnum getTxtMsgClass()
    {
        return txtMsgClass;
    }

    @SuppressWarnings({"unused", "For Smile reflection"})
    public double getRatioAperturas()
    {
        return ratioAperturas;
    }

    @SuppressWarnings({"unused", "For Smile reflection"})
    public PG1 getPg1WithOne()
    {
        return pg1WithOne;
    }

    @SuppressWarnings({"unused", "For Smile reflection"})
    public PG1 getPg1WithTwo()
    {
        return pg1WithTwo;
    }

    // ====================== Static members ======================

    public static final RowMapper<PredictorRowDf> mapper = (rs, ctx) -> new PredictorRowDfBuilder(rs).build();

    public static final List<String> instanceFields = asList("mercado", "concepto", "diasRegistro", "vtaMediaDiariaPg1Exp",
            "duracionPromo", "quarterPromo", "incentivo", "medioCom", "txtMsgClass", "ratioAperturas", "pg1WithOne", "pg1WithTwo");

    // ============================= Builder =========================

    /**
     * User: pedro@didekin
     * Date: 29/03/2020
     * Time: 13:43
     */
    public static class PredictorRowDfBuilder implements BeanBuilder<PredictorRowDf> {

        Mercado mercado;
        ConceptoTaller concepto;
        int diasRegistro;
        double vtaMediaDiariaPg1Exp;
        int duracionPromo;
        Quarter quarterPromo;
        Incentivo incentivo;
        PromoMedioComunica proMedCom;
        public double ratioAperturas;
        PG1 pg1WithOne;
        PG1 pg1WithTwo;

        public PredictorRowDfBuilder()
        {
        }

        public PredictorRowDfBuilder(ResultSet rs)
        {
            this();
            mercado(rs)
                    .concepto(rs)
                    .diasRegistro(rs)
                    .vtaMediaDiariaPg1Exp(rs)
                    .duracionPromo(rs)
                    .quarterProm(rs)
                    .incentivo(rs)
                    .medioComunica(rs)
                    .ratioAperturas(rs)
                    .pg1WithOne(rs)
                    .pg1WithTwo(rs);
        }

        /**
         * Constructor for tests.
         */
        public PredictorRowDfBuilder(Mercado mercado, ConceptoTaller concepto, int diasRegistro, double vtaMediaDiariaPg1Exp,
                                     int duracionPromo, Quarter quarterPromo, Incentivo incentivo,
                                     PromoMedioComunica proMedComIn, double ratioAperturas, PG1 pg1WithOne, PG1 pg1WithTwo)
        {
            this.mercado = mercado;
            this.concepto = concepto;
            this.diasRegistro = diasRegistro;
            this.vtaMediaDiariaPg1Exp = vtaMediaDiariaPg1Exp;
            this.duracionPromo = duracionPromo;
            this.quarterPromo = quarterPromo;
            this.incentivo = incentivo;
            proMedCom = proMedComIn;
            this.ratioAperturas = ratioAperturas;
            this.pg1WithOne = pg1WithOne;
            this.pg1WithTwo = pg1WithTwo;
        }

        public PredictorRowDfBuilder(Promocion promoIn, LocalDate fechaRegistroParticip, double vtaMediaDiariaPg1Exp, double ratioAperturas)
        {
            this(
                    fromIntToMercado(promoIn.mercados.get(0)),
                    fromIntToConcepto(promoIn.conceptos.get(0)),
                    (int) DAYS.between(fechaRegistroParticip, now()),
                    vtaMediaDiariaPg1Exp,
                    promoIn.getDuracionDias(),
                    fromIntToQuarter(promoIn.getQuarter()),
                    fromIntToIncentivo(promoIn.incentivo),
                    promoIn.promoMedioComunica,
                    ratioAperturas,
                    fromIntPg1(promoIn.getPg1IdsZeroPadded().get(1)),
                    fromIntPg1(promoIn.getPg1IdsZeroPadded().get(2))
            );
        }

        public PredictorRowDfBuilder mercado(ResultSet rs)
        {
            try {
                mercado = fromIntToMercado(rs.getShort("mercado_id"));
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + "mercado_id " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder concepto(ResultSet rs)
        {
            try {
                concepto = fromIntToConcepto(rs.getShort("concepto_id"));
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + "concepto_id " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder diasRegistro(ResultSet rs)
        {
            try {
                diasRegistro = rs.getInt("dias_registro");
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + "dias_registro " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder vtaMediaDiariaPg1Exp(ResultSet rs)
        {
            try {
                vtaMediaDiariaPg1Exp = rs.getDouble("vta_media_diaria_pg1_exp");
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + "vta_media_diaria_pg1_exp " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder duracionPromo(ResultSet rs)
        {
            try {
                duracionPromo = rs.getInt(duracion_promo.name());
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + duracion_promo.name() + " " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder quarterProm(ResultSet rs)
        {
            try {
                quarterPromo = fromIntToQuarter(rs.getShort(quarter_promo.name()));
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + quarter_promo.name() + " " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder incentivo(ResultSet rs)
        {
            try {
                incentivo = fromIntToIncentivo(rs.getShort(incentivo_id.name()));
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + incentivo_id.name() + " " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder medioComunica(ResultSet rs)
        {
            try {
                proMedCom = new PromoMedioComunica.PromoMedComBuilder()
                        .medioId(rs.getInt(medio_id.name()))
                        .msgClassifier(rs.getInt(msg_classification.name()))
                        .build();
                if (proMedCom.medioId == ninguna.id && !proMedCom.textMsg.equals(NA.name())) {
                    throw new ProcessArgException(error_build_text_clasificacion);
                }
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + medio_id.name() + " " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder ratioAperturas(ResultSet rs)
        {
            try {
                ratioAperturas = rs.getDouble(ratio_aperturas.name());
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + ratio_aperturas.name() + " " + e.getMessage());
            }
        }

        public PredictorRowDfBuilder pg1WithOne(ResultSet rs)
        {
            try {
                pg1WithOne = fromIntPg1(rs.getShort(pg1_id_with_1.name()));
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + pg1_id_with_1.name() + " " + e.getMessage());
            }
        }

        @SuppressWarnings("UnusedReturnValue")
        public PredictorRowDfBuilder pg1WithTwo(ResultSet rs)
        {
            try {
                pg1WithTwo = fromIntPg1(rs.getShort(pg1_id_with_2.name()));
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + pg1_id_with_2.name() + " " + e.getMessage());
            }
        }

        boolean isAnyFieldWrong()
        {
            // No controlo las medias de cantidad porque admitimos cantidades negativas (correctoras) en los ficheros.
            // La aplicación no controla que el balance final de cantidades es >= 0 por participante en cada carga de ficheros.
            return diasRegistro < 0
                    || mercado == null
                    || concepto == null
                    || incentivo == null
                    || quarterPromo == null
                    || pg1WithOne == null
                    || pg1WithTwo == null;
        }

        @Override
        public PredictorRowDf build()
        {
            PredictorRowDf instance = new PredictorRowDf(this);
            if (isAnyFieldWrong()) {
                throw new ProcessArgException(error_dataframe_modelo_wrong);
            }
            return instance;
        }
    }
}
