package com.lebenlab.core.simulacion;

import com.lebenlab.ProcessArgException;

import java.util.Map;

import smile.math.Random;

import static com.lebenlab.ProcessArgException.error_quarter;
import static java.util.EnumSet.allOf;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * User: pedro@didekin
 * Date: 23/03/2020
 * Time: 18:10
 */
public enum Quarter {
    _1(1),
    _2(2),
    _3(3),
    _4(4),
    ;

    public final int quarterId;

    Quarter(int quarterId)
    {
        this.quarterId = quarterId;
    }

    private static final Map<Integer, Quarter> intToQuarter =
            allOf(Quarter.class).stream().collect(toMap(q -> q.quarterId, q -> q));

    public static Quarter fromIntToQuarter(int quarterId)
    {
        return ofNullable(intToQuarter.get(quarterId)).orElseThrow(() -> new ProcessArgException(error_quarter + quarterId));
    }

    public static Quarter randomInstance(Random rnd)
    {
        return values()[rnd.nextInt(allOf(Quarter.class).size())];
    }
}
