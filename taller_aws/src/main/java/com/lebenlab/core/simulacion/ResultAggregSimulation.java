package com.lebenlab.core.simulacion;

import com.lebenlab.BeanBuilder;
import com.lebenlab.Jsonable;
import com.lebenlab.core.Pg1Promocion;
import com.lebenlab.core.tbmaster.PG1;

import java.util.List;
import java.util.function.Function;

import static com.lebenlab.DataPatterns.getDecimalNumberStr;
import static com.lebenlab.ProcessArgException.no_data_for_prediction;
import static com.lebenlab.core.tbmaster.PG1.fromIntPg1;
import static java.util.Arrays.stream;
import static java.util.Objects.hash;
import static java.util.stream.Collectors.toList;

/**
 * User: pedro@didekin.es
 * Date: 12/10/2019
 * Time: 17:36
 */
public class ResultAggregSimulation implements Jsonable {

    public final List<ResultAvgPG1> resultAvgPG1s;
    public final int numParticipantes;
    public final String mensaje;

    private ResultAggregSimulation(ResultByPG1Builder builder)
    {
        resultAvgPG1s = builder.resultAvgPG1s;
        numParticipantes = builder.numeroParticipantes;
        mensaje = builder.mensaje;
    }

    @SuppressWarnings({"unused", "velocity"})
    public List<ResultAvgPG1> getResultAvgPG1s()
    {
        return resultAvgPG1s;
    }

    @SuppressWarnings({"unused", "velocity"})
    public int getNumParticipantes()
    {
        return numParticipantes;
    }

    public String getMensaje()
    {
        return mensaje;
    }

    //    ==================== BUILDER ====================

    public static class ResultByPG1Builder implements BeanBuilder<ResultAggregSimulation> {

        List<ResultAvgPG1> resultAvgPG1s;
        int numeroParticipantes;
        String mensaje;

        private ResultByPG1Builder()
        {
        }

        public ResultByPG1Builder(List<ResultsPG1> predictionsByPg1)
        {
            this();
            resultAvgPG1s(predictionsByPg1);
        }

        void resultAvgPG1s(List<ResultsPG1> predictionsByPg1)
        {
            for (ResultsPG1 result : predictionsByPg1) {
                if (result.mediaDiariaTaller.length == 1 && result.mediaDiariaTaller[0] == 0) {
                    mensaje(no_data_for_prediction + result.pg1.name());
                }
            }
            resultAvgPG1s = predictionsByPg1.stream()
                    .map(rPG1 -> new ResultAvgPG1(
                                    rPG1.pg1,
                                    stream(rPG1.mediaDiariaTaller).average().orElse(0),
                                    (rPG1.mediaDiariaTaller.length == 1
                                            && rPG1.mediaDiariaTaller[0] == 0) ? 0 : rPG1.mediaDiariaTaller.length
                            )
                    )
                    .collect(toList());
        }

        ResultByPG1Builder numeroParticipantes(int numeroParticipantes)
        {
            this.numeroParticipantes = numeroParticipantes;
            return this;
        }

        void mensaje(String mensajeIn)
        {
            if (mensaje == null) {
                mensaje = mensajeIn;
            } else {
                mensaje += "\n" + mensajeIn;
            }
        }

        @Override
        public ResultAggregSimulation build()
        {
            return new ResultAggregSimulation(this);
        }
    }

    // ======================= Static classes =====================

    public final static class ResultAvgPG1 {

        public final PG1 pg1;
        public final double mediaGlobalTalleres;
        public final int numEstimaciones;

        public ResultAvgPG1(PG1 pg1, double dayAvgOverAllTalleres, int numEstimaciones)
        {
            this.pg1 = pg1;
            this.mediaGlobalTalleres = dayAvgOverAllTalleres;
            this.numEstimaciones = numEstimaciones;
        }

        public PG1 getPg1()
        {
            return pg1;
        }

        @SuppressWarnings({"unused", "velocity"})
        public String getMediaGlobalTalleresStr()
        {
            return getDecimalNumberStr(mediaGlobalTalleres, 2);
        }

        @SuppressWarnings({"unused", "velocity"})
        public int getNumEstimaciones()
        {
            return numEstimaciones;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof ResultAvgPG1) {
                ResultAvgPG1 objectIn = (ResultAvgPG1) obj;
                return pg1 == objectIn.pg1
                        && mediaGlobalTalleres == objectIn.mediaGlobalTalleres
                        && numEstimaciones == objectIn.numEstimaciones;
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return hash(pg1, mediaGlobalTalleres, numEstimaciones);
        }
    }

    public final static class ResultsPG1 {

        public final PG1 pg1;
        public final double[] mediaDiariaTaller;

        public ResultsPG1(Pg1Promocion pg1Promocion, Function<Pg1Promocion, double[]> predictionsSupplier)
        {
            this.pg1 = fromIntPg1(pg1Promocion.pg1);
            this.mediaDiariaTaller = predictionsSupplier.apply(pg1Promocion);
        }
    }
}
