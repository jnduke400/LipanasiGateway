package com.hybrid9.pg.Lipanasi.route.tqs;

import com.hybrid9.pg.Lipanasi.component.mixxtqs.TigoResponseParser;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.HaloPesaConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.MixxByYasConfig;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.mixxtqs.TigoTransactionService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//@Component
public class MixxTransactionQueryRoute extends RouteBuilder {

    private final PushUssdService pushUssdService;
    private final TigoResponseParser tigoResponseParser;
    private final TigoTransactionService tigoTransactionService;
    private final DepositService depositService;
    private final SessionManagementService sessionManagementService;

    public MixxTransactionQueryRoute(PushUssdService pushUssdService, TigoResponseParser tigoResponseParser,
                                     TigoTransactionService tigoTransactionService,DepositService depositService, SessionManagementService sessionManagementService) {
        this.pushUssdService = pushUssdService;
        this.tigoResponseParser = tigoResponseParser;
        this.tigoTransactionService = tigoTransactionService;
        this.depositService = depositService;
        this.sessionManagementService = sessionManagementService;
    }

    @Override
    public void configure() throws Exception {
        // Error Handler
        onException(Exception.class)
                .maximumRedeliveries(3)
                .redeliveryDelay(1000) // 1 second delay between retries
                .backOffMultiplier(2)  // Exponential backoff
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .logRetryAttempted(true)
                .logStackTrace(true)
                .end();

        ExecutorService airtelExecutor = Executors.newFixedThreadPool(10);
        getContext().getRegistry().bind("mixxTqsThreadPool", airtelExecutor);

        // Main route to process NEW transactions
        from("quartz://tqs/mixxbyyas?cron=0+0/50+*+*+*+?") // Run every 5 minutes
                //.routeId("tigopesa-query-transactions")
                .transacted()
                //.threads().executorService("mixxTqsThreadPool")
                .process(exchange -> {
                    List<PushUssd> transactions = this.pushUssdService.getNewTransactions();
                    if (transactions != null && !transactions.isEmpty()) {
                        exchange.getIn().setBody(transactions);
                    } else {
                        // Skip processing if no transactions
                        exchange.setProperty("CamelRouteStop", Boolean.TRUE);
                    }
                })
                .split(body())
                .toD("direct:processTransaction");

        // Process individual transaction
        from("direct:processTransaction")
                .routeId("tigopesa-process-transaction")
                .process(exchange -> {
                    PushUssd transaction = exchange.getIn().getBody(PushUssd.class);
                    if (transaction != null) {
                        String referenceId = transaction.getReference();
                        String requestXml = createQueryRequestXml(transaction, referenceId);
                        exchange.getIn().setBody(requestXml);
                        exchange.setProperty("originalTransaction", transaction);
                        exchange.setProperty("networkConfig", (MixxByYasConfig) this.sessionManagementService.getSession(depositService.findByReference(transaction.getReference()).orElseThrow(()->new CustomExcpts.TransactionNotFoundException("Transaction Not found with Reference: "+transaction.getReference())).getSessionId()).orElseThrow(()->
                                new RuntimeException("Session not found for ID: " + depositService.findByReference(transaction.getReference()).orElseThrow(()->new CustomExcpts.TransactionNotFoundException("Transaction Not found with Reference: "+transaction.getReference())).getSessionId())).getNetworkConfig());
                    } else {
                        // Skip processing this null transaction
                        exchange.setProperty("CamelRouteStop", Boolean.TRUE);
                    }
                })
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("text/xml"))
                .setHeader("Connection", constant("keep-alive"))
                .log(">>>>>>>>>>Sending TQS Request to TigoPesa Gateway ${body}")
                .toD("${exchangeProperty.networkConfig.tqsUrl}?bridgeEndpoint=true&httpMethod=POST"+
                        "&connectTimeout=120000" +  // 120 seconds
                        "&socketTimeout=120000" +   // 120 seconds
                        "&compress=true" +
                        "&useSystemProperties=false" +
                        "&sslContextParameters=#noopSslContext")
                .log(">>>>>>>>>Response from TigoPesa TQS Gateway: ${body}")
                .toD("direct:processResponse");

        // Process API response
        from("direct:processResponse")
                .routeId("tigopesa-process-response")
                .process(exchange -> {
                    String responseXml = exchange.getIn().getBody(String.class);
                    PushUssd originalTx = exchange.getProperty("originalTransaction", PushUssd.class);
                    if (!responseXml.contains("Internal service error occurred. Please contact Tigo Support for further details.")) {
                        this.tigoTransactionService.handleApiResponse(responseXml, originalTx.getId());
                    }
                });
    }

    private String createQueryRequestXml(PushUssd transaction, String referenceId) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <COMMAND>
                    <TYPE>MTPGGetSODetails</TYPE>
                    <REFERENCEID>%s</REFERENCEID>
                    <MSISDN>%s</MSISDN>
                    <PIN>%s</PIN>
                    <EXTERNALREFID>%s</EXTERNALREFID>
                </COMMAND>
                """.formatted(
                referenceId,
                "${exchangeProperty.networkConfig.billerMsisdn}",
                getConfiguredPin(),
                transaction.getReference() //Generate a unique reference for each transaction
        );
    }

    private Object getConfiguredPin() {
        return "${exchangeProperty.networkConfig.tqsPassword}";
    }
}
