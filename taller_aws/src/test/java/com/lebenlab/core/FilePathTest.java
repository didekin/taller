package com.lebenlab.core;

import org.junit.Test;

import static com.lebenlab.core.FilePath.existDir;
import static com.lebenlab.core.FilePath.modelsDir;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * User: pedro@didekin
 * Date: 30/03/2020
 * Time: 16:23
 */
public class FilePathTest {

    @Test
    public void test_ExistModelsDir()
    {
        System.out.println(System.getenv("BOSCH_HOME"));
        assertThatCode(() -> existDir(modelsDir)).doesNotThrowAnyException();
    }
}
