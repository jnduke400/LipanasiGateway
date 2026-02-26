package com.hybrid9.pg.Lipanasi.route.resources;

import com.hybrid9.pg.Lipanasi.dto.deposit.DepositRequest;
import com.hybrid9.pg.Lipanasi.dto.VendorDto;
import com.hybrid9.pg.Lipanasi.dto.deposit.VendorInfo;
import com.hybrid9.pg.Lipanasi.entities.operators.MobileMoneyChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.entities.payments.pymtmethods.PaymentMethod;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorManager;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.ExternalResources;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelServiceImpl;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionService;
import com.hybrid9.pg.Lipanasi.services.payments.PaymentMethodService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.payments.vendorx.VendorManagementService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class DepositResources {
    @Value("${order.session.expiry.default:30}")
    private Integer DEFAULT_SESSION_EXPIRY;

    @Autowired
    @Qualifier("ioExecutor")
    private Executor ioExecutor;

    @Autowired
    @Qualifier("depositProcessorVirtualThread")
    private ExecutorService depositProcessorVirtualThread;

    @Autowired
    @Qualifier("commissionProcessorVirtualThread")
    private ExecutorService commissionProcessorVirtualThread;

    private final VendorManagementService vendorManagementService;
    private final SessionManagementService sessionManagementService;
    private final CommissionService commissionService;
    private final VendorService vendorService;
    private final PaymentMethodService paymentMethodService;
    private final ExternalResources externalResources;
    private final MobileMoneyChannelServiceImpl mobileMoneyChannelService;
    private final PushUssdRefService pushUssdRefService;
    private final PushUssdService pushUssdService;
    private final MnoServiceImpl mnoService;

    public DepositResources(VendorManagementService vendorManagementService, SessionManagementService sessionManagementService,
                            CommissionService commissionService, VendorService vendorService, PaymentMethodService paymentMethodService,
                            ExternalResources externalResources, MobileMoneyChannelServiceImpl mobileMoneyChannelService,
                            PushUssdRefService pushUssdRefService, PushUssdService pushUssdService,MnoServiceImpl mnoService) {
        this.vendorManagementService = vendorManagementService;
        this.sessionManagementService = sessionManagementService;
        this.commissionService = commissionService;
        this.vendorService = vendorService;
        this.paymentMethodService = paymentMethodService;
        this.externalResources = externalResources;
        this.mobileMoneyChannelService = mobileMoneyChannelService;
        this.pushUssdRefService = pushUssdRefService;
        this.pushUssdService = pushUssdService;
        this.mnoService = mnoService;
    }

    /**
     * Process commission by checking if session is active or not
     *
     * @param vendorDto
     * @param sessionId
     * @param amount
     * @param msisdn
     * @param collectionType
     * @param paymentReference
     * @param pushUssd
     * @param payBillPayment
     * @return
     */
    @Async("commissionProcessorVirtualThread")
    public CompletableFuture<Void> processCommission(VendorDto vendorDto, String sessionId, BigDecimal amount, String msisdn, String collectionType, String paymentReference, PushUssd pushUssd, PayBillPayment payBillPayment, CardPayment cardPayment) {
        log.debug("Processing commission for sessionId: " + sessionId);
        return CompletableFuture.runAsync(() -> {
            sessionManagementService.getSession(sessionId).ifPresentOrElse(session -> {
                if (session.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                    // session expired
                    PaymentMethodType paymentMethodType = PaymentMethodType.valueOf("MOBILE_MONEY");
                    PaymentMethod paymentMethod = this.paymentMethodService.findByType(paymentMethodType);
                    MobileMoneyChannel mobileMoneyChannel = this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(collectionType));
                    this.externalResources.getOperator(msisdn).ifPresent(operator -> {
                        Optional.of(this.vendorService.findVendorDetailsByVendorExternalId(vendorDto.getExternalId())).ifPresent(vendor -> {
                            if (vendor.getHasCommission().equalsIgnoreCase("true")) {
                                this.commissionService.calculateCommission(vendor.getVendorExternalId(), amount, paymentMethodType, paymentMethod.getId(), operator.getMnoMapping().getId(), mobileMoneyChannel.getId(), paymentReference,pushUssd,payBillPayment, cardPayment).join();
                            }
                        });
                    });

                } else {
                    // session is active
                    PaymentMethodType paymentMethodType = session.getCommissionConfig().getPaymentMethod().getType();
                    String paymentChanelName = session.getCommissionConfig().getPaymentMethodChanelName();
                    this.vendorManagementService.getVendor(vendorDto.getExternalId()).ifPresent(vendorManager -> {
                        if (vendorManager.getVendorHasCommission().equalsIgnoreCase("true")) {
                            this.commissionService.calculateCommissionWhenSessionIsActive(amount, paymentMethodType, msisdn, paymentChanelName, session, vendorManager, paymentReference,pushUssd,payBillPayment, cardPayment).join();
                        }
                    });
                }
            }, () -> {
                // session expired
                PaymentMethodType paymentMethodType = PaymentMethodType.valueOf("MOBILE_MONEY");
                PaymentMethod paymentMethod = this.paymentMethodService.findByType(paymentMethodType);
                MobileMoneyChannel mobileMoneyChannel = this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(collectionType));
                this.externalResources.getOperator(msisdn).ifPresent(operator -> {
                    Optional.of(this.vendorService.findVendorDetailsByVendorExternalId(vendorDto.getExternalId())).ifPresent(vendor -> {
                        if (vendor.getHasCommission().equalsIgnoreCase("true")) {
                            this.commissionService.calculateCommission(vendor.getVendorExternalId(), amount, paymentMethodType, paymentMethod.getId(), operator.getMnoMapping().getId(), mobileMoneyChannel.getId(), paymentReference,pushUssd, payBillPayment, cardPayment).join();
                        }
                    });
                });
            });
        }, commissionProcessorVirtualThread);

    }


    /**
     * Process commission by checking if session is active or not
     *
     * @param vendorDto
     * @param sessionId
     * @param amount
     * @param msisdn
     * @param collectionType
     * @param paymentReference
     * @return
     */
    @Async("commissionProcessorVirtualThread")
    public CompletableFuture<Void> processBankCommission(VendorDto vendorDto, String sessionId, BigDecimal amount, String msisdn, String collectionType, String operator, String paymentReference,PushUssd pushUssd, CardPayment cardPayment) {
        log.debug("Processing commission for sessionId: " + sessionId);
        return CompletableFuture.runAsync(() -> {
            sessionManagementService.getSession(sessionId).ifPresentOrElse(session -> {
                if (session.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                    // session expired
                    PaymentMethodType paymentMethodType = PaymentMethodType.valueOf("CREDIT_CARD");
                    PaymentMethod paymentMethod = this.paymentMethodService.findByType(paymentMethodType);
                    MobileMoneyChannel mobileMoneyChannel = this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(collectionType));

                    Optional.of(this.vendorService.findVendorDetailsByVendorExternalId(vendorDto.getExternalId())).ifPresent(vendor -> {
                        if (vendor.getHasCommission().equalsIgnoreCase("true")) {
                            this.commissionService.calculateCommission(vendor.getVendorExternalId(), amount, paymentMethodType, paymentMethod.getId(), this.mnoService.findByName(operator).getId(), mobileMoneyChannel.getId(),paymentReference, pushUssd, null, cardPayment).join();
                        }
                    });


                } else {
                    // session is active
                    PaymentMethodType paymentMethodType = session.getCommissionConfig().getPaymentMethod().getType();
                    String paymentChanelName = session.getCommissionConfig().getPaymentMethodChanelName();
                    this.vendorManagementService.getVendor(vendorDto.getExternalId()).ifPresent(vendorManager -> {
                        if (vendorManager.getVendorHasCommission().equalsIgnoreCase("true")) {
                            this.commissionService.calculateCommissionWhenSessionIsActive(amount, paymentMethodType, msisdn, paymentChanelName, session, vendorManager,paymentReference, pushUssd, null, cardPayment).join();
                        }
                    });
                }
            }, () -> {
                // session expired
                PaymentMethodType paymentMethodType = PaymentMethodType.valueOf("CREDIT_CARD");
                PaymentMethod paymentMethod = this.paymentMethodService.findByType(paymentMethodType);
                MobileMoneyChannel mobileMoneyChannel = this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(collectionType));

                Optional.of(this.vendorService.findVendorDetailsByVendorExternalId(vendorDto.getExternalId())).ifPresent(vendor -> {
                    if (vendor.getHasCommission().equalsIgnoreCase("true")) {
                        this.commissionService.calculateCommission(vendor.getVendorExternalId(), amount, paymentMethodType, paymentMethod.getId(), 0L, mobileMoneyChannel.getId(), paymentReference, pushUssd, null, cardPayment).join();
                    }
                });

            });
        }, commissionProcessorVirtualThread);

    }


    /**
     * Extract original reference from exchange first checking if session is active or not
     *
     * @param exchange
     * @return
     */
    @Async("depositProcessorVirtualThread")
    public CompletableFuture<String> extractOriginalReference(Exchange exchange) {
        return CompletableFuture.supplyAsync(() -> {
            String sessionId = exchange.getProperty("sessionId", String.class);
            AtomicReference<String> originalReference = new AtomicReference<>();

            sessionManagementService.getSession(sessionId).ifPresentOrElse(session -> {
                if (session.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                    // session expired - get from PushUssd
                    extractFromPushUssd(exchange, originalReference);
                } else {
                    // session is active
                    originalReference.set(session.getReceiptNumber());
                }
            }, () -> {
                // session not found - get from PushUssd
                extractFromPushUssd(exchange, originalReference);
            });

            return originalReference.get();
        }, depositProcessorVirtualThread);
    }

    /**
     * Helper method to extract reference from PushUssd
     */
    private void extractFromPushUssd(Exchange exchange, AtomicReference<String> originalReference) {
        PushUssd pushUssd = exchange.getIn().getHeader("pushUssd", PushUssd.class);
        if (pushUssd != null) {
            originalReference.set(this.pushUssdRefService.getRefByMappingRef(pushUssd.getReference()).getReference());
        }
    }


    /**
     * Retrieves vendor details from session (if active) or database (if session expired)
     */
    @Async("depositProcessorVirtualThread")
    public CompletableFuture<VendorInfo> getVendorDetails(Exchange exchange) {
        DepositRequest depositRequest = exchange.getProperty("depositRequest", DepositRequest.class);

        // First, try to get vendor details from active session
        if (StringUtils.hasText(depositRequest.getPaymentSessionId())) {
            log.debug("Retrieving vendor details from active session: {}", depositRequest.getPaymentSessionId());
            return getVendorFromSession(depositRequest.getPaymentSessionId())
                    .thenCompose(vendorInfo -> {
                        log.debug("Vendor details from active session: {}", vendorInfo);
                        if (vendorInfo != null) {
                            log.debug("Retrieved vendor details from active session: {}", depositRequest.getPaymentSessionId());
                            return CompletableFuture.completedFuture(vendorInfo);
                        }
                        // If session is expired or not found, get vendor details from database
                        if (StringUtils.hasText(depositRequest.getReference())) {
                            return getVendorFromDatabase(depositRequest.getReference());
                        }
                        return CompletableFuture.completedFuture(null);
                    });
        }

        // If no session ID, try database directly
        if (StringUtils.hasText(depositRequest.getReference())) {
            return getVendorFromDatabase(depositRequest.getReference());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Retrieves vendor details from active session
     */
    private CompletableFuture<VendorInfo> getVendorFromSession(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<UserSession> sessionOpt = sessionManagementService.getSession(sessionId);
                if (sessionOpt.isPresent()) {
                    UserSession session = sessionOpt.get();
                    String merchantId = session.getMerchantId();
                    if (StringUtils.hasText(merchantId)) {
                        Optional<VendorManager> vendorOpt = vendorManagementService.getVendor(merchantId);
                        if (vendorOpt.isPresent()) {
                            VendorManager vendor = vendorOpt.get();
                            return VendorInfo.builder()
                                    .vendorId(vendor.getVendorId())
                                    .vendorExternalId(vendor.getVendorExternalId())
                                    .vendorCallbackUrl(vendor.getVendorCallbackUrl())
                                    .vendorName(vendor.getVendorName())
                                    .source("SESSION")
                                    .build();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error retrieving vendor details from session: {}", sessionId, e);
            }
            return null;
        }, depositProcessorVirtualThread);
    }

    /**
     * Retrieves vendor details from database using PushUssd entity
     */
    private CompletableFuture<VendorInfo> getVendorFromDatabase(String reference) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PushUssd pushUssd = pushUssdService.findByReference(reference);
                if (pushUssd != null && pushUssd.getVendorDetails() != null) {
                    log.debug("Retrieved vendor details from database for reference: {}", reference);
                    return VendorInfo.builder()
                            .vendorId(pushUssd.getVendorDetails().getId().toString())
                            .vendorExternalId(pushUssd.getVendorDetails().getVendorExternalId())
                            .vendorCallbackUrl(pushUssd.getVendorDetails().getCallbackUrl())
                            .vendorName(pushUssd.getVendorDetails().getVendorName())
                            .source("DATABASE")
                            .build();
                }
            } catch (Exception e) {
                log.error("Error retrieving vendor details from database for reference: {}", reference, e);
            }
            return null;
        }, depositProcessorVirtualThread);
    }


    /**
     * Retrieves vendor details from session (if active) or database (if session expired)
     */
    /*@Async("depositProcessorVirtualThread")*/
    /*public VendorInfo getVendorDetails(Exchange exchange) {
        DepositRequest depositRequest = exchange.getProperty("depositRequest", DepositRequest.class);
        VendorInfo vendorInfo = null;

        // First, try to get vendor details from active session
        if (StringUtils.hasText(depositRequest.getPaymentSessionId())) {
            vendorInfo = getVendorFromSession(depositRequest.getPaymentSessionId());

            if (vendorInfo != null) {
                log.debug("Retrieved vendor details from active session: {}", depositRequest.getPaymentSessionId());
                return vendorInfo;
            }
        }

        // If session is expired or not found, get vendor details from database
        if (StringUtils.hasText(depositRequest.getReference())) {
            vendorInfo = getVendorFromDatabase(depositRequest.getReference());

            if (vendorInfo != null) {
                log.debug("Retrieved vendor details from database for reference: {}", depositRequest.getReference());
            }
        }

        return vendorInfo;
    }


    *//**
     * Retrieves vendor details from active session
     *//*
    private VendorInfo getVendorFromSession(String sessionId) {
        try {
            Optional<UserSession> sessionOpt = sessionManagementService.getSession(sessionId);

            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                String merchantId = session.getMerchantId();

                if (StringUtils.hasText(merchantId)) {
                    Optional<VendorManager> vendorOpt = vendorManagementService.getVendor(merchantId);

                    if (vendorOpt.isPresent()) {
                        VendorManager vendor = vendorOpt.get();
                        return VendorInfo.builder()
                                .vendorId(vendor.getVendorId())
                                .vendorExternalId(vendor.getVendorExternalId())
                                .vendorCallbackUrl(vendor.getVendorCallbackUrl())
                                .vendorName(vendor.getVendorName())
                                .source("SESSION")
                                .build();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving vendor details from session: {}", sessionId, e);
        }

        return null;
    }

    *//**
     * Retrieves vendor details from database using PushUssd entity
     *//*
    private VendorInfo getVendorFromDatabase(String reference) {
        try {
            PushUssd pushUssd = pushUssdService.findByReference(reference);

            if (pushUssd != null && pushUssd.getVendorDetails() != null) {
                // Assuming VendorDetails has the necessary fields
                return VendorInfo.builder()
                        .vendorId(pushUssd.getVendorDetails().getId().toString())
                        .vendorExternalId(pushUssd.getVendorDetails().getVendorExternalId())
                        .vendorCallbackUrl(pushUssd.getVendorDetails().getCallbackUrl())
                        .vendorName(pushUssd.getVendorDetails().getVendorName())
                        .source("DATABASE")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error retrieving vendor details from database for reference: {}", reference, e);
        }

        return null;
    }*/

}
