package com.hybrid9.pg.Lipanasi.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.component.halopesac2bresponse.Body;
import com.hybrid9.pg.Lipanasi.component.halopesac2bresponse.Header;
import com.hybrid9.pg.Lipanasi.component.halopesac2bresponse.PaymentResponse;
import com.hybrid9.pg.Lipanasi.component.halopesac2bresponse.Response;
import com.hybrid9.pg.Lipanasi.component.halotelcollection.request.CollectionRequestPayloadDTO;
import com.hybrid9.pg.Lipanasi.component.halotelcollection.response.TransactionResultPayloadDTO;
import com.hybrid9.pg.Lipanasi.dto.TransactionStatusDTO;
import com.hybrid9.pg.Lipanasi.dto.halopesa.HalopesaCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.halopesa.HalopesaResponse;
import com.hybrid9.pg.Lipanasi.dto.mpesa.CallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.mpesa.USSDCallback;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.events.PaymentEventPublisher;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.resources.PushUssdResource;
import com.hybrid9.pg.Lipanasi.resources.halopesa.HalopesaCallback;
import com.hybrid9.pg.Lipanasi.resources.threads.ThreadsProcessor;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.services.payments.TransactionStatusService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdCallbackService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
//import net.minidev.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/collection")
public class CollectionController {
    @Value("${payment-gateway.halopesa.api-url}")
    private String apiUrl;
    @Value("${payment-gateway.halopesa.sp-id}")
    private String spId;
    @Value("${payment-gateway.halopesa.secret-key}")
    private String secretKey;
    @Value("${payment-gateway.halopesa.merchant-code}")
    private String merchantCode;

    private final ObjectMapper objectMapper;

    private final PushUssdResource pushUssdResource;
    private final PushUssdService pushUssdService;
    private final PushUssdRefService pushUssdRefService;
    private final PushUssdCallbackService ussdCallbackService;
    private final PaymentEventPublisher publisher;
    private final PaymentUtilities paymentUtilities;
    private final MnoServiceImpl mnoService;

    private final PayBillPaymentService payBillPaymentService;
    private final VendorService vendorService;
    private final MainAccountService mainAccountService;
    private final DepositService depositService;

    private final TransactionStatusService transactionStatusService;

    private final ThreadsProcessor threadsProcessor;

    private final HalopesaCallback halopesaCallback;

    public CollectionController(PushUssdResource pushUssdResource, PushUssdService pushUssdService, PushUssdRefService pushUssdRefService,
                                PushUssdCallbackService ussdCallbackService, PaymentEventPublisher publisher, PaymentUtilities paymentUtilities,
                                MnoServiceImpl mnoService, PayBillPaymentService payBillPaymentService, VendorService vendorService,
                                MainAccountService mainAccountService, ObjectMapper objectMapper, DepositService depositService,
                                TransactionStatusService transactionStatusService, ThreadsProcessor threadsProcessor, HalopesaCallback halopesaCallback) {
        this.pushUssdResource = pushUssdResource;
        this.pushUssdService = pushUssdService;
        this.pushUssdRefService = pushUssdRefService;
        this.ussdCallbackService = ussdCallbackService;
        this.publisher = publisher;
        this.paymentUtilities = paymentUtilities;
        this.mnoService = mnoService;
        this.payBillPaymentService = payBillPaymentService;
        this.vendorService = vendorService;
        this.mainAccountService = mainAccountService;
        this.objectMapper = objectMapper;
        this.depositService = depositService;
        this.transactionStatusService = transactionStatusService;
        this.threadsProcessor = threadsProcessor;
        this.halopesaCallback = halopesaCallback;
    }

    @PostMapping("/mpesa/payment/")
    @ResponseBody
    public ResponseEntity<CallbackResponse> processMpesaPayment(@RequestBody String xmlPayload) {
        CallbackResponse response = null;
        HttpStatus httpStatus = HttpStatus.OK;
        try {
            // Create DOM parser
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse XML string to Document
            Document document = builder.parse(new InputSource(new StringReader(xmlPayload)));

            // Extract values from XML
            USSDCallback callback = extractCallbackData(document);

            //Start logging
            /*String originalReference = this.pushUssdRefService.getRefByMappingRef(callback.getThirdPartyReference()).getReference();*/
            AtomicReference<String> originalReference = new AtomicReference<>();
            Optional.ofNullable(this.pushUssdRefService.getRefByMappingRef(callback.getThirdPartyReference())).ifPresent(pushUssdRef -> originalReference.set(pushUssdRef.getReference()));

            log.info("Transaction Id: {}, Reference: {}, OriginalReference: {}, Received Mpesa Callback: {}", callback.getTransID(), callback.getThirdPartyReference(), originalReference.get(), xmlPayload);
            //End logging

            // Save to database
            JSONObject result = this.mpesaUssdCallback(callback);
            if (result.has("status") && result.get("status").toString().equalsIgnoreCase("success")) {
                // Return acknowledgment response as specified in documentation
                response = new CallbackResponse(
                        callback.getOriginatorConversationID(),
                        callback.getTransID(),
                        "0",  // ResponseCode for success
                        "Received"  // ResponseDesc for success
                );
            } else {
                // Return acknowledgment response as specified in documentation
                response = new CallbackResponse(
                        callback.getOriginatorConversationID(),
                        callback.getTransID(),
                        "-1",  // ResponseCode for failed
                        "Failed"  // ResponseDesc for failed
                );
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing callback: ", e);
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(httpStatus)
                    .body(new CallbackResponse(null, null, "-1", "Failed to process callback"));
        }

    }

    @GetMapping("/status/{originalReference}")
    public ResponseEntity<TransactionStatusDTO> checkTransactionStatus(
            @PathVariable String originalReference) {
        log.info("Checking transaction status for reference: {}", originalReference);
        TransactionStatusDTO status = transactionStatusService.checkTransactionStatus(originalReference);
        return ResponseEntity.ok(status);
    }

    /**
     * Processes the Halopesa callback payload.
     * @param rawPayload
     * @return
     */
    @PostMapping("/payment/halopesa")
    @ResponseBody
    public ResponseEntity<String> processPaymentHalopesa(@RequestBody String rawPayload) {
        log.debug("Received callback payload for Halopesa: >>>>>>>>>>>>>> " + rawPayload);
        try {
            if (StringUtils.isEmpty(rawPayload)) {
                return ResponseEntity.badRequest().body("Payload cannot be null or empty");
            }

            // Parse the callback payload
            HalopesaCallbackResponse callback = this.halopesaCallback.parseCallback(rawPayload);

            //Check if the callback is valid
            if (!callback.getResponseType().equalsIgnoreCase("FIN")) {
                return ResponseEntity.badRequest().body("Invalid callback payload");
            }

            JSONObject jsonObject = this.halopesaUssdCallback(callback);

            //Return the response
            return ResponseEntity.ok(jsonObject.toString());


        } catch (Exception e) {
            log.error("Error parsing callback payload", e);
            return ResponseEntity.badRequest().body("Invalid callback payload");
        }
    }


    @PostMapping("/payment")
    @ResponseBody
    public Object processPayment(@RequestBody String rawPayload) {
        log.debug("Received callback payload for Halopesa: >>>>>>>>>>>>>> " + rawPayload);
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Parse the raw JSON to determine the type
            JsonNode rootNode = objectMapper.readTree(rawPayload);
            JsonNode bodyNode = rootNode.get("body");

            // Check the payload type based on specific fields
            if (bodyNode.has("request")) {
                // This is a Collection Request
                CollectionRequestPayloadDTO collectionRequest = objectMapper.readValue(rawPayload, CollectionRequestPayloadDTO.class);
                return this.payBillRequest(collectionRequest);
            } else if (bodyNode.has("result")) {
                // This is a Transaction Result
                TransactionResultPayloadDTO transactionResult = objectMapper.readValue(rawPayload, TransactionResultPayloadDTO.class);
                return this.receiveUssdCallback(transactionResult);
            } else {
                return ResponseEntity.badRequest().body("Invalid payload format");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing request: " + e.getMessage());
        }
    }

    private PaymentResponse payBillRequest(CollectionRequestPayloadDTO paymentPayload) throws NoSuchAlgorithmException {
        PaymentResponse response = null;

        try {
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
                    .paymentReference(paymentPayload.getBody().getRequest().getReference())
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

    /**
     * Processes the Halopesa USSD callback
     *
     * @param callbackResponse The Halopesa callback response object
     * @return The response object
     */
    private JSONObject halopesaUssdCallback(HalopesaCallbackResponse callbackResponse) {
        JSONObject response = new JSONObject();
        try {
            response = threadsProcessor.processHalopesaCallbackData(callbackResponse, null, response);
            if (!response.has("status")) {
                response.put("status", "success");
                response.put("message", "Push Ussd updated successfully");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "failed");
            response.put("message", "Something went wrong, Operation failed");
        }
        return response;

    }


    private String receiveUssdCallback(TransactionResultPayloadDTO transactionResult) throws JsonProcessingException {

        //Start logging
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> transactionId = new AtomicReference<>();
        AtomicReference<String> originalReference = new AtomicReference<>();
        Optional.ofNullable(this.pushUssdRefService.getRefByMappingRef(transactionResult.getBody().getResult().getReferenceNumber())).ifPresent(pushUssdRef -> originalReference.set(pushUssdRef.getReference()));
        this.depositService.findByReference(transactionResult.getBody().getResult().getReferenceNumber()).ifPresent(deposit -> {
            transactionId.set(deposit.getTransactionId());
        });

        log.info("Transaction Id: {}, Reference: {}, OriginalReference: {}, Received Callback for Halopesa: {}", transactionId.get(), transactionResult.getBody().getResult().getReferenceNumber(), originalReference.get(), mapper.writeValueAsString(transactionResult));
        //End logging

        JSONObject response = new JSONObject();
        try {
            Gson g = new Gson();

            JsonObject jsonObject = new Gson().toJsonTree(transactionResult).getAsJsonObject();
            if (spId.equals(jsonObject.get("header").getAsJsonObject().get("spId").getAsString())) {
                //start processing
                response = threadsProcessor.processCallbackData(jsonObject, null, response);
                if (!response.has("status")) {
                    response.put("status", "success");
                    response.put("message", "Push Ussd updated successfully");
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "failed");
            response.put("message", "Something went wrong, Operation failed");
        }
        return response.toString();

    }

    private JSONObject mpesaUssdCallback(USSDCallback ussdCallback) {
        JSONObject response = new JSONObject();
        try {
            response = threadsProcessor.processMpesaCallbackData(ussdCallback, null, response);
            if (!response.has("status")) {
                response.put("status", "success");
                response.put("message", "Push Ussd updated successfully");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "failed");
            response.put("message", "Something went wrong, Operation failed");
        }
        return response;

    }


    private USSDCallback extractCallbackData(Document document) {
        USSDCallback callback = new USSDCallback();

        // Get all dataItem elements
        NodeList dataItems = document.getElementsByTagName("dataItem");

        // Iterate through dataItems and extract values
        for (int i = 0; i < dataItems.getLength(); i++) {
            Element dataItem = (Element) dataItems.item(i);
            String name = getElementValue(dataItem, "name");
            String value = getElementValue(dataItem, "value");

            switch (name) {
                case "ResultType":
                    callback.setResultType(value);
                    break;
                case "ResultCode":
                    callback.setResultCode(value);
                    break;
                case "ResultDesc":
                    callback.setResultDesc(value);
                    break;
                case "TransactionStatus":
                    callback.setTransactionStatus(value);
                    break;
                case "OriginatorConversationID":
                    callback.setOriginatorConversationID(value);
                    break;
                case "ConversationID":
                    callback.setConversationID(value);
                    break;
                case "TransID":
                    callback.setTransID(value);
                    break;
                case "BusinessNumber":
                    callback.setBusinessNumber(value);
                    break;
                case "Currency":
                    callback.setCurrency(value);
                    break;
                case "Amount":
                    callback.setAmount(value);
                    break;
                case "Date":
                    callback.setDate(value);
                    break;
                case "ThirdPartyReference":
                    callback.setThirdPartyReference(value);
                    break;
                case "InsightReference":
                    callback.setInsightReference(value);
                    break;
            }
        }

        return callback;
    }

    private String getElementValue(Element parent, String elementName) {
        NodeList nodeList = parent.getElementsByTagName(elementName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }


}
