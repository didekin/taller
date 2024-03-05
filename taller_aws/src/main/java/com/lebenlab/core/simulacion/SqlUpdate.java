package com.lebenlab.core.simulacion;

/**
 * User: pedro@didekin
 * Date: 21/03/2020
 * Time: 18:07
 */
public enum SqlUpdate {

    // @formatter:off
    delete_all_promo_simulacion("DELETE FROM promo_simulacion"),
    insert_promo_simulation("INSERT INTO promo_simulacion(promo_json) VALUES(:promo_json)"),
    // @formatter:on
    insert_result_for_model_test("INSERT INTO result_for_model_test (avg_day_qty, mercado_id, concepto_id, dias_registro, activ_prev," +
            "                                   avg_prev, duracion_promo, quarter_promo, incentivo_id, pg1_id, pg1_id_with_1," +
            "                                   pg1_id_with_2) " +
            " VALUES (:avg_day_qty," +
            "        :mercado_id," +
            "        :concepto_id," +
            "        :dias_registro," +
            "        :activ_prev," +
            "        :avg_prev," +
            "        :duracion_promo," +
            "        :quarter_promo," +
            "        :incentivo_id," +
            "        :pg1_id," +
            "        :pg1_id_with_1," +
            "        :pg1_id_with_2);"),;

    public final String statement;

    SqlUpdate(String statementIn)
    {
        statement = statementIn;
    }
}
