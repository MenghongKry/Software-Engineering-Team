package edu.itc.se.shop_jpa.support;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;

/** Test helper: counts JDBC statements prepared by Hibernate since reset(). */
@Component
public class QueryCounter {

    private final Statistics stats;

    public QueryCounter(EntityManagerFactory emf) {
        this.stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
    }

    public void reset() { stats.clear(); }

    /** Number of JDBC PreparedStatements Hibernate created since reset(). */
    public long count() { return stats.getPrepareStatementCount(); }

    /** Number of entity INSERTs since reset() (one per entity, batched or not). */
    public long inserts() { return stats.getEntityInsertCount(); }
}
