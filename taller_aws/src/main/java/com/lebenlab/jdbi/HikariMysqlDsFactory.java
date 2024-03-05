package com.lebenlab.jdbi;

import com.lebenlab.DataSourceFactory;
import com.lebenlab.core.FilePath;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * User: pedro@didekin.es
 * Date: 22/01/2020
 * Time: 17:43
 */
public class HikariMysqlDsFactory implements DataSourceFactory {

    public static final DataSourceFactory dsFactory = new HikariMysqlDsFactory();

    private final DataSource ds;

    private HikariMysqlDsFactory()
    {
        ds = doDataSource();
    }

    /*
     * For details, see: 'Configuration Properties' en MySQL Connector/J Developer Guide
     */
    @SuppressWarnings("CommentedOutCode")
    private DataSource doDataSource()
    {
        HikariConfig config = new HikariConfig(FilePath.appfilesPath.resolve("hikari.properties").toString());

        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.addDataSourceProperty("serverTimezone", "Europe/Madrid");
        config.addDataSourceProperty("minimumIdle", "1");
        // For optimal throughput this number should be somewhere near (core_count * 2) + effective_spindle_count (Disks)
        config.setMaximumPoolSize(3);
        // Parameters related to mysql --wait_timeout: it should be about 60 second less.
        config.setMaxLifetime(540000);
        // Performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "150");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("useLocalTransactionState", "true");
        // Turn to true for optimize multiple insertions.
        config.addDataSourceProperty("rewriteBatchedStatements", "false");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        // Logging.
//        config.addDataSourceProperty("logSlowQueries", "true");
        config.addDataSourceProperty("dumpQueriesOnException", "true");

        return new HikariDataSource(config);
    }

    @Override
    public DataSource getDataSource()
    {
        return ds;
    }
}
