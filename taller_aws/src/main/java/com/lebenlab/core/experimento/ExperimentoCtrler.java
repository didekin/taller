package com.lebenlab.core.experimento;

import com.lebenlab.ProcessArgException;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Optional;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;

import static com.lebenlab.HttpConstant.zip_content_type;
import static com.lebenlab.ProcessArgException.upload_file_error_msg;
import static com.lebenlab.core.UrlPath.exp_pg1_clusters_path;
import static com.lebenlab.core.UrlPath.exp_statistics_path;
import static com.lebenlab.core.UrlPath.landingPgPath;
import static com.lebenlab.core.UrlPath.pg1_id_param;
import static com.lebenlab.core.ViewPath.aperturas_file_form;
import static com.lebenlab.core.ViewPath.aperturas_file_result;
import static com.lebenlab.core.ViewPath.communications_file_form;
import static com.lebenlab.core.ViewPath.communications_file_result;
import static com.lebenlab.core.ViewPath.exp_pg1_clusters;
import static com.lebenlab.core.ViewPath.experimento_form;
import static com.lebenlab.core.ViewPath.experimento_list;
import static com.lebenlab.core.ViewPath.experimento_statistics;
import static com.lebenlab.core.ViewPath.landing_pg_sms;
import static com.lebenlab.core.ViewPath.ventas_file_form;
import static com.lebenlab.core.ViewPath.ventas_file_result;
import static com.lebenlab.core.experimento.ExpNewFlow.newExperimentFlow;
import static com.lebenlab.core.experimento.ParticipanteDao.countParticipantes;
import static com.lebenlab.csv.CsvConstant.multipart_form_file_param;
import static com.lebenlab.csv.CsvConstant.point_csv;
import static com.lebenlab.csv.CsvConstant.point_zip;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin.es
 * Date: 18/10/2019
 * Time: 11:31
 */
public class ExperimentoCtrler {

    private static final Logger logger = getLogger(ExperimentoCtrler.class);

    // ......... Formulario nuevo experimento  .........

    public static final Handler serveNewExperiment = ctx -> {
        logger.info("serveNewExperiment");
        ctx.render(experimento_form.path_vm, experimento_form.getModelView());
    };

    public static final Handler newExperiment = ctx -> {

        logger.info("newExperimentOffline handler");

        var experimentoIn = new Experimento.ExperimentoBuilder(ctx.formParamMap()).build();
        UploadedFile uploadedFile = ctx.uploadedFile(multipart_form_file_param.toString());
        boolean isUploadedOk = uploadedFile != null &&
                uploadedFile.getExtension().equalsIgnoreCase(point_csv.toString()) &&
                uploadedFile.getSize() > 0;

        if (!isUploadedOk && countParticipantes() == 0) {
            logger.error("newExperimentOffline handler; exception thrown. Uploaded file is null or not .csv");
            throw new ProcessArgException(upload_file_error_msg);
        }

        final Optional<? extends InputStream> zipFileOptional;
        if (isUploadedOk) {
            zipFileOptional = newExperimentFlow.apply(of(uploadedFile.getContent()), experimentoIn);
        } else {
            zipFileOptional = newExperimentFlow.apply(empty(), experimentoIn);
        }

        if (zipFileOptional.isPresent()){
            ctx.contentType(zip_content_type.toString());
            ctx.header("Content-Disposition", "attachment; filename=\"" + experimentoIn.nombre + point_zip + "\"");
            ctx.result(zipFileOptional.get());
        } else {
            ctx.render(experimento_list.path_vm, experimento_list.getModelView());
        }
    };

    // ......... Resultados de experimentos .........

    public static final Handler handleExperimentList = ctx -> {
        logger.info("handleExperimentList");
        ctx.render(experimento_list.path_vm, experimento_list.getModelView());
    };

    public static final Handler handleStatisticsExperiment = ctx -> {
        logger.info("handleStatisticsExperiment");
        ctx.render(experimento_statistics.path_vm, experimento_statistics.getModelView(exp_statistics_path.pathParamValue(ctx)));
    };

    public static final Handler handleExpPg1Clusters = ctx -> {
        logger.info("handleExpPg1Clusters {}", ctx.fullUrl());
        var experimentoId = parseLong(exp_pg1_clusters_path.pathParamValue(ctx));
        var pg1Id = parseInt(requireNonNull(ctx.queryParam(pg1_id_param)));
        ctx.render(exp_pg1_clusters.path_vm, exp_pg1_clusters.getModelView(pg1Id, experimentoId));
    };

    // ......... Carga del fichero de aperturas .........

    public static final Handler serveFormFileAperturas = ctx -> {
        logger.info("serveFormFileAperturas");
        ctx.render(aperturas_file_form.path_vm, aperturas_file_form.getModelView());
    };

    public static final Handler handleFileAperturas = ctx -> {
        logger.info("updateWithFileAperturas");
        ctx.render(aperturas_file_result.path_vm, aperturas_file_result.getModelView(getUploadedFile(ctx).getContent()));
    };

    // ......... Carga del fichero de comunicaciones .........

    public static final Handler serveFormFileCommunications = ctx -> {
        logger.info("serveFormFileCommunications");
        ctx.render(communications_file_form.path_vm, communications_file_form.getModelView());
    };

    public static final Handler handleFileCommunications = ctx -> {
        logger.info("handleFileCommunications");
        ctx.render(communications_file_result.path_vm, communications_file_result.getModelView(getUploadedFile(ctx).getContent()));
    };

    // ......... Carga del fichero de ventas .........

    public static final Handler serveFormFileVentas = ctx -> {
        logger.info("serveFormFileVentas");
        ctx.render(ventas_file_form.path_vm, ventas_file_form.getModelView());
    };

    public static final Handler handleFileVentas = ctx -> {
        logger.info("handleFileVentas");
        ctx.render(ventas_file_result.path_vm, ventas_file_result.getModelView(getUploadedFile(ctx).getContent()));
    };

    // ......... Apertura SMS .........

    public static final Handler serveLandingPgSms = ctx -> {
        logger.info("serveLandingPgSms");
        ctx.render(landing_pg_sms.path_vm, landing_pg_sms.getModelView(landingPgPath.pathParamValue(ctx)));
    };

    // ========================= Utilities ========================

    @NotNull
    private static UploadedFile getUploadedFile(Context ctx)
    {
        UploadedFile uploadedFile = ctx.uploadedFile(multipart_form_file_param.toString());
        if (uploadedFile == null
                || uploadedFile.getSize() == 0
                || !uploadedFile.getExtension().equalsIgnoreCase(point_csv.toString())) {
            logger.error("getUploadedFile: exception thrown");
            throw new ProcessArgException(upload_file_error_msg);
        }
        return uploadedFile;
    }
}


