package com.lebenlab.core;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lebenlab.ProcessArgException;
import com.lebenlab.jwt.TokenException;

import io.javalin.Javalin;

import static com.lebenlab.ProcessArgException.handleProcessException;
import static com.lebenlab.core.UrlPath.aperturas_file;
import static com.lebenlab.core.UrlPath.communications_file;
import static com.lebenlab.core.UrlPath.exp_list_path;
import static com.lebenlab.core.UrlPath.exp_pg1_clusters_path;
import static com.lebenlab.core.UrlPath.exp_statistics_path;
import static com.lebenlab.core.UrlPath.experimentoPath;
import static com.lebenlab.core.UrlPath.ficherosPath;
import static com.lebenlab.core.UrlPath.jettyHttpPort;
import static com.lebenlab.core.UrlPath.landingPgPath;
import static com.lebenlab.core.UrlPath.login;
import static com.lebenlab.core.UrlPath.sim_pg1_cluster_path;
import static com.lebenlab.core.UrlPath.simulacionPath;
import static com.lebenlab.core.UrlPath.smsPath;
import static com.lebenlab.core.UrlPath.ventas_file;
import static com.lebenlab.core.experimento.ExperimentoCtrler.handleExpPg1Clusters;
import static com.lebenlab.core.experimento.ExperimentoCtrler.handleExperimentList;
import static com.lebenlab.core.experimento.ExperimentoCtrler.handleFileAperturas;
import static com.lebenlab.core.experimento.ExperimentoCtrler.handleFileCommunications;
import static com.lebenlab.core.experimento.ExperimentoCtrler.handleFileVentas;
import static com.lebenlab.core.experimento.ExperimentoCtrler.handleStatisticsExperiment;
import static com.lebenlab.core.experimento.ExperimentoCtrler.newExperiment;
import static com.lebenlab.core.experimento.ExperimentoCtrler.serveFormFileAperturas;
import static com.lebenlab.core.experimento.ExperimentoCtrler.serveFormFileCommunications;
import static com.lebenlab.core.experimento.ExperimentoCtrler.serveFormFileVentas;
import static com.lebenlab.core.experimento.ExperimentoCtrler.serveLandingPgSms;
import static com.lebenlab.core.experimento.ExperimentoCtrler.serveNewExperiment;
import static com.lebenlab.core.login.LoginCtrler.handleLogin;
import static com.lebenlab.core.simulacion.SimulacionCtrler.handleAllPg1Results;
import static com.lebenlab.core.simulacion.SimulacionCtrler.handleEstimatesPg1Clusters;
import static com.lebenlab.core.simulacion.SimulacionCtrler.serveFormPg1Results;
import static com.lebenlab.jwt.TokenException.handleTkException;
import static io.javalin.Javalin.create;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.plugin.json.JavalinJson.setFromJsonMapper;
import static io.javalin.plugin.json.JavalinJson.setToJsonMapper;


/**
 * User: pedro@didekin.es
 * Date: 11/10/2019
 * Time: 13:16
 */
public final class TallerBoschApp {

    public static void setGson()
    {
        Gson gson = new GsonBuilder().create();
        setFromJsonMapper(gson::fromJson);
        //noinspection NullableProblems
        setToJsonMapper(gson::toJson);
    }

    public static Javalin initApp()
    {
        setGson();
        return create
                (config -> {
                            config.enableDevLogging()
                                    .enableWebjars()
                                    .addStaticFiles("/css")
                                    .addStaticFiles("/imagen");
                            // config.dynamicGzip = false;  // For tests.
                        }
                )
                .get(login.path, serveNewExperiment)  // TODO: pendiente 'serveLoginPage'
                .post(login.path, handleLogin)
//                .before(closePath.path + "/*", handleAuthHeader)  // TODO: pendiente login
                .routes(
                        () -> path(simulacionPath.path, () ->
                                {
                                    get(serveFormPg1Results);
                                    post(handleAllPg1Results);
                                    path(sim_pg1_cluster_path.path, () -> get(handleEstimatesPg1Clusters));
                                }
                        )
                )
                .routes(
                        () -> path(experimentoPath.path, () ->
                                {
                                    get(serveNewExperiment);
                                    post(newExperiment);
                                    path(exp_list_path.path, () -> get(handleExperimentList));
                                    path(exp_statistics_path.path, () -> get(handleStatisticsExperiment));
                                    path(exp_pg1_clusters_path.path, () -> get(handleExpPg1Clusters));
                                }
                        )
                )
                .routes(
                        () -> path(ficherosPath.path, () ->
                                {
                                    path(ventas_file.path, () -> {
                                                get(serveFormFileVentas);
                                                post(handleFileVentas);
                                            }
                                    );
                                    path(aperturas_file.path, () -> {
                                                get(serveFormFileAperturas);
                                                post(handleFileAperturas);
                                            }
                                    );
                                    path(communications_file.path, () -> {
                                                get(serveFormFileCommunications);
                                                post(handleFileCommunications);
                                            }
                                    );
                                }
                        )
                )
                .routes(() -> path(smsPath.path, () ->
                                path(landingPgPath.path, () -> get(serveLandingPgSms))
                        )
                )
                .exception(TokenException.class, handleTkException)
                .exception(ProcessArgException.class, handleProcessException);
    }

    public static void main(String[] args)
    {
        initApp().start(jettyHttpPort);
    }
}
