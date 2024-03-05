package com.lebenlab.core.simulacion;


import com.lebenlab.AfterJarTest;

import org.junit.ClassRule;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;

import static com.lebenlab.core.util.WebConnTestUtils.initLocalJar;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 14:19
 */
@Category(AfterJarTest.class)
public class SimulacionCtrlerJarTest extends SimulacionCtrlerLocalTest {
    @ClassRule
    public static final ExternalResource resource = initLocalJar();
}