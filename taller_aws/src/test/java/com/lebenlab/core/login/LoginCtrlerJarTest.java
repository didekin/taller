package com.lebenlab.core.login;

import com.lebenlab.AfterJarTest;

import org.junit.ClassRule;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExternalResource;

import static com.lebenlab.core.util.WebConnTestUtils.initLocalJar;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 14:18
 */
@Category(AfterJarTest.class)
public class LoginCtrlerJarTest {
    @ClassRule
    public static final ExternalResource resource = initLocalJar();

}