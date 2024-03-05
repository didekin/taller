package com.lebenlab.core.simulacion;

import org.junit.Test;

import static com.lebenlab.core.FilePath.modelsDir;
import static com.lebenlab.core.simulacion.ModelFilePath.rndName;
import static com.lebenlab.core.tbmaster.PG1.PG1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * User: pedro@didekin
 * Date: 30/03/2020
 * Time: 18:13
 */
public class ModelFilePathTest {

    @Test
    public void test_FilePath()
    {
        assertThat(catchThrowable(() -> new ModelFilePath(PG1_1))).doesNotThrowAnyException();
        assertThat(new ModelFilePath(PG1_1).path().toString()).containsPattern(modelsDir + "/" + rndName + PG1_1.name());
    }
}