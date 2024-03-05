package com.lebenlab.core.experimento;

import com.lebenlab.core.Promocion;
import com.lebenlab.core.mediocom.PromoMedioComunica.PromoMedComBuilder;

import org.junit.After;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import static com.lebenlab.core.experimento.CsvParserParticipantes.HeaderParticipantes.getHeaderStr;
import static com.lebenlab.core.experimento.ExpNewFlow.newExperimentFlow;
import static com.lebenlab.core.experimento.ExperimentoDao.promoParticipantesPg1;
import static com.lebenlab.core.mediocom.DataTestMedioCom.medioOne;
import static com.lebenlab.core.mediocom.MedioComunicacion.MedioLabels.NA;
import static com.lebenlab.core.tbmaster.Incentivo.evento_deportivo;
import static com.lebenlab.core.tbmaster.Incentivo.evento_musical;
import static com.lebenlab.core.util.DataTestExperiment.alter_seq_particip;
import static com.lebenlab.core.util.DataTestExperiment.alter_seq_promo;
import static com.lebenlab.core.util.DataTestExperiment.cleanExpTables;
import static com.lebenlab.core.util.DataTestExperiment.empty_inzip_file;
import static com.lebenlab.csv.CsvConstant.newLine;
import static com.lebenlab.csv.CsvConstant.point_csv;
import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDate.parse;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

/**
 * User: pedro@didekin
 * Date: 22/05/2021
 * Time: 12:46
 */
public class ExpNewFlowTest_B {

    @After
    public void clean()
    {
        cleanExpTables();
    }

    // ===============================  Main functions =============================

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    public void test_newExperimentFlow() throws IOException
    {
        InputStream participStream = new ByteArrayInputStream((getHeaderStr() + newLine + "B12345X;12;7;;;2004-12-31").getBytes(UTF_8));
        final var promo1 = new Promocion.PromoBuilder()
                .codPromo("promo11")
                .fechaInicio(parse("2020-01-05"))
                .fechaFin(parse("2020-01-06"))
                .experimentoId(2)
                .conceptos(singletonList(7))
                .mercados(singletonList(1))
                .pg1s(singletonList(17))
                .incentivo(evento_deportivo.incentivoId)
                .medio(new PromoMedComBuilder().medioId(medioOne).textMsg(NA.name()).build())
                .build();
        final var promo2 = new PromoVariante.VarianteBuilder("variante_exp", promo1.pg1s, evento_musical.incentivoId, promo1.promoMedioComunica).build();
        final var experimento = new Experimento.ExperimentoBuilder().promocion(promo1).variante(promo2).nombre("experimento_A").build();

        jdbiFactory.getJdbi().useHandle(h -> {
            h.execute(alter_seq_particip);
            h.execute(alter_seq_promo);
        });

        // Run
        final var zipStream = new ZipInputStream(newExperimentFlow.apply(of(participStream), experimento).get(), UTF_8);
        // Check
        await().until(() -> (promoParticipantesPg1(1L).size() == 1) || (promoParticipantesPg1(2L).size() == 1));

        assertThat(requireNonNull(zipStream.getNextEntry()).getName()).isEqualTo("1" + point_csv);
        assertThat(new String(zipStream.readAllBytes(), UTF_8)).isEqualTo(empty_inzip_file);
        assertThat(requireNonNull(zipStream.getNextEntry()).getName()).isEqualTo("2" + point_csv);
        assertThat(new String(zipStream.readAllBytes(), UTF_8)).isEqualTo(
                "participante_id;id_fiscal;mercado_id;provincia_id;concepto_id" + newLine + "1;B12345X;1;12;7");

        assertThat(promoParticipantesPg1(2L))
                .extracting("promoId", "participanteId", "pg1Id", "vtaMediaDiariaPg1Exp", "vtaMediaDiariaPg1")
                .containsExactlyInAnyOrder(tuple(2L, 1L, 17, 0d, 0d));

    }

    // ======================================= Utilities =======================================
}