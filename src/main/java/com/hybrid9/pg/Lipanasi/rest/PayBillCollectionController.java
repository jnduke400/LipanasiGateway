package com.hybrid9.pg.Lipanasi.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.component.airtelmoney.paybill.XmlRequestParser;
import com.hybrid9.pg.Lipanasi.component.airtelmoney.paybill.XmlRequestParserThree;
import com.hybrid9.pg.Lipanasi.component.airtelmoney.paybill.XmlRequestParserTwo;
import com.hybrid9.pg.Lipanasi.component.halopesac2breq.PaymentPayload;
import com.hybrid9.pg.Lipanasi.component.halopesac2bresponse.Body;
import com.hybrid9.pg.Lipanasi.component.halopesac2bresponse.Header;
import com.hybrid9.pg.Lipanasi.component.halopesac2bresponse.PaymentResponse;
import com.hybrid9.pg.Lipanasi.component.halopesac2bresponse.Response;
import com.hybrid9.pg.Lipanasi.component.mixxbyyas.MixxXmlRequestParser;
import com.hybrid9.pg.Lipanasi.component.mixxbyyas.MixxXmlResponseParser;
import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.PayBillRequestDTO;
import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2.*;
import com.hybrid9.pg.Lipanasi.dto.mixxpaybill.BillPayRequest;
import com.hybrid9.pg.Lipanasi.dto.mixxpaybill.v2.SyncBillPayRequest;
import com.hybrid9.pg.Lipanasi.dto.mixxpaybill.v2.SyncBillPayResponse;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.MpesaBrokerRequest;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.MpesaBroker;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.resources.PaybillResource;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.APProcessPaymentService;
import com.hybrid9.pg.Lipanasi.services.APValidationService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.paybill.MixxPayBillService;
import com.hybrid9.pg.Lipanasi.services.paybill.MpesaService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/pay/bill")
public class PayBillCollectionController {
    @Value("${payment-gateway.halopesa.api-url}")
    private String apiUrl;
    @Value("${payment-gateway.halopesa.sp-id}")
    private String spId;
    @Value("${payment-gateway.halopesa.secret-key}")
    private String secretKey;
    @Value("${payment-gateway.halopesa.merchant-code}")
    private String merchantCode;


    private ProducerTemplate payBillTemplate;

    private final PaymentUtilities paymentUtilities;
    private final PayBillPaymentService payBillPaymentService;
    private final VendorService vendorService;
    private final MnoServiceImpl mnoService;
    private final MainAccountService mainAccountService;
    private final PaybillResource paybillResource;
    private final APValidationService validationService;

    private final XmlRequestParser xmlRequestParser;
    private final APProcessPaymentService apProcessPaymentService;
    private final XmlRequestParserTwo xmlRequestParserTwo;
    private final XmlRequestParserThree xmlRequestParserThree;
    private final MixxXmlResponseParser mixxXmlResponseParser;
    private final MixxXmlRequestParser mixxXmlRequestParser;
    private final MixxPayBillService mixxPayBillService;
    private final MpesaService mpesaService;

    public PayBillCollectionController(PaymentUtilities paymentUtilities, PayBillPaymentService payBillPaymentService, VendorService vendorService, MnoServiceImpl mnoService, MainAccountService mainAccountService, PaybillResource paybillResource, APValidationService validationService, XmlRequestParser xmlRequestParser, APProcessPaymentService apProcessPaymentService
            , XmlRequestParserTwo xmlRequestParserTwo, XmlRequestParserThree xmlRequestParserThree, MixxXmlResponseParser mixxXmlResponseParser, MixxXmlRequestParser mixxXmlRequestParser,
                                       MixxPayBillService mixxPayBillService, MpesaService mpesaService, ProducerTemplate payBillTemplate) {
        this.paymentUtilities = paymentUtilities;
        this.payBillPaymentService = payBillPaymentService;
        this.vendorService = vendorService;
        this.mnoService = mnoService;
        this.mainAccountService = mainAccountService;
        this.paybillResource = paybillResource;
        this.validationService = validationService;
        this.xmlRequestParser = xmlRequestParser;
        this.apProcessPaymentService = apProcessPaymentService;
        this.xmlRequestParserTwo = xmlRequestParserTwo;
        this.xmlRequestParserThree = xmlRequestParserThree;
        this.mixxXmlResponseParser = mixxXmlResponseParser;
        this.mixxXmlRequestParser = mixxXmlRequestParser;
        this.mixxPayBillService = mixxPayBillService;
        this.mpesaService = mpesaService;
        this.payBillTemplate = payBillTemplate;
    }

    //START UAT ENDPOINTS

    //validate transaction
    /*@PostMapping(value = "/airtelMoney/validate/",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public APResponse validateTransactionUAT(@RequestBody String xmlRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof AnonymousAuthenticationToken)) {
                log.info("AirtelMoney Validation Payload is {}", xmlRequest);

                // Parse the XML request string to Request object
                PayBillValidationRequest request = xmlRequestParser.parseXmlRequest(xmlRequest, PayBillValidationRequest.class);
                log.info("AirtelMoney Parsed validation request object: {}", request);

                return validationService.validateTransactionUAT(request);
            }else{
                APResponse errorResponse = new APResponse();
                errorResponse.setStatus("400");
                errorResponse.setMessage("Unauthorized");
                return errorResponse;
            }
        } catch (JAXBException e) {
            log.error("Error processing XML request: ", e);
            APResponse errorResponse = new APResponse();
            errorResponse.setStatus("400");
            errorResponse.setMessage("Invalid XML format: " + e.getMessage());
            return errorResponse;
        }
    }

    // Process Payment
    @PostMapping(value = "/airtelMoney/payment/",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public APPaymentResponse processPaymentUAT(@RequestBody String xmlRequest) {
        try {
            log.info("AirtelMoney Payment Payload is {}", xmlRequest);

            // Parse the XML request string to Request object
            PayBillPaymentRequest request = xmlRequestParserTwo.parseXmlRequest(xmlRequest, PayBillPaymentRequest.class);
            log.info("AirtelMoney Parsed payment request object: {}", request);

            return apProcessPaymentService.validateAndProcessPaymentUAT(request);
        } catch (JAXBException e) {
            log.error("Error processing XML request: ", e);
            APPaymentResponse errorResponse = new APPaymentResponse();
            errorResponse.setStatus("400");
            errorResponse.setMessage("Invalid XML format: " + e.getMessage());
            return errorResponse;
        }
    }


    // Enquiry Payment
    @PostMapping(value = "/airtelMoney/enquiry/",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public EnquiryResponse enquiryUAT(@RequestBody String xmlRequest) {
        try {
            log.info("AirtelMoney Enquiry Payload is {}", xmlRequest);

            // Parse the XML request string to Request object
            EnquiryRequest request = xmlRequestParserThree.parseXmlRequest(xmlRequest, EnquiryRequest.class);
            log.info("AirtelMoney Parsed enquiry request object: {}", request);

            return apProcessPaymentService.enquiryTransaction(request);
        } catch (JAXBException e) {
            log.error("Error processing XML request: ", e);
            EnquiryResponse errorResponse = new EnquiryResponse();
            errorResponse.setStatus("400");
            errorResponse.setMessage("Invalid XML format: " + e.getMessage());
            return errorResponse;
        }
    }*/


    //END UAT ENDPOINTS

    // START PROD ENDPOINTS

    //validate transaction
    @PostMapping(value = "/airtelMoney/validate/prod/",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public APResponse validateTransaction(@RequestBody String xmlRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof AnonymousAuthenticationToken)) {
                log.info("AirtelMoney Validation Payload is {}", xmlRequest);

                // Parse the XML request string to Request object
                PayBillValidationRequest request = xmlRequestParser.parseXmlRequest(xmlRequest, PayBillValidationRequest.class);
                log.info("AirtelMoney Parsed validation request object: {}", request);

                return validationService.validateTransaction(request);
            }else{
                APResponse errorResponse = new APResponse();
                errorResponse.setStatus("400");
                errorResponse.setMessage("Unauthorized");
                return errorResponse;
            }
        } catch (JAXBException e) {
            log.error("Error processing XML request: ", e);
            APResponse errorResponse = new APResponse();
            errorResponse.setStatus("400");
            errorResponse.setMessage("Invalid XML format: " + e.getMessage());
            return errorResponse;
        }
    }

    // Process Payment
    @PostMapping(value = "/airtelMoney/payment/prod/",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public APPaymentResponse processPayment(@RequestBody String xmlRequest) {
        try {
            log.info("AirtelMoney Payment Payload is {}", xmlRequest);

            // Parse the XML request string to Request object
            PayBillPaymentRequest request = xmlRequestParserTwo.parseXmlRequest(xmlRequest, PayBillPaymentRequest.class);
            log.info("AirtelMoney Parsed payment request object: {}", request);

            return apProcessPaymentService.validateAndProcessPayment(request);
        } catch (JAXBException e) {
            log.error("Error processing XML request: ", e);
            APPaymentResponse errorResponse = new APPaymentResponse();
            errorResponse.setStatus("400");
            errorResponse.setMessage("Invalid XML format: " + e.getMessage());
            return errorResponse;
        }
    }

    // Enquiry Payment
    @PostMapping(value = "/airtelMoney/enquiry/prod/",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public EnquiryResponse enquiry(@RequestBody String xmlRequest) {
        try {
            log.info("AirtelMoney Enquiry Payload is {}", xmlRequest);

            // Parse the XML request string to Request object
            EnquiryRequest request = xmlRequestParserThree.parseXmlRequest(xmlRequest, EnquiryRequest.class);
            log.info("AirtelMoney Parsed enquiry request object: {}", request);

            return apProcessPaymentService.enquiryTransaction(request);
        } catch (JAXBException e) {
            log.error("Error processing XML request: ", e);
            EnquiryResponse errorResponse = new EnquiryResponse();
            errorResponse.setStatus("400");
            errorResponse.setMessage("Invalid XML format: " + e.getMessage());
            return errorResponse;
        }
    }

    //get paybill payment request for aitelMoney
    @RequestMapping(value = "/aitelMoney/payment/v1/", method = RequestMethod.POST)
    public ResponseEntity<String> getPayBillUssdRequestAitelMoney(@RequestBody String paymentPayload) throws NoSuchAlgorithmException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        log.info("Payment Payload for AitelMoney Payment [pay bill] is {}", paymentPayload);
        PayBillPayment response = null;
        try {
            // Extract payload from SOAP envelope
            PayBillRequestDTO request = this.paybillResource.extractPayloadFromSoap(paymentPayload);
            // Process the payment
            PayBillPayment payment = paybillResource.processPayment(request, payBillPaymentService, mnoService, paymentUtilities, mainAccountService, vendorService);
            if (payment != null) {
                return ResponseEntity.ok("Payment Successful");
            } else {
                return ResponseEntity.badRequest().body("Payment Failed");
            }
        } catch (PaybillResource.ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing payment", e);
            return ResponseEntity.internalServerError().body("Error processing payment");
        }
    }

    // Mpesa Pay Bill Payment V2
    @PostMapping(value = "/mpesa/payment/prod/",
            consumes = MediaType.TEXT_XML_VALUE,
            produces = MediaType.TEXT_XML_VALUE)
    public MpesaBroker handleC2BTransaction(@RequestBody MpesaBroker request) {
        log.info("Mpesa Pay Bill Payment V2 request received"+request.toString());
        try {
            return mpesaService.processC2BTransaction(request);
        } catch (PaybillResource.ValidationException e) {
            logMessage(e);
            sendCallback(e, request);
            return getMpesaBroker(request);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error processing Mpesa [pay bill] request", e.getMessage());
            sendCallbackV2(e, request);
            return getMpesaBroker(request);
        }
    }

    private void sendCallback(PaybillResource.ValidationException e, MpesaBroker request) {
        JSONObject json = new JSONObject();
        json.put("isDuplicate", "false");
        json.put("TransactionStatus", "failed");
        json.put("ErrorMessage", "failure, " + e.getMessage());
        json.put("transactionId", request.getRequest().getTransaction().getTransactionID());
        json.put("amount", request.getRequest().getTransaction().getAmount());
        json.put("initiator", request.getRequest().getTransaction().getInitiator());
        json.put("accountReference", request.getRequest().getTransaction().getAccountReference());
        json.put("conversationId", request.getRequest().getTransaction().getConversationID());
        json.put("originatorConversationId", request.getRequest().getTransaction().getOriginatorConversationID());
        json.put("mpesaReceipt", request.getRequest().getTransaction().getMpesaReceipt());
        payBillTemplate.sendBody("direct:mpesa-paybill-callback", json.toString());
    }

    private void sendCallbackV2(Exception e, MpesaBroker request) {
        JSONObject json = new JSONObject();
        json.put("isDuplicate", "false");
        json.put("TransactionStatus", "failed");
        json.put("ErrorMessage", "failure, " + e.getMessage());
        json.put("transactionId", request.getRequest().getTransaction().getTransactionID());
        json.put("amount", request.getRequest().getTransaction().getAmount());
        json.put("initiator", request.getRequest().getTransaction().getInitiator());
        json.put("accountReference", request.getRequest().getTransaction().getAccountReference());
        json.put("conversationId", request.getRequest().getTransaction().getConversationID());
        json.put("originatorConversationId", request.getRequest().getTransaction().getOriginatorConversationID());
        json.put("mpesaReceipt", request.getRequest().getTransaction().getMpesaReceipt());
        payBillTemplate.sendBody("direct:mpesa-paybill-callback", json.toString());
    }

    private static void logMessage(PaybillResource.ValidationException e) {
        log.error("Error processing Mpesa [pay bill] request", e.getMessage());
    }

    private static MpesaBroker getMpesaBroker(MpesaBroker request) {
        MpesaBroker response = new MpesaBroker();
        com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.Response resp = new com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.Response();
        resp.setConversationID(request.getRequest().getTransaction().getConversationID());
        resp.setOriginatorConversationID(request.getRequest().getTransaction().getOriginatorConversationID());
        resp.setTransactionID(request.getRequest().getTransaction().getTransactionID());
        resp.setResponseCode("0");
        resp.setResponseDesc("Received");
        resp.setServiceStatus("Success");
        response.setResponse(resp);
        return response;
    }

    //get paybill payment request for mpesa
    @RequestMapping(value = "/mpesa/payment/", method = RequestMethod.POST)
    public ResponseEntity<String> getPayBillUssdRequestMpesa(@RequestBody MpesaBrokerRequest paymentPayload) throws NoSuchAlgorithmException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        log.info("Payment Payload for Mpesa Payment [pay bill] is {}", mapper.writeValueAsString(paymentPayload));
        PaymentResponse response = null;
        try {
            PayBillPayment payment = paybillResource.processPayment(paymentPayload, payBillPaymentService, mnoService, paymentUtilities, mainAccountService, vendorService);
            if (payment != null) {
                return ResponseEntity.ok("Payment Successful");
            } else {
                return ResponseEntity.badRequest().body("Payment Failed");
            }
        } catch (PaybillResource.ValidationException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error processing payment", e);
            return ResponseEntity.badRequest().body("Error processing payment");
        }
    }

    /*@ExceptionHandler(PaybillResource.InvalidPaymentRequestException.class)
    public ResponseEntity<Map<String, String>> handleInvalidPaymentRequest(PaybillResource.InvalidPaymentRequestException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }*/

    @PostMapping(
            path = "/mixx_by_yas/payment/",
            consumes = MediaType.TEXT_XML_VALUE,
            produces = MediaType.TEXT_XML_VALUE
    )
    public SyncBillPayResponse processBillPay(
            @RequestBody String xmlRequest
    ) {
        /*SyncBillPayResponse response = w2aService.processW2ATransaction(request);
        return ResponseEntity.ok(response);*/
        try {
            log.info("Mixx By Yas [Pay Bill] Payment Payload is {}", xmlRequest);

            // Parse the XML request string to Request object
            SyncBillPayRequest request = mixxXmlRequestParser.parseXmlRequest(xmlRequest, SyncBillPayRequest.class);
            log.info("Mixx By Yas [Pay Bill] Parsed payment request object: {}", request);

            return mixxPayBillService.validateAndProcessPayment(request);
        } catch (JAXBException e) {
            log.error("Error processing XML request: ", e);
            String uniqueId = UUID.randomUUID().toString();
            SyncBillPayResponse errorResponse = new SyncBillPayResponse();
            errorResponse.setType("SYNC_BILLPAY_RESPONSE");
            errorResponse.setTxnId("");
            errorResponse.setRefId(uniqueId);
            errorResponse.setResult("TF");
            errorResponse.setErrorCode("error113");
            errorResponse.setErrorDesc("Invalid XML format");
            errorResponse.setMsisdn("");
            errorResponse.setFlag("N");
            errorResponse.setContent("Xml format error: " + e.getMessage());
            return errorResponse;
        }
    }

    //get paybill payment request for mixx_by_yas
    @RequestMapping(value = "/mixx_by_yas/payment/old/", method = RequestMethod.POST)
    public ResponseEntity<String> getPayBillUssdRequestMixxByYas(@RequestBody String paymentPayload) throws NoSuchAlgorithmException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        log.info("Received XML body: {}", paymentPayload);
        PayBillPayment response = null;
        try {
            String jsonPaymentPayload = PaybillResource.convertXmlToJson(paymentPayload.trim());
            BillPayRequest payRequest = mapper.readValue(jsonPaymentPayload, BillPayRequest.class);
            log.info("Payment Payload for Mixx By Yas Payment [pay bill] is: {}", paymentPayload);
            paybillResource.validateMixxRequest(payRequest);
            PayBillPayment payment = paybillResource.processPayment(payRequest, payBillPaymentService, mnoService, paymentUtilities, vendorService, mainAccountService);
            if (payment != null) {
                return ResponseEntity.ok("Payment Successful");
            } else {
                return ResponseEntity.badRequest().body("Payment Failed");
            }
        } catch (PaybillResource.InvalidRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payment");
        }

    }

    //

    //get paybill payment request for halopesa
    @RequestMapping(value = "/halopesa/payment/", method = RequestMethod.POST)
    public PaymentResponse getPayBillUssdRequest(@RequestBody PaymentPayload paymentPayload) throws NoSuchAlgorithmException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        log.info("Payment Payload for Halopesa Payment [pay bill] is {}", mapper.writeValueAsString(paymentPayload));
        PaymentResponse response = null;

        try {
            // Create timestamp
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String amount = String.valueOf(paymentPayload.getBody().getRequest().getAmount());
            String msisdn = paymentPayload.getBody().getRequest().getMsisdn();

            // Generate spPassword
            String rawPassword = spId + secretKey + timestamp + amount.replaceAll("\\.\\d+", "") + msisdn;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes());
            String spPassword = Base64.encodeBase64String(hash);

            if (paymentPayload == null) {

                response = PaymentResponse.builder()
                        .header(Header.builder()
                                .merchantCode(merchantCode)
                                .spId(spId)
                                .spPassword(spPassword)
                                .timestamp(timestamp)
                                .build())
                        .body(Body.builder()
                                .response(Response.builder()
                                        .apiVersion("5.0")
                                        .gatewayId(Long.parseLong(String.valueOf(this.paymentUtilities.genRandam(100000000, 999999999))))
                                        .responseCode("999")
                                        .responseStatus("Transaction processing failed, not Identified Request")
                                        .reference(null)
                                        .build())
                                .build())
                        .build();
                log.info("Halopesa Payment Gateway Response: {}", response);
                return response;
            } else if (!paymentPayload.getHeader().getSpId().equals(spId) || !paymentPayload.getHeader().getMerchantCode().equals(merchantCode)) {
                response = PaymentResponse.builder()
                        .header(Header.builder()
                                .merchantCode(merchantCode)
                                .spId(spId)
                                .spPassword(spPassword)
                                .timestamp(timestamp)
                                .build())
                        .body(Body.builder()
                                .response(Response.builder()
                                        .apiVersion("5.0")
                                        .gatewayId(Long.parseLong(paymentPayload.getBody().getRequest().getGatewayId()))
                                        .responseCode("999")
                                        .responseStatus("Transaction processing failed, spId or merchantCode not match")
                                        .reference(paymentPayload.getBody().getRequest().getReference())
                                        .build())
                                .build())
                        .build();
                log.info("Halopesa Payment Gateway Response: {}", response);
                return response;
            }
            PayBillPayment payBillPayment = PayBillPayment.builder()
                    .amount(Float.parseFloat(String.valueOf(paymentPayload.getBody().getRequest().getAmount())))
                    .msisdn(paymentUtilities.formatPhoneNumber("255", paymentPayload.getBody().getRequest().getMsisdn()))
                    .payBillId(paymentPayload.getBody().getRequest().getGatewayId())
                    .collectionStatus(CollectionStatus.COLLECTED)
                    .receiptNumber(paymentPayload.getBody().getRequest().getReceiptNumber())
                    .paymentReference(paymentUtilities.generateRefNumber())
                    .originalReference(paymentPayload.getBody().getRequest().getReference())
                    .vendorDetails(this.vendorService.findVendorDetailsByCode("SC001").orElseThrow())
                    .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", paymentPayload.getBody().getRequest().getMsisdn())))
                    .accountId(String.valueOf(this.mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                    .currency("TZS")
                    .build();
            payBillPaymentService.createPayBill(payBillPayment);
            response = PaymentResponse.builder()
                    .header(Header.builder()
                            .merchantCode(merchantCode)
                            .spId(spId)
                            .spPassword(spPassword)
                            .timestamp(timestamp)
                            .build())
                    .body(Body.builder()
                            .response(Response.builder()
                                    .apiVersion("5.0")
                                    .gatewayId(Long.parseLong(paymentPayload.getBody().getRequest().getGatewayId()))
                                    .responseCode("0")
                                    .responseStatus("Transaction Request Processed Successfully")
                                    .reference(paymentPayload.getBody().getRequest().getReference())
                                    .build())
                            .build())
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Failed to create Pay Bill, {}", e.getMessage());
            // Create timestamp
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String amount = String.valueOf(paymentPayload.getBody().getRequest().getAmount());
            String msisdn = paymentPayload.getBody().getRequest().getMsisdn();

            // Generate spPassword
            String rawPassword = spId + secretKey + timestamp + amount.replace(".", "") + msisdn;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes());
            String spPassword = Base64.encodeBase64String(hash);

            //response = new ResponseMessage("Internal error occured", "500",HttpStatus.INTERNAL_SERVER_ERROR.name());
            response = PaymentResponse.builder()
                    .header(Header.builder()
                            .merchantCode(merchantCode)
                            .spId(spId)
                            .spPassword(spPassword)
                            .timestamp(timestamp)
                            .build())
                    .body(Body.builder()
                            .response(Response.builder()
                                    .apiVersion("5.0")
                                    .gatewayId(Long.parseLong(paymentPayload.getBody().getRequest().getGatewayId()))
                                    .responseCode("999")
                                    .responseStatus("Transaction processing failed, internal error occured")
                                    .reference(paymentPayload.getBody().getRequest().getReference())
                                    .build())
                            .build())
                    .build();
            log.info("Halopesa Payment Gateway Response: {}", response);
            return response;
        }
        log.info("Halopesa Payment Gateway Response: {}", response);
        return response;
    }

    //init payment
    @RequestMapping(value = "init")
    public String initPayBillUssd(String ussdStr) {
        return null;
    }
}
