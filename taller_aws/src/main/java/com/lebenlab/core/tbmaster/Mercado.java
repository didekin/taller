package com.lebenlab.core.tbmaster;

import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;

import org.jdbi.v3.core.mapper.RowMapper;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import smile.math.Random;

import static com.lebenlab.ProcessArgException.error_mercados;
import static java.util.EnumSet.allOf;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * User: pedro@didekin.es
 * Date: 12/10/2019
 * Time: 16:10
 */
public enum Mercado implements Jsonable {
    ES(1, "España"),
    PT(2, "Portugal"),
    AN(3, "Andorra"),
    ;

    public final int id;
    public final String nombre;

    Mercado(int idIn, String nombreIn)
    {
        id = idIn;
        nombre = nombreIn;
    }

    public int getId()
    {
        return id;
    }

    public String getNombre()
    {
        return nombre;
    }

    // =========================== Static members ===========================

    public static final EnumSet<Mercado> allMercados = allOf(Mercado.class);

    public static final int maxId = allMercados.stream().mapToInt(m -> m.id)
            .max().orElseThrow(() -> new ProcessArgException(error_mercados));

    public static final int minId = allMercados.stream().mapToInt(m -> m.id)
            .min().orElseThrow(() -> new ProcessArgException(error_mercados));

    public static boolean checkMercadosIn(List<Integer> mercadosIn)
    {
        return allMercados.stream().map(value -> value.id).collect(toSet()).containsAll(mercadosIn);
    }

    private static final Map<Integer, Mercado> intToMercado =
            allOf(Mercado.class).stream().collect(toMap(m -> m.id, m -> m));

    public static Mercado fromIntToMercado(int mercadoId)
    {
        return ofNullable(intToMercado.get(mercadoId)).orElseThrow(() -> new ProcessArgException(error_mercados + mercadoId));
    }

    public static Mercado randomInstance(Random rnd)
    {
        return values()[rnd.nextInt(allMercados.size())];
    }

    // =========================== Static classes ===========================

    @SuppressWarnings("unused")
    public static class MercadoForJson implements Jsonable {

        public static final RowMapper<MercadoForJson> mapper =
                (rs, ctx) -> new MercadoForJson(rs.getInt("mercado_id"), rs.getString("sigla"), rs.getString("nombre"));

        public final int mercadoId;
        public final String sigla;
        public final String nombre;

        public MercadoForJson(int mercado_id, String sigla, String nombre)
        {
            mercadoId = mercado_id;
            this.sigla = sigla;
            this.nombre = nombre;
        }

        public int getMercadoId()
        {
            return mercadoId;
        }

        public String getSigla()
        {
            return sigla;
        }

        public String getNombre()
        {
            return nombre;
        }
    }
}
