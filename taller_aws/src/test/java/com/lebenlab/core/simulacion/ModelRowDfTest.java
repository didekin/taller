package com.lebenlab.core.simulacion;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 26/03/2020
 * Time: 14:49
 */
public class ModelRowDfTest {

    @Test
    public void test_instanceFieldsCount()
    {
        Assertions.assertThat(ModelRowDf.varCount).isEqualTo(PredictorRowDf.instanceFields.size() + 1);
        List<String> listFields = new ArrayList<>(PredictorRowDf.instanceFields);
        listFields.add("vtaMediaDiariaPg1");
        assertThat(ModelRowDf.instanceFields).containsExactlyInAnyOrder(listFields.toArray(String[]::new));
    }
}