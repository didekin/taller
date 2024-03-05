package com.lebenlab.core.login;

import com.lebenlab.AwsTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.lebenlab.core.UrlPath.login;
import static com.lebenlab.core.util.WebConnTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 15:02
 */
@Category(AwsTest.class)
public class LoginCtrlerAwsTest {

    // ================== Welcome ======================

    @Test  // GET  https://lebendata1.net/
    public void test_welcomeAws_1() {
        assertThat(doJsonGet(getArecordHttp(login)).asString().getStatus()).isEqualTo(200);
    }

    @Test  // GET  https://www.lebendata1.net/
    public void test_welcomeAws_2() {
        assertThat(doJsonGet(getCnameHttp(login)).asString().getStatus()).isEqualTo(200);
    }
}
