package com.lebenlab.core;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;

/**
 * User: pedro@didekin.es
 * Date: 18/10/2019
 * Time: 18:46
 */
public enum UrlPath {

    root("", null),
    login("/", root),
    closePath("/close", root),
    // ====== Experimento ======
    experimentoPath("/close/experimento", root),
    exp_list_path("list", experimentoPath),
    exp_statistics_path(":experimento_id", experimentoPath) {
        @Override
        public String actualPath(Object... addPathElement)
        {
            return urlNoPathParam() + addPathElement[0];
        }
    },
    exp_pg1_clusters_path("clusters/" + ":experimento_id", experimentoPath){
        @Override
        public String actualPath(Object... addPathElement)
        {
            return urlNoPathParam() + addPathElement[0] + "?" + pg1_id_param + "=" + addPathElement[1];
        }
    },
    // ====== Simulación ======
    simulacionPath("/close/simulacion", root),
    sim_pg1_cluster_path(":promo_id", simulacionPath) {
        @Override
        public String actualPath(Object... addPathElement)
        {
            return urlNoPathParam() + addPathElement[0] + "?" + pg1_id_param + "=" + addPathElement[1];
        }
    },
    // ====== Ficheros ======
    ficherosPath("/close/ficheros", root),
    aperturas_file("aperturas", ficherosPath),
    communications_file("comunicaciones",ficherosPath),
    ventas_file("ventas", ficherosPath),
    // ======= SMS ==========
    smsPath("/open/sms", root),
    landingPgPath(":substitution_id", smsPath){
        @Override
        public String actualPath(Object... addPathElement)
        {
            return urlNoPathParam() + addPathElement[0];
        }
    },
    ;



    public static final String pg1_id_param = "pg1_Id";

    // ............ Instance members .............

    public final String path;
    public final UrlPath parentPath;
    public final String fullPath;

    UrlPath(String pathIn, UrlPath parentPathIn)
    {
        path = pathIn;
        parentPath = parentPathIn;
        fullPath = (parentPath != null && !parentPath.path.equals("")) ? parentPath.path + "/" + path : path;
    }

    public String actualPath(Object... addPathElement)
    {
        return fullPath;
    }

    @NotNull
    public String pathParamValue(Context ctx)
    {
        return ctx.pathParam(pathParamName());
    }

    public String urlNoPathParam(){
        return fullPath.split(":")[0];
    }

    /**
     *  Mainly for http clients in tests.
     */
    public String pathParamName()
    {
        return path.split(":")[1];
    }

    /**
     *  Mainly for http clients in tests.
     */
    public String urlBracesPathParam()
    {
        return urlNoPathParam() + "{" + pathParamName() + "}";
    }

    // ............ Static members .............

    public static final int jettyHttpPort = 8080;
}
