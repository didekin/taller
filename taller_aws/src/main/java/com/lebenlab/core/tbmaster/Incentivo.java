package com.lebenlab.core.tbmaster;


import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;

import org.jdbi.v3.core.mapper.RowMapper;

import java.util.Map;

import smile.math.Random;

import static com.lebenlab.ProcessArgException.error_incentivo;
import static java.util.Arrays.stream;
import static java.util.EnumSet.allOf;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * User: pedro@didekin.es
 * Date: 12/10/2019
 * Time: 16:48
 */
public enum Incentivo implements Jsonable {

    dto_dinero(1, "Descuento en metálico"),
    tarjeta_regalo(2, "Tarjetas regalo"),
    gadgets_electr(3, "Dispositivos electrónicos"),
    bici_patinete_electr(4, "Bicicletas o patinetes eléctricos"),
    viaje(5, "Viajes"),
    evento_deportivo(6, "Entradas eventos deportivos"),
    evento_musical(7, "Entradas eventos musicales"),
    otras_entradas(8, "Otros tipos de entradas"),
    fidelizacion_ext(9, "Puntos de programas de fidelización externos"),
    otros(10, "Otro tipo de incentivos"),
    ;

    public final int incentivoId;
    public final String nombre;

    Incentivo(int incentivoId, String nombre)
    {
        this.incentivoId = incentivoId;
        this.nombre = nombre;
    }

    // =========================== Static members ===========================

    public static final int maxIncentivoId =
            stream(Incentivo.values()).mapToInt(i -> i.incentivoId).max().orElseThrow(() -> new ProcessArgException(error_incentivo));

    private static final Map<Integer, Incentivo> intToIncentivo =
            allOf(Incentivo.class).stream().collect(toMap(incentivo -> incentivo.incentivoId, incentivo -> incentivo));

    public static final RowMapper<IncentivoJson> mapper =
            (rs, ctx) -> new IncentivoJson(fromIntToIncentivo(rs.getInt("incentivo_id")));

    public static Incentivo fromIntToIncentivo(int incentivoId)
    {
        return ofNullable(intToIncentivo.get(incentivoId)).orElseThrow(() -> new ProcessArgException(error_incentivo + incentivoId));
    }

    public static Incentivo randomInstance(Random rnd)
    {
        return values()[rnd.nextInt(allOf(Incentivo.class).size())];
    }

    // =========================== Static classes ===========================

    public enum IncentivoId {
        _1(1),
        _2(2),
        _3(3),
        _4(4),
        _5(5),
        _6(6),
        _7(7),
        _8(8),
        _9(9),
        _10(10),
        ;

        public final int id;

        IncentivoId(int id)
        {
            this.id = id;
        }

        private static final Map<Integer, IncentivoId> intToIncentivoId =
                allOf(IncentivoId.class).stream().collect(toMap(m -> m.id, m -> m));

        public static IncentivoId fromIntToIncentivoId(int incentivoId)
        {
            return ofNullable(intToIncentivoId.get(incentivoId)).orElseThrow(() -> new ProcessArgException(error_incentivo + incentivoId));
        }
    }

    public static class IncentivoJson implements Jsonable {

        public final int incentivoId;
        public final String nombre;

        public IncentivoJson(Incentivo incentivo)
        {
            incentivoId = incentivo.incentivoId;
            nombre = incentivo.nombre;
        }

        public int getIncentivoId()
        {
            return incentivoId;
        }

        public String getNombre()
        {
            return nombre;
        }
    }
}
