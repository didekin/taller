package com.lebenlab;

import javax.sql.DataSource;

/**
 * User: pedro@didekin.es
 * Date: 23/01/2020
 * Time: 18:37
 */
public interface DataSourceFactory {

    DataSource getDataSource();
}
