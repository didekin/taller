package com.lebenlab;

import java.io.InputStream;
import java.util.Optional;

/**
 * User: pedro@didekin
 * Date: 08/06/2021
 * Time: 19:34
 */
public final class IOutil {

    public static Optional<? extends InputStream> emptyOptStream(){
        return Optional.empty();
    }
}
