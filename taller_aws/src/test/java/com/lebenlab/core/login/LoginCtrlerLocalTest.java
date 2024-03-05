package com.lebenlab.core.login;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;

import static com.lebenlab.core.util.WebConnTestUtils.initJavalinInTest;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 14:18
 */
public class LoginCtrlerLocalTest {

    @ClassRule
    public static final ExternalResource resource = initJavalinInTest();
}