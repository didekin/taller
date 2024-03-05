package com.lebenlab.jdbi;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.Test;

import static com.lebenlab.jdbi.HikariMysqlDsFactory.dsFactory;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * User: pedro@didekin.es
 * Date: 22/01/2020
 * Time: 19:14
 */
public class HikariMysqlDsFactoryTest {

    @Test
    public void test_GetDataSource_1()
    {
        HikariDataSource ds = (HikariDataSource) dsFactory.getDataSource();
        assertThat(ds.getDataSourceProperties().getProperty("user")).isEqualTo("pedro");
        assertThat(ds.getJdbcUrl()).contains("localhost:3306/bosch");
    }
}