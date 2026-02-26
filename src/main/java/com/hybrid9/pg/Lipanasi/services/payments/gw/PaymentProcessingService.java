package com.hybrid9.pg.Lipanasi.services.payments.gw;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.configs.gw.PaymentGatewayProperties;
import com.hybrid9.pg.Lipanasi.dto.commission.PaymentMethodConfig;
import com.hybrid9.pg.Lipanasi.dto.customer.CustomerDto;
import com.hybrid9.pg.Lipanasi.dto.order.OrderRequestDto;
import com.hybrid9.pg.Lipanasi.dto.order.VendorInfo;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.PaymentRequest;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.VerificationRequest;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.VerificationResponse;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoPrefix;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.BillingInformation;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.payments.gw.GatewayTransaction;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.enums.ListOfErrorCode;
import com.hybrid9.pg.Lipanasi.enums.MobileNetworkType;
import com.hybrid9.pg.Lipanasi.enums.TransactionStatus;
import com.hybrid9.pg.Lipanasi.models.pgmodels.PaymentGatewayRequest;
import com.hybrid9.pg.Lipanasi.models.pgmodels.PaymentGatewayResponse;
import com.hybrid9.pg.Lipanasi.models.pgmodels.ThreeDsVerificationRequest;
import com.hybrid9.pg.Lipanasi.models.pgmodels.ThreeDsVerificationResponse;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.MobileNetworkConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.OperatorMapping;
import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorManager;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.repositories.payments.GatewayTransactionRepository;
import com.hybrid9.pg.Lipanasi.resources.*;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.payments.PgTransactionService;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorManagementService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class PaymentProcessingService {

    private final RestTemplate restTemplate;
    private final PaymentGatewayProperties properties;
    private final PgTransactionService gatewatTransactionService;
    private final OrderService orderService;
    private final PaymentUtilities paymentUtilities;
    private final OperatorManagementService operatorManagementService;
    private final MnoServiceImpl mnoService;
    private final VendorManagementService vendorManagementService;
    private final NetworkConfResource networkConfResource;
    private final MobileNetworkConfigService networkConfigService;
    private final SessionManagementService sessionManagementService;
    private final PushUssdResource pushUssdResource;
    private final CardPaymentResource cardPaymentResource;
    private final PushUssdService pushUssdService;
    private final ExternalResources externalResources;

    @Value("${order.session.expiry.default:30}")
    private int DEFAULT_SESSION_EXPIRY;

    public PaymentProcessingService(
            RestTemplate restTemplate,
            PaymentGatewayProperties properties,
            GatewayTransactionRepository transactionRepository,
            OrderService orderService,
            PaymentUtilities paymentUtilities,
            OperatorManagementService operatorManagementService,
            MnoServiceImpl mnoService,
            VendorManagementService vendorManagementService,
            NetworkConfResource networkConfResource,
            MobileNetworkConfigService networkConfigService,
            SessionManagementService sessionManagementService,
            PushUssdResource pushUssdResource,
            PushUssdService pushUssdService,
            PgTransactionService gatewatTransactionService,
            ExternalResources externalResources,
            CardPaymentResource cardPaymentResource) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.gatewatTransactionService = gatewatTransactionService;
        this.orderService = orderService;
        this.paymentUtilities = paymentUtilities;
        this.operatorManagementService = operatorManagementService;
        this.mnoService = mnoService;
        this.vendorManagementService = vendorManagementService;
        this.networkConfResource = networkConfResource;
        this.networkConfigService = networkConfigService;
        this.sessionManagementService = sessionManagementService;
        this.pushUssdResource = pushUssdResource;
        this.pushUssdService = pushUssdService;
        this.externalResources = externalResources;
        this.cardPaymentResource = cardPaymentResource;
    }

    /**
     * Process a payment request
     */
    @Transactional
    public CompletableFuture<Object> processPayment(PaymentRequest request, UserSession session, HttpServletRequest httpRequest) {
        log.info("Processing payment for user: {}, merchant: {}",
                session.getUserId(), session.getMerchantId());

        try {
            Order order = this.orderService.findByOrderNumber(request.getOrderNumber())
                    .orElseThrow(() -> new CustomExcpts.OrderNotFoundException(
                            "Order not found: " + request.getOrderNumber()));

            if(request.getMsisdn() == null || request.getMsisdn().isEmpty()){
                throw new CustomExcpts.PhoneNumberException("Phone number is required");
            }

            /*PaymentMethod paymentMethod = this.externalResources.getPaymentMethod(request.getPaymentMethod());
            String paymentChannel = this.externalResources.getPaymentChannel(request.getPaymentChannel()).name();

            // Update commission config with new payment method and channel on Redis
             sessionManagementService.updateCommissionConfig(httpRequest.getHeader("Session-Id"), paymentMethod.getType().name(), paymentMethod, paymentChannel);*/


            // Check if the payment method is active
            /*session.getCommissionConfig().getPaymentMethodConfig().forEach((key, value) -> {
                if (key.equals(paymentMethod.getType().name())) {
                    if (value instanceof PaymentMethodConfig config) {
                        if (!config.getIsActive()) {
                            throw new CustomExcpts.PaymentMethodNotActiveException("Payment method [" + key + "] is not active");
                        } else {
                            log.debug("Payment method [" + key + "] is active");
                        }
                    } else {
                        throw new CustomExcpts.PaymentMethodNotSupportedException("Invalid configuration object for payment method: " + key);
                    }
                }
            });*/

            if (request.getPaymentMethod().equalsIgnoreCase("mobile")) {

                // Check if the mobile network operator is supported
                if (this.getCurrentMobileNetworkConfig(request.getMsisdn(), session).get("mobileNetworkConfig") instanceof MobileNetworkConfig conf) {
                    if (!conf.getStatus().equalsIgnoreCase("ACTIVE")) {
                        throw new CustomExcpts.MobileNetworkOperatorNotActiveException("Mobile network operator [" + this.getCurrentMobileNetworkConfig(request.getMsisdn(), session).get("operatorName") + "] is not active");
                    }
                }
            }


            // Create internal transaction record
            GatewayTransaction transaction = GatewayTransaction.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(session.getUserId())
                    .merchantId(session.getMerchantId())
                    .amount(BigDecimal.valueOf(order.getAmount()))
                    .currency(order.getCurrency())
                    .cardToken(order.getCardToken())
                    .status(TransactionStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            gatewatTransactionService.createTransaction(transaction);

            // Prepare payment gateway request
            PaymentGatewayRequest gatewayRequest = mapToGatewayRequest(request, session, order, httpRequest);
            log.debug("Gateway Request >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {}", gatewayRequest);
            // Call payment gateway API and process the result asynchronously
            return callPaymentMethods(order, gatewayRequest)
                    .thenApply(gatewayResponse -> {
                        // Handle a 3DS challenge
                        if (gatewayResponse.isRequires3dsChallenge()) {
                            // Update transaction status
                            transaction.setStatus(TransactionStatus.AWAITING_3DS);
                            transaction.setPaymentGatewayId(gatewayResponse.getGatewayTransactionId());
                            transaction.setThreeDsUrl(gatewayResponse.getThreeDsUrl());
                            transaction.setThreeDsPayload(gatewayResponse.getThreeDsPayload());
                            gatewatTransactionService.createTransaction(transaction);


                            // Return response indicating 3DS required
                            Map<String, Object> paymentResponse = new HashMap<>();
                            paymentResponse.put("transactionId", transaction.getId());
                            paymentResponse.put("requires3dsChallenge", true);
                            paymentResponse.put("threeDsUrl", gatewayResponse.getThreeDsUrl());
                            paymentResponse.put("threeDsPayload", gatewayResponse.getThreeDsPayload());
                            paymentResponse.put("status", "PENDING_3DS");
                            paymentResponse.put("successful", false);
                            paymentResponse.put("errorCode", ListOfErrorCode.PAYMENT_PENDING.getCode());
                            return (Object) paymentResponse;

                        }

                        // Process normal (non-3DS) response
                        boolean successful = "SUCCESS".equals(gatewayResponse.getStatus());
                        TransactionStatus finalStatus = successful ?
                                TransactionStatus.INITIATED : TransactionStatus.FAILED;

                        // Update transaction
                        transaction.setStatus(finalStatus);
                        transaction.setPaymentGatewayId(gatewayResponse.getGatewayTransactionId());
                        transaction.setResponseCode(gatewayResponse.getResponseCode());
                        transaction.setResponseMessage(gatewayResponse.getMessage());
                        transaction.setCompletedAt(LocalDateTime.now());
                        gatewatTransactionService.createTransaction(transaction);

                        // Return response to client
                        Map<String, Object> paymentResponse = new HashMap<>();
                        paymentResponse.put("transactionId", transaction.getId());
                        paymentResponse.put("gatewayTransactionId", gatewayResponse.getGatewayTransactionId());
                        paymentResponse.put("status", gatewayResponse.getStatus());
                        paymentResponse.put("message", gatewayResponse.getMessage());
                        paymentResponse.put("successful", successful);
                        paymentResponse.put("errorCode", successful ? ListOfErrorCode.PAYMENT_SUCCESS.getCode() : ListOfErrorCode.PAYMENT_FAILED.getCode());
                        return (Object) paymentResponse;

                    })
                    .exceptionally(e -> {
                        log.error("Payment processing error", e);

                        // Update transaction as failed
                        transaction.setStatus(TransactionStatus.FAILED);
                        transaction.setResponseMessage("System error: " + e.getMessage());
                        gatewatTransactionService.createTransaction(transaction);

                        // Return error response
                        Map<String, Object> paymentResponse = new HashMap<>();
                        paymentResponse.put("status", "FAILED");
                        paymentResponse.put("message", "Payment processing failed: " + e.getMessage());
                        paymentResponse.put("successful", false);
                        paymentResponse.put("errorCode", ListOfErrorCode.PAYMENT_FAILED.getCode());
                        return (Object) paymentResponse;

                    });

        } catch (Exception e) {
            log.error("Payment preparation error", e);
            Map<String, Object> paymentResponse = new HashMap<>();
            paymentResponse.put("status", "FAILED");
            paymentResponse.put("message", "Payment preparation failed: " + e.getMessage());
            paymentResponse.put("successful", false);
            paymentResponse.put("errorCode", ListOfErrorCode.PAYMENT_FAILED.getCode());
            return CompletableFuture.completedFuture((Object) paymentResponse);

        }
    }

    private CompletableFuture<PaymentGatewayResponse> callPaymentMethods(Order order, PaymentGatewayRequest gatewayRequest) {
        return switch (gatewayRequest.getPaymentMethod().toLowerCase()) {
            case "card" ->
                // Call payment gateway API
                    initCardPaymentAsync(gatewayRequest, order);
            case "mobile" ->
                // Call Mobile Money API
                    initPushUssdAsync(gatewayRequest, order);
            default -> CompletableFuture.failedFuture(
                    new CustomExcpts.InvalidPaymentMethodException(
                            "Unsupported payment method: " + gatewayRequest.getPaymentMethod())
            );
        };
    }

    /**
     * Verify 3DS challenge response from the issuing bank
     */
    @Transactional
    public VerificationResponse verify3dsChallenge(VerificationRequest request, UserSession session) {
        log.info("Processing 3DS verification for transaction: {}", request.getTransactionId());

        // Retrieve the transaction
        GatewayTransaction transaction = gatewatTransactionService.findById(request.getTransactionId())
                .orElseThrow(() -> new CustomExcpts.TransactionNotFoundException(
                        "Transaction not found: " + request.getTransactionId()));

        // Security validation - ensure the transaction belongs to this user/session
        if (!transaction.getUserId().equals(session.getUserId())) {
            log.warn("Unauthorized 3DS verification attempt. Session user: {}, Transaction user: {}",
                    session.getUserId(), transaction.getUserId());
            throw new CustomExcpts.UnauthorizedException("Unauthorized transaction access");
        }

        // Validate transaction is in correct state
        if (transaction.getStatus() != TransactionStatus.AWAITING_3DS) {
            log.warn("Invalid transaction state for 3DS verification: {}", transaction.getStatus());
            return VerificationResponse.builder()
                    .transactionId(transaction.getId())
                    .verified(false)
                    .status("INVALID_STATE")
                    .message("Transaction is not awaiting 3DS verification")
                    .build();
        }

        try {
            // Prepare verification request to payment gateway
            ThreeDsVerificationRequest verificationRequest = ThreeDsVerificationRequest.builder()
                    .gatewayTransactionId(transaction.getPaymentGatewayId())
                    .paRes(request.getPaRes())  // Payment Authentication Response from issuer
                    .md(request.getMd())        // Merchant Data
                    .build();

            // Call payment gateway to verify 3DS response
            ThreeDsVerificationResponse verificationResponse = verify3dsWithGateway(verificationRequest);

            // Process verification result
            boolean verified = "SUCCESS".equals(verificationResponse.getStatus());
            TransactionStatus finalStatus = verified ?
                    TransactionStatus.COMPLETED : TransactionStatus.FAILED;

            // Update transaction
            transaction.setStatus(finalStatus);
            transaction.setResponseCode(verificationResponse.getResponseCode());
            transaction.setResponseMessage(verificationResponse.getMessage());
            transaction.setLiabilityShifted(verificationResponse.isLiabilityShifted());
            transaction.setEci(verificationResponse.getEci()); // Electronic Commerce Indicator
            transaction.setCavv(verificationResponse.getCavv()); // Cardholder Authentication Verification Value
            transaction.setXid(verificationResponse.getXid()); // Transaction ID for 3DS
            transaction.setCompletedAt(LocalDateTime.now());
            gatewatTransactionService.createTransaction(transaction);

            // Return response to client
            return VerificationResponse.builder()
                    .transactionId(transaction.getId())
                    .gatewayTransactionId(transaction.getPaymentGatewayId())
                    .verified(verified)
                    .status(verificationResponse.getStatus())
                    .message(verificationResponse.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("3DS verification error", e);

            // Update transaction as failed
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setResponseMessage("3DS verification error: " + e.getMessage());
            gatewatTransactionService.createTransaction(transaction);

            // Return error response
            return VerificationResponse.builder()
                    .transactionId(transaction.getId())
                    .verified(false)
                    .status("ERROR")
                    .message("3DS verification failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Call payment gateway API to process initial payment
     */
    private PaymentGatewayResponse callMobileMoney(PaymentGatewayRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey());

        HttpEntity<PaymentGatewayRequest> entity = new HttpEntity<>(request, headers);

        try {
            return restTemplate.postForObject(
                    properties.getEndpoint() + "/v1/payments",
                    entity,
                    PaymentGatewayResponse.class);
        } catch (HttpStatusCodeException e) {
            log.error("Payment gateway error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomExcpts.PaymentGatewayException("Payment gateway error: " + e.getMessage());
        }
    }

    public CompletableFuture<PaymentGatewayResponse> initPushUssdAsync(PaymentGatewayRequest request, Order order) {
        return CompletableFuture.supplyAsync(() -> {
            /*JSONObject response = new JSONObject();*/
            PaymentGatewayResponse response = new PaymentGatewayResponse();
            try {
                String ussdStr = this.mapToJsonString(request, order);
                // Parse ussdStr to JSONObject
                JsonObject pushUssdJsonObject = JsonParser.parseString(ussdStr).getAsJsonObject();

                // Call the async method and get the future
                Deposit deposit = this.pushUssdResource.createPushUssdReq(
                        pushUssdJsonObject, this.pushUssdService).join();

                if (deposit != null) {
                    // response.put("status", "success");
                    // response.put("message", "Push Ussd created successfully");
                    response.setGatewayTransactionId(deposit.getTransactionId());
                    response.setMessage("Push Ussd created successfully");
                    response.setStatus("SUCCESS");
                    response.setRequires3dsChallenge(false);
                    response.setResponseCode("GT000");
                } else {
                    response.setGatewayTransactionId(null);
                    response.setMessage("Push Ussd creation failed");
                    response.setStatus("FAILED");
                    response.setRequires3dsChallenge(false);
                    response.setResponseCode("GT009");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.setGatewayTransactionId(null);
                response.setMessage("Something went wrong, Operation failed: " + e.getMessage());
                response.setStatus("FAILED");
                response.setRequires3dsChallenge(false);
                response.setResponseCode("GT999");
            }
            return response;
        });
    }

    public CompletableFuture<PaymentGatewayResponse> initCardPaymentAsync(PaymentGatewayRequest request, Order order) {
        return CompletableFuture.supplyAsync(() -> {
            /*JSONObject response = new JSONObject();*/
            PaymentGatewayResponse response = new PaymentGatewayResponse();
            try {
                String cardString = this.mapToBankJson(request, order);
                // Parse ussdStr to JSONObject
                JsonObject cardPaymentJsonObject = JsonParser.parseString(cardString).getAsJsonObject();

                // Call the async method and get the future
                CardPayment cardPayment = this.cardPaymentResource.createCardPaymentReq(
                        cardPaymentJsonObject, this.pushUssdService).join();

                if (cardPayment != null) {
                    // response.put("status", "success");
                    // response.put("message", "Push Ussd created successfully");
                    response.setGatewayTransactionId(cardPayment.getTransactionId());
                    response.setMessage("Card Payment initiated successfully");
                    response.setStatus("SUCCESS");
                    response.setRequires3dsChallenge(false);
                    response.setResponseCode("CD-GT000");
                } else {
                    response.setGatewayTransactionId(null);
                    response.setMessage("Card Payment initiation failed");
                    response.setStatus("FAILED");
                    response.setRequires3dsChallenge(false);
                    response.setResponseCode("CD-GT009");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.setGatewayTransactionId(null);
                response.setMessage("Something went wrong, Operation failed: " + e.getMessage());
                response.setStatus("FAILED");
                response.setRequires3dsChallenge(false);
                response.setResponseCode("GT999");
            }
            return response;
        });
    }

    private String mapToJsonString(PaymentGatewayRequest request, Order order) {
        return "{\n" +
                "\"operator\": \"" + request.getOperator() + "\",\n" +
                "\"amount\": " + request.getAmount() + ",\n" +
                "\"currency\": \"" + request.getCurrency() + "\",\n" +
                "\"msisdn\": \"" + request.getMsisdn() + "\",\n" +
                "\"code\": \"" + request.getPartnerCode() + "\",\n" +
                "\"accountNumber\": \"" + request.getAccountNumber() + "\",\n" +
                "\"reference\": \"" + request.getReference() + "\",\n" +
                "\"sessionId\": \"" + request.getSessionId() + "\"\n" +
                "}";
    }

    private String mapToBankJson(PaymentGatewayRequest request, Order order) {
        BillingInformation billing = this.getBillingInfo(request.getBillingString());
        return "{\n" +
                "  \"operator\": \"" + request.getOperator() + "\",\n" +
                "  \"amount\": " + request.getAmount() + ",\n" +
                "  \"currency\": \"" + request.getCurrency() + "\",\n" +
                "  \"msisdn\": \"" + request.getMsisdn() + "\",\n" +
                "  \"code\": \"" + request.getPartnerCode() + "\",\n" +
                "  \"accountNumber\": \"" + request.getAccountNumber() + "\",\n" +
                "  \"reference\": \"" + request.getReference() + "\",\n" +
                "  \"sessionId\": \"" + request.getSessionId() + "\",\n" +
                "  \"orderId\": \"" + order.getOrderNumber() + "\",\n" +
                "  \"capture\": true,\n" +
                "  \"transientToken\": \"" + request.getCardToken() + "\",\n" +
                "  \"billingInfo\": {\n" +
                "    \"firstName\": \"" + billing.getFirstName() + "\",\n" +
                "    \"lastName\": \"" + billing.getLastName() + "\",\n" +
                "    \"email\": \"" + billing.getEmail() + "\",\n" +
                "    \"address1\": \"" + billing.getAddress1() + "\",\n" +
                "    \"city\": \"" + billing.getCity() + "\",\n" +
                "    \"state\": \"" + billing.getState() + "\",\n" +
                "    \"postalCode\": \"" + billing.getPostalCode() + "\",\n" +
                "    \"country\": \"" + billing.getCountry() + "\",\n" +
                "    \"phone\": \"" + billing.getPhone() + "\"\n" +
                "  }\n" +
                "}";

    }

    /**
     * Call payment gateway API to process initial payment
     */
    private CompletableFuture<PaymentGatewayResponse> callPaymentGateway(PaymentGatewayRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey());

        HttpEntity<PaymentGatewayRequest> entity = new HttpEntity<>(request, headers);
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return restTemplate.postForObject(
                            properties.getEndpoint() + "/v1/payments",
                            entity,
                            PaymentGatewayResponse.class);
                } catch (HttpStatusCodeException e) {
                    log.error("Payment gateway error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw new CustomExcpts.PaymentGatewayException("Payment gateway error: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Payment gateway error: {}", e.getMessage());
            throw new CustomExcpts.PaymentGatewayException("Payment gateway error: " + e.getMessage());
        }

    }

    /**
     * Call payment gateway to verify 3DS challenge
     */
    private ThreeDsVerificationResponse verify3dsWithGateway(ThreeDsVerificationRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getApiKey());

        HttpEntity<ThreeDsVerificationRequest> entity = new HttpEntity<>(request, headers);

        try {
            return restTemplate.postForObject(
                    properties.getEndpoint() + "/v1/payments/3ds-verify",
                    entity,
                    ThreeDsVerificationResponse.class);
        } catch (HttpStatusCodeException e) {
            log.error("3DS verification gateway error: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomExcpts.PaymentGatewayException("3DS verification gateway error: " + e.getMessage());
        }
    }

    /**
     * Map internal request to gateway format
     */
    private PaymentGatewayRequest mapToGatewayRequest(PaymentRequest request, UserSession session, Order order, HttpServletRequest httpRequest) throws JsonProcessingException {

        // Get operator mapping and configurations
        //OperatorMapping mapping = getOperatorMappingAndConfigurations(request, session, order, httpRequest);
        Object result = request.getPaymentMethod().equalsIgnoreCase("mobile") ?
                getOperatorMappingAndConfigurations(request, session, order, httpRequest)
                : this.refreshSession(request, session, order, httpRequest);

        Optional<VendorManager> vendorManager = this.vendorManagementService.getVendor(order.getPartnerId());
        if (vendorManager.isEmpty()) {
            throw new CustomExcpts.VendorNotFoundException("VendorDetails not found for ID: " + order.getPartnerId());
        }

        return PaymentGatewayRequest.builder()
                .merchantId(session.getMerchantId())
                .amount(BigDecimal.valueOf(order.getAmount()))
                .currency(order.getCurrency())
                .cardToken(request.getTransientToken())
                .paymentMethod(request.getPaymentMethod())
                .paymentChannel(request.getPaymentChannel())
                .description(order.getDescription())
                .customerIp(session.getIpAddress())
                .customerEmail(order.getCustomer().getEmail())
                .accountNumber(vendorManager.get().getVendorAccountNumber())
                .partnerCode(vendorManager.get().getVendorCode())
                .operator((result instanceof OperatorMapping mapping) ? mapping.getOperatorName() : (String) result)
                .msisdn((result instanceof OperatorMapping mapping) ? request.getMsisdn() : (order.getCustomer().getPhoneNumber() != null && !order.getCustomer().getPhoneNumber().isEmpty()) ? order.getCustomer().getPhoneNumber() : null)
                .reference(order.getReceipt())
                .returnUrl(properties.getReturnUrl())
                .sessionId(order.getPaymentSessionId())
                .billingString(request.getBillingInfo()) // Billing information
                /*.metadata(Map.of(
                        "userId", session.getUserId(),
                        "transactionId", session.getCurrentTransactionId(),
                        "deviceInfo", session.getDeviceInfo()
                ))*/
                .build();
    }

    private OperatorMapping getOperatorMappingAndConfigurations(PaymentRequest request, UserSession session, Order order, HttpServletRequest httpRequest) {
        AtomicReference<Optional<OperatorMapping>> operatorMapping = new AtomicReference<>();
        AtomicReference<MnoPrefix> mnoPrefixAtomic = new AtomicReference<>();
        // Get mobile network operator
        String operatorPrefix = this.paymentUtilities.getOperatorPrefix(request.getMsisdn());
        //Retrieve MNO from Redis
        Optional<OperatorMapping> operator = this.operatorManagementService.getOperator(operatorPrefix);
        if (operator.isEmpty()) {
            // If not found in Redis, call database service to get MNO
            MnoPrefix prefix = this.mnoService.getMno(paymentUtilities.formatPhoneNumber("255", request.getMsisdn()));
            //validate result
            if (prefix == null) {
                throw new CustomExcpts.OperatorNotFoundException("Operator not found for prefix: " + operatorPrefix);
            }
            // Get Mno and map the value to record in Redis
            operator = Optional.of(new OperatorMapping(String.valueOf(prefix.getMnoMapping().getId()), prefix.getMnoMapping().getMno(), prefix.getPrefix(), prefix.getMnoMapping(), "TZ", LocalDateTime.now(), LocalDateTime.now()));
            this.operatorManagementService.createOperator(operator.get());
            mnoPrefixAtomic.set(prefix);
        }
        operatorMapping.set(operator);

        OperatorMapping mapping = this.extractOperatorDetails(operatorMapping.get(), mnoPrefixAtomic.get());
        // check for network configuration from Redis
        log.debug("Session value is:- " + String.valueOf(order.getPaymentSessionId()));
        Optional<UserSession> userSession = this.sessionManagementService.getSession(order.getPaymentSessionId());
        log.debug("Session is present:- " + String.valueOf(userSession.isPresent()));
        Optional<OperatorMapping> finalOperator = operator;
        userSession.ifPresentOrElse(userSession1 -> {
            /*if(userSession1.getNetworkConfig() == null){

            }*/
            // check if the session is still valid
            if (userSession1.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                log.debug("Session is expired:- " + String.valueOf(userSession1.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))));

                // validate vendor
                VendorInfo vendorInfo = externalResources.validateVendor(order, this.vendorManagementService, mapping.getOperatorName(), this.vendorManagementService.getVendor(userSession1.getMerchantId()));

                // create Payment Method on and Payment Channel on Database

                externalResources.createPaymentMethod(vendorInfo);
                externalResources.createPaymentChannel(getOrderRequestDto(order), vendorInfo);


                PaymentMethod paymentMethod = this.externalResources.getPaymentMethod(request.getPaymentMethod());
                String paymentChannel = this.externalResources.getPaymentChannel(request.getPaymentChannel()).name();

                // create commissionTire on Database

                CommissionTier commissionTireFuture = externalResources.createCommissionTire(vendorInfo, getOrderRequestDto(order), request.getPaymentMethod(),paymentChannel,mapping.getMnoMapping()).join();

                // set commission configurations for Redis
                CommissionConfig commissionConfig = this.externalResources.getCommissionConfig(vendorInfo, commissionTireFuture, paymentMethod, paymentChannel, finalOperator);

                // session expired, create a new session
                UserSession sessionForUser = this.initializePaymentSession(String.valueOf(order.getCustomer().getId()), order.getPartnerId());

                // Store CommissionConfig in Redis
                sessionForUser.setCommissionConfig(commissionConfig);

                // Store IP and device info
                sessionForUser.setIpAddress(this.paymentUtilities.getClientIp(httpRequest));
                sessionForUser.setDeviceInfo(httpRequest.getHeader("User-Agent"));

                // start checking for operator configurations from external service
                MobileNetworkType networkType = networkConfResource.getNetworkType(mapping.getOperatorName(), userSession1);

                // Get network configuration
                MobileNetworkConfig networkConfig = this.networkConfResource.getOperatorConf(mapping.getOperatorName(), userSession1);

                // Set network configuration in user session
                sessionForUser.setSelectedNetworkType(networkType);
                sessionForUser.setNetworkConfig(networkConfig);

                // Store Order Details
                sessionForUser.setOrderNumber(order.getOrderNumber());
                sessionForUser.setReceiptNumber(order.getReceipt());


                // Create session in Redis
                String sessionId = this.sessionManagementService.createSession(sessionForUser);

                //update order with new session ID
                order.setPaymentSessionId(sessionId);
                this.orderService.updateOrder(order);

            } else {
               // log.debug(">>>>>>>>>>>>>>>Vendor External Id"+this.vendorManagementService.getVendor(userSession1.getMerchantId()).get().getVendorExternalId());
                // TODO: Validate vendor - (to be removed if not needed)
                // validate vendor
                VendorInfo vendorInfo = externalResources.validateVendor(order, this.vendorManagementService, mapping.getOperatorName(), this.vendorManagementService.getVendor(userSession1.getMerchantId()));

                // create Payment Method on and Payment Channel on Database

                externalResources.createPaymentMethod(vendorInfo);
                externalResources.createPaymentChannel(getOrderRequestDto(order), vendorInfo);

                PaymentMethod paymentMethod = this.externalResources.getPaymentMethod(request.getPaymentMethod());
                String paymentChannel = this.externalResources.getPaymentChannel(request.getPaymentChannel()).name();

                // create commissionTire on Database

                CommissionTier commissionTireFuture = externalResources.createCommissionTire(vendorInfo, getOrderRequestDto(order), request.getPaymentMethod(), paymentChannel, mapping.getMnoMapping()).join();


                // set commission configurations for Redis
                CommissionConfig commissionConfig = this.externalResources.getCommissionConfig(vendorInfo, commissionTireFuture, paymentMethod, paymentChannel, finalOperator);


                // Store CommissionConfig in Redis
                userSession1.setCommissionConfig(commissionConfig);

                if (userSession1.getNetworkConfig() != null) {
                    // check if the network configuration has changed
                    MobileNetworkType networkType = networkConfResource.getNetworkType(mapping.getOperatorName(), userSession1);
                    log.debug("Network Type:- " + String.valueOf(networkType));
                    if (!userSession1.getSelectedNetworkType().equals(networkType)) {
                        // update network configuration
                        MobileNetworkConfig networkConfig = networkConfigService.getConfigByNetworkType(networkType);
                        userSession1.setSelectedNetworkType(networkType);
                        userSession1.setNetworkConfig(networkConfig);
                        //AirtelMoneyConfig airtelMoneyConfig = (AirtelMoneyConfig) userSession1.getNetworkConfig();

                        //log.debug("Airtel Money Token URL:- " + String.valueOf(airtelMoneyConfig.getTokenUrl()));
                        this.sessionManagementService.updateSession(order.getPaymentSessionId(), userSession1);
                    }
                }
                // start checking for operator configurations from external service
                MobileNetworkType networkType = networkConfResource.getNetworkType(mapping.getOperatorName(), userSession1);

                // Get network configuration
                MobileNetworkConfig networkConfig = this.networkConfResource.getOperatorConf(mapping.getOperatorName(), userSession1);


                // Set network configuration in user session
                userSession1.setSelectedNetworkType(networkType);
                userSession1.setNetworkConfig(networkConfig);
                // update last accessed time
                userSession1.setLastAccessedAt(LocalDateTime.now());

                //AirtelMoneyConfig airtelMoneyConfig = (AirtelMoneyConfig) userSession1.getNetworkConfig();
                //log.debug("Airtel Money Token URL:- " + String.valueOf(airtelMoneyConfig.getTokenUrl()));
                this.sessionManagementService.updateSession(order.getPaymentSessionId(), userSession1);
            }

        }, () -> {
            // start checking for operator configurations from external service
            MobileNetworkType networkType = networkConfResource.getNetworkType(mapping.getOperatorName(), session);

            // Get network configuration
            MobileNetworkConfig networkConfig = this.networkConfResource.getOperatorConf(mapping.getOperatorName(), session);

            // Set network configuration in user session
            session.setSelectedNetworkType(networkType);
            session.setNetworkConfig(networkConfig);
        });
        return mapping;
    }


    private Object refreshSession(PaymentRequest request, UserSession session, Order order, HttpServletRequest httpRequest) {
        // check for network configuration from Redis
        log.debug("Session value is:- " + String.valueOf(order.getPaymentSessionId()));
        Optional<UserSession> userSession = this.sessionManagementService.getSession(order.getPaymentSessionId());
        log.debug("Session is present:- " + String.valueOf(userSession.isPresent()));
        userSession.ifPresent(userSession1 -> {
            if (userSession1.getNetworkConfig() == null) {

            }
            // check if the session is still valid
            if (userSession1.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                log.debug("Session is expired:- " + String.valueOf(userSession1.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))));

                // validate vendor
                VendorInfo vendorInfo = externalResources.validateVendor(order, this.vendorManagementService,"CRDB", this.vendorManagementService.getVendor(userSession1.getMerchantId()));

                // create Payment Method on and Payment Channel on Database
                externalResources.createPaymentMethod(vendorInfo);
                externalResources.createPaymentChannel(getOrderRequestDto(order), vendorInfo);

                PaymentMethod paymentMethod = this.externalResources.getPaymentMethod(request.getPaymentMethod());
                String paymentChannel = this.externalResources.getPaymentChannel(request.getPaymentChannel()).name();

                // create commissionTire on Database
                CommissionTier commissionTireFuture = externalResources.createCommissionTire(vendorInfo, getOrderRequestDto(order), request.getPaymentMethod(), paymentChannel, this.mnoService.findByName("CRDB")).join();

                // set commission configurations for Redis
                CommissionConfig commissionConfig = this.externalResources.getCommissionConfig(vendorInfo, commissionTireFuture, paymentMethod, paymentChannel);

                // session expired, create a new session
                UserSession sessionForUser = this.initializePaymentSession(String.valueOf(order.getCustomer().getId()), order.getPartnerId());

                // Store CommissionConfig in Redis
                sessionForUser.setCommissionConfig(commissionConfig);

                // Store IP and device info
                sessionForUser.setIpAddress(this.paymentUtilities.getClientIp(httpRequest));
                sessionForUser.setDeviceInfo(httpRequest.getHeader("User-Agent"));


                // Store Order Details
                sessionForUser.setOrderNumber(order.getOrderNumber());
                sessionForUser.setReceiptNumber(order.getReceipt());


                // Create session in Redis
                String sessionId = this.sessionManagementService.createSession(sessionForUser);

                //update order with new session ID
                order.setPaymentSessionId(sessionId);
                this.orderService.updateOrder(order);

            } else {
                 // TODO: Validate vendor - (to be removed if not needed)
                // validate vendor
                VendorInfo vendorInfo = externalResources.validateVendor(order, this.vendorManagementService,"CRDB",this.vendorManagementService.getVendor(userSession1.getMerchantId()));

                // create Payment Method on and Payment Channel on Database
                externalResources.createPaymentMethod(vendorInfo);
                externalResources.createPaymentChannel(getOrderRequestDto(order), vendorInfo);

                PaymentMethod paymentMethod = this.externalResources.getPaymentMethod(request.getPaymentMethod());
                String paymentChannel = this.externalResources.getPaymentChannel(request.getPaymentChannel()).name();

                // create commissionTire on Database
                CommissionTier commissionTireFuture = externalResources.createCommissionTire(vendorInfo, getOrderRequestDto(order), request.getPaymentMethod(), paymentChannel, this.mnoService.findByName("CRDB")).join();

                // set commission configurations for Redis
                CommissionConfig commissionConfig = this.externalResources.getCommissionConfig(vendorInfo, commissionTireFuture, paymentMethod, paymentChannel);

                // Store CommissionConfig in Redis
                userSession1.setCommissionConfig(commissionConfig);

                // update last accessed time
                userSession1.setLastAccessedAt(LocalDateTime.now());

                //AirtelMoneyConfig airtelMoneyConfig = (AirtelMoneyConfig) userSession1.getNetworkConfig();
                //log.debug("Airtel Money Token URL:- " + String.valueOf(airtelMoneyConfig.getTokenUrl()));
                this.sessionManagementService.updateSession(order.getPaymentSessionId(), userSession1);
            }

        });
        return order.getCustomer().getPhoneNumber() != null ? getCurrentMobileNetworkConfig(order.getCustomer().getPhoneNumber(), session).get("operatorName") : null;
    }

    private static OrderRequestDto getOrderRequestDto(Order order) {
        return OrderRequestDto.builder()
                .amount((int) order.getAmount())
                .currency(order.getCurrency())
                .receipt(order.getReceipt())
                //.paymentMethod(switchPaymentMethod(order.getPaymentMethod().getType().name()))
                //.paymentChannel(order.getChannel())
                //.email(order.getCustomer().getEmail())
                .metadata(order.getMetadata())
                .customers(CustomerDto.builder()
                        .firstName(order.getCustomer().getFirstName())
                        .lastName(order.getCustomer().getLastName())
                        .email(order.getCustomer().getEmail())
                        .phoneNumber(order.getCustomer().getPhoneNumber() != null ? order.getCustomer().getPhoneNumber() : null)
                        .build())
                .build();
    }

    private static String switchPaymentMethod(String name) {
        return switch (name) {
            case "BANK_TRANSFER" -> "bank";
            case "MOBILE_MONEY" -> "mobile";
            case "CASH_ON_DELIVERY" -> "cash";
            case "CREDIT_CARD" -> "card";
            default -> throw new RuntimeException("Unknown payment method: " + name);
        };
    }


    private OperatorMapping extractOperatorDetails(Optional<OperatorMapping> operatorMapping, MnoPrefix mnoPrefix) {
        if (mnoPrefix != null) {
            return OperatorMapping.builder()
                    .operatorId(String.valueOf(mnoPrefix.getId()))
                    .createdAt(LocalDateTime.now())
                    .lastAccessedAt(LocalDateTime.now())
                    .operatorCountryCode(mnoPrefix.getCountryName())
                    .operatorName(mnoPrefix.getMnoMapping().getMno())
                    .operatorPrefix(mnoPrefix.getPrefix())
                    .build();

        }
        if (operatorMapping.isPresent()) {
            return operatorMapping.get();
        } else {
            throw new CustomExcpts.OperatorNotFoundException("Operator not found for prefix: ");
        }

    }

    /**
     * Initialize a payment session with 3DS capabilities
     */
    public UserSession initializePaymentSession(String userId, String merchantId) {

        // Add 3DS capabilities flag to session
        // session.addAttribute("3dsEnabled", properties.isEnable3ds());

        return UserSession.builder()
                .userId(userId)
                .merchantId(merchantId)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .authenticated(true)
                .mfaVerified(false)
                .transactionStatus(UserSession.TransactionStatus.PENDING)
                .roles(Collections.singletonList("PAYMENT_USER"))
                .attributes(new HashMap<>())
                .build();
    }


    private BillingInformation getBillingInfo(String encodedString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String decodedString = new String(Base64.getDecoder().decode(encodedString), "UTF-8");
            return objectMapper.readValue(decodedString, BillingInformation.class);
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    private Map<String, Object> getCurrentMobileNetworkConfig(String msisdn, UserSession session) {
        AtomicReference<Optional<OperatorMapping>> operatorMapping = new AtomicReference<>();
        AtomicReference<MnoPrefix> mnoPrefixAtomic = new AtomicReference<>();
        // Get mobile network operator
        String operatorPrefix = this.paymentUtilities.getOperatorPrefix(msisdn);
        //Retrieve MNO from Redis
        Optional<OperatorMapping> operator = this.operatorManagementService.getOperator(operatorPrefix);
        if (operator.isEmpty()) {
            // If not found in Redis, call database service to get MNO
            MnoPrefix prefix = this.mnoService.getMno(paymentUtilities.formatPhoneNumber("255", msisdn));
            //validate result
            if (prefix == null) {
                throw new CustomExcpts.OperatorNotFoundException("Operator not found for prefix: " + operatorPrefix);
            }
            // Get Mno and map the value to record in Redis
            operator = Optional.of(new OperatorMapping(String.valueOf(prefix.getMnoMapping().getId()), prefix.getMnoMapping().getMno(), prefix.getPrefix(), prefix.getMnoMapping(), "TZ", LocalDateTime.now(), LocalDateTime.now()));
            this.operatorManagementService.createOperator(operator.get());
            mnoPrefixAtomic.set(prefix);
        }
        operatorMapping.set(operator);

        OperatorMapping mapping = this.extractOperatorDetails(operatorMapping.get(), mnoPrefixAtomic.get());

        // Construct the mobile network config
        Map<String, Object> mobileNetworkConfig = new HashMap<>();
        mobileNetworkConfig.put("mobileNetworkConfig", this.networkConfResource.getMobileNetworkConfig(mapping.getOperatorName(), session));
        mobileNetworkConfig.put("operatorName", mapping.getOperatorName());


        // return the mobile network config
        return mobileNetworkConfig;
    }
}
