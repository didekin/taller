package com.lebenlab.core.login;

import io.javalin.http.Handler;

import java.util.HashMap;

import static com.lebenlab.core.ViewPath.experimento_form;
import static com.lebenlab.core.ViewPath.login;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 14:15
 */
public class LoginCtrler {

    // GET
    public static final Handler serveLoginPage = ctx -> ctx.render(login.path_vm, new HashMap<>(0));

    // POST
    public static final Handler handleLogin = ctx -> ctx.render(experimento_form.path_vm, new HashMap<>(0)); // TODO: implementar.
}
