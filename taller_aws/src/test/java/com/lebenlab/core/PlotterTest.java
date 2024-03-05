package com.lebenlab.core;



import com.lebenlab.core.util.DataTestExperiment;

import org.junit.Test;

import static com.lebenlab.core.Plotter.doArrColumn;
import static com.lebenlab.core.Plotter.doClustersLabels;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 28/08/2020
 * Time: 16:25
 */
public class PlotterTest {

    @Test
    public void test_DoArrColumn()
    {
        final var arrTest = new double[][]{{2, 1, 10.5, 11.5}, {3, 2, 20.5, 21.5}};
        assertThat(doArrColumn(arrTest, 0)).contains(2, 3);
        assertThat(doArrColumn(arrTest, 1)).contains(1, 2);
        assertThat(doArrColumn(arrTest, 2)).contains(10.5, 20.5);
        assertThat(doArrColumn(arrTest, 3)).contains(11.5, 21.5);
    }

    @Test
    public void test_MultiplyByScalar()
    {
        double[] arrIn = new double[]{2, 3, 7, 8};
        assertThat(Plotter.multiplyByScalar(arrIn, 2)).contains(4, 6, 14, 16);
    }

    @Test
    public void test_DoClustersLabels()
    {
        double[][] x = DataTestExperiment.getXDataCluster();
        assertThat(doClustersLabels(x).length).isEqualTo(x.length);
    }
}