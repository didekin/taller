package com.lebenlab;

import org.jetbrains.annotations.NotNull;

import static java.lang.String.join;

/**
 * User: pedro@didekin.es
 * Date: 19/11/2019
 * Time: 19:46
 */
public enum HttpConstant {

    auth_request_header("Authorization"),
    www_authenticate_resp_header("WWW-Authenticate"),
    auth_bearer_scheme("Bearer"),
    realm_attribute("realm"),
    bosch_realm_value("userbosch@lebendata1"),
    csv_mimetype("text/csv"),
    csv_charset("charset=utf-8"),
    csv_content_type(join(";",csv_mimetype.toString(),csv_charset.toString())),
    zip_content_type("aplication/zip"),
    octect_content_type("application/octet-stream"),
    ;

    @NotNull
    public static String valueForWwwAuthHeader()
    {
        return auth_bearer_scheme.toString() + " " + realm_attribute.toString() + "=" + bosch_realm_value.toString();
    }

    private final String literal;

    HttpConstant(String constantIn)
    {
        literal = constantIn;
    }

    @Override
    public String toString()
    {
        return literal;
    }
}
