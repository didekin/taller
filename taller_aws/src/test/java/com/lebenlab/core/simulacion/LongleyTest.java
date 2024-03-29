package com.lebenlab.core.simulacion;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.DoubleVector;
import smile.regression.RandomForest;

import static org.assertj.core.api.Assertions.assertThat;
import static smile.data.DataFrame.of;

/**
 * @author Haifeng
 */
public class LongleyTest {

    public static final double[][] x = {
            {234.289, 235.6, 159.0, 107.608, 1947, 60.323},
            {259.426, 232.5, 145.6, 108.632, 1948, 61.122},
            {258.054, 368.2, 161.6, 109.773, 1949, 60.171},
            {284.599, 335.1, 165.0, 110.929, 1950, 61.187},
            {328.975, 209.9, 309.9, 112.075, 1951, 63.221},
            {346.999, 193.2, 359.4, 113.270, 1952, 63.639},
            {365.385, 187.0, 354.7, 115.094, 1953, 64.989},
            {363.112, 357.8, 335.0, 116.219, 1954, 63.761},
            {397.469, 290.4, 304.8, 117.388, 1955, 66.019},
            {419.180, 282.2, 285.7, 118.734, 1956, 67.857},
            {442.769, 293.6, 279.8, 120.445, 1957, 68.169},
            {444.546, 468.1, 263.7, 121.950, 1958, 66.513},
            {482.704, 381.3, 255.2, 123.366, 1959, 68.655},
            {502.601, 393.1, 251.4, 125.368, 1960, 69.564},
            {518.173, 480.6, 257.2, 127.852, 1961, 69.331},
            {554.894, 400.7, 282.7, 130.081, 1962, 70.551}
    };

    public static final double[] y = {
            83.0, 88.5, 88.2, 89.5, 96.2, 98.1, 99.0, 100.0, 101.2,
            104.6, 108.4, 110.8, 112.6, 114.2, 115.7, 116.9
    };

    public static DataFrame data = of(x, "GNP", "unemployed", "armed_forces", "population", "year", "employed").merge(DoubleVector.of("deflator", y));

    public static Formula formula = Formula.lhs("deflator");
    static final int ntrees = 10;

    @NotNull
    static RandomForest longleyForest()
    {
        return RandomForest.fit(formula, data, ntrees, 3, 20, 10, 3, 1.0);
    }

    @Test
    public void test_formula1()
    {
        RandomForest model = longleyForest();

        Formula formulaMod = model.formula();
        assertThat(formulaMod).isNotNull();
        double[] preds = model.predict(data.drop("deflator"));
        assertThat(preds.length).isEqualTo(x.length);
    }
}
