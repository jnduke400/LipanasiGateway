package com.hybrid9.pg.Lipanasi.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.dto.*;
import com.hybrid9.pg.Lipanasi.dto.commission.PaymentChannelConfig;
import com.hybrid9.pg.Lipanasi.dto.commission.PaymentMethodConfig;
import com.hybrid9.pg.Lipanasi.dto.order.OrderRequestDto;
import com.hybrid9.pg.Lipanasi.dto.order.SessionInitRequest;
import com.hybrid9.pg.Lipanasi.dto.order.VendorInfo;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoPrefix;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannelConfig;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethodConfigEntity;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Customer;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CommissionStatus;
import com.hybrid9.pg.Lipanasi.enums.OrderSessionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.order.OrderSession;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.OperatorMapping;
import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorManager;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelConfigServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.order.OrderSessionServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.vendorx.CustomerServiceImpl;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTierService;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.PaymentMethodConfigService;
import com.hybrid9.pg.Lipanasi.services.payments.PaymentMethodService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.OperatorManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.PaymentProcessingService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorManagementService;
import com.hybrid9.pg.Lipanasi.services.payscoopconfig.PayScoopApiService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.utilities.OrderNumberGenerator;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import com.hybrid9.pg.Lipanasi.utilities.VendorUtilities;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderResource {
    @Autowired
    @Qualifier("orderProcessorVirtualThread")
    private ExecutorService orderProcessorVirtualThread;

    @Value("${partner.validation.url:https://api.business.payscoop.com/api/validate}")
    private String partnerValidationUrl;

    @Value("${network.config.url:https://api.business.payscoop.com/api/collection-credential}")
    private String networkConfigUrl;

    @Autowired
    @Qualifier("cpuExecutor")
    private Executor cpuExecutor;
    private final OrderSessionServiceImpl orderSessionService;
    private final PaymentUtilities paymentUtilities;
    private final PaymentMethodService paymentMethodService;
    private final OrderService orderService;
    private final CustomerServiceImpl customerService;
    private final PaymentProcessingService paymentService;
    private final SessionManagementService sessionService;
    private final CommissionTierService commissionTierService;
    private final VendorService vendorService;
    private final MnoServiceImpl mnoService;
    private final VendorResource vendorResource;
    private final VendorUtilities vendorUtilities;
    private final VendorManagementService vendorManagementService;
    private final PaymentMethodConfigService paymentMethodConfigService;
    private final MobileMoneyChannelConfigServiceImpl mobileMoneyChannelConfigService;
    private final MobileMoneyChannelServiceImpl mobileMoneyChannelService;
    private final OperatorManagementService operatorManagementService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final RestTemplate restTemplate;

    public OrderResource(OrderSessionServiceImpl orderSessionService, PaymentUtilities paymentUtilities, PaymentMethodService paymentMethodService, OrderService orderService,
                         CustomerServiceImpl customerService, PaymentProcessingService paymentService, SessionManagementService sessionService, CommissionTierService commissionTierService,
                         VendorService vendorService, MnoServiceImpl mnoService, VendorResource vendorResource, VendorUtilities vendorUtilities, VendorManagementService vendorManagementService,
                         PaymentMethodConfigService paymentMethodConfigService, MobileMoneyChannelConfigServiceImpl mobileMoneyChannelConfigService, MobileMoneyChannelServiceImpl mobileMoneyChannelService,
                         OperatorManagementService operatorManagementService, RestTemplate restTemplate, OrderNumberGenerator orderNumberGenerator) {
        this.orderSessionService = orderSessionService;
        this.paymentUtilities = paymentUtilities;
        this.paymentMethodService = paymentMethodService;
        this.orderService = orderService;
        this.customerService = customerService;
        this.paymentService = paymentService;
        this.sessionService = sessionService;
        this.commissionTierService = commissionTierService;
        this.vendorService = vendorService;
        this.mnoService = mnoService;
        this.vendorResource = vendorResource;
        this.vendorUtilities = vendorUtilities;
        this.vendorManagementService = vendorManagementService;
        this.paymentMethodConfigService = paymentMethodConfigService;
        this.mobileMoneyChannelConfigService = mobileMoneyChannelConfigService;
        this.mobileMoneyChannelService = mobileMoneyChannelService;
        this.operatorManagementService = operatorManagementService;
        this.orderNumberGenerator = orderNumberGenerator;
        this.restTemplate = restTemplate;
    }

    public void validateOrderRequest(OrderRequestDto orderRequestDto, Optional<String> authorizationHeader) {
        if (orderRequestDto == null) {
            throw new InvalidRequestException("Order request is required");
        }
        this.checkIfHasValidCredentials(authorizationHeader);
        // This is a dummy response
        // In a real application, this would be a call to a service that would validate the order
        /**
         * This is a dummy response
         * In a real application, this would be a call to a service that would validate the order
         */
        // return "{\"partnerId\":\"1234\",\"apiKey\":\"1234\",\"status\":\"active\"}";
        return;

    }

    private void checkIfHasValidCredentials(Optional<String> authorizationHeader) {
        authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
        if (!(authorizationHeader.get().startsWith("Basic "))) {
            throw new InvalidRequestException("Credentials are required");
        }
    }

    public CompletableFuture<Boolean> checkIfSessionIsValid(Optional<String> authorizationHeader, OrderRequestDto orderRequestDto) {
        return CompletableFuture.supplyAsync(() -> {
            authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
            Optional<OrderSession> orderSession = this.orderSessionService.findByCredentialsAndOrderEmail(authorizationHeader.get().substring(6), orderRequestDto.getCustomers().getEmail());
            return orderSession.map(session -> session.getStatus().equals(OrderSessionStatus.ACTIVE)).orElse(false);
        }, orderProcessorVirtualThread);
    }

    public CompletableFuture<Object> createOrder(OrderRequestDto orderRequestDto, Optional<String> authorizationHeader, HttpServletRequest httpRequest, VendorInfo vendorData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (authorizationHeader.isEmpty()) {
                    throw new InvalidRequestException("Authorization header is required");
                }

                AtomicReference<Order> orderReference = new AtomicReference<>();
                Optional<Customer> customer = this.createCustomer(orderRequestDto);
                if (customer.isPresent()) {
                    Order order = Order.builder()
                            .amount(orderRequestDto.getAmount())
                            .currency(orderRequestDto.getCurrency())
                            .description("")
                            .metadata(orderRequestDto.getMetadata())
                            .orderNumber(this.orderNumberGenerator.generateOrderNumber(vendorData.getPartnerId()))
                            .orderItem("")
                            .partnerId(vendorData.getPartnerId())
                            .paymentMethod(this.getPaymentMethod("mobile"))
                            .orderToken(this.paymentUtilities.generateOrderToken())
                            .signature(authorizationHeader.get().substring(6))
                            .customer(customer.orElseThrow(() -> new RuntimeException("Customer not found")))
                            .receipt(orderRequestDto.getReceipt())
                            .build();
                    orderReference.set(this.orderService.createOrder(order));
                    /*Order orderResult = this.orderService.createOrder(order);*/
                }

                // create commissionTire on Database
                //TODO: Fix this (uncomment when commission is needed)
                //CompletableFuture<CommissionTier> commissionTireFuture = createCommissionTire(vendorData, orderRequestDto);

                // create Payment Method on and Payment Channel on Database

                createPaymentMethod(authorizationHeader, vendorData);
                createPaymentChannel(orderRequestDto, authorizationHeader, vendorData);

                 //TODO: Fix this (uncomment when commission is needed)
                //PaymentMethod paymentMethod = this.getPaymentMethod("mobile");
                //String paymentChannel = this.getPaymentChannel("PUSH_USSD").name();

                // get operator mapping and configurations
                //MnoMapping mnoMapping = this.mnoService.searchMnoObject(orderRequestDto.getCustomers().getPhoneNumber());
                //Optional<OperatorMapping> operator = (orderRequestDto.getCustomers().getPhoneNumber() != null && !orderRequestDto.getCustomers().getPhoneNumber().isEmpty()) ? this.getOperator(orderRequestDto.getCustomers().getPhoneNumber()) : Optional.empty();

                // set commission configurations for Redis
                //TODO: Fix this (uncomment when commission is needed)
               // CommissionConfig commissionConfig = operator.isEmpty() ? getCommissionConfig(vendorData, commissionTireFuture.join(), paymentMethod, paymentChannel) : getCommissionConfig(vendorData, commissionTireFuture.join(), paymentMethod, paymentChannel, operator);
                // Create a payment Redis session request
                SessionInitRequest sessionInitRequest = this.mapToSessionRequest(orderReference.get());

                // Create a session with payment context
                UserSession session = paymentService.initializePaymentSession(
                        sessionInitRequest.getUserId(),
                        sessionInitRequest.getMerchantId()
                );

                //TODO: Fix this (uncomment when commission is needed)

                // Store CommissionConfig in Redis
               // session.setCommissionConfig(commissionConfig);

                // Store IP and device info
                session.setIpAddress(this.paymentUtilities.getClientIp(httpRequest));
                session.setDeviceInfo(httpRequest.getHeader("User-Agent"));

                // Store Order Number
                session.setOrderNumber(orderReference.get().getOrderNumber());
                session.setReceiptNumber(orderReference.get().getReceipt());

                // Create session in Redis
                String sessionId = this.sessionService.createSession(session);

                // update order with session id
                orderReference.get().setPaymentSessionId(sessionId);
                this.orderService.updateOrder(orderReference.get());

                return this.composeResponse(orderReference, authorizationHeader, sessionId);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, orderProcessorVirtualThread);
    }

    private Optional<OperatorMapping> getOperator(String phoneNumber) {
        //Retrieve MNO from Redis
        Optional<OperatorMapping> operator = this.operatorManagementService.getOperator(phoneNumber.substring(0, 5));
        if (operator.isEmpty()) {
            // If not found in Redis, call database service to get MNO
            MnoPrefix prefix = this.mnoService.getMno(paymentUtilities.formatPhoneNumber("255", phoneNumber));
            //validate result
            if (prefix == null) {
                throw new CustomExcpts.OperatorNotFoundException("Operator not found for prefix: " + phoneNumber.substring(0, 5));
            }
            // Get Mno and map the value to record in Redis
            operator = Optional.of(new OperatorMapping(String.valueOf(prefix.getMnoMapping().getId()), prefix.getMnoMapping().getMno(), prefix.getPrefix(), prefix.getMnoMapping(), "TZ", LocalDateTime.now(), LocalDateTime.now()));
            String operatorResult = this.operatorManagementService.createOperator(operator.get());
            if (operatorResult == null) {
                throw new CustomExcpts.OperatorNotFoundException("Operator not found for prefix: " + phoneNumber.substring(0, 5));
            }
            return this.operatorManagementService.getOperator(phoneNumber.substring(0, 5));
        }
        return operator;
    }

    private CompletableFuture<Void> createPaymentChannel(OrderRequestDto orderRequestDto, Optional<String> authorizationHeader, VendorInfo vendorData) {
        return CompletableFuture.runAsync(() -> {
            authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is missing"));
            VendorDetails vendorDetails = Optional.ofNullable(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()))
                    .orElseThrow(() -> new CustomExcpts.VendorNotFoundException("Vendor not found"));

            List<MobileMoneyChannelConfig> mobileMoneyChannelConfigList = new ArrayList<>();

            vendorData.getCommissionConfig().getPaymentChannelConfigs()
                    .forEach(paymentChannelConfig -> {
                        this.mobileMoneyChannelConfigService.findByMobileMoneyChannelAndVendorAndMobileOperator(this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannelConfig.getMobileMoneyChannel())), vendorDetails, this.mnoService.findByName(paymentChannelConfig.getMobileOperator()))
                                .ifPresentOrElse(mobileMoneyChannelConfigEntity -> {
                                    mobileMoneyChannelConfigEntity.setStatus(CommissionStatus.valueOf(paymentChannelConfig.getCommissionStatus()));
                                    mobileMoneyChannelConfigList.add(mobileMoneyChannelConfigEntity);
                                }, () -> {
                                    MobileMoneyChannelConfig mobileMoneyChannelConfig = MobileMoneyChannelConfig.builder()
                                            .mobileMoneyChannel(this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannelConfig.getMobileMoneyChannel())))
                                            .mobileOperator(this.mnoService.findByName(paymentChannelConfig.getMobileOperator()))
                                            .status(CommissionStatus.valueOf(paymentChannelConfig.getCommissionStatus()))
                                            .vendor(vendorDetails)
                                            .build();
                                    mobileMoneyChannelConfigList.add(mobileMoneyChannelConfig);
                                });
                    });
            this.mobileMoneyChannelConfigService.createOrUpdateMobileMoneyChannelConfig(mobileMoneyChannelConfigList);
        }, orderProcessorVirtualThread);
    }

    private CompletableFuture<Void> createPaymentMethod(Optional<String> authorizationHeader, VendorInfo vendorData) {
        return CompletableFuture.runAsync(() -> {
            authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is missing"));
            VendorDetails vendorDetails = Optional.ofNullable(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()))
                    .orElseThrow(() -> new CustomExcpts.VendorNotFoundException("Vendor not found"));

            List<PaymentMethodConfigEntity> paymentMethodConfigList = new ArrayList<>();

            vendorData.getCommissionConfig().getPaymentMethodConfigs()
                    .forEach(paymentMethodConfig -> {
                        this.paymentMethodConfigService.findByPaymentMethodAndVendor(this.paymentMethodService
                                        .findByType(PaymentMethodType.valueOf(paymentMethodConfig.getPaymentMethod())), vendorDetails)
                                .ifPresentOrElse(paymentMethodConfigEntity -> {
                                    paymentMethodConfigEntity.setIsActive(paymentMethodConfig.getIsActive());
                                    paymentMethodConfigList.add(paymentMethodConfigEntity);
                                }, () -> {
                                    PaymentMethodConfigEntity paymentMethodEntity = PaymentMethodConfigEntity.builder()
                                            .paymentMethod(this.paymentMethodService.findByType(PaymentMethodType
                                                    .valueOf(paymentMethodConfig.getPaymentMethod())))
                                            .vendor(vendorDetails)
                                            .isActive(paymentMethodConfig.getIsActive())
                                            .build();

                                    paymentMethodConfigList.add(paymentMethodEntity);
                                });
                    });
            this.paymentMethodConfigService.createOrUpdatePaymentMethodConfig(paymentMethodConfigList);
        }, orderProcessorVirtualThread);
    }

    private Optional<Customer> createCustomer(OrderRequestDto orderRequestDto) {
        Optional<Customer> customerOptional = this.customerService.findByEmailAndPhoneNumber(orderRequestDto.getCustomers().getEmail(), orderRequestDto.getCustomers().getPhoneNumber());
        if (customerOptional.isPresent()) {
            return customerOptional;
        }
        Customer customer = Customer.builder()
                .firstName(orderRequestDto.getCustomers().getFirstName())
                .lastName(orderRequestDto.getCustomers().getLastName())
                .email(orderRequestDto.getCustomers().getEmail())
                .phoneNumber(orderRequestDto.getCustomers().getPhoneNumber())
                .build();
        return Optional.ofNullable(this.customerService.createCustomer(customer));
    }

    private PaymentMethod getPaymentMethod(String paymentMethod) {
        return switch (paymentMethod.toLowerCase()) {
            case "card" -> this.paymentMethodService.findByType(PaymentMethodType.valueOf("CREDIT_CARD"));
            case "bank" -> this.paymentMethodService.findByType(PaymentMethodType.valueOf("BANK_TRANSFER"));
            case "mobile" -> this.paymentMethodService.findByType(PaymentMethodType.valueOf("MOBILE_MONEY"));
            case "cash" -> this.paymentMethodService.findByType(PaymentMethodType.valueOf("CASH_ON_DELIVERY"));
            default -> throw new InvalidRequestException("Invalid payment method");
        };
    }

    public CompletableFuture<String> validateVendor(OrderRequestDto orderRequestDto, Optional<String> authorizationHeader) {
        return CompletableFuture.supplyAsync(() -> {
            authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
            try {
                // This is a dummy response
                // In a real application, this would be a call to a service that would validate the vendorx
                /**
                 * This is a dummy response
                 * In a real application, this would be a call to a service that would validate the vendorx
                 */


                AtomicReference<Object> objectReference = new AtomicReference<>();
                orderService.findBySignatureWithOptional(authorizationHeader.get().substring(6).trim()).ifPresentOrElse(order -> {
                    log.info(">>>>>>>>>>>>>>>Order found in Redis");
                    // Get configurations from Redis
                    objectReference.set(getConfigurationsFromRedis(order.getPartnerId(),authorizationHeader.get().substring(6).trim()));
                }, () -> {
                    log.info(">>>>>>>>>>>>>>>>Order not found in Redis");
                    // Get configurations from Rest API
                    objectReference.set(this.getConfigurations(authorizationHeader.get().substring(6), null));
                });
                String json = new Gson().toJson(objectReference.get());
                JsonNode jsonNode = new ObjectMapper().readTree(json);
                String partnerId = jsonNode.get("partner_id").asText();
                String status = jsonNode.get("status").asText();
                String token = jsonNode.get("token").asText();
                boolean valid = jsonNode.get("valid").asBoolean();
                String hasVfd = jsonNode.get("hasVfd").asText();
                String vfdType = jsonNode.get("vfd_type").asText();
                String callbackUrl = jsonNode.get("callback_url").asText();

                String vendorInfo = "{\n" +
                        "  \"partnerId\": \"" + partnerId + "\",\n" +
                        "  \"apiKey\": \"" + token + "\",\n" +
                        "  \"status\": \"" + status + "\",\n" +
                        "  \"charges\": 0,\n" +
                        "  \"hasVat\": \""+hasVfd+"\",\n" +
                        "  \"vfdType\":\""+vfdType+"\",\n" +
                        "  \"hasCommission\": \"true\",\n" +
                        "  \"callbackUrl\": \""+callbackUrl+"\",\n" +
                        "  \"commissionConfig\": {\n" +
                        "    \"minimumAmount\": 1000,\n" +
                        "    \"maximumAmount\": 100000,\n" +
                        "    \"baseFee\": 0,\n" +
                        "    \"percentageRate\": 0,\n" +
                        "    \"commissionStatus\": \"ACTIVE\",\n" +
                        "    \"paymentMethodConfigs\": [\n" +
                        "      {\n" +
                        "        \"paymentMethod\": \"BANK_TRANSFER\",\n" +
                        "        \"isActive\": true\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"paymentMethod\": \"MOBILE_MONEY\",\n" +
                        "        \"isActive\": true\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"paymentMethod\": \"CASH_ON_DELIVERY\",\n" +
                        "        \"isActive\": true\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"paymentMethod\": \"CREDIT_CARD\",\n" +
                        "        \"isActive\": true\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"paymentChannelConfigs\": [\n" +
                        "      {\n" +
                        "        \"mobileOperator\": \"AirtelMoney-Tanzania\",\n" +
                        "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                        "        \"commissionStatus\": \"ACTIVE\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"mobileOperator\": \"Mixx_by_yas-Tanzania\",\n" +
                        "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                        "        \"commissionStatus\": \"ACTIVE\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"mobileOperator\": \"Mpesa-Tanzania\",\n" +
                        "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                        "        \"commissionStatus\": \"ACTIVE\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"mobileOperator\": \"Halopesa-Tanzania\",\n" +
                        "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                        "        \"commissionStatus\": \"ACTIVE\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"mobileOperator\": \"Tpesa-Tanzania\",\n" +
                        "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                        "        \"commissionStatus\": \"ACTIVE\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"mobileOperator\": \"ZPesa-Tanzania\",\n" +
                        "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                        "        \"commissionStatus\": \"ACTIVE\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"mobileOperator\": \"CRDB\",\n" +
                        "        \"mobileMoneyChannel\": \"BANK_PAYMENT_GATEWAY\",\n" +
                        "        \"commissionStatus\": \"ACTIVE\"\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}\n";
                log.debug("Vendor Info >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + vendorInfo);
                VendorInfo vendorData = this.parseVendorInfo(vendorInfo);
                // check if vendorx exists in Redis
                this.vendorManagementService.getVendor(vendorData.getPartnerId())
                .ifPresentOrElse(vendor -> {

                    // update vendor data
                    vendor.setVendorKey(vendorData.getApiKey());
                    vendor.setVendorStatus(vendorData.getStatus());
                    vendor.setVendorCallbackUrl(vendorData.getCallbackUrl());
                    vendor.setVendorHasVat(vendorData.getHasVat());
                    vendor.setVendorVatType(vendorData.getVfdType());

                    this.vendorManagementService.updateVendor(vendorData.getPartnerId(), vendor);
                }, () -> {
                    VendorCreatorDto vendor = vendorResource.createVendorObject(vendorData, orderRequestDto);
                    // create a vendorx
                    vendorResource.createVendor(vendor, this.vendorManagementService, orderRequestDto);
                });

                // return partner / vendorx configuration info
                return vendorInfo;
            } catch (Exception e) {
                e.printStackTrace();
                //throw new CustomExcpts.PartnerValidationException("Partner validation failed");
            }
            return null;
        }, orderProcessorVirtualThread);

    }

    public VendorInfo parseVendorInfo(String vendorInfo) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            VendorInfo vendorInfoResult = objectMapper.readValue(vendorInfo, VendorInfo.class);
            if (vendorInfoResult.getStatus() == null) {
                throw new InvalidVendorInfoException("VendorDetails info response is invalid");
            }
            return vendorInfoResult;
        } catch (Exception e) {
            throw new InvalidVendorInfoException("VendorDetails info parsing failed");
        }
    }

    public CompletableFuture<Void> createSession(OrderRequestDto orderRequestDto, Optional<String> authorizationHeader, VendorInfo vendorData) {
        return CompletableFuture.runAsync(() -> {
            authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
            OrderSession orderSession = OrderSession.builder()
                    .partnerId(vendorData.getPartnerId())
                    .sessionId(vendorData.getApiKey())
                    .credential(authorizationHeader.get().substring(6))
                    .expiryDate(this.paymentUtilities.getExpiryDate())
                    .duration("1 hour")
                    .orderEmail(orderRequestDto.getCustomers().getEmail())
                    .status(OrderSessionStatus.ACTIVE)
                    .build();

            this.orderSessionService.createSession(orderSession);
        }, orderProcessorVirtualThread);
    }


    private CompletableFuture<CommissionTier> createCommissionTire(VendorInfo vendorData, OrderRequestDto orderRequestDto) {
        return CompletableFuture.supplyAsync(() -> {
            CommissionTier commissionTireResult = this.commissionTierService.findCommissionTierByVendorDetails(
                    Optional.ofNullable(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()))
                            .orElseThrow(() -> new RuntimeException("Vendor not found")));

            if (commissionTireResult == null) {
                CommissionTier commissionTier = CommissionTier.builder()
                        .vendor(Optional.ofNullable(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()))
                                .orElseThrow(() -> new RuntimeException("Vendor not found")))
                        .paymentMethod(this.getPaymentMethod("mobile"))
                        .minimumAmount(vendorData.getCommissionConfig().getMinimumAmount())
                        .maximumAmount(vendorData.getCommissionConfig().getMaximumAmount())
                        .baseFee(vendorData.getCommissionConfig().getBaseFee())
                        .percentageRate(vendorData.getCommissionConfig().getPercentageRate())
                        .isActive(vendorData.getCommissionConfig().getCommissionStatus().equalsIgnoreCase("active"))
                        .build();
                return this.commissionTierService.createCommissionTier(commissionTier);
            } else {
                commissionTireResult.setPaymentMethod(this.getPaymentMethod("mobile"));
                commissionTireResult.setMinimumAmount(vendorData.getCommissionConfig().getMinimumAmount());
                commissionTireResult.setMaximumAmount(vendorData.getCommissionConfig().getMaximumAmount());
                commissionTireResult.setBaseFee(vendorData.getCommissionConfig().getBaseFee());
                commissionTireResult.setPercentageRate(vendorData.getCommissionConfig().getPercentageRate());
                commissionTireResult.setIsActive(vendorData.getCommissionConfig().getCommissionStatus().equalsIgnoreCase("active"));
                return this.commissionTierService.updateCommissionTier(commissionTireResult);
            }
        }, orderProcessorVirtualThread);
    }

    private CommissionConfig getCommissionConfig(VendorInfo vendorData, CommissionTier commissionTier, PaymentMethod paymentMethod, String paymentChannel, Optional<OperatorMapping> operatorMapping) {
        operatorMapping.orElseThrow(() -> new RuntimeException("Operator not found for prefix: "));
        CommissionConfig commissionConfig = new CommissionConfig();
        commissionConfig.setCommissionTireId(String.valueOf(commissionTier.getId()));
        commissionConfig.setCommissionStatus(vendorData.getCommissionConfig().getCommissionStatus().equalsIgnoreCase("active") ? CommissionConfig.CommissionStatus.ACTIVE : CommissionConfig.CommissionStatus.INACTIVE);
        commissionConfig.setPaymentMethod(paymentMethodService.findByType(paymentMethod.getType()));
        commissionConfig.setPaymentMethodName(paymentMethod.getType().name());
        commissionConfig.setPaymentMethodChanelName(paymentChannel);
        commissionConfig.setBaseFee(vendorData.getCommissionConfig().getBaseFee());
        commissionConfig.setMaximumAmount(vendorData.getCommissionConfig().getMaximumAmount());
        commissionConfig.setMinimumAmount(vendorData.getCommissionConfig().getMinimumAmount());
        commissionConfig.setPercentageRate(vendorData.getCommissionConfig().getPercentageRate());
        commissionConfig.setPaymentMethodConfig(vendorData.getCommissionConfig().getPaymentMethodConfigs().stream().collect(Collectors.toMap(PaymentMethodConfig::getPaymentMethod, Function.identity())));
        commissionConfig.setPaymentChannelConfig(vendorData.getCommissionConfig().getPaymentChannelConfigs().stream().collect(Collectors.toMap(PaymentChannelConfig::getMobileOperator, Function.identity())));
        return commissionConfig;
    }


    private CommissionConfig getCommissionConfig(VendorInfo vendorData, CommissionTier commissionTier, PaymentMethod paymentMethod, String paymentChannel) {
        CommissionConfig commissionConfig = new CommissionConfig();
        commissionConfig.setCommissionTireId(String.valueOf(commissionTier.getId()));
        commissionConfig.setCommissionStatus(vendorData.getCommissionConfig().getCommissionStatus().equalsIgnoreCase("active") ? CommissionConfig.CommissionStatus.ACTIVE : CommissionConfig.CommissionStatus.INACTIVE);
        commissionConfig.setPaymentMethod(paymentMethodService.findByType(paymentMethod.getType()));
        commissionConfig.setPaymentMethodName(paymentMethod.getType().name());
        commissionConfig.setPaymentMethodChanelName(paymentChannel);
        commissionConfig.setBaseFee(vendorData.getCommissionConfig().getBaseFee());
        commissionConfig.setMaximumAmount(vendorData.getCommissionConfig().getMaximumAmount());
        commissionConfig.setMinimumAmount(vendorData.getCommissionConfig().getMinimumAmount());
        commissionConfig.setPercentageRate(vendorData.getCommissionConfig().getPercentageRate());
        commissionConfig.setPaymentMethodConfig(vendorData.getCommissionConfig().getPaymentMethodConfigs().stream().collect(Collectors.toMap(PaymentMethodConfig::getPaymentMethod, Function.identity())));
        commissionConfig.setPaymentChannelConfig(vendorData.getCommissionConfig().getPaymentChannelConfigs().stream().collect(Collectors.toMap(PaymentChannelConfig::getMobileOperator, Function.identity())));
        return commissionConfig;
    }

    private PaymentChannel getPaymentChannel(String paymentChannel) {
        return switch (paymentChannel.toLowerCase()) {
            case "pay_bill" -> PaymentChannel.PAY_BILL;
            case "push_ussd" -> PaymentChannel.PUSH_USSD;
            case "bank_gateway" -> PaymentChannel.BANK_PAYMENT_GATEWAY;
            default -> throw new InvalidRequestException("Invalid payment channel");
        };
    }


    public Object composeResponse(AtomicReference<Order> orderReference, Optional<String> authorizationHeader, String paymentSessionId) {
        authorizationHeader.orElseThrow(() -> new RuntimeException("Authorization header is required"));
        Map<String, Object> response = new HashMap<>();
        response.put("orderNumber", this.orderService.findBySignatureAndOrderToken(authorizationHeader.get().substring(6), orderReference.get().getOrderToken()).getOrderNumber());
        response.put("orderToken", this.orderService.findBySignatureAndOrderToken(authorizationHeader.get().substring(6), orderReference.get().getOrderToken()).getOrderToken());
        response.put("amount", orderReference.get().getAmount());
        response.put("currency", orderReference.get().getCurrency());
        response.put("metadata", orderReference.get().getMetadata());
        response.put("paymentMethod", orderReference.get().getPaymentMethod().getType().name());
        response.put("receipt", orderReference.get().getReceipt());
        response.put("status", "created");
        response.put("message", "Order created successfully");
        response.put("sessionId", paymentSessionId);
        response.put("sessionStatus", "active");
        return response;
    }

    public Object composeInvalidResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "FAILED");
        response.put("message", message);
        response.put("errorCode", "987");

        return response;
    }

    private SessionInitRequest mapToSessionRequest(Order order) {
        SessionInitRequest sessionInitRequest = new SessionInitRequest();
        sessionInitRequest.setMerchantId(order.getPartnerId());
        sessionInitRequest.setUserId(String.valueOf(order.getCustomer().getId()));
        return sessionInitRequest;
    }

    /**
     * Get configurations from Redis
     */
    public Object getConfigurationsFromRedis(String partnerId, String orderSignature) {
        AtomicReference<Object> result = new AtomicReference<>();
        this.vendorManagementService.getVendor(partnerId).ifPresentOrElse(vendorManager -> {
            Map<String, Object> configurations = new HashMap<>();
            configurations.put("partner_id", partnerId);
            configurations.put("token", vendorManager.getVendorKey());
            configurations.put("status", vendorManager.getVendorStatus());
            configurations.put("valid", vendorManager.getVendorStatus().equalsIgnoreCase("ACTIVE"));
            configurations.put("callback_url", vendorManager.getVendorCallbackUrl());
            configurations.put("hasVfd", vendorManager.getVendorHasVat());
            configurations.put("vfd_type", vendorManager.getVendorVatType());
            /*configurations.put("charges", 0);
            configurations.put("hasVat", vendorManager.getHasVat());
            configurations.put("hasCommission", vendorManager.getHasCommission());
            configurations.put("callbackUrl", vendorManager.getCallbackUrl());
            configurations.put("commissionConfig", vendorManager.getCommissionConfig());
            configurations.put("vat", vendorManager.getVat());
            configurations.put("commissionTireId", vendorManager.getCommissionTireId());
            configurations.put("commissionStatus", vendorManager.getCommissionStatus());*/
            result.set(configurations);
        },() ->{
            result.set(this.getConfigurations(orderSignature, null));
        } );
        return result.get();
    }

    /**
     * Get configurations from Rest API
     */
    private Object getConfigurations(String orderSignature, String operatorName) {


        // Decode the order signature to get the api key and secret
        log.debug("Order Signature: {}", orderSignature);

        String apiKey;
        String apiSecret;

        try {
            // First, check if the signature is already in JSON format
            if (orderSignature.startsWith("{") && orderSignature.endsWith("}")) {
                // Handle JSON format signature
                log.debug("Signature appears to be in JSON format");
                JsonNode jsonNode = new ObjectMapper().readTree(orderSignature);
                apiKey = jsonNode.get("api_key").asText();
                apiSecret = jsonNode.get("api_secret").asText();
            } else {
                // Handle Base64 encoded format
                log.debug("Attempting to decode Base64 signature");

                // Validate Base64 format before decoding
                if (!isValidBase64(orderSignature)) {
                    log.error("Invalid Base64 signature format: {}", orderSignature);
                    throw new RuntimeException("Invalid signature format - not valid Base64 or JSON");
                }

                Base64.Decoder decoder = Base64.getDecoder();
                byte[] decodedBytes = decoder.decode(orderSignature);
                String decodedString = new String(decodedBytes);
                log.debug("Decoded signature: {}", decodedString);

                String[] credentials = decodedString.split(":");
                if (credentials.length != 2) {
                    log.error("Invalid credential format after decoding: {}", decodedString);
                    throw new RuntimeException("Invalid credential format - expected 'apikey:secret'");
                }

                apiKey = credentials[0];
                apiSecret = credentials[1];
            }

            log.debug("Extracted API Key: {}, API Secret: [REDACTED]", apiKey);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON signature: {}", orderSignature, e);
            throw new RuntimeException("Failed to parse signature as JSON", e);
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode Base64 signature: {}", orderSignature, e);
            throw new RuntimeException("Failed to decode Base64 signature: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error processing signature: {}", orderSignature, e);
            throw new RuntimeException("Unexpected error processing signature", e);
        }

        // Validate extracted credentials
        if (apiKey == null || apiKey.trim().isEmpty() || apiSecret == null || apiSecret.trim().isEmpty()) {
            log.error("Invalid API credentials extracted - API Key: {}, API Secret: [{}]",
                    apiKey, (apiSecret == null || apiSecret.trim().isEmpty()) ? "EMPTY" : "PRESENT");
            throw new RuntimeException("Invalid API credentials extracted from signature");
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("api_key", apiKey.trim());
        requestBody.put("api_secret", apiSecret.trim());
        String requestJson = new Gson().toJson(requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(partnerValidationUrl, HttpMethod.POST, entity, String.class);

            // Check if the response is successful
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                //Use JsonNode to parse the response
                try {
                    JsonNode jsonNode = new ObjectMapper().readTree(response.getBody());
                    if (!jsonNode.has("token")) {
                        log.error("Token not found in validation response: {}", response.getBody());
                        throw new RuntimeException("Token not found in validation response");
                    }

                    String token = jsonNode.get("token").asText();
                    if (token == null || token.trim().isEmpty()) {
                        log.error("Empty token received in validation response");
                        throw new RuntimeException("Empty token received in validation response");
                    }


                    // Compose the request body
                    Map<String, Object> responseBody = new HashMap<>();
                    responseBody.put("partner_id", jsonNode.get("partner_id").asText());
                    responseBody.put("status", jsonNode.get("status").asText());
                    responseBody.put("token", jsonNode.get("token").asText());
                    responseBody.put("valid", jsonNode.get("valid").asBoolean());
                    responseBody.put("callback_url", jsonNode.get("callback_url").asText());
                    responseBody.put("hasVfd", jsonNode.get("vfd").asBoolean());
                    if(jsonNode.get("vfd").asBoolean()){
                        responseBody.put("vfd_type", jsonNode.get("vfd_type").asText());
                    }else{
                        responseBody.put("vfd_type", "N/A");
                    }


                    /*// Call Rest API to get the Mobile Network config after successful validation
                    Map<String, String> requestBodyForConfig = new HashMap<>();
                    requestBodyForConfig.put("mno", operatorName);
                    String requestJsonForConfig = new Gson().toJson(requestBodyForConfig);*/

                    /*// Set the headers for the request to get the Mobile Network config
                    HttpHeaders configHeaders = new HttpHeaders();
                    configHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    configHeaders.set("Authorization", "Bearer " + token);
                    HttpEntity<String> configEntity = new HttpEntity<>(requestJsonForConfig, configHeaders);

                    ResponseEntity<String> config_response = restTemplate.exchange(networkConfigUrl, HttpMethod.POST, configEntity, String.class);
                    if (config_response.getStatusCode().is2xxSuccessful() && config_response.getBody() != null) {
                        log.debug("Successfully retrieved network configuration for operator: {}", operatorName);
                        JsonNode credentialNode = new ObjectMapper().readTree(config_response.getBody());
                        return credentialNode.get("credential").asText();
                    } else {
                        log.error("Failed to retrieve network configuration. Status: {}, Body: {}",
                                config_response.getStatusCode(), config_response.getBody());
                        throw new RuntimeException("Failed to retrieve network configuration");
                    }*/

                    return responseBody;
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse validation response: {}", response.getBody(), e);
                    throw new RuntimeException("Failed to parse validation response", e);
                }
            } else {
                log.error("Partner validation failed. Status: {}, Body: {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to validate the partner details");
            }
        } catch (Exception e) {
            log.error("Error during API calls for operator: {}", operatorName, e);
            throw new RuntimeException("Error during network configuration retrieval", e);
        }
    }

    /**
     * Validates if a string is valid Base64 format
     *
     * @param str the string to validate
     * @return true if valid Base64, false otherwise
     */
    private boolean isValidBase64(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }

        try {
            // Base64 strings should only contain A-Z, a-z, 0-9, +, /, and = for padding
            // Length should be multiple of 4 (with padding)
            String cleaned = str.replaceAll("\\s", ""); // Remove whitespace

            if (cleaned.length() % 4 != 0) {
                return false;
            }

            // Check for valid Base64 characters
            if (!cleaned.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                return false;
            }

            // Try to decode to verify it's valid
            Base64.getDecoder().decode(cleaned);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    public static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }

    public static class InvalidSessionException extends RuntimeException {
        public InvalidSessionException(String message) {
            super(message);
        }
    }

    public static class InvalidVendorInfoException extends RuntimeException {
        public InvalidVendorInfoException(String message) {
            super(message);
        }
    }
}
