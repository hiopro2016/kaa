package org.kaaproject.kaa.server.datamigration;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import org.kaaproject.kaa.server.datamigration.utils.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointSpecificConfigurationMigration {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointSpecificConfigurationMigration.class);

    private Cluster cluster;
    private String dbName;
    private String nosql;
    private Session cassandraSession;

    public EndpointSpecificConfigurationMigration(String host, String db, String nosql) {
        cluster = Cluster.builder()
                .addContactPoint(host)
                .build();
        dbName = db;
        this.nosql = nosql;
    }

    public void transform() {
        if (!Options.DEFAULT_NO_SQL.equalsIgnoreCase(nosql)) {
            try {
                cassandraSession = cluster.connect(dbName);
                addEndpointSpecificConfigurationTable();
                alterEndpointProfileTable();
            } finally {
                cassandraSession.close();
                cluster.close();
            }
        }
    }

    private void alterEndpointProfileTable() {
        try {
            cassandraSession.execute("ALTER TABLE ep_profile ADD eps_cf_hash blob;");
        } catch (InvalidQueryException e) {
            LOG.warn("Failed to alter ep_profile table: {}", e.getMessage());
        }
    }

    private void addEndpointSpecificConfigurationTable() {
        cassandraSession.execute("CREATE TABLE IF NOT EXISTS ep_specific_conf (\n" +
                "    ep_key_hash text,\n" +
                "    cf_ver int,\n" +
                "    body text,\n" +
                "    opt_lock bigint,\n" +
                "    PRIMARY KEY((ep_key_hash), cf_ver)\n" +
                ") WITH CLUSTERING ORDER BY (cf_ver DESC);");
    }
}
