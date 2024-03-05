package com.lebenlab.sinch;

import com.sinch.xms.ApiConnection.Builder;

import static com.sinch.xms.ApiConnection.builder;

/**
 * User: pedro@didekin
 * Date: 30/04/2021
 * Time: 15:26
 */
public final class ConfigSinch {

    static final String sinch_api_token = "9d24fe52fa2c4ec2a6ed4e8abfc75232";
    static final String service_plan_ID = "95f6f9da06fa40cc966251ef46316e7b";
    static final String free_test_sinch_number = "+447537454601";
    static final String sinch_url_us = "https://us.sms.api.sinch.com";
    @SuppressWarnings("unused, documentation")
    static final String sinch_url_eu = "https://eu.sms.api.sinch.com";


    static Builder usReportEndPoint()
    {
        return builder().endpoint(sinch_url_us.concat("/xms")).servicePlanId(service_plan_ID).token(sinch_api_token);
    }

    static Builder usSendMsgEndPoint()
    {
        return builder().servicePlanId(service_plan_ID).token(sinch_api_token);
    }
}
