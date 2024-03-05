package com.lebenlab.core;

import com.lebenlab.jwt.TokenException;

import java.util.NoSuchElementException;

import io.javalin.http.Handler;

import static com.lebenlab.HttpConstant.auth_bearer_scheme;
import static com.lebenlab.jwt.TkErrorCodes.no_token_in_header;
import static com.lebenlab.HttpConstant.auth_request_header;
import static com.lebenlab.core.TkConsumer.consumerSingle;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

/**
 * User: pedro@didekin.es
 * Date: 19/11/2019
 * Time: 19:40
 */
class AuthHeaderCtrler {

    static final Handler handleAuthHeader = ctx -> {
        try {
            consumerSingle.checkToken(getTokenFromHeader(ctx.header(auth_request_header.toString())));
        } catch (NoSuchElementException ne) {
            throw new TokenException(no_token_in_header, no_token_in_header.description());
        }
    };

    static String getTokenFromHeader(String authHeaderIn) throws NoSuchElementException
    {
        return ofNullable(authHeaderIn)
                .flatMap(header -> {
                    String[] split = header.trim().split(" ");
                    if (split.length != 2 || !split[0].equals(auth_bearer_scheme.toString())) {
                        return empty();
                    }
                    return of(split[1]);
                }).orElseThrow();
    }
}
