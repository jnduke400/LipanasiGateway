package com.hybrid9.pg.Lipanasi.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.orange.ReturnData;
import com.hybrid9.pg.Lipanasi.dto.orange.S2MResponseDto;
import com.hybrid9.pg.Lipanasi.dto.orange.S2MResponseWrapper;
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
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
//@Component
public class OrangeCongoRoute extends RouteBuilder {
    private final PaymentUtilities paymentUtilities;
    @Value("${payment-gateway.congo_orangemoney.api-url}")
    private String apiUrl;
    @Value("${payment-gateway.congo_orangemoney.callbackurl}")
    private String callbackUrl;
    @Value("${payment-gateway.congo_orangemoney.mermsisdn}")
    private String mermsisdn;
    @Value("${payment-gateway.congo_orangemoney.partnId}")
    private String partnId;


    private final BillerPaymentRequestBuilder requestBuilder;
    private final CashInLogService cashInLogService;
    private final MnoServiceImpl mnoService;
    private final DepositService depositService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PushUssdService pushUssdService;

    public OrangeCongoRoute(BillerPaymentRequestBuilder requestBuilder, CashInLogService cashInLogService, MnoServiceImpl mnoService, DepositService depositService, MainAccountService mainAccountService, VendorService vendorService, PushUssdService pushUssdService, PaymentUtilities paymentUtilities) {
        this.requestBuilder = requestBuilder;
        this.cashInLogService = cashInLogService;
        this.mnoService = mnoService;
        this.depositService = depositService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.pushUssdService = pushUssdService;
        this.paymentUtilities = paymentUtilities;
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

        MnoMapping orangeCongo = this.mnoService.findMappingByMno("Orange-Congo");

        //Add Mno to a List
        List<String> mnoList = new ArrayList<>();
        mnoList.add(orangeCongo.getMno());


        //Add Collection Status to a List
        List<RequestStatus> requestStatuses = new ArrayList<>();
        requestStatuses.add(RequestStatus.NEW);


        from("timer://fetchRecords?period=1000") // Trigger every 1 second
                .routeId("init-orange-congo-deposits-producer")
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
                .to(CamelConfiguration.RABBIT_PRODUCER_ORANGE_MONEY_CONGO_INIT_URI)
                .log("Record sent to RabbitMQ: ${body}")
                .end();

        // Main route
        from(CamelConfiguration.RABBIT_CONSUMER_ORANGE_MONEY_CONGO_INIT_URI)
                .id("init-orange-money-congo-deposits-consumer-")
                .process(exchange -> {
                    // Extract the request data
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

                    // Create initial push ussd payload
                    Map<String, String> request = new HashMap<>();
                    request.put("subsmsisdn", exchange.getProperty("msisdn", String.class));
                    request.put("partnId", partnId);
                    request.put("mermsisdn", mermsisdn);
                    request.put("transid", exchange.getProperty("transactionId", String.class));
                    request.put("currency", "CDF");
                    request.put("amount", exchange.getProperty("amount", String.class));
                    request.put("callbackurl", callbackUrl);

                    exchange.getIn().setBody(request);
                })
                .setBody(simple(requestXml)) // Set the SOAP request body
                .setHeader(Exchange.CONTENT_TYPE, constant("application/xml"))
                //log request headers
                .log("Orange Money Congo Request Headers: ${headers}")
                .log("Orange Money Congo Payment Body: ${body}")
                .to(apiUrl + "?bridgeEndpoint=true&httpMethod=POST")
                // Handle the response
                .log("Orange Money Congo Response: ${body}")
                .choice()
                .when(header("CamelHttpResponseCode").isEqualTo("200"))
                .to("direct:handle-orange-money-congo-success-response")
                .otherwise()
                .to("direct:handle-orange-money-congo-failure-response");

        // Success Response Handler
        from("direct:handle-orange-money-congo-success-response")
                .id("orangeMoney-congo-successResponseRoute")
                .unmarshal().jacksonXml(S2MResponseWrapper.class) // Convert XML response to Java object
                .process(exchange -> {
                    S2MResponseWrapper wrapper = exchange.getIn().getBody(S2MResponseWrapper.class);
                    S2MResponseDto s2mResponseObj = new S2MResponseDto();

                    if (wrapper != null && wrapper.getBody() != null &&
                            wrapper.getBody().getDoS2MResponse() != null &&
                            wrapper.getBody().getDoS2MResponse().getReturnData() != null) {

                        ReturnData returnData = wrapper.getBody().getDoS2MResponse().getReturnData();
                        s2mResponseObj.setPartnId(returnData.getPartnId());
                        s2mResponseObj.setResultCode(returnData.getResultCode());
                        s2mResponseObj.setResultDesc(returnData.getResultDesc());
                        s2mResponseObj.setSystemId(returnData.getSystemId());
                        s2mResponseObj.setTransId(returnData.getTransId());
                    } else {
                        // Handle malformed response
                        throw new RuntimeException("Invalid response structure from Orange Money API");
                    }

                    // Set the response object as the body
                    exchange.getIn().setBody(s2mResponseObj);
                })
                .process(exchange -> {
                    /*ObjectMapper mapper = new ObjectMapper();*/
                    S2MResponseDto response = exchange.getMessage().getBody(S2MResponseDto.class);
                    exchange.getMessage().setHeader("responseStatus", response.getResultDesc());

                    // Store transaction details for callback
                    this.depositService.findByTransactionId(exchange.getProperty("transactionId", String.class)).ifPresent(deposit -> {
                        PushUssd pushUssd = PushUssd.builder()
                                .status(response.getResultCode())
                                .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                                .message(response.getResultDesc())
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
        from("direct:handle-orange-money-congo-failure-response")
                .id("orangeMoney-congo-errorResponseRoute")
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

    // Define the request XML template
    String requestXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ser=\"http://services.ws1.com/\">\n" +
            "    <soapenv:Header/>\n" +
            "    <soapenv:Body>\n" +
            "        <ser:doS2M>\n" +
            "            <subsmsisdn>${body[subsmsisdn]}</subsmsisdn>\n" +
            "            <PartnId>${body[partnId]}</PartnId>\n" +
            "            <mermsisdn>${body[mermsisdn]}</mermsisdn>\n" +
            "            <transid>${body[transid]}</transid>\n" +
            "            <currency>${body[currency]}</currency>\n" +
            "            <amount>${body[amount]}</amount>\n" +
            "            <callbackurl>${body[callbackurl]}</callbackurl>\n" +
            "            <message_s2m>${body[messageS2m]}</message_s2m>\n" +
            "        </ser:doS2M>\n" +
            "    </soapenv:Body>\n" +
            "</soapenv:Envelope>";
}
