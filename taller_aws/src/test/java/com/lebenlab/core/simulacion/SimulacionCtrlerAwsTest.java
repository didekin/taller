package com.lebenlab.core.simulacion;

import com.lebenlab.AwsTest;
import com.lebenlab.core.util.FileTestUtil;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.file.Path;

import kong.unirest.HttpResponse;

import static com.lebenlab.core.UrlPath.simulacionPath;
import static com.lebenlab.core.util.DataTestExperiment.promocion1;
import static com.lebenlab.core.util.WebConnTestUtils.getArecordHttp;
import static com.lebenlab.jwt.JwtTestUtil.doBasicClaims;
import static com.lebenlab.jwt.JwtTestUtil.doHeaderToken;
import static com.lebenlab.jwt.JwtTestUtil.putFechasOk;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.list;
import static java.nio.file.Files.newDirectoryStream;
import static kong.unirest.Unirest.put;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 14:29
 */
@Category(AwsTest.class)
public class SimulacionCtrlerAwsTest {

    @Before
    public void setUp() throws IOException
    {
        assertThat(isDirectory(FileTestUtil.downloadDir) && list(FileTestUtil.downloadDir).count() == 0).isTrue();
    }

    @After
    public void clean() throws IOException
    {
        for (Path file : newDirectoryStream(FileTestUtil.downloadDir)) {
            delete(file);
        }
    }

    @Test  // PUT   https://lebendata1.net/close/simulacion
    public void test_handlePG1Results() throws JoseException, InvalidJwtException
    {

        String[] tokenHeader = doHeaderToken(putFechasOk(doBasicClaims()));
        HttpResponse<String> response = put(getArecordHttp(simulacionPath))
                .header(tokenHeader[0], tokenHeader[1])
                .header("accept", "application/json")
                .body(promocion1)
                .asString();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
