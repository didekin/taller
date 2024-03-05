package com.lebenlab.core.tbmaster;

import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;

import org.jdbi.v3.core.mapper.RowMapper;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import smile.math.Random;

import static com.lebenlab.ProcessArgException.error_conceptos;
import static java.util.EnumSet.allOf;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * User: pedro@didekin.es
 * Date: 12/10/2019
 * Time: 16:15
 */
public enum ConceptoTaller implements Jsonable {

    AD_Talleres(1),
    AllTrucks(2),
    AutoCrew(3),
    Autotaller(4),
    BDC_BDS(5),
    Bosch_Car_Service(6),
    CGA_Car_Service(7),
    Confortauto(8),
    Euro_Repar(9),
    Euromaster(10),
    EuroTaller(11),
    Otros(12),
    Cecauto(13),
    Profesional_Plus(14),
    ;

    public final int conceptoId;

    ConceptoTaller(int conceptoId)
    {
        this.conceptoId = conceptoId;
    }

    public int getCodConceptoId()
    {
        return conceptoId;
    }

    public String getNombre()
    {
        return toString();
    }

    // =========================== Static members ===========================

    public static final EnumSet<ConceptoTaller> allConceptos = allOf(ConceptoTaller.class);

    public static final int maxConceptoId =
            allConceptos.stream().mapToInt(c -> c.conceptoId).max().orElseThrow(() -> new ProcessArgException(error_conceptos));

    public static ConceptoTaller randomInstance(Random rnd)
    {
        return values()[rnd.nextInt(allConceptos.size())];
    }

    public static boolean checkConceptosIn(List<Integer> conceptosIn)
    {
        return allConceptos.stream().map(value -> value.conceptoId).collect(toSet()).containsAll(conceptosIn);
    }

    private static final Map<Integer, ConceptoTaller> intToConcepto =
            allOf(ConceptoTaller.class).stream().collect(toMap(co -> co.conceptoId, co -> co));

    public static ConceptoTaller fromIntToConcepto(int conceptoId)
    {
        return ofNullable(intToConcepto.get(conceptoId)).orElseThrow(() -> new ProcessArgException(error_conceptos + conceptoId));
    }

    // =========================== Static classes ===========================

    public static class ConceptoForJson implements Jsonable {

        public static final RowMapper<ConceptoForJson> mapper =
                (rs, ctx) -> new ConceptoForJson(rs.getInt("concepto_id"), rs.getString("nombre"));

        public final int conceptoId;
        public final String nombre;

        ConceptoForJson(int conceptoId, String nombre)
        {
            this.conceptoId = conceptoId;
            this.nombre = nombre;
        }

        public int getConceptoId()
        {
            return conceptoId;
        }

        public String getNombre()
        {
            return nombre;
        }
    }
}
