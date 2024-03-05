package com.lebenlab;

/**
 * User: pedro@didekin
 * Date: 31/08/15
 * Time: 13:09
 */
public interface BeanBuilder<T extends Jsonable> {

    T build();
}