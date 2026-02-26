package com.hybrid9.pg.Lipanasi.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.AirtelMoneyTokenResponse;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.ResponseDto;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.logs.CashInLog;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.route.handler.mixbyyas.BillerPaymentRequestBuilder;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.CashInLogService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.payments.InitDepositDeduplicationService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
//@Component
public class AirtelMoneyCongoRoute extends RouteBuilder {
    @Value("${payment-gateway.airtel_money_congo.token-url}")
    private String tokenUrl;
    @Value("${payment-gateway.airtel_money_congo.api-url}")
    private String apiUrl;


    private final BillerPaymentRequestBuilder requestBuilder;
    private final CashInLogService cashInLogService;
    private final MnoServiceImpl mnoService;
    private final DepositService depositService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PushUssdService pushUssdService;

    public AirtelMoneyCongoRoute(BillerPaymentRequestBuilder requestBuilder, CashInLogService cashInLogService, MnoServiceImpl mnoService, DepositService depositService, MainAccountService mainAccountService, VendorService vendorService, PushUssdService pushUssdService) {
        this.requestBuilder = requestBuilder;
        this.cashInLogService = cashInLogService;
        this.mnoService = mnoService;
        this.depositService = depositService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.pushUssdService = pushUssdService;
    }

    @Override
    public void configure() throws Exception {
        // Error Handler with retry policy
        errorHandler(defaultErrorHandler()
                .maximumRedeliveries(3)
                .redeliveryDelay(1000)
                .backOffMultiplier(2)
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .onRedelivery(exchange -> {
                    // Update retry count and status in cash_in_logs
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        int retryCount = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);

                        cashInLog.setRetryCount(retryCount);
                        if (retryCount < 3) {
                            cashInLog.setStatus(RequestStatus.MARKED_FOR_RETRY);
                        } else {
                            cashInLog.setStatus(RequestStatus.FAILED);
                            // Update deposit transaction when max retries reached
                            String transactionId = exchange.getProperty("transactionId", String.class);
                            depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                                deposit.setRequestStatus(RequestStatus.FAILED);
                                deposit.setErrorMessage("Maximum retry attempts reached - Transaction failed");
                                depositService.recordDeposit(deposit);
                            });
                        }
                        cashInLogService.recordLog(cashInLog);
                    }
                }));

        MnoMapping airtelmoneyCongo = this.mnoService.findMappingByMno("AirtelMoney-Congo");

        //Add Mno to a List
        List<String> mnoList = new ArrayList<>();
        mnoList.add(airtelmoneyCongo.getMno());


        //Add Collection Status to a List
        List<RequestStatus> requestStatuses = new ArrayList<>();
        requestStatuses.add(RequestStatus.NEW);


        from("timer://fetchRecords?period=1000") // Trigger every 1 second
                .routeId("init-airtelMoney-congo-deposits-producer")
                .transacted() // Transactional
                // Use the repository to fetch data
                .process(exchange -> {
                    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() ->
                            this.depositService.findByRequestStatusAndOperator(requestStatuses, mnoList)

                    ).thenApplyAsync(result -> {
                        result.forEach(record -> record.setRequestStatus(RequestStatus.INITIATED));
                        return result;

                    }).thenApplyAsync(this.depositService::updateAllRequestStatus
                    ).thenAcceptAsync(result ->
                            exchange.getIn().setBody(result)
                    );
                    future.join();

                })
                .split(body()) // Split the records to process each individually
                .log("Records fetched: ${body}")
                //.process(exchange -> InitDepositProcessor.process(exchange))
                .process(exchange -> {
                    Deposit deposit = exchange.getIn().getBody(Deposit.class);
                    DepositDto depositDto = DepositDto.builder()
                            .id(deposit.getId())
                            .msisdn(deposit.getMsisdn())
                            .amount(deposit.getAmount())
                            .channel(deposit.getChannel())
                            .paymentReference(deposit.getPaymentReference())
                            .originalReference(deposit.getOriginalReference())
                            .transactionId(deposit.getTransactionId())
                            .operator(deposit.getOperator())
                            .requestStatus(deposit.getRequestStatus())
                            .vendorDto(VendorDto.builder()
                                    .vendorName(deposit.getVendorDetails().getVendorName())
                                    .vendorCode(deposit.getVendorDetails().getVendorCode())
                                    .billNumber(deposit.getVendorDetails().getBillNumber())
                                    .hasCommission(deposit.getVendorDetails().getHasCommission())
                                    .hasVat(deposit.getVendorDetails().getHasVat())
                                    .charges(deposit.getVendorDetails().getCharges())
                                    .build())
                            .build();

                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(depositDto);
                    exchange.getIn().setBody(json);
                })
                // Send records to RabbitMQ
                .to(CamelConfiguration.RABBIT_PRODUCER_AIRTEL_MONEY_CONGO_INIT_URI)
                .log("Record sent to RabbitMQ: ${body}")
                .end();

        // Token Management Route
        from("direct:getAirtelMoneyCongoToken")
                .id("getCongoTokenRoute")
                // Store original body in a property
                .setProperty("originalBody", simple("${body}"))
                .setBody(constant("""
                        {
                            "client_id": "49c83718-f1c4-4374-a23f-f0ed6068b136",
                            "client_secret": "8a2b02d3-cb69-429c-8023-663eec131f5c",
                            "grant_type": "client_credentials"
                        }
                        """))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("*/*"))
                .log("Airtel Money Congo Token Request Body: ${body}")
                .to(tokenUrl + "?bridgeEndpoint=true&httpMethod=POST")
                .convertBodyTo(String.class)
                .log("Airtel Money Congo Token Response: ${body}")
                //.unmarshal().json(JsonLibrary.Jackson)
                .process(exchange -> {
                    ObjectMapper mapper = new ObjectMapper();
                    AirtelMoneyTokenResponse tokenResponse = mapper.readValue(exchange.getIn().getBody(String.class), AirtelMoneyTokenResponse.class);
                    exchange.getMessage().setHeader("auth_token", tokenResponse.getAccess_token());
                    // Restore original body
                    exchange.getMessage().setBody(exchange.getProperty("originalBody"));
                })
                .setProperty("auth_token", simple("${header.auth_token}"));

        // Main Payment Route
        from(CamelConfiguration.RABBIT_CONSUMER_AIRTEL_MONEY_CONGO_INIT_URI)
                .id("init-airtel-money-congo-deposits-consumer-")
                .bean(InitDepositDeduplicationService.class, "checkAndMarkInitDeposited")
                .filter(simple("${body} != null"))  // Only proceed if not a duplicate
                .process(exchange -> {
                    //set header
                    ObjectMapper mapper = new ObjectMapper();
                    DepositDto depositDto = mapper.readValue(exchange.getIn().getBody(String.class), DepositDto.class);
                    exchange.setProperty("msisdn", depositDto.getMsisdn());
                    exchange.setProperty("amount", String.valueOf(depositDto.getAmount()).replaceAll("\\.\\d+", ""));
                    exchange.setProperty("reference", depositDto.getPaymentReference());
                    exchange.setProperty("operator", depositDto.getOperator());
                    exchange.setProperty("transactionId", depositDto.getTransactionId());

                    // Create initial log entry
                    CashInLog cashInLog = CashInLog.builder()
                            .cashInRequest(exchange.getMessage().getBody(String.class))
                            .retryCount(0)
                            .status(RequestStatus.INITIATED)
                            .build();
                    cashInLogService.recordLog(cashInLog);
                    exchange.setProperty("cashInLogId", cashInLog.getId());

                    // Extract payment details from the request
                    String msisdn = exchange.getProperty("msisdn", String.class);
                    String amount = exchange.getProperty("amount", String.class);
                    String transactionId = exchange.getProperty("transactionId", String.class);
                    String reference = exchange.getProperty("reference", String.class);


                    // Construct payment request body
                    String paymentBody = String.format("""
                            {
                                "reference": "%s",
                                "subscriber": {
                                    "country": "CD",
                                    "currency": "CDF",
                                    "msisdn": "%s"
                                },
                                "transaction": {
                                    "amount": %s,
                                    "country": "CD",
                                    "currency": "CDF",
                                    "id": "%s"
                                }
                            }
                            
                            """, reference, msisdn.substring(3), amount, transactionId);

                    exchange.getIn().setBody(paymentBody);
                })
                // First get the token
                .to("direct:getAirtelMoneyCongoToken")
                // Then make the payment request
                // Remove all existing headers except the ones you want to keep
                .removeHeaders("*")
                // Then set your specific headers
                //.setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("*/*"))
                .setHeader("X-Country", constant("CD"))
                .setHeader("X-Currency", constant("CDF"))
                .setHeader("Authorization", simple("Bearer ${exchangeProperty.auth_token}"))
                //log request headers
                .log("Airtel Money Congo Request Headers: ${headers}")
                .log("Airtel Money Congo Payment Body: ${body}")
                .to(apiUrl + "?bridgeEndpoint=true&httpMethod=POST")
                // Handle the response
                .log("Airtel Money Congo Response: ${body}")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                //.setBody(simple("{\"status\": \"success\", \"transactionId\": \"${body[data][transaction][id]}\"}"))
                .to("direct:handle-airtel-money-congo-success-response")
                .otherwise()
                //.setBody(simple("{\"status\": \"failed\", \"message\": \"${body[status][message]}\"}"));
                .to("direct:handle-airtel-money-congo-failure-response");

        // Success Response Handler
        from("direct:handle-airtel-money-congo-success-response")
                .id("AirtelMoney-congo-successResponseRoute")
                //.unmarshal().json(JsonLibrary.Jackson, BillerPaymentResponse.class)
                .process(exchange -> {
                    ObjectMapper mapper = new ObjectMapper();
                    ResponseDto response = mapper.readValue(exchange.getMessage().getBody(String.class), ResponseDto.class);
                    exchange.getMessage().setHeader("responseStatus", response.getStatus().getSuccess());

                    // Store transaction details for callback
                    this.depositService.findByTransactionId(exchange.getProperty("transactionId", String.class)).ifPresent(deposit -> {
                        PushUssd pushUssd = PushUssd.builder()
                                .status(response.getStatus().getSuccess() ? "0" : "-1")
                                .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                                .message(response.getStatus().getMessage())
                                .reference(deposit.getPaymentReference())
                                .currency(deposit.getCurrency())
                                .operator(this.mnoService.searchMno(deposit.getMsisdn()))
                                //.transactionNumber(exchange.getMessage().getBody(TransactionResponse.class).getInsightReference())
                                .accountId(String.valueOf(mainAccountService.findByVendorDetails(deposit.getVendorDetails()).getId()))
                                .vendorDetails(deposit.getVendorDetails())
                                .msisdn(deposit.getMsisdn())
                                .build();
                        this.pushUssdService.update(pushUssd);
                        exchange.setProperty("transactionId", exchange.getProperty("transactionId", String.class));
                    });
                })
                .log("Response Status: ${header.responseStatus}")
                .end();


        // Error Response Handler for Network Connection Issues
        from("direct:handle-airtel-money-congo-failure-response")
                .id("airtelMoney-congo-errorResponseRoute")
                .process(exchange -> {
                    // Update cash_in_logs
                    String cashInLogId = exchange.getProperty("cashInLogId", String.class);
                    if (cashInLogId != null) {
                        CashInLog cashInLog = cashInLogService.findById(Long.parseLong(cashInLogId)).orElse(null);
                        cashInLog.setStatus(RequestStatus.NETWORK_CONNECTION_ISSUE);
                        cashInLogService.recordLog(cashInLog);
                    }

                    // Update deposit transaction
                    String transactionId = exchange.getProperty("transactionId", String.class);
                    depositService.findByTransactionId(transactionId).ifPresent(deposit -> {
                        deposit.setRequestStatus(RequestStatus.NETWORK_CONNECTION_ISSUE);
                        deposit.setErrorMessage("Network connection issue with mobile operator");
                        depositService.recordDeposit(deposit);
                    });
                })
                .log(LoggingLevel.ERROR, "Network connection issue encountered: ${exception.message}");


    }
}
