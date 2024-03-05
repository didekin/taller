package com.lebenlab.sinch;

import com.lebenlab.ProcessArgException;
import com.sinch.xms.ApiConnection;
import com.sinch.xms.ApiException;
import com.sinch.xms.api.BatchDeliveryReport;
import com.sinch.xms.api.MtBatchTextSmsResult;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.lebenlab.ProcessArgException.error_sinch_parameter_map;
import static com.lebenlab.ProcessArgException.error_sinch_retrieving_sms_report;
import static com.lebenlab.ProcessArgException.error_sinch_sending_sms;
import static com.lebenlab.sinch.ConfigSinch.free_test_sinch_number;
import static com.lebenlab.sinch.ConfigSinch.usReportEndPoint;
import static com.lebenlab.sinch.ConfigSinch.usSendMsgEndPoint;
import static com.sinch.xms.SinchSMSApi.batchDeliveryReportParams;
import static com.sinch.xms.SinchSMSApi.batchTextSms;
import static com.sinch.xms.SinchSMSApi.parameterValues;
import static com.sinch.xms.api.BatchId.of;
import static com.sinch.xms.api.DeliveryStatus.DELIVERED;
import static java.util.stream.Collectors.toList;
import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * User: pedro@didekin
 * Date: 31/05/2021
 * Time: 14:51
 */
public class SinchFacade {

    private static final Logger logger = getLogger(SinchFacade.class);
    public static final String param_pathId = "path_id";
    static final String param_pathId_template = "${".concat(param_pathId).concat("}");

    static BatchDeliveryReport reportSync(String batchIdStr)
    {
        logger.info("reportSync() id: {}", batchIdStr);

        try (ApiConnection conn = usReportEndPoint().start()) {
            final var batchId = of(batchIdStr);
            return conn.fetchDeliveryReport(
                    batchId,
                    batchDeliveryReportParams().fullReport().build()
            );
        } catch (IOException | InterruptedException | ApiException e) {
            logger.error(e.getMessage());
            throw new ProcessArgException(error_sinch_retrieving_sms_report);
        }
    }

    /**
     * @return a list of distinct telephones which receive the sms sent.
     */
    public static List<String> tfnosDevilered(String batchIdStr)
    {
        logger.info("tfnosDevilered()");
        return reportSync(batchIdStr).statuses().stream()
                .filter(status -> status.status() == DELIVERED)
                .map(BatchDeliveryReport.Status::recipients)
                .flatMap(Collection::stream)
                .distinct()
                .collect(toList());
    }

    public static String sendSms(String message, Supplier<Map<String, String>> paramsSupplier, String... recipients)
    {
        logger.info("sendSms()");

        final var mapParams = paramsSupplier.get();
        if (mapParams.size() != recipients.length) {
            logger.error("sendSms(): mapPathIdValues.size() != recipients.length ");
            throw new ProcessArgException(error_sinch_parameter_map);
        }

        try (ApiConnection conn = usSendMsgEndPoint().start()) {
            MtBatchTextSmsResult batch =
                    conn.createBatch(
                            batchTextSms()
                                    .sender(free_test_sinch_number)
                                    .addRecipient(recipients)
                                    .body(message.concat(param_pathId_template))
                                    .putParameter(
                                            param_pathId,
                                            parameterValues()
                                                    .substitutions(mapParams)
                                                    .build()
                                    )
                                    .build()
                    );
            return batch.id().toString();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new ProcessArgException(error_sinch_sending_sms + ": " + e.getMessage());
        }
    }
}
