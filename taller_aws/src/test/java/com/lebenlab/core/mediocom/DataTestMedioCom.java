package com.lebenlab.core.mediocom;

import com.lebenlab.core.experimento.CsvParserApertura.HeaderAperturaCsv;

import org.jetbrains.annotations.NotNull;

import smile.math.Random;

import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.NA;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static com.lebenlab.core.mediocom.MedioComunicacion.randomInstance;
import static com.lebenlab.core.mediocom.MedioComunicacion.sms;
import static com.lebenlab.core.mediocom.SqlComunicacion.del_all_textcomunicacion;
import static com.lebenlab.core.mediocom.SqlComunicacion.del_all_word_class_prob;
import static com.lebenlab.core.mediocom.TextClassifier.CsvCommHeader.getHeaderStr;
import static com.lebenlab.core.mediocom.TextClassifier.TextClassEnum.values;
import static com.lebenlab.csv.CsvConstant.newLine;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.time.LocalDate.now;
import static java.util.EnumSet.allOf;

/**
 * User: pedro@didekin.es
 * Date: 18/10/2019
 * Time: 19:36
 */
public final class DataTestMedioCom {

    private DataTestMedioCom()
    {
    }

    public static void cleanMedCommTables()
    {
        final var jdbi = jdbiFactory.getJdbi();
        jdbi.withHandle(handle -> handle.execute(del_all_textcomunicacion.statement));
        jdbi.withHandle(handle -> handle.execute(del_all_word_class_prob.statement));
    }

    public static final int medioOne = 1;
    public static final int medioTwo = 2;
    public static final int medioThree = 3;

    public static final String testStr500 = "abcde".repeat(100);
    public static final String text_rnd_msg = "text_rnd_msg_";

    public static String msgForMedio(MedioComunicacion medioIn)
    {
        if (medioIn == ninguna) return "";
        return (medioIn == sms) ? "SMSMsg_text" : "EmailMsg_text";
    }

    // =================== Scripts BD ==================

    // =================== Ficheros CSV ==================

    @NotNull
    public static String upCsvAperturas()
    {
        return HeaderAperturaCsv.getHeaderStr() + newLine +
                "1111;11;" + now() + newLine +
                "2222;12;" + now() + newLine +
                "1111;13;" + now();
    }

    public static final String upCsvCommunications =
            getHeaderStr() + newLine +
                    testStr500 + ";1" + newLine +
                    testStr500.substring(0, 100) + ";2";

    // =================== Smile ==================

    public static PromoMedioComunica randInstance(Random rnd)
    {
        final var medioId = randomInstance(rnd).id;
        if (medioId == 1) {
            return new PromoMedioComunica.PromoMedComBuilder().medioId(medioId).textMsg(NA.name()).build();
        } else {
            final var randomInd = rnd.nextInt(allOf(TextClassifier.TextClassEnum.class).size() - 1);
            // Add +1 to avoid codTextClass = 0.
            return new PromoMedioComunica.PromoMedComBuilder()
                    .medioId(medioId)
                    .textMsg(text_rnd_msg + randomInd)
                    .msgClassifier(values()[randomInd + 1].codigoNum).build();
        }
    }

    // =============== Tablas maestras ===============

    //@formatter:off
    public static final String medioComunicaTb = "mediocomunicacion";
    public static final String mediosComunicaJson =
            "[" +
                    "{\"medioId\":1,\"nombre\":\"ninguna\"}," +
                    "{\"medioId\":2,\"nombre\":\"sms\"}," +
                    "{\"medioId\":3,\"nombre\":\"email\"}" +
                    "]";
    //@formatter:on

}


