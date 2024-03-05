package com.lebenlab.core.tbmaster;

import com.lebenlab.jdbi.JdbiFactory;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 24/01/2020
 * Time: 12:53
 */
public class TbMasterTest {

    @Test
    public void test_conceptoTable()
    {
        List<String> nombres = JdbiFactory.jdbiFactory.getJdbi().withHandle(
                handle -> handle.createQuery("select nombre from concepto").mapTo(String.class).list()
        );
        assertThat(nombres).containsExactlyInAnyOrder((stream(ConceptoTaller.values()).map(Enum::name).toArray(String[]::new)));
    }

    @Test
    public void test_MercadoTable()
    {
        List<String> siglas = JdbiFactory.jdbiFactory.getJdbi().withHandle(
                handle -> handle.createQuery("select sigla from mercado").mapTo(String.class).list()
        );
        assertThat(siglas).containsExactlyInAnyOrder(stream(Mercado.values()).map(Enum::name).toArray(String[]::new));
    }

    @Test
    public void test_provinciaTable()
    {
        int numProvincias = JdbiFactory.jdbiFactory.getJdbi().withHandle(
                handle -> handle.createQuery("select count(*) from provincia").mapTo(Integer.class).one()
        );
        assertThat(numProvincias).isEqualTo(73);
    }

    @Test
    public void test_participanteTable()
    {
        int numParticipantes = JdbiFactory.jdbiFactory.getJdbi().withHandle(
                handle -> handle.createQuery("select count(participante_id) from participante").mapTo(Integer.class).one()
        );
        assertThat(numParticipantes).isEqualTo(0);
    }
}
