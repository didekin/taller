package com.lebenlab.core.util;

import java.util.Random;

/**
 * User: pedro@didekin
 * Date: 17/03/2021
 * Time: 12:37
 */
public class RandomUtil {

    private static final Random rnd = new Random();

    public static long randomLong30(){
        return rnd.nextInt(30);
    }
}
