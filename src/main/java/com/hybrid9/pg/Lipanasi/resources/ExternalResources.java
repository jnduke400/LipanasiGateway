package com.hybrid9.pg.Lipanasi.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.dto.VendorCreatorDto;
import com.hybrid9.pg.Lipanasi.dto.commission.PaymentChannelConfig;
import com.hybrid9.pg.Lipanasi.dto.commission.PaymentMethodConfig;
import com.hybrid9.pg.Lipanasi.dto.customer.CustomerDto;
import com.hybrid9.pg.Lipanasi.dto.operator.OperatorResponseDto;
import com.hybrid9.pg.Lipanasi.dto.order.OrderRequestDto;
import com.hybrid9.pg.Lipanasi.dto.order.VendorInfo;
import com.hybrid9.pg.Lipanasi.dto.payscoopconfig.MerchantCharge;
import com.hybrid9.pg.Lipanasi.dto.payscoopconfig.MerchantChargesResponse;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoMapping;
import com.hybrid9.pg.Lipanasi.entities.operators.MnoPrefix;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannelConfig;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethodConfigEntity;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CommissionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.OperatorMapping;
import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorManager;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.excpts.CustomExcpts;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelConfigServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelServiceImpl;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTierService;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.PaymentMethodConfigService;
import com.hybrid9.pg.Lipanasi.services.payments.PaymentMethodService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.OperatorManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorNetworkChargesService;
import com.hybrid9.pg.Lipanasi.services.payscoopconfig.PayScoopApiService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExternalResources {

    private final Object commissionTierLock = new Object();

    @Value("${partner.validation.url:https://api.business.payscoop.com/api/validate}")
    private String partnerValidationUrl;

    @Value("${network.config.url:https://api.business.payscoop.com/api/collection-credential}")
    private String networkConfigUrl;

    private final VendorResource vendorResource;
    private final VendorService vendorService;
    private final CommissionTierService commissionTierService;
    private final PaymentMethodService paymentMethodService;
    private final PaymentMethodConfigService paymentMethodConfigService;
    private final MobileMoneyChannelConfigServiceImpl mobileMoneyChannelConfigService;
    private final MobileMoneyChannelServiceImpl mobileMoneyChannelService;
    private final MnoServiceImpl mnoService;
    private final OperatorManagementService operatorManagementService;
    private final PaymentUtilities paymentUtilities;
    private final RestTemplate restTemplate;
    private final OrderService orderService;
    private final VendorManagementService vendorManagementService;
    private final PayScoopApiService payScoopApiService;
    private final VendorNetworkChargesService vendorNetworkChargesService;

    public ExternalResources(VendorResource vendorResource, VendorService vendorService,
                             CommissionTierService commissionTierService, PaymentMethodService paymentMethodService,
                             PaymentMethodConfigService paymentMethodConfigService, MobileMoneyChannelConfigServiceImpl mobileMoneyChannelConfigService,
                             MobileMoneyChannelServiceImpl mobileMoneyChannelService, MnoServiceImpl mnoService,
                             OperatorManagementService operatorManagementService, PaymentUtilities paymentUtilities,
                             RestTemplate restTemplate, OrderService orderService, VendorManagementService vendorManagementService,
                             PayScoopApiService payScoopApiService, VendorNetworkChargesService vendorNetworkChargesService) {
        this.vendorResource = vendorResource;
        this.vendorService = vendorService;
        this.commissionTierService = commissionTierService;
        this.paymentMethodService = paymentMethodService;
        this.paymentMethodConfigService = paymentMethodConfigService;
        this.mobileMoneyChannelConfigService = mobileMoneyChannelConfigService;
        this.mobileMoneyChannelService = mobileMoneyChannelService;
        this.mnoService = mnoService;
        this.operatorManagementService = operatorManagementService;
        this.paymentUtilities = paymentUtilities;
        this.restTemplate = restTemplate;
        this.orderService = orderService;
        this.vendorManagementService = vendorManagementService;
        this.payScoopApiService = payScoopApiService;
        this.vendorNetworkChargesService = vendorNetworkChargesService;
    }

    @Autowired
    @Qualifier("externalOrderProcessorVirtualThread")
    private ExecutorService externalOrderProcessorVirtualThread;


    public VendorInfo validateVendor(Order order, VendorManagementService vendorManagementService, String operatorName, Optional<VendorManager> vendorFromRedis) {
        vendorFromRedis.orElseThrow(() -> new CustomExcpts.VendorNotFoundException("Vendor not found"));
        try {
            // This is a dummy response
            // In a real application, this would be a call to a service that would validate the vendorx
            /**
             * This is a dummy response
             * In a real application, this would be a call to a service that would validate the vendorx
             */
            log.debug(">>>>>>>>>>>>>Vendor External Id: " + vendorFromRedis.get().getVendorExternalId());
            log.debug(">>>>>>>>>>>>>Order Signature: " + order.getSignature());

            // Get merchant basic configurations from Redis
            Object configurations = this.getConfigurationsFromRedis(vendorFromRedis.get().getVendorExternalId(), order.getSignature());
            String json = new Gson().toJson(configurations);
            JsonNode jsonNode = new ObjectMapper().readTree(json);
            String partnerId = jsonNode.get("partner_id").asText();
            String status = jsonNode.get("status").asText();
            String token = jsonNode.get("token").asText();
            boolean valid = jsonNode.get("valid").asBoolean();
            String hasVfd = jsonNode.get("hasVfd").asText();
            String vfdType = jsonNode.get("vfd_type").asText();
            String callbackUrl = jsonNode.get("callback_url").asText();

            // log values
            log.debug("partnerId: " + partnerId);
            log.debug("status: " + status);
            log.debug("token: " + token);
            log.debug("valid: " + valid);
            log.debug("callbackUrl: " + callbackUrl);

            log.debug("order.getPartnerId(): " + order.getPartnerId());

            // Get merchant charges related configurations from PayScoop API or Redis
            MerchantChargesResponse merchantChargesResponse = this.vendorNetworkChargesService
                    .getVendor(switchOperator(operatorName) + "-" + order.getPartnerId())
                    .map(vendor -> {
                        // Data retrieved from Redis - create response from vendor data
                        MerchantChargesResponse redisResponse = new MerchantChargesResponse();
                        redisResponse.setSuccess(true);
                        redisResponse.setData(new ArrayList<>());

                        // Create merchant charge from vendor data
                        MerchantCharge merchantCharge = new MerchantCharge();
                        merchantCharge.setChargeType("collection");
                        merchantCharge.setMerchantId(vendor.getMerchantId());
                        merchantCharge.setMno(vendor.getMno());
                        merchantCharge.setRate(vendor.getRate());
                        merchantCharge.setIsActive(vendor.getIsActive());

                        redisResponse.getData().add(merchantCharge);
                        return redisResponse;
                    })
                    .orElseGet(() -> {
                        // Data retrieved from PayScoop API
                        return operatorName.contains("Mpesa") ?
                                this.payScoopApiService.getVodacomCollectionCharges(vendorFromRedis.get().getVendorKey())
                                : operatorName.contains("Airtel") ? this.payScoopApiService.getAirtelCollectionCharges(vendorFromRedis.get().getVendorKey())
                                : operatorName.contains("Halopesa") ? this.payScoopApiService.getHalopesaCollectionCharges(vendorFromRedis.get().getVendorKey())
                                : operatorName.contains("Mixx") ? this.payScoopApiService.getMixxCollectionCharges(vendorFromRedis.get().getVendorKey())
                                : this.payScoopApiService.getcrdbCollectionCharges(vendorFromRedis.get().getVendorKey());
                    });

            log.debug("merchantChargesResponse.getData().getFirst().getMerchantId(): " + merchantChargesResponse.getData().getFirst().getMerchantId());


            // Validate the merchant charges response
            if (!merchantChargesResponse.getSuccess()) {
                throw new RuntimeException("Failed to get merchant charges from PayScoop API");
            }

            if (merchantChargesResponse.getData().getFirst().getMerchantId() != Long.parseLong(partnerId)) {
                throw new RuntimeException("Invalid merchant ID in PayScoop API response");
            }

            String vendorInfo = "{\n" +
                    "  \"partnerId\": \"" + partnerId + "\",\n" +
                    "  \"apiKey\": \"" + token + "\",\n" +
                    "  \"status\": \"" + status + "\",\n" +
                    "  \"charges\": 0,\n" +
                    "  \"hasVat\": \""+hasVfd+"\",\n" +
                    "  \"vfdType\":\""+vfdType+"\",\n" +
                    "  \"hasCommission\": \"true\",\n" +
                    "  \"callbackUrl\": \"" + callbackUrl + "\",\n" +
                    "  \"commissionConfig\": {\n" +
                    "    \"minimumAmount\": 0,\n" +
                    "    \"maximumAmount\": 1000000000,\n" +
                    "    \"baseFee\": 0,\n" +
                    "    \"percentageRate\": " + merchantChargesResponse.getData().getFirst().getRate() + ",\n" +
                    "    \"commissionStatus\": \"" +
                    (merchantChargesResponse.getData().getFirst().getIsActive() ? "ACTIVE" : "INACTIVE") +
                    "\",\n" +
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
                    "        \"commissionStatus\": \"" + (operatorName.contains("Airtel") ? merchantChargesResponse.getData().getFirst().getIsActive() ? "ACTIVE" : "INACTIVE" : "ACTIVE") + "\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"mobileOperator\": \"Mixx_by_yas-Tanzania\",\n" +
                    "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                    "        \"commissionStatus\": \"" + (operatorName.contains("Mixx") ? merchantChargesResponse.getData().getFirst().getIsActive() ? "ACTIVE" : "INACTIVE" : "ACTIVE") + "\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"mobileOperator\": \"Mpesa-Tanzania\",\n" +
                    "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                    "        \"commissionStatus\": \"" + (operatorName.contains("Mpesa") ? merchantChargesResponse.getData().getFirst().getIsActive() ? "ACTIVE" : "INACTIVE" : "ACTIVE") + "\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"mobileOperator\": \"Halopesa-Tanzania\",\n" +
                    "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                    "        \"commissionStatus\": \"" + (operatorName.contains("Halopesa") ? merchantChargesResponse.getData().getFirst().getIsActive() ? "ACTIVE" : "INACTIVE" : "ACTIVE") + "\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"mobileOperator\": \"Tpesa-Tanzania\",\n" +
                    "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                    "        \"commissionStatus\": \"" + (operatorName.contains("Tpesa") ? merchantChargesResponse.getData().getFirst().getIsActive() ? "ACTIVE" : "INACTIVE" : "ACTIVE") + "\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"mobileOperator\": \"ZPesa-Tanzania\",\n" +
                    "        \"mobileMoneyChannel\": \"PUSH_USSD\",\n" +
                    "        \"commissionStatus\": \"" + (operatorName.contains("ZPesa") ? merchantChargesResponse.getData().getFirst().getIsActive() ? "ACTIVE" : "INACTIVE" : "ACTIVE") + "\"\n" +
                    "      },\n" +
                    "      {\n" +
                    "        \"mobileOperator\": \"CRDB\",\n" +
                    "        \"mobileMoneyChannel\": \"BANK_PAYMENT_GATEWAY\",\n" +
                    "        \"commissionStatus\": \"" + (operatorName.contains("CRDB") ? merchantChargesResponse.getData().getFirst().getIsActive() ? "ACTIVE" : "INACTIVE" : "ACTIVE") + "\"\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }\n" +
                    "}\n";
            VendorInfo vendorData = this.parseVendorInfo(vendorInfo);
            log.debug("Vendor Info >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + vendorData);
            OrderRequestDto orderRequestDto = OrderRequestDto.builder()
                    .amount((int) order.getAmount())
                    .currency(order.getCurrency())
                    .receipt(order.getReceipt())
                    //.paymentMethod(order.getPaymentMethod().getType().name())
                    //.paymentChannel(order.getChannel())
                    .metadata(order.getMetadata())
                    .customers(CustomerDto.builder()
                            .firstName(order.getCustomer().getFirstName())
                            .lastName(order.getCustomer().getLastName())
                            .email(order.getCustomer().getEmail())
                            .build())
                    .build();
            // check if vendorx exists in Redis
            Optional<VendorManager> vendorManager = vendorManagementService.getVendor(vendorData.getPartnerId());
            vendorManager.ifPresentOrElse(vendor -> {

                // update vendor data
                vendor.setVendorKey(vendorData.getApiKey());
                vendor.setVendorStatus(vendorData.getStatus());
                vendor.setVendorCallbackUrl(vendorData.getCallbackUrl());
                vendor.setVendorHasCommission(vendorData.getHasCommission());
                vendor.setVendorHasVat(vendorData.getHasVat());
                vendor.setVendorCharges(vendorData.getCharges());
                vendor.setVendorVatType(vendorData.getVfdType());

                vendorManagementService.updateVendor(vendorData.getPartnerId(), vendor);

                // update vendor details in database - this is needed for commission calculation and callback
                this.vendorResource.updateVendor(vendorData);
            }, () -> {
                VendorCreatorDto vendor = vendorResource.createVendorObject(vendorData, orderRequestDto);
                // create a vendorx
                vendorResource.createVendor(vendor, vendorManagementService, orderRequestDto);
            });

            // return partner / vendorx configuration info
            return vendorData;
        } catch (Exception e) {
            throw new OrderResource.InvalidVendorInfoException("VendorDetails info response is invalid");
        }

    }

    public VendorInfo parseVendorInfo(String vendorInfo) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            VendorInfo vendorInfoResult = objectMapper.readValue(vendorInfo, VendorInfo.class);
            if (vendorInfoResult.getStatus() == null) {
                throw new OrderResource.InvalidVendorInfoException("VendorDetails info response is invalid");
            }
            return vendorInfoResult;
        } catch (Exception e) {
            throw new OrderResource.InvalidVendorInfoException("VendorDetails info parsing failed");
        }
    }

    public CompletableFuture<CommissionTier> createCommissionTire(VendorInfo vendorData, OrderRequestDto orderRequestDto, String paymentMethod, String paymentChannel, MnoMapping operator) {
        return CompletableFuture.supplyAsync(() -> {
            MobileMoneyChannel paymentChannelResult = this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannel));
            CommissionTier commissionTireResult = this.commissionTierService.findCommissionTierByVendorDetailsAndOperatorAndPaymentChannel(
                    Optional.of(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()))
                            .orElseThrow(() -> new RuntimeException("Vendor not found")), operator, paymentChannelResult);

            if (commissionTireResult == null) {
                CommissionTier commissionTier = CommissionTier.builder()
                        .vendor(Optional.of(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()))
                                .orElseThrow(() -> new RuntimeException("Vendor not found")))
                        .paymentMethod(this.getPaymentMethod(paymentMethod))
                        .paymentChannel(paymentChannelResult)
                        .operator(operator)
                        .minimumAmount(vendorData.getCommissionConfig().getMinimumAmount())
                        .maximumAmount(vendorData.getCommissionConfig().getMaximumAmount())
                        .baseFee(vendorData.getCommissionConfig().getBaseFee())
                        .percentageRate(vendorData.getCommissionConfig().getPercentageRate())
                        .isActive(vendorData.getCommissionConfig().getCommissionStatus().equalsIgnoreCase("active"))
                        .build();
                return this.commissionTierService.createCommissionTier(commissionTier);
            } else {
                commissionTireResult.setPaymentMethod(this.getPaymentMethod(paymentMethod));
                commissionTireResult.setPaymentChannel(this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannel)));
                commissionTireResult.setOperator(operator);
                commissionTireResult.setMinimumAmount(vendorData.getCommissionConfig().getMinimumAmount());
                commissionTireResult.setMaximumAmount(vendorData.getCommissionConfig().getMaximumAmount());
                commissionTireResult.setBaseFee(vendorData.getCommissionConfig().getBaseFee());
                commissionTireResult.setPercentageRate(vendorData.getCommissionConfig().getPercentageRate());
                commissionTireResult.setIsActive(vendorData.getCommissionConfig().getCommissionStatus().equalsIgnoreCase("active"));
                return this.commissionTierService.updateCommissionTier(commissionTireResult);
            }
        }, externalOrderProcessorVirtualThread);
    }
    @Retryable(value = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public CommissionConfig getCommissionConfig(VendorInfo vendorInfo, CommissionTier commissionTier, PaymentMethod paymentMethod, String paymentChannel, Optional<OperatorMapping> operatorMapping) {
        operatorMapping.orElseThrow(() -> new RuntimeException("Operator not found for prefix: "));
        CommissionConfig commissionConfig = new CommissionConfig();
        commissionConfig.setCommissionTireId(String.valueOf(commissionTier.getId()));
        commissionConfig.setCommissionStatus(vendorInfo.getCommissionConfig().getCommissionStatus().equalsIgnoreCase("active") ? CommissionConfig.CommissionStatus.ACTIVE : CommissionConfig.CommissionStatus.INACTIVE);
        commissionConfig.setPaymentMethod(paymentMethodService.findByType(paymentMethod.getType()));
        commissionConfig.setPaymentMethodName(paymentMethod.getType().name());
        commissionConfig.setPaymentMethodChanelName(paymentChannel);
        //commissionConfig.setMobileMoneyChannelConfig(mobileMoneyChannelConfigService.findByMobileMoneyChannelAndVendorAndMobileOperator(mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannel)), Optional.of(vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId())).orElseThrow(() -> new RuntimeException("Vendor not found")),operatorMapping.get().getMnoMapping()).orElseThrow(() -> new RuntimeException("Mobile Money Channel not found")));
        //commissionConfig.setVendorId(Optional.of(vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId())).orElseThrow(() -> new RuntimeException("Vendor not found")).getId().toString());
        commissionConfig.setBaseFee(vendorInfo.getCommissionConfig().getBaseFee());
        commissionConfig.setMaximumAmount(vendorInfo.getCommissionConfig().getMaximumAmount());
        commissionConfig.setMinimumAmount(vendorInfo.getCommissionConfig().getMinimumAmount());
        commissionConfig.setPercentageRate(vendorInfo.getCommissionConfig().getPercentageRate());
        commissionConfig.setPaymentMethodConfig(vendorInfo.getCommissionConfig().getPaymentMethodConfigs().stream().collect(Collectors.toMap(PaymentMethodConfig::getPaymentMethod, Function.identity())));
        commissionConfig.setPaymentChannelConfig(vendorInfo.getCommissionConfig().getPaymentChannelConfigs().stream().collect(Collectors.toMap(config -> config.getMobileOperator() + "-" + config.getMobileMoneyChannel(), Function.identity())));

        return commissionConfig;
    }

    @Retryable(value = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public CommissionConfig updateCommissionConfig(UserSession userSession, PaymentMethod paymentMethod, String paymentChannel, Optional<OperatorMapping> operatorMapping) {
        operatorMapping.orElseThrow(() -> new RuntimeException("Operator not found for prefix: "));
        CommissionConfig commissionConfig = new CommissionConfig();
        commissionConfig.setCommissionTireId(String.valueOf(userSession.getCommissionConfig().getCommissionTireId()));
        commissionConfig.setCommissionStatus(userSession.getCommissionConfig().getCommissionStatus().name().equalsIgnoreCase("ACTIVE") ? CommissionConfig.CommissionStatus.ACTIVE : CommissionConfig.CommissionStatus.INACTIVE);
        commissionConfig.setPaymentMethod(paymentMethodService.findByType(paymentMethod.getType()));
        commissionConfig.setPaymentMethodName(paymentMethod.getType().name());
        commissionConfig.setPaymentMethodChanelName(paymentChannel);
        // commissionConfig.setMobileMoneyChannelConfig(mobileMoneyChannelConfigService.findByMobileMoneyChannelAndVendorAndMobileOperator(mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannel)), Optional.of(vendorService.findVendorDetailsByVendorExternalId(userSession.getMerchantId())).orElseThrow(() -> new RuntimeException("Vendor not found")),operatorMapping.get().getMnoMapping()).orElseThrow(() -> new RuntimeException("Mobile Money Channel not found")));
        //commissionConfig.setVendorId(Optional.of(vendorService.findVendorDetailsByVendorExternalId(userSession.getMerchantId())).orElseThrow(() -> new RuntimeException("Vendor not found")).getId().toString());
        commissionConfig.setBaseFee(userSession.getCommissionConfig().getBaseFee());
        commissionConfig.setMaximumAmount(userSession.getCommissionConfig().getMaximumAmount());
        commissionConfig.setMinimumAmount(userSession.getCommissionConfig().getMinimumAmount());
        commissionConfig.setPercentageRate(userSession.getCommissionConfig().getPercentageRate());
        commissionConfig.setPaymentMethodConfig(userSession.getCommissionConfig().getPaymentMethodConfig());
        commissionConfig.setPaymentChannelConfig(userSession.getCommissionConfig().getPaymentChannelConfig());

        synchronized (commissionTierLock) {
            // update commission tier
            commissionTierService.findByCommissionTireId(Long.parseLong(commissionConfig.getCommissionTireId())).ifPresent(commissionTier -> {
                commissionTier.setPaymentMethod(paymentMethodService.findByType(paymentMethod.getType()));
                commissionTierService.updateCommissionTier(commissionTier);
            });
        }

        return commissionConfig;
    }

    public CompletableFuture<Void> createPaymentChannel(OrderRequestDto orderRequestDto, VendorInfo vendorData) {
        return CompletableFuture.runAsync(() -> {
            VendorDetails vendorDetails = Optional.of(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()))
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
        }, externalOrderProcessorVirtualThread);
    }

    public CompletableFuture<Void> createPaymentMethod(VendorInfo vendorData) {
        return CompletableFuture.runAsync(() -> {
            VendorDetails vendorDetails = Optional.of(this.vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId()))
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
        }, externalOrderProcessorVirtualThread);
    }

    public Optional<OperatorMapping> getOperator(String phoneNumber) {
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

    public PaymentMethod getPaymentMethod(String paymentMethod) {
        return switch (paymentMethod.toLowerCase()) {
            case "card" -> this.paymentMethodService.findByType(PaymentMethodType.valueOf("CREDIT_CARD"));
            case "bank" -> this.paymentMethodService.findByType(PaymentMethodType.valueOf("BANK_TRANSFER"));
            case "mobile" -> this.paymentMethodService.findByType(PaymentMethodType.valueOf("MOBILE_MONEY"));
            case "cash" -> this.paymentMethodService.findByType(PaymentMethodType.valueOf("CASH_ON_DELIVERY"));
            default -> throw new OrderResource.InvalidRequestException("Invalid payment method");
        };
    }

    public PaymentChannel getPaymentChannel(String paymentChannel) {
        return switch (paymentChannel.toLowerCase()) {
            case "pay_bill" -> PaymentChannel.PAY_BILL;
            case "push_ussd" -> PaymentChannel.PUSH_USSD;
            case "bank_gateway" -> PaymentChannel.BANK_PAYMENT_GATEWAY;
            default -> throw new OrderResource.InvalidRequestException("Invalid payment channel");
        };
    }

    public void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new CustomExcpts.PhoneNumberException("Phone number is required");
        }

        // Remove spaces or hyphens if present
        String cleanedNumber = phoneNumber.replaceAll("[\\s-]", "");

        // Check for valid prefixes (255, +255, or 0)
        if (!cleanedNumber.matches("^(\\+?255|0)\\d+$")) {
            throw new CustomExcpts.PhoneNumberException("Phone number is not a valid Tanzanian number");
        }

        // Validate length based on prefix
        if (cleanedNumber.startsWith("0") && cleanedNumber.length() != 10) {
            throw new CustomExcpts.PhoneNumberException("Local Tanzanian numbers must be 10 digits (e.g., 0688044555)");
        }
        if ((cleanedNumber.startsWith("255") || cleanedNumber.startsWith("+255"))
                && cleanedNumber.length() != 12 && cleanedNumber.length() != 13) {
            throw new CustomExcpts.PhoneNumberException("International Tanzanian numbers must be 12 digits (255...) or 13 digits (+255...)");
        }
    }

    public OperatorResponseDto prepareOperatorResponse(String message, String status) {
        return OperatorResponseDto.builder()
                .message(message)
                .status(status)
                .build();

    }
    @Retryable(value = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public CommissionConfig getCommissionConfig(VendorInfo vendorInfo, CommissionTier commissionTier, PaymentMethod paymentMethod, String paymentChannel) {

        CommissionConfig commissionConfig = new CommissionConfig();
        commissionConfig.setCommissionTireId(String.valueOf(commissionTier.getId()));
        commissionConfig.setCommissionStatus(vendorInfo.getCommissionConfig().getCommissionStatus().equalsIgnoreCase("active") ? CommissionConfig.CommissionStatus.ACTIVE : CommissionConfig.CommissionStatus.INACTIVE);
        commissionConfig.setPaymentMethod(paymentMethodService.findByType(paymentMethod.getType()));
        commissionConfig.setPaymentMethodName(paymentMethod.getType().name());
        commissionConfig.setPaymentMethodChanelName(paymentChannel);
        //commissionConfig.setMobileMoneyChannelConfig(mobileMoneyChannelConfigService.findByMobileMoneyChannelAndVendorAndMobileOperator(mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannel)), Optional.of(vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId())).orElseThrow(() -> new RuntimeException("Vendor not found")),operatorMapping.get().getMnoMapping()).orElseThrow(() -> new RuntimeException("Mobile Money Channel not found")));
        //commissionConfig.setVendorId(Optional.of(vendorService.findVendorDetailsByVendorExternalId(vendorData.getPartnerId())).orElseThrow(() -> new RuntimeException("Vendor not found")).getId().toString());
        commissionConfig.setBaseFee(vendorInfo.getCommissionConfig().getBaseFee());
        commissionConfig.setMaximumAmount(vendorInfo.getCommissionConfig().getMaximumAmount());
        commissionConfig.setMinimumAmount(vendorInfo.getCommissionConfig().getMinimumAmount());
        commissionConfig.setPercentageRate(vendorInfo.getCommissionConfig().getPercentageRate());
        commissionConfig.setPaymentMethodConfig(vendorInfo.getCommissionConfig().getPaymentMethodConfigs().stream().collect(Collectors.toMap(PaymentMethodConfig::getPaymentMethod, Function.identity())));
        commissionConfig.setPaymentChannelConfig(vendorInfo.getCommissionConfig().getPaymentChannelConfigs().stream().collect(Collectors.toMap(config -> config.getMobileOperator() + "-" + config.getMobileMoneyChannel(), Function.identity())));

        return commissionConfig;
    }
    @Retryable(value = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public CommissionConfig updateCommissionConfig(UserSession userSession, PaymentMethod paymentMethod, String paymentChannel) {
        CommissionConfig commissionConfig = new CommissionConfig();
        commissionConfig.setCommissionTireId(String.valueOf(userSession.getCommissionConfig().getCommissionTireId()));
        commissionConfig.setCommissionStatus(userSession.getCommissionConfig().getCommissionStatus().name().equalsIgnoreCase("ACTIVE") ? CommissionConfig.CommissionStatus.ACTIVE : CommissionConfig.CommissionStatus.INACTIVE);
        commissionConfig.setPaymentMethod(paymentMethodService.findByType(paymentMethod.getType()));
        commissionConfig.setPaymentMethodName(paymentMethod.getType().name());
        commissionConfig.setPaymentMethodChanelName(paymentChannel);
        // commissionConfig.setMobileMoneyChannelConfig(mobileMoneyChannelConfigService.findByMobileMoneyChannelAndVendorAndMobileOperator(mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannel)), Optional.of(vendorService.findVendorDetailsByVendorExternalId(userSession.getMerchantId())).orElseThrow(() -> new RuntimeException("Vendor not found")),operatorMapping.get().getMnoMapping()).orElseThrow(() -> new RuntimeException("Mobile Money Channel not found")));
        //commissionConfig.setVendorId(Optional.of(vendorService.findVendorDetailsByVendorExternalId(userSession.getMerchantId())).orElseThrow(() -> new RuntimeException("Vendor not found")).getId().toString());
        commissionConfig.setBaseFee(userSession.getCommissionConfig().getBaseFee());
        commissionConfig.setMaximumAmount(userSession.getCommissionConfig().getMaximumAmount());
        commissionConfig.setMinimumAmount(userSession.getCommissionConfig().getMinimumAmount());
        commissionConfig.setPercentageRate(userSession.getCommissionConfig().getPercentageRate());
        commissionConfig.setPaymentMethodConfig(userSession.getCommissionConfig().getPaymentMethodConfig());
        commissionConfig.setPaymentChannelConfig(userSession.getCommissionConfig().getPaymentChannelConfig());

        synchronized (commissionTierLock) {
            // update commission tier
            commissionTierService.findByCommissionTireId(Long.parseLong(commissionConfig.getCommissionTireId())).ifPresent(commissionTier -> {
                commissionTier.setPaymentMethod(paymentMethodService.findByType(paymentMethod.getType()));
                commissionTierService.updateCommissionTier(commissionTier);
            });
        }

        return commissionConfig;
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
        }, () -> {
            result.set(this.getConfigurations(orderSignature, null));
        });
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


    private String switchOperator(String operatorName) {
        if (operatorName.toLowerCase().contains("mpesa")) {
            return "Vodacom";
        } else if (operatorName.toLowerCase().contains("mixx")) {
            return "Mixx";
        } else if (operatorName.toLowerCase().contains("halopesa")) {
            return "Halopesa";

        } else if (operatorName.toLowerCase().contains("airtel")) {
            return "Airtel";

        } else if (operatorName.toLowerCase().contains("crdb")) {
            return "CRDB";
        } else {
            return "TPESA";
        }

    }
}
