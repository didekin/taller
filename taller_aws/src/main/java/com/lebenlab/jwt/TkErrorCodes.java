package com.lebenlab.jwt;

import org.jetbrains.annotations.NotNull;

import static com.lebenlab.HttpConstant.valueForWwwAuthHeader;

/**
 * User: pedro@didekin.es
 * Date: 21/11/2019
 * Time: 17:28
 * <p>
 * Example:
 * HTTP/1.1 401 Unauthorized
 * WWW-Authenticate: Bearer realm="userbosch@lebendata1",
 * error="invalid_token",
 * error_description="The access token expired"
 * <p>
 * If the request lacks any authentication information, the resource server SHOULD NOT
 * include an error code or other error information. Example:
 * HTTP/1.1 401 Unauthorized
 * WWW-Authenticate: Bearer realm="userbosch@lebendata1"
 */
public enum TkErrorCodes {

    // Estoy empleando este código para los dos casos: invalid_token, insufficient scope.
    invalid_token("Token con insuficientes privilegios o fechas invalidas", 401),
    insufficient_scope("Token con insuficientes privilegios", 403),
    no_token_in_header("No hay token en la cabecera del mensaje", 401){
        @NotNull
        @Override
        public String valueForWwwAuthAttr()
        {
            return valueForWwwAuthHeader();
        }
    },
    ;

    static final String errorStrAttr = "error=";
    static final String errorDecrStrAttr = "error_description=";

    private final String description;
    private final int statusCode;

    TkErrorCodes(String descriptionIn, int httpCodeIn)
    {
        description = descriptionIn;
        statusCode = httpCodeIn;
    }

    public String description()
    {
        return description;
    }
    public int statusCode()
    {
        return statusCode;
    }

    @NotNull
    public String valueForWwwAuthAttr(){
        return valueForWwwAuthHeader() + ","
                + errorStrAttr + "\"" +  this.toString() + "\"" + ","
                + errorDecrStrAttr + "\"" +  this.description()+ "\"" ;
    }
}