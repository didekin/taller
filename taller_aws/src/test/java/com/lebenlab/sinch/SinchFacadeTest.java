package com.lebenlab.sinch;

import org.junit.Test;

import java.util.Map;

import static com.lebenlab.ProcessArgException.error_sinch_sending_sms;
import static com.lebenlab.sinch.SinchFacade.reportSync;
import static com.lebenlab.sinch.SinchFacade.sendSms;
import static com.lebenlab.sinch.SinchFacade.tfnosDevilered;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * User: pedro@didekin
 * Date: 31/05/2021
 * Time: 14:55
 */
public class SinchFacadeTest {

    @Test
    public void test_report_1()
    {
        final var mapParams = Map.of("34615201910", "pathId_1");
        final var batchIdIn = sendSms("Hola amapola. ", () -> mapParams, "34615201910");
        assertThat(batchIdIn).isNotNull();
        System.out.println("batchId: " + batchIdIn);

        await().atMost(10, SECONDS).until(() -> reportSync(batchIdIn) != null);
        assertThat(reportSync(batchIdIn).batchId().toString()).isEqualTo(batchIdIn);
    }

    @Test
    public void test_report_2()
    {
        final Map<String, String> mapParams = Map.of();
        assertThatThrownBy(() -> sendSms("Hola amapola. ", () -> mapParams)).hasMessageContaining(error_sinch_sending_sms);
    }

    @Test
    public void test_tfnosDevilered()
    {
        /*"BatchDeliveryReport
         {
          batchId=01F8392Q6C3VNM0B54E87T107P,
          totalMessageCount=2,
          statuses=
              [Status
                  {
                   code=0,
                   status=DeliveryStatus{status=Delivered},
                   count=2,
                   recipients= [34615201910,34622261512]
                  }
              ]
        }"*/
        var recipientStatuses = tfnosDevilered("01F8392Q6C3VNM0B54E87T107P");
        assertThat(recipientStatuses).containsExactlyInAnyOrder("34615201910", "34622261512");

        /*
        "BatchDeliveryReport
         {
          batchId=01F839PYKP63K319ZXN2Q90F8X,
          totalMessageCount=2,
          statuses=[
              Status{
                  code=361,
                  status=DeliveryStatus{status=Failed},
                  count=1,
                  recipients=[34123456789]
              },
              Status{
                  code=0,
                  status=DeliveryStatus{status=Delivered},
                  count=1,
                  recipients=[34615201910]
              }
          ]
         }"
        */
        recipientStatuses = tfnosDevilered("01F839PYKP63K319ZXN2Q90F8X");
        assertThat(recipientStatuses).containsExactly("34615201910");
    }
}