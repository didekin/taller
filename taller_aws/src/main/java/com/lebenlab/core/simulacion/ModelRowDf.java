package com.lebenlab.core.simulacion;

import com.lebenlab.ProcessArgException;
import com.lebenlab.core.mediocom.PromoMedioComunica;
import com.lebenlab.core.tbmaster.ConceptoTaller;
import com.lebenlab.core.tbmaster.Incentivo;
import com.lebenlab.core.tbmaster.Mercado;
import com.lebenlab.core.tbmaster.PG1;

import org.jdbi.v3.core.mapper.RowMapper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.lebenlab.ProcessArgException.error_dataframe_modelo_wrong;
import static com.lebenlab.ProcessArgException.error_jdbi_statement;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * User: pedro@didekin
 * Date: 21/03/2020
 * Time: 19:35
 */
public class ModelRowDf extends PredictorRowDf {

    /**
     * Name to be used for identifying the left hand side of random forest formula.
     */
    public static final String responseVar = "vtaMediaDiariaPg1";

    /**
     * Average of result (quantity) by day of promotion elapsed (quantities/durantion elapsed)
     */
    public final double vtaMediaDiariaPg1;

    private ModelRowDf(ModelRowDfBuilder builder)
    {
        super(builder);
        vtaMediaDiariaPg1 = builder.vtaMediaDiariaPg1;
    }

    @SuppressWarnings({"unused", "for Smile reflection"})
    public double getVtaMediaDiariaPg1()
    {
        return vtaMediaDiariaPg1;
    }

    // ====================== Static members =====================

    public static final RowMapper<ModelRowDf> mapper = (rs, ctx) -> new ModelRowDfBuilder(rs).build();

    public static final List<String> instanceFields = stream(ModelRowDf.class.getFields())
            .filter(field -> !isStatic(field.getModifiers())).map(Field::getName).collect(toList());

    public static final long varCount = instanceFields.size();

    // ====================== Builder ======================

    @SuppressWarnings("UnusedReturnValue")
    public static final class ModelRowDfBuilder extends PredictorRowDfBuilder {

        double vtaMediaDiariaPg1;

        /**
         * Constructor for tests.
         */
        public ModelRowDfBuilder(double vtaMediaDiariaPg1, Mercado mercado, ConceptoTaller concepto, int diasRegistro,
                                 double vtaMediaDiariaPg1Exp, int duracionPromo, Quarter quarterPromo, Incentivo incentivo,
                                 PromoMedioComunica promoMedioComunica, double ratioAperturas, PG1 pg1WithOne, PG1 pg1WithTwo)
        {
            super(mercado, concepto, diasRegistro, vtaMediaDiariaPg1Exp, duracionPromo, quarterPromo, incentivo, promoMedioComunica, ratioAperturas, pg1WithOne, pg1WithTwo);
            this.vtaMediaDiariaPg1 = vtaMediaDiariaPg1;
        }

        public ModelRowDfBuilder(ResultSet rs)
        {
            super(rs);
            vtaMediaDiariaPg1(rs);
        }

        public PredictorRowDfBuilder vtaMediaDiariaPg1(ResultSet rs)
        {
            try {
                vtaMediaDiariaPg1 = rs.getDouble("vta_media_diaria_pg1");
                return this;
            } catch (SQLException e) {
                throw new ProcessArgException(error_jdbi_statement + "vta_media_diaria_pg1 " + e.getMessage());
            }
        }

        @Override
        public ModelRowDf build()
        {
            ModelRowDf respDf = new ModelRowDf(this);
            if (isAnyFieldWrong()) {
                throw new ProcessArgException(error_dataframe_modelo_wrong);
            }
            return respDf;
        }
    }
}
