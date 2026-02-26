package com.hybrid9.pg.Lipanasi.services;/*
package com.gtl.pg.scoop.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

//import com.gtl.pg.scoop.configs.RoutingDataSource;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicBoolean;

*/
/**
 * Service to control database routing behavior based on system state
 *//*

@Service
class TransactionRoutingService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionRoutingService.class);

    private final DataSource routingDataSource;
    private final AtomicBoolean readOnlyRoutingEnabled = new AtomicBoolean(true);
    private final AtomicBoolean sensitiveReadsRouteToPrimary = new AtomicBoolean(false);

    @Autowired
    public TransactionRoutingService(@Qualifier("routingDataSource") DataSource routingDataSource) {
        this.routingDataSource = routingDataSource;
    }

    */
/**
     * Enable or disable read-only routing to replica databases
     *
     * @param enabled Whether routing should be enabled
     *//*

    public void setReadOnlyRoutingEnabled(boolean enabled) {
        boolean previous = readOnlyRoutingEnabled.getAndSet(enabled);

        if (previous != enabled) {
            logger.info("Setting read-only routing to: {}", enabled);

            if (routingDataSource instanceof RoutingDataSource) {
                ((RoutingDataSource) routingDataSource).setReadOnlyRoutingEnabled(enabled);
            }
        }
    }

    */
/**
     * Configure whether sensitive financial reads should route to primary
     * This is useful during replication lag to ensure critical financial data is fresh
     *
     * @param routeToPrimary Whether sensitive reads should go to primary
     *//*

    public void setSensitiveReadsRouteToPrimary(boolean routeToPrimary) {
        boolean previous = sensitiveReadsRouteToPrimary.getAndSet(routeToPrimary);

        if (previous != routeToPrimary) {
            logger.info("Setting sensitive reads route to primary: {}", routeToPrimary);

            if (routingDataSource instanceof RoutingDataSource) {
                ((RoutingDataSource) routingDataSource).setSensitiveReadsRouteToPrimary(routeToPrimary);
            }
        }
    }

    */
/**
     * Check if read-only routing is currently enabled
     *//*

    public boolean isReadOnlyRoutingEnabled() {
        return readOnlyRoutingEnabled.get();
    }

    */
/**
     * Check if sensitive reads are currently routed to primary
     *//*

    public boolean isSensitiveReadsRouteToPrimary() {
        return sensitiveReadsRouteToPrimary.get();
    }
}
*/
