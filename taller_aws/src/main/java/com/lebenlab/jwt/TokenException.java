package com.lebenlab.jwt;

import io.javalin.http.ExceptionHandler;

import static com.lebenlab.HttpConstant.www_authenticate_resp_header;

/**
 * User: pedro@didekin.es
 * Date: 22/11/2019
 * Time: 15:05
 */
public class TokenException extends RuntimeException {

    private final TkErrorCodes errorCode;
    private final String message;

    public TokenException(TkErrorCodes errorCodeIn, String messageIn)
    {
        errorCode = errorCodeIn;
        message = messageIn;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    public static final ExceptionHandler<TokenException> handleTkException = (e, ctx) -> {
        ctx.header(
                www_authenticate_resp_header.toString(),
                e.errorCode.valueForWwwAuthAttr()
        );
        ctx.status(e.errorCode.statusCode());
        // Mensaje en body.
        ctx.result(e.message);
    };
}
