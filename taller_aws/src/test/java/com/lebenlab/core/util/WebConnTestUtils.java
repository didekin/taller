package com.lebenlab.core.util;

import com.lebenlab.core.UrlPath;
import com.lebenlab.core.TallerBoschApp;

import org.apache.logging.log4j.Logger;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import io.javalin.Javalin;
import kong.unirest.GetRequest;
import kong.unirest.Unirest;

import static com.lebenlab.core.FilePath.appfilesPath;
import static com.lebenlab.core.UrlPath.jettyHttpPort;
import static com.lebenlab.jwt.JwtTestUtil.doBasicClaims;
import static com.lebenlab.jwt.JwtTestUtil.doHeaderToken;
import static com.lebenlab.jwt.JwtTestUtil.putFechasOk;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getenv;
import static java.nio.file.Paths.get;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.SECONDS;
import static kong.unirest.Unirest.shutDown;
import static org.apache.logging.log4j.LogManager.getLogger;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 18/10/2019
 * Time: 18:14
 */
public final class WebConnTestUtils {

    private static final Logger logger = getLogger(WebConnTestUtils.class);

    public static final Path upLoadDir = appfilesPath.resolve("uploads");
    public static final String mainClassName = "com.lebenlab.core.TallerBoschApp";
    private static final String aRecord = "lebendata1.net";
    private static final String cName = "www.lebendata1.net";
    private static final String local = "localhost";

    private WebConnTestUtils()
    {
    }

    public static String getArecordHttp(UrlPath urlPath)
    {
        final var url = "https://" + aRecord + urlPath.fullPath;
        System.out.println(url);
        return url;
    }

    public static String getArecordHttpStr(String boschUrl)
    {
        final var url = "https://" + aRecord + boschUrl;
        System.out.println(url);
        return url;
    }

    public static String getCnameHttp(UrlPath urlPath)
    {
        final var url = "https://" + cName + urlPath.fullPath;
        System.out.println(url);
        return url;
    }

    public static String getLocalHttpStr(String boschUrlStr)
    {
        return "http://" + local + ":" + jettyHttpPort + boschUrlStr;
    }

    public static String getLocalHttp(UrlPath urlPath)
    {
        return "http://" + local + ":" + jettyHttpPort + urlPath.fullPath;
    }

    public static GetRequest doJsonGet(String httpUrl)
    {
        return Unirest.get(httpUrl).header("accept", "application/json");
    }

    public static GetRequest doHtmlGetWithTk(String httpUrl) throws InvalidJwtException, JoseException
    {
        logger.debug("doHtmlGetWithTk()");
        String[] tokenHeader = doHeaderToken(putFechasOk(doBasicClaims()));
        logger.debug("doHtmlGetWithTk(); after tokenHeader");
        return Unirest.get(httpUrl).header(tokenHeader[0], tokenHeader[1]).header("accept", "text/html");
    }

    public static GetRequest doHtmlGetWithoutTk(String httpUrl)
    {
        logger.debug("doHtmlGetWithoutTk()");
        return Unirest.get(httpUrl).header("accept", "text/html");
    }

    public static ExternalResource initLocalJar()
    {
        return new ExternalResource() {

            private final AtomicReference<Process> shell = new AtomicReference<>();
            private final Path testJarFile = get(getenv("BOSCH_HOME")).resolve("target").resolve(of("bosch_aws-2.0-SNAPSHOT.jar").get());

            @Override
            protected void before() throws IOException, InterruptedException
            {
                assertThat(testJarFile).isNotNull();
                // Arranco el jar en local.
                shell.compareAndSet(null, getRuntime().exec(new String[]{"java", "-cp", testJarFile.toString(), mainClassName}));
                SECONDS.sleep(2);
                assertThat(shell.get().isAlive()).isTrue();
            }

            @Override
            protected void after()
            {
                shell.get().destroy();
                shutDown();
                try {
                    SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                assertThat(shell.get().isAlive()).isFalse();
            }
        };
    }

    public static ExternalResource initJavalinInTest()
    {
        return new ExternalResource() {
            private final Javalin myJavaline = TallerBoschApp.initApp();

            @Override
            protected void before()
            {
                myJavaline.start(jettyHttpPort);
            }

            @Override
            protected void after()
            {
                myJavaline.stop();
                shutDown();
            }
        };
    }
}
