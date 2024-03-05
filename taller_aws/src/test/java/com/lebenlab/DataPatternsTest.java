package com.lebenlab;

import org.junit.Test;

import static com.lebenlab.DataPatterns.EXPERIMENTO_ID;
import static com.lebenlab.DataPatterns.TFNO_MOVIL;
import static com.lebenlab.DataPatterns.jwt_with_direct_key;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * User: pedro@didekin.es
 * Date: 23/11/2019
 * Time: 15:03
 */
public class DataPatternsTest {

    @Test
    public void test_tkEncrypted_direct_1()
    {
        String realTkStr = "eyJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0." +
                "." +
                "_L86WbOFHY-3g0E2EXejJg." +
                "UB1tHZZq0TYFTZKPVZXY83GRxHz770Aq7BuMCEbNn." +
                "RIvTWRrsyoJ1mpl8vUhQDQ";
        assertThat(jwt_with_direct_key.isPatternOk(realTkStr)).isTrue();
    }

    @Test
    public void test_tkEncrypted_direct_2()
    {
        String realTkStr = "eyJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0." +
                "bOFHY." +
                "_L86WbOFHY-3g0E2EXejJg." +
                "UB1tHZZq0TYFTZKPVZXY83GRxHz770Aq7BuMCEbNn." +
                "RIvTWRrsyoJ1mpl8vUhQDQ";
        assertThat(jwt_with_direct_key.isPatternOk(realTkStr)).isFalse();

        realTkStr = "eyJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0." +
                "_L86WbOFHY-3g0E2EXejJg." +
                "RIvTWRrsyoJ1mpl8vUhQDQ";
        assertThat(jwt_with_direct_key.isPatternOk(realTkStr)).isFalse();
    }

    @Test
    public void test_IsExperimentoIdOk()
    {
        assertThat(EXPERIMENTO_ID.isPatternOk("0")).isFalse();
        assertThat(EXPERIMENTO_ID.isPatternOk("-123")).isFalse();
        assertThat(EXPERIMENTO_ID.isPatternOk("*2")).isFalse();
        assertThat(EXPERIMENTO_ID.isPatternOk("234A")).isFalse();
        assertThat(EXPERIMENTO_ID.isPatternOk("12345678")).isTrue();
    }

    @Test
    public void test_tfno()
    {
        assertThat(TFNO_MOVIL.isPatternOk("34715201910")).isTrue();
        assertThat(TFNO_MOVIL.isPatternOk("34615201911")).isTrue();
        assertThat(TFNO_MOVIL.isPatternOk("35715201910")).isFalse();
        assertThat(TFNO_MOVIL.isPatternOk("+34715201910")).isFalse();
        assertThat(TFNO_MOVIL.isPatternOk("0034715201910")).isFalse();
    }
}