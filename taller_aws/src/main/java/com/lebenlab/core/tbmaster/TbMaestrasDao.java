package com.lebenlab.core.tbmaster;

import com.lebenlab.core.mediocom.MedioComunicacion;
import com.lebenlab.core.tbmaster.ConceptoTaller.ConceptoForJson;
import com.lebenlab.core.tbmaster.Incentivo.IncentivoJson;
import com.lebenlab.core.tbmaster.Mercado.MercadoForJson;
import com.lebenlab.core.experimento.SqlQuery;

import java.util.List;

import static com.lebenlab.jdbi.JdbiFactory.jdbiFactory;

/**
 * User: pedro@didekin.es
 * Date: 11/02/2020
 * Time: 14:22
 */
public class TbMaestrasDao {

    public static final TbMaestrasDao maestrasDao = new TbMaestrasDao();

    public List<ConceptoForJson> getConceptos()
    {
        return jdbiFactory.getJdbi().withHandle(handle -> handle.createQuery(SqlQuery.conceptos.query).map(ConceptoForJson.mapper).list());
    }

    public List<IncentivoJson> getIncentivos()
    {
        return jdbiFactory.getJdbi().withHandle(handle -> handle.createQuery(SqlQuery.incentivos.query).map(Incentivo.mapper).list());
    }

    public List<MedioComunicacion.MedioComunicaJson> getMediosComunicacion()
    {
        return jdbiFactory.getJdbi().withHandle(h -> h.createQuery(SqlQuery.medioscomunicacion.query).map(MedioComunicacion.mapper).list());
    }

    public List<MercadoForJson> getMercados()
    {
        return jdbiFactory.getJdbi().withHandle(handle -> handle.createQuery(SqlQuery.mercados.query).map(MercadoForJson.mapper).list());
    }

    public List<PG1.Pg1ForJson> getPg1s()
    {
        return jdbiFactory.getJdbi().withHandle(handle -> handle.createQuery(SqlQuery.pg1s.query).map(PG1.Pg1ForJson.mapper).list());
    }

    public List<PG1.Pg1ForJson> pg1sWithPg1_0()
    {
        List<PG1.Pg1ForJson> widePg1s = getPg1s();
        // Añado pg1.0, que no está en BD.
        widePg1s.add(new PG1.Pg1ForJson(PG1.PG1_0.idPg1, PG1.PG1_0.toString()));
        return widePg1s;
    }

    public List<Provincia> getProvincias()
    {
        return jdbiFactory.getJdbi().withHandle(h -> h.createQuery(SqlQuery.provincias.query).map(Provincia.mapper).list());
    }
}
