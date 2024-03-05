package com.lebenlab.jdbi;

import com.lebenlab.DataSourceFactory;

import org.jdbi.v3.core.Jdbi;

/**
 * User: pedro@didekin.es
 * Date: 23/01/2020
 * Time: 18:36
 */
public class JdbiFactory {

    public static final JdbiFactory jdbiFactory = new JdbiFactory(HikariMysqlDsFactory.dsFactory);

    private final Jdbi jdbi;

    private JdbiFactory(DataSourceFactory dsFactoryIn)
    {
        jdbi = Jdbi.create(dsFactoryIn.getDataSource());
    }

    public Jdbi getJdbi()
    {
        return jdbi;
    }
}
