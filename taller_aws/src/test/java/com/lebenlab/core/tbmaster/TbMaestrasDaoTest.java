package com.lebenlab.core.tbmaster;

import com.lebenlab.core.tbmaster.PG1.Pg1ForJson;

import org.junit.Test;

import java.util.List;

import static com.lebenlab.core.mediocom.MedioComunicacion.email;
import static com.lebenlab.core.mediocom.MedioComunicacion.ninguna;
import static com.lebenlab.core.mediocom.MedioComunicacion.sms;
import static com.lebenlab.core.tbmaster.TbMaestrasDao.maestrasDao;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * User: pedro@didekin.es
 * Date: 11/02/2020
 * Time: 16:35
 */
public class TbMaestrasDaoTest {

    @Test
    public void test_GetConceptos()
    {
        assertThat(maestrasDao.getConceptos().stream().mapToInt(value -> value.conceptoId).toArray())
                .containsExactlyInAnyOrder(stream(ConceptoTaller.values()).mapToInt(c -> c.conceptoId).toArray());
    }

    @Test
    public void test_getIncentivo()
    {
        assertThat(maestrasDao.getIncentivos().stream().mapToInt(incentivo -> incentivo.incentivoId).toArray())
                .containsExactlyInAnyOrder(stream(Incentivo.values()).mapToInt(i -> i.incentivoId).toArray());
    }

    @Test
    public void test_MediosComunicacion()
    {
        assertThat(maestrasDao.getMediosComunicacion().stream().mapToInt(value -> value.medioId).toArray())
                .containsExactlyInAnyOrder(1,2,3);
        assertThat(maestrasDao.getMediosComunicacion().stream().map(value -> value.nombre).toArray())
                .containsExactlyInAnyOrder(ninguna.name(), sms.name(), email.name());
    }

    @Test
    public void test_GetMercados()
    {
        assertThat(maestrasDao.getMercados().stream().mapToInt(value -> value.mercadoId).toArray())
                .containsExactlyInAnyOrder(stream(Mercado.values()).mapToInt(m -> m.id).toArray());
    }

    @Test
    public void test_GetPg1s()
    {
        List<Pg1ForJson> widePg1s = maestrasDao.pg1sWithPg1_0();
        assertThat(widePg1s.stream().mapToInt(value -> value.idPg1).toArray())
                .containsExactlyInAnyOrder(stream(PG1.values()).mapToInt(pg1 -> pg1.idPg1).toArray());
    }

    @Test
    public void test_GetProvincias()
    {
        assertThat(maestrasDao.getProvincias()).hasSize(73);
    }
}