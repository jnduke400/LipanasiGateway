package com.hybrid9.pg.Lipanasi.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.AirtelMoneyCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.tz.AirtelMoneyTzCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.mixxbyyas.CallbackRequest;
import com.hybrid9.pg.Lipanasi.dto.mixxbyyas.MixxCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.orange.callback.OrangeCallbackRequest;
import com.hybrid9.pg.Lipanasi.events.PaymentEventPublisher;
import com.hybrid9.pg.Lipanasi.resources.MpesaCongoCallbackResource;
import com.hybrid9.pg.Lipanasi.resources.PushUssdResource;
import com.hybrid9.pg.Lipanasi.resources.threads.ThreadsProcessor;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdCallbackService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
//import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/cngtz/collection")
public class CongoTzCollectionController {
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
    private final ThreadsProcessor threadsProcessor;

    public CongoTzCollectionController(PushUssdResource pushUssdResource, PushUssdService pushUssdService,
                                       PushUssdRefService pushUssdRefService, PushUssdCallbackService ussdCallbackService,
                                       PaymentEventPublisher publisher, PaymentUtilities paymentUtilities, MnoServiceImpl mnoService,
                                       PayBillPaymentService payBillPaymentService, VendorService vendorService, MainAccountService mainAccountService,
                                       ObjectMapper objectMapper, DepositService depositService,ThreadsProcessor threadsProcessor) {
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
        this.threadsProcessor = threadsProcessor;
    }

    @PostMapping("/airtelmoney-congo/payment/")
    public ResponseEntity<String> handleAirtelMoeyCongoCallback(@RequestBody AirtelMoneyCallbackResponse callbackResponse) throws JsonProcessingException {
        //Start logging
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> referenceNumber = new AtomicReference<>();
        AtomicReference<String> originalReferenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(callbackResponse.getTransaction().getId()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
            originalReferenceNumber.set(deposit.getOriginalReference());
        });

        log.info("Transaction Id: {}, Reference: {}, OriginalReference: {}, Received Callback for AirtelMoney Congo: {}", callbackResponse.getTransaction().getId(), referenceNumber.get(), originalReferenceNumber.get(), mapper.writeValueAsString(callbackResponse));
        //End logging

        HttpStatus httpStatus = HttpStatus.OK;
        try {
            JSONObject result = this.airtelMoneyUssdCallback(callbackResponse);
            if (result.has("status") && result.get("status").toString().equalsIgnoreCase("success")) {
                // Return acknowledgment response as specified in documentation
                //response = createSuccessResponse(request.getReferenceID());
                return ResponseEntity.ok("success");
            } else {
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("failed");
                //response = createErrorResponse(result.get("message").toString(), request.getReferenceID());
            }

        } catch (CallbackProcessingException e) {
            log.error("Error processing callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("failed");
        }
    }

    @PostMapping("/mpesa-congo/payment/")
    public ResponseEntity<String> handleMpesaCongoCallback(@RequestBody String callbackResponse) throws JsonProcessingException {
        try {
            if (StringUtils.isEmpty(callbackResponse)) {
                return ResponseEntity.badRequest().body("Payload cannot be null or empty");
            }

            JSONObject result = new MpesaCongoCallbackResource().processCallback(callbackResponse, pushUssdRefService, pushUssdResource, pushUssdService, pushUssdRefService, ussdCallbackService, publisher, paymentUtilities, depositService);
            if (result.has("status") && result.get("status").toString().equalsIgnoreCase("success")) {
                // Return acknowledgment response as specified in documentation
                //response = createSuccessResponse(request.getReferenceID());
                return ResponseEntity.ok("success");
            } else {
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("failed");
                //response = createErrorResponse(result.get("message").toString(), request.getReferenceID());
            }
        } catch (Exception e) {
            log.error("Error processing callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing callback: " + e.getMessage());
        }
    }


    @PostMapping("/orange-congo/payment/")
    public ResponseEntity<String> handleOrangeCongoCallback(@RequestBody String callbackResponse) throws JsonProcessingException {
        HttpStatus httpStatus = HttpStatus.OK;
        try {
            // Create JAXB context and unmarshaller
            JAXBContext jaxbContext = JAXBContext.newInstance(OrangeCallbackRequest.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Convert XML string to CallbackRequest object
            java.io.StringReader reader = new java.io.StringReader(callbackResponse);
            OrangeCallbackRequest request = (OrangeCallbackRequest) unmarshaller.unmarshal(reader);

            // Process the request and save to database
            JSONObject result = this.processOrangeCallback(request, callbackResponse);
            if (result.has("status") && result.get("status").toString().equalsIgnoreCase("success")) {
                // Return acknowledgment response as specified in documentation
                //response = createSuccessResponse(request.getReferenceID());
                return ResponseEntity.ok("success");
            } else {
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("failed");
                //response = createErrorResponse(result.get("message").toString(), request.getReferenceID());
            }

        } catch (Exception e) {
            log.error("Error processing callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("failed");
        }
    }

    private JSONObject processOrangeCallback(OrangeCallbackRequest request, String callbackResponse) {
        if (request == null || request.getBody() == null ||
                request.getBody().getDoCallback() == null) {
            throw new IllegalArgumentException("Callback request cannot be null");
        }

        OrangeCallbackRequest.DoCallback callback = request.getBody().getDoCallback();

        //Start logging
        AtomicReference<String> transactionId = new AtomicReference<>();
        AtomicReference<String> originalReference = new AtomicReference<>();
        AtomicReference<String> reference = new AtomicReference<>();
        this.depositService.findByTransactionId(callback.getTransid()).ifPresent(deposit -> {
            transactionId.set(deposit.getTransactionId());
            originalReference.set(deposit.getOriginalReference());
            reference.set(deposit.getPaymentReference());
        });

        log.info("Transaction Id: {}, Reference: {}, OriginalReference: {}, Received Callback for Orange money congo: {}", transactionId.get(), reference.get(), originalReference.get(), callbackResponse);
        //End logging

        return orangeMoneyUssdCallback(callback);

    }


    @PostMapping(
            "/old/airtelmoney/payment"
    )
    @ResponseBody
    public ResponseEntity<String> handleUatCallback(@RequestBody String callbackResponse) throws JsonProcessingException {
        log.info("Received callback for Airtel Money Tz {}",callbackResponse);
       /* ObjectMapper mapper = new ObjectMapper();
        //log.info("Received callback for Airtel Money Tz {}",mapper.writeValueAsString(callbackResponse));
        //Start logging

        AtomicReference<String> referenceNumber = new AtomicReference<>();
        AtomicReference<String> originalReferenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(callbackResponse.getPayload().getTransaction().getId()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
            originalReferenceNumber.set(deposit.getOriginalReference());
        });

        log.info("Transaction Id: {}, Reference: {}, OriginalReference: {}, Received Callback for AirtelMoney Tz: {}", callbackResponse.getPayload().getTransaction().getId(), referenceNumber.get(), originalReferenceNumber.get(), mapper.writeValueAsString(callbackResponse));
        //End logging

        HttpStatus httpStatus = HttpStatus.OK;
        try {
            JSONObject result = this.airtelMoneyTzUssdCallback(callbackResponse);

            if (result.has("status") && result.get("status").toString().equalsIgnoreCase("success")) {
                // Return acknowledgment response as specified in documentation
                //response = createSuccessResponse(request.getReferenceID());
                return ResponseEntity.ok("success");
            } else {
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("failed");
                //response = createErrorResponse(result.get("message").toString(), request.getReferenceID());
            }

        } catch (CallbackProcessingException e) {
            log.error("Error processing callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("failed");
        }*/
        return null;

    }

    @PostMapping(
            "/prod/airtelmoney/payment"
    )
    @ResponseBody
    public ResponseEntity<String> handleCallback(@RequestBody AirtelMoneyCallbackResponse callbackResponse) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        //log.info("Received callback for Airtel Money Tz {}",mapper.writeValueAsString(callbackResponse));
        //Start logging

        AtomicReference<String> referenceNumber = new AtomicReference<>();
        AtomicReference<String> originalReferenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(callbackResponse.getTransaction().getId()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
            originalReferenceNumber.set(deposit.getOriginalReference());
        });

        log.info("Transaction Id: {}, Reference: {}, OriginalReference: {}, Received Callback for AirtelMoney Tz: {}", callbackResponse.getTransaction().getId(), referenceNumber.get(), originalReferenceNumber.get(), mapper.writeValueAsString(callbackResponse));
        //End logging

        HttpStatus httpStatus = HttpStatus.OK;
        try {
            JSONObject result = this.airtelMoneyUssdCallback(callbackResponse);
            if (result.has("status") && result.get("status").toString().equalsIgnoreCase("success")) {
                // Return acknowledgment response as specified in documentation
                //response = createSuccessResponse(request.getReferenceID());
                return ResponseEntity.ok("success");
            } else {
                return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("failed");
                //response = createErrorResponse(result.get("message").toString(), request.getReferenceID());
            }

        } catch (CallbackProcessingException e) {
            log.error("Error processing callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("failed");
        }

    }

    private JSONObject airtelMoneyUssdCallback(AirtelMoneyCallbackResponse ussdCallback) {
        JSONObject response = new JSONObject();
        try {
            Gson g = new Gson();

            response = this.threadsProcessor.processAirtelMoneyCallbackData(ussdCallback, null, response);
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


    private JSONObject airtelMoneyTzUssdCallback(AirtelMoneyTzCallbackResponse ussdCallback) {
        JSONObject response = new JSONObject();
        try {
            Gson g = new Gson();


            response = this.threadsProcessor.processAirtelMoneyTzCallbackData(ussdCallback, null, response);
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


    @PostMapping("/tigopesa/payment/")
    @ResponseBody
    public ResponseEntity<MixxCallbackResponse> handleCallback(@RequestBody CallbackRequest request) throws JsonProcessingException {
        log.info("Received callback for Mixx by yas: {}", request.toString());
        //Start logging
        ObjectMapper mapper = new ObjectMapper();

        AtomicReference<String> originalReference = new AtomicReference<>();
        Optional.ofNullable(this.pushUssdRefService.getRefByMappingRef(request.getReferenceId())).ifPresent(pushUssdRef -> originalReference.set(pushUssdRef.getReference()));

        log.info("Mfs Transaction Id: {}, Reference: {}, OriginalReference: {}, Received Callback for Mixx by yas: {}", request.getMfsTransactionId(), request.getReferenceId(), originalReference.get(), mapper.writeValueAsString(request));
        //End logging

        MixxCallbackResponse response = null;
        HttpStatus httpStatus = HttpStatus.OK;
        try {
            /*MixxCallbackResponse response = callbackService.processCallback(request);*/
            JSONObject result = this.tigopesaUssdCallback(request);
            if (result.has("status") && result.get("status").toString().equalsIgnoreCase("success")) {
                // Return acknowledgment response as specified in documentation
                response = createSuccessResponse(request.getReferenceId());

            } else {
                response = createErrorResponse(result.get("message").toString(), request.getReferenceId());
            }
            return ResponseEntity.ok(response);
        } catch (CallbackProcessingException e) {
            log.error("Error processing callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(e.getMessage(), request.getReferenceId()));
        }

    }

    private MixxCallbackResponse createErrorResponse(String message, String referenceId) {
        return MixxCallbackResponse.builder()
                .responseCode("BILLER-30-3030-E")
                .responseStatus(false)
                .responseDescription("Callback failed: " + message)
                .referenceID(referenceId)
                .build();
    }

    private MixxCallbackResponse createSuccessResponse(String referenceId) {
        return MixxCallbackResponse.builder()
                .responseCode("BILLER-30-0000-S")
                .responseStatus(true)
                .responseDescription("Callback successful")
                .referenceID(referenceId)
                .build();
    }

    // Custom Exceptions
    public class CallbackProcessingException extends RuntimeException {
        public CallbackProcessingException(String message) {
            super(message);
        }

        public CallbackProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public class CallbackValidationException extends RuntimeException {
        public CallbackValidationException(String message) {
            super(message);
        }
    }

    public class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String message) {
            super(message);
        }
    }

    private JSONObject tigopesaUssdCallback(CallbackRequest ussdCallback) {
        JSONObject response = new JSONObject();
        try {
            Gson g = new Gson();

            response = this.threadsProcessor.processTigopesaCallbackData(ussdCallback, null, response);
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


    private JSONObject orangeMoneyUssdCallback(OrangeCallbackRequest.DoCallback ussdCallback) {
        JSONObject response = new JSONObject();
        try {
            Gson g = new Gson();

            response = this.threadsProcessor.processOrangeMoneyCallbackData(ussdCallback, null, response);
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

}
