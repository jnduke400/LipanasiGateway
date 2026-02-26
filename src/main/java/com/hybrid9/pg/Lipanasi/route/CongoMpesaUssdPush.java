package com.hybrid9.pg.Lipanasi.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.CamelConfiguration;
import com.hybrid9.pg.Lipanasi.dto.DepositDto;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.congo.mpesa.C2BPaymentRequest;
import com.hybrid9.pg.Lipanasi.dto.congo.mpesa.C2BPaymentResponse;
import com.hybrid9.pg.Lipanasi.dto.congo.mpesa.LoginRequest;
import com.hybrid9.pg.Lipanasi.dto.congo.mpesa.LoginResponse;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.CashInLogService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.support.builder.Namespaces;
import org.springframework.beans.factory.annotation.Value;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;

import org.xml.sax.InputSource;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.namespace.NamespaceContext;


import java.util.concurrent.CompletableFuture;

//@Component
public class CongoMpesaUssdPush extends RouteBuilder {
    @Value("${payment-gateway.congo_mpesa.api-base-url}")
    private String apiBaseUrl;

    @Value("${payment-gateway.congo_mpesa.username}")
    private String username;

    @Value("${payment-gateway.congo_mpesa.password}")
    private String password;

    @Value("${payment-gateway.congo_mpesa.event-id}")
    private String eventId;

    @Value("${payment-gateway.congo_mpesa.event-id-c2b-payment}")
    private String eventIdc2bPayment;

    @Value("${payment-gateway.congo_mpesa.command-id}")
    private String commandId;

    @Value("${payment-gateway.congo_mpesa.short-code}")
    private String shortCode;

    @Value("${payment-gateway.congo_mpesa.callback-url}")
    private String callbackUrl;

    private static final String EMAIL_ADDRESS = "ndukep@gmail.com";
    private static final int MAX_RETRIES = 3;

    private final Namespaces ns = new Namespaces("soapenv", "http://schemas.xmlsoap.org/soap/envelope/")
            .add("soap", "http://www.4cgroup.co.za/soapauth")
            .add("gen", "http://www.4cgroup.co.za/genericsoap");

    private final CashInLogService cashInLogService;
    private final MnoServiceImpl mnoService;
    private final DepositService depositService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PushUssdService pushUssdService;


    public CongoMpesaUssdPush(CashInLogService cashInLogService, MnoServiceImpl mnoService, DepositService depositService, MainAccountService mainAccountService, VendorService vendorService, PushUssdService pushUssdService) {
        this.cashInLogService = cashInLogService;
        this.mnoService = mnoService;
        this.depositService = depositService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.pushUssdService = pushUssdService;
    }

    @Override
    public void configure() throws Exception {

        MnoMapping mpesaCongoTanzania = this.mnoService.findMappingByMno("Mpesa-Congo");

        //Add Mno to a List
        List<String> mnoList = new ArrayList<>();
        mnoList.add(mpesaCongoTanzania.getMno());


        //Add Collection Status to a List
        List<RequestStatus> requestStatuses = new ArrayList<>();
        requestStatuses.add(RequestStatus.NEW);


        from("timer://fetchRecords?period=1000") // Trigger every 1 second
                .routeId("init-mpesa-congo-deposits-producer")
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
                .to(CamelConfiguration.RABBIT_PRODUCER_MPESA_CONGO_INIT_URI)
                .log("Record sent to RabbitMQ: ${body}")
                .end();

        // Login route
        from(CamelConfiguration.RABBIT_CONSUMER_MPESA_CONGO_INIT_URI)
                .id("mpesa-congo-login-route")
                .setHeader("SOAPAction", constant(""))
                .setHeader("EventID", constant(eventId))
                .process(exchange -> {
                    // Store the original body
                    exchange.setProperty("OriginalBody", exchange.getIn().getBody(String.class));
                    LoginRequest request = new LoginRequest();
                    request.setUsername(username);
                    request.setPassword(password);
                    exchange.getIn().setBody(createLoginSoapRequest(request));
                })
                .setHeader(Exchange.CONTENT_TYPE, constant("application/xml"))
                //.setHeader(Exchange.CONTENT_TYPE, constant("text/xml;charset=UTF-8"))
                .to(apiBaseUrl + "?bridgeEndpoint=true&httpMethod=POST")
                .process(exchange -> new LoginResponseProcessor().process(exchange))
                .choice()
                .when(header("LoginCode").isEqualTo("3"))
                .log("Login successful, proceeding with transaction")
                .process(exchange -> {
                    LoginResponse loginResponse = exchange.getIn().getBody(LoginResponse.class);
                    exchange.setProperty("sessionId", loginResponse.getSessionId());
                    // Keep the original DepositDto in the body
                    String originalBody = exchange.getProperty("OriginalBody", String.class);
                    exchange.getIn().setBody(originalBody);
                })
                .to("direct:initiateCongoC2BPayment")
                .otherwise()
                //.to("direct:handleLoginFailure")
                .log("Login failed, stopping process")
                .end();


        // C2B Payment route
        from("direct:initiateCongoC2BPayment")
                .id("c2bPaymentRoute")
                .setHeader("SOAPAction", constant(""))
                .setHeader("EventID", constant(eventIdc2bPayment))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/xml"))
                .process(exchange -> {
                    //set properties
                    ObjectMapper mapper = new ObjectMapper();
                    DepositDto depositDto = mapper.readValue(exchange.getIn().getBody(String.class), DepositDto.class);
                    exchange.setProperty("msisdn", depositDto.getMsisdn());
                    exchange.setProperty("amount", String.valueOf(depositDto.getAmount()).replaceAll("\\.\\d+", ""));
                    exchange.setProperty("reference", depositDto.getPaymentReference());
                    exchange.setProperty("operator", depositDto.getOperator());
                    exchange.setProperty("transactionId", depositDto.getTransactionId());

                    C2BPaymentRequest request = new C2BPaymentRequest();
                    request.setCustomerMsisdn(depositDto.getMsisdn());
                    request.setServiceProviderCode(shortCode);
                    request.setCurrency("CDF");
                    request.setAmount(String.valueOf(depositDto.getAmount()).replaceAll("\\.\\d+", ""));
                    request.setDate(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
                    request.setThirdPartyReference(depositDto.getPaymentReference());
                    request.setCommandId(commandId);
                    request.setLanguage("EN");
                    request.setCallBackChannel("4");
                    request.setCallBackDestination(callbackUrl);
                    String token = exchange.getProperty("sessionId", String.class);
                    exchange.getIn().setBody(createC2BPaymentSoapRequest(request, token));
                })
                .log("Sending C2B Payment Request [Mpesa Congo]: ${body}")
                .to(apiBaseUrl + "?bridgeEndpoint=true&httpMethod=POST")
                .process(exchange -> new C2BResponseProcessor().process(exchange))
                .log("C2B Payment Response [Mpesa Congo]: ${body}")
                .to("direct:congo-mpesa-transaction-response")
                .end();

        from("direct:congo-mpesa-transaction-response")
                .routeId("congo-mpesa-transaction-response-route")
                .process(exchange -> {
                    this.depositService.findByReference(exchange.getProperty("reference", String.class)).ifPresent(deposit -> {
                        PushUssd pushUssd = PushUssd.builder()
                                .status(exchange.getMessage().getBody(C2BPaymentResponse.class).getResponseCode())
                                .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                                .message(exchange.getMessage().getBody(C2BPaymentResponse.class).getResponseCode().equals("0") ? "Ussd Push Initiated Successfully" : "Ussd Push Initiated Failed")
                                .reference(exchange.getMessage().getBody(C2BPaymentResponse.class).getThirdPartyReference())
                                .currency(deposit.getCurrency())
                                .operator(this.mnoService.searchMno(deposit.getMsisdn()))
                                .transactionNumber(exchange.getMessage().getBody(C2BPaymentResponse.class).getInsightReference())
                                .accountId(String.valueOf(mainAccountService.findByVendorDetails(deposit.getVendorDetails()).getId()))
                                .vendorDetails(deposit.getVendorDetails())
                                .msisdn(deposit.getMsisdn())
                                .build();
                        this.pushUssdService.update(pushUssd);
                    });
                })
                .log("Waiting for callback for transaction: ${header.transactionId}")
                .end();


    }

    private static class LoginResponseProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            String xmlResponse = exchange.getMessage().getBody(String.class);
            System.out.println("Login Response: " + xmlResponse);
            LoginResponse response = new LoginResponse();

            // Define XPath expressions with namespaces
            XPathBuilder sessionIdXPath = XPathBuilder.xpath("//response/dataItem[name='SessionID']/value/text()")
                    .namespace("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

            XPathBuilder codeXPath = XPathBuilder.xpath("//eventInfo/code/text()");
            XPathBuilder descXPath = XPathBuilder.xpath("//eventInfo/description/text()");
            XPathBuilder detailXPath = XPathBuilder.xpath("//eventInfo/detail/text()");
            XPathBuilder transactionIdXPath = XPathBuilder.xpath("//eventInfo/transactionID/text()");

            // Extract values using XPath
            response.setSessionId(sessionIdXPath.evaluate(exchange.getContext(), xmlResponse, String.class));
            response.setCode(codeXPath.evaluate(exchange.getContext(), xmlResponse, String.class));
            response.setDescription(descXPath.evaluate(exchange.getContext(), xmlResponse, String.class));
            response.setDetail(detailXPath.evaluate(exchange.getContext(), xmlResponse, String.class));
            response.setTransactionId(transactionIdXPath.evaluate(exchange.getContext(), xmlResponse, String.class));

            exchange.getMessage().setHeader("LoginCode", response.getCode());
            exchange.getMessage().setHeader("LoginDetail", response.getDetail());
            exchange.getMessage().setBody(response);
        }
    }

    private class C2BResponseProcessor implements Processor {
        @Override
        public void process(Exchange exchange) throws Exception {
            String xmlResponse = exchange.getMessage().getBody(String.class);
            System.out.println("C2B Response [Mpesa Congo] >>>>>>>> : " + xmlResponse);

            C2BPaymentResponse response = new C2BPaymentResponse();

            try {
                // Create DocumentBuilder
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true); // Enable namespace support
                DocumentBuilder builder = factory.newDocumentBuilder();

                // Parse XML string to Document using InputSource
                InputSource is = new InputSource(new StringReader(xmlResponse));
                Document document = builder.parse(is);

                // Create XPath
                XPath xpath = XPathFactory.newInstance().newXPath();

                // Configure namespaces
                SimpleNamespaceContext namespaces = new SimpleNamespaceContext();
                Map<String, String> nsMap = new HashMap<>();
                nsMap.put("S", "http://schemas.xmlsoap.org/soap/envelope/");
                nsMap.put("ns2", "http://www.4cgroup.co.za/genericsoap");
                nsMap.put("ns3", "http://www.4cgroup.co.za/soapauth");
                namespaces.setBindings(nsMap);
                xpath.setNamespaceContext(namespaces);

                // Define XPath expressions
                String insightRefExp = "//ns2:getGenericResultResponse/SOAPAPIResult/response/dataItem[name='InsightReference']/value/text()";
                String responseCodeExp = "//ns2:getGenericResultResponse/SOAPAPIResult/response/dataItem[name='ResponseCode']/value/text()";
                String msisdnExp = "//ns2:getGenericResultResponse/SOAPAPIResult/response/dataItem[name='CustomerMSISDN']/value/text()";
                String transactionIDExp = "//ns2:getGenericResultResponse/SOAPAPIResult/eventInfo/transactionID/text()";
                String thirdPartyReferenceExp = "//ns2:getGenericResultResponse/SOAPAPIResult/request/dataItem[name='ThirdPartyReference']/value/text()";

                // Extract values
                String insightRef = evaluateXPath(xpath, document, insightRefExp);
                String responseCode = evaluateXPath(xpath, document, responseCodeExp);
                String msisdn = evaluateXPath(xpath, document, msisdnExp);
                String transactionId = evaluateXPath(xpath, document, transactionIDExp);
                String thirdPartyReference = evaluateXPath(xpath, document, thirdPartyReferenceExp);

                // Set values in response object
                response.setInsightReference(insightRef);
                response.setResponseCode(responseCode);
                response.setCustomerMsisdn(msisdn);
                response.setThirdPartyReference(thirdPartyReference);

                // Set the response in the exchange body
                exchange.getMessage().setBody(response);

                // Set reference header for use in subsequent route
                exchange.getMessage().setHeader("reference", response.getThirdPartyReference());

            } catch (Exception e) {
                log.error("Error processing C2B response: " + e.getMessage(), e);
                // Set default values in case of parsing error
                response.setResponseCode("-10"); // Indicate error
                response.setInsightReference("ERROR");
                response.setCustomerMsisdn(exchange.getProperty("msisdn", String.class));
                response.setThirdPartyReference(exchange.getProperty("reference", String.class));
                exchange.getMessage().setBody(response);
            }
        }

        private String evaluateXPath(XPath xpath, Document document, String expression) {
            try {
                return xpath.evaluate(expression, document);
            } catch (XPathExpressionException e) {
                log.warn("Error evaluating XPath expression {}: {}", expression, e.getMessage());
                return null;
            }
        }

        // Inner class for namespace context
        private static class SimpleNamespaceContext implements NamespaceContext {
            private final Map<String, String> namespaces = new HashMap<>();

            public void setBindings(Map<String, String> bindings) {
                namespaces.putAll(bindings);
            }

            @Override
            public String getNamespaceURI(String prefix) {
                return namespaces.get(prefix);
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return namespaces.entrySet().stream()
                        .filter(e -> e.getValue().equals(namespaceURI))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return namespaces.entrySet().stream()
                        .filter(e -> e.getValue().equals(namespaceURI))
                        .map(Map.Entry::getKey)
                        .iterator();
            }
        }
    }

    private String createLoginSoapRequest(LoginRequest request) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                 xmlns:soap="http://www.4cgroup.co.za/soapauth"
                                 xmlns:gen="http://www.4cgroup.co.za/genericsoap">
                    <soapenv:Header>
                        <soap:EventID>%s</soap:EventID>
                    </soapenv:Header>
                    <soapenv:Body>
                        <gen:getGenericResult>
                            <Request>
                                <dataItem>
                                    <name>Username</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem>
                                    <name>Password</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                            </Request>
                        </gen:getGenericResult>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(eventId, request.getUsername(), request.getPassword());
    }

    private String createC2BPaymentSoapRequest(C2BPaymentRequest request, String token) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                 xmlns:soap="http://www.4cgroup.co.za/soapauth"
                                 xmlns:gen="http://www.4cgroup.co.za/genericsoap">
                    <soapenv:Header>
                        <soap:Token>%s</soap:Token>
                        <soap:EventID>%s</soap:EventID>
                    </soapenv:Header>
                    <soapenv:Body>
                        <gen:getGenericResult>
                            <Request>
                                <dataItem>
                                    <name>CustomerMSISDN</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem>
                                    <name>ServiceProviderCode</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem>
                                    <name>Currency</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem>
                                    <name>Amount</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem>
                                 <name>Date</name>
                                 <type>String</type>
                                 <value>%s</value>
                                </dataItem>
                                <dataItem>
                                 <name>ThirdPartyReference</name>
                                 <type>String</type>
                                 <value>%s</value>
                                </dataItem>
                                <dataItem>
                                    <name>CommandId</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem>
                                    <name>Language</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem>
                                    <name>CallBackChannel</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem>
                                    <name>CallBackDestination</name>
                                    <type>String</type>
                                    <value>%s</value>
                                </dataItem>
                                <dataItem> 
                                 <name>Surname</name> 
                                 <type>String</type> 
                                 <value>Surname</value> 
                                </dataItem> 
                                <dataItem>
                                 <name>Initials</name> 
                                 <type>String</type> 
                                 <value>Initials</value>
                                </dataItem>
                            </Request>
                        </gen:getGenericResult>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                token,
                eventIdc2bPayment,
                request.getCustomerMsisdn(),
                request.getServiceProviderCode(),
                request.getCurrency(),
                request.getAmount(),
                request.getDate(),
                request.getThirdPartyReference(),
                request.getCommandId(),
                request.getLanguage(),
                request.getCallBackChannel(),
                request.getCallBackDestination()
        );
    }
}
