package com.lebenlab.core.simulacion;

import com.lebenlab.core.Promocion;

import org.apache.logging.log4j.Logger;

import io.javalin.http.Handler;

import static com.lebenlab.core.UrlPath.pg1_id_param;
import static com.lebenlab.core.UrlPath.sim_pg1_cluster_path;
import static com.lebenlab.core.ViewPath.sim_pg1_clusters;
import static com.lebenlab.core.ViewPath.simulacion_form;
import static com.lebenlab.core.ViewPath.simulacion_result;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Objects.requireNonNull;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 10/05/2020
 * Time: 14:19
 */
public class SimulacionCtrler {

    private static final Logger logger = getLogger(SimulacionCtrler.class);

    // ......... Formulario simulación .........

    public static final Handler serveFormPg1Results = ctx -> {
        logger.info("serveFormPg1Results: {}", ctx.fullUrl());
        ctx.render(simulacion_form.path_vm, simulacion_form.getModelView());
    };

    public static final Handler handleAllPg1Results = ctx -> {
        logger.info("handleAllPg1Results: {}", ctx.fullUrl());
        var promoIn = new Promocion.PromoBuilder(ctx.formParamMap()).build();
        ctx.render(simulacion_result.path_vm, simulacion_result.getModelView(promoIn));
        // TODO: pendiente mensajes cuando no hay datos para simulación.
    };

    public static final Handler handleEstimatesPg1Clusters = ctx -> {
        logger.info("handleResultsPg1Clusters(): {}", ctx.fullUrl());
        var promoId = parseLong(sim_pg1_cluster_path.pathParamValue(ctx));
        var pg1Id = parseInt(requireNonNull(ctx.queryParam(pg1_id_param)));
        ctx.render(sim_pg1_clusters.path_vm, sim_pg1_clusters.getModelView(promoId, pg1Id));
    };
}
