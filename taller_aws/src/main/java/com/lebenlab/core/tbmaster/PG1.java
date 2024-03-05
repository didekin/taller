package com.lebenlab.core.tbmaster;

import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;

import org.jdbi.v3.core.mapper.RowMapper;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import smile.math.Random;

import static com.lebenlab.ProcessArgException.error_pg1s;
import static java.util.Collections.sort;
import static java.util.EnumSet.allOf;
import static java.util.List.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * User: pedro@didekin.es
 * Date: 12/10/2019
 * Time: 16:37
 */
public enum PG1 {

    // To control wrong pg1 values in files.
    PG1_0(0),
    PG1_1(1),
    PG1_2(2),
    PG1_3(3),
    PG1_4(4),
    PG1_5(5),
    PG1_6(6),
    PG1_7(7),
    PG1_8(8),
    PG1_10(10),
    PG1_11(11),
    PG1_12(12),
    PG1_13(13),
    PG1_14(14),
    PG1_15(15),
    PG1_17(17),
    ;

    public int idPg1;

    PG1(int idPg1In)
    {
        idPg1 = idPg1In;
    }

    public int getIdPg1()
    {
        return idPg1;
    }

    // ===================== Static members  ========================

    private static final Map<Integer, PG1> intToPg1 = allOf(PG1.class).stream().collect(toMap(pg1 -> pg1.idPg1, pg1 -> pg1));

    public static int fromInt(int idPg1In)
    {
        return fromIntPg1(idPg1In).idPg1;
    }

    public static PG1 fromIntPg1(int idPg1In)
    {
        return ofNullable(intToPg1.get(idPg1In)).orElse(PG1_0);
    }

    public static boolean checkPg1sIn(List<Integer> pg1sIn)
    {
        return allOf(PG1.class).stream().map(value -> value.idPg1).collect(toSet()).containsAll(pg1sIn);
    }

    public static Map<PG1, List<PG1>> permutationsPromo(List<Integer> pg1sPromo)
    {
        sort(pg1sPromo);
        final var mapP1 = new HashMap<PG1, List<PG1>>(pg1sPromo.size());
        switch (pg1sPromo.size()) {
            case 1:
                mapP1.put(fromIntPg1(pg1sPromo.get(0)), of(PG1_0, PG1_0));
                break;
            case 2:
                mapP1.put(fromIntPg1(pg1sPromo.get(0)), of(fromIntPg1(pg1sPromo.get(1)), PG1_0));
                mapP1.put(fromIntPg1(pg1sPromo.get(1)), of(fromIntPg1(pg1sPromo.get(0)), PG1_0));
                break;
            case 3:
                mapP1.put(fromIntPg1(pg1sPromo.get(0)), of(fromIntPg1(pg1sPromo.get(1)), fromIntPg1(pg1sPromo.get(2))));
                mapP1.put(fromIntPg1(pg1sPromo.get(1)), of(fromIntPg1(pg1sPromo.get(0)), fromIntPg1(pg1sPromo.get(2))));
                mapP1.put(fromIntPg1(pg1sPromo.get(2)), of(fromIntPg1(pg1sPromo.get(0)), fromIntPg1(pg1sPromo.get(1))));
                break;
            default:
                throw new ProcessArgException(error_pg1s);
        }
        return mapP1;
    }

    /**
     * Utility for tests.
     */
    public static PG1 randomInstance(final PG1 pg1ToExclude, Random rnd)
    {
        EnumSet<PG1> allPg1s = allOf(PG1.class);
        allPg1s.remove(pg1ToExclude);
        return allPg1s.toArray(PG1[]::new)[rnd.nextInt(allPg1s.size())];
    }

    /**
     * Utility for tests.
     */
    public static PG1 randomInstance(Random rnd)
    {
        return values()[rnd.nextInt(allOf(PG1.class).size())];
    }

    public static PG1 randomInstance(Random rnd, EnumSet<PG1> pg1sToInclude)
    {
        return pg1sToInclude.toArray(PG1[]::new)[rnd.nextInt(pg1sToInclude.size())];
    }

    // ===================== Inner classes  ========================

    public static class Pg1ForJson implements Jsonable {

        public static final RowMapper<Pg1ForJson> mapper =
                (rs, ctx) -> new Pg1ForJson(rs.getInt("pg1_id"), rs.getString("descripcion"));

        public final int idPg1;
        public final String descripcion;

        public Pg1ForJson(int idPg1, String descripcion)
        {
            this.idPg1 = idPg1;
            this.descripcion = descripcion;
        }

        public int getIdPg1()
        {
            return idPg1;
        }

        public String getDescripcion()
        {
            return descripcion;
        }
    }
}
