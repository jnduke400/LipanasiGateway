package com.hybrid9.pg.Lipanasi.route.processor.bank;

import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.component.BalanceLedger;
import com.hybrid9.pg.Lipanasi.dto.crdb.cybersource.PaymentResponse;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositResponse;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.Ledger;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.Transaction;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.payments.tax.TransactionTax;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.*;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.route.processor.DepositProcessor;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelServiceImpl;
import com.hybrid9.pg.Lipanasi.services.bank.CardPaymentService;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTierService;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTransactionService;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.*;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.tax.TransactionTaxService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class BankDepositProcessor {
    @Value("${order.session.expiry.default:30}")
    private Integer DEFAULT_SESSION_EXPIRY;

    private static final int OPERATION_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    static Gson g = new Gson();

    @Autowired
    @Qualifier("initDepositVirtualThread")
    private ExecutorService initDepositVirtualThread;

    @Autowired
    @Qualifier("completeDepositVirtualThread")
    private ExecutorService completeDepositVirtualThread;

    @Autowired
    @Qualifier("balanceUpdateVirtualThread")
    private ExecutorService balanceUpdateVirtualThread;

    @Autowired
    @Qualifier("depositUpdateVirtualThread")
    private ExecutorService depositUpdateVirtualThread;

    private final MnoServiceImpl mnoService;
    private final TransactionLogService transactionLogService;
    private final LedgerService ledgerService;
    private final CashOutService cashOutService;
    private final DepositService depositService;
    private final CashInLogService cashInLogService;
    private final CashOutLogService cashOutLogService;
    private final MainAccountService mainAccountService;
    private final PayBillPaymentService payBillPaymentService;
    private final SubAccountService subAccountService;
    private final SubLedgerService subLedgerService;
    private final SessionManagementService sessionManagementService;
    private final PaymentMethodService paymentMethodService;
    private final OrderService orderService;
    private CommissionTierService commissionTierService;
    private CommissionTransactionService commissionTransactionService;
    private final TransactionTaxService transactionTaxService;
    private final CardPaymentService cardPaymentService;
    private final MobileMoneyChannelServiceImpl mobileMoneyChannelService;


    public BankDepositProcessor(MnoServiceImpl mnoService, TransactionLogService transactionLogService,
                                LedgerService ledgerService, CashOutService cashOutService, DepositService depositService, CashInLogService cashInLogService,
                                CashOutLogService cashOutLogService, MainAccountService mainAccountService, PayBillPaymentService payBillPaymentService,
                                SubAccountService subAccountService, SubLedgerService subLedgerService, SessionManagementService sessionManagementService,
                                PaymentMethodService paymentMethodService, OrderService orderService, CommissionTierService commissionTierService,
                                CommissionTransactionService commissionTransactionService, TransactionTaxService transactionTaxService,
                                CardPaymentService cardPaymentService, MobileMoneyChannelServiceImpl mobileMoneyChannelService) {
        this.mnoService = mnoService;
        this.transactionLogService = transactionLogService;
        this.ledgerService = ledgerService;
        this.cashOutService = cashOutService;
        this.depositService = depositService;
        this.cashInLogService = cashInLogService;
        this.cashOutLogService = cashOutLogService;
        this.mainAccountService = mainAccountService;
        this.payBillPaymentService = payBillPaymentService;
        this.subAccountService = subAccountService;
        this.subLedgerService = subLedgerService;
        this.sessionManagementService = sessionManagementService;
        this.paymentMethodService = paymentMethodService;
        this.orderService = orderService;
        this.commissionTierService = commissionTierService;
        this.commissionTransactionService = commissionTransactionService;
        this.transactionTaxService = transactionTaxService;
        this.cardPaymentService = cardPaymentService;
        this.mobileMoneyChannelService = mobileMoneyChannelService;
    }

    public void processDeposit(String msisdn, String accountNumber, String amount, String reference, String clientName, String status, String invoiceNo, String pushUssdId) {

    }

    Logger logger = LoggerFactory.getLogger(DepositProcessor.class);

    // Dedicated executor for IO-bound tasks
    private final Executor ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Dedicated executor for CPU-bound tasks
    private final Executor cpuExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Async("initDepositVirtualThread")
    public CompletableFuture<Void> initDeposit(CardPayment cardPayment) {
        return CompletableFuture.runAsync(() -> {
            // Initiate the transaction for the deposit
            Transaction transaction = Transaction.builder()
                    .amount(cardPayment.getAmount())
                    .operator(cardPayment.getBankName())
                    .paymentReference(cardPayment.getPaymentReference())
                    .transactionType(TransactionType.DEPOSIT)
                    .requestStatus(RequestStatus.NEW)
                    .status(TransactionStatus.PENDING)
                    .vendorDetails(cardPayment.getVendorDetails())
                    .currency(cardPayment.getCurrency())
                    .cardPayment(cardPayment)
                    .build();

            // Get details from Redis / Database
            this.sessionManagementService.getSession(cardPayment.getSessionId()).ifPresentOrElse(session -> {
                if (session.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                    // session expired - get details from database
                    populateTransactionFromDatabase(cardPayment, transaction);
                } else {
                    // for active session - get details from Redis
                    populateTransactionFromSession(cardPayment, transaction, session);
                }
            }, () -> {
                // session not found - get details from database
                populateTransactionFromDatabase(cardPayment, transaction);
            });

            // Record the transaction
            this.transactionLogService.recordTransaction(transaction);
        }, initDepositVirtualThread);
    }

    /**
     * Populate transaction details from database (expired/missing session)
     */
    private void populateTransactionFromDatabase(CardPayment cardPayment, Transaction transaction) {
        this.orderService.findByPaymentSessionId(cardPayment.getSessionId()).ifPresent(order -> {
            transaction.setPaymentMethod(order.getPaymentMethod());
            transaction.setOriginalReference(order.getReceipt());
            transaction.setOrderNumber(order.getOrderNumber());
            transaction.setChannel(determinePaymentChannel(order.getPaymentMethod().getType().name(), cardPayment.getCollectionType()));
        });
    }

    /**
     * Populate transaction details from active session
     */
    private void populateTransactionFromSession(CardPayment cardPayment, Transaction transaction, UserSession session) {
        transaction.setPaymentMethod(session.getCommissionConfig().getPaymentMethod());
        transaction.setOriginalReference(session.getReceiptNumber());
        transaction.setOrderNumber(session.getOrderNumber());
        transaction.setChannel(determinePaymentChannel(session.getCommissionConfig().getPaymentMethod().getType().name(), cardPayment.getCollectionType()));
    }

    /**
     * Determine payment channel based on payment method type and collection type
     */
    private PaymentChannel determinePaymentChannel(String paymentMethodType, String collectionType) {
        if (paymentMethodType.equalsIgnoreCase("MOBILE_MONEY")) {
            return collectionType.equalsIgnoreCase("PUSH USSD") ? PaymentChannel.PUSH_USSD : PaymentChannel.PAY_BILL;
        } else if (paymentMethodType.equalsIgnoreCase("BANK_TRANSFER") ||
                paymentMethodType.equalsIgnoreCase("CREDIT_CARD")) {
            return PaymentChannel.BANK_PAYMENT_GATEWAY;
        } else {
            return PaymentChannel.CASH;
        }
    }

    /**
     * Complete deposit transaction
     */
    @Async("depositProcessorVirtualThread")
    public CompletableFuture<Void> completeDeposit(DepositResponse depositResponse, CardPayment cardPayment) {
        return CompletableFuture.runAsync(() -> {
            this.transactionLogService.findByChannelAndPaymentReference(
                    PaymentChannel.BANK_PAYMENT_GATEWAY,
                    cardPayment.getPaymentReference()
            ).ifPresent(transaction -> {
                updateTransactionStatus(transaction, depositResponse, cardPayment.getSessionId(), cardPayment.getStatus());

            });
        }, completeDepositVirtualThread);
    }

    /**
     * Update transaction status and response based on deposit response
     */
    private void updateTransactionStatus(Transaction transaction, DepositResponse depositResponse, String sessionId, String status) {
        if (depositResponse.getStatus().equalsIgnoreCase("success")) {
            transaction.setRequestStatus(RequestStatus.COMPLETED);
            transaction.setThirdPartyResponse("bank deposit success");
            transaction.setStatus(TransactionStatus.DEPOSITED);
        } else {
            transaction.setRequestStatus(RequestStatus.FAILED);
            transaction.setThirdPartyResponse("bank deposit failed");
            transaction.setStatus(TransactionStatus.FAILED);
        }
        transactionLogService.updateTransaction(transaction);

        // update session
        this.updateSession(sessionId, depositResponse, status);

    }

    private void updateSession(String sessionId, DepositResponse depositResponse, String status) {
        UserSession s = sessionManagementService.getSession(sessionId).orElseThrow(() -> new RuntimeException("Session not found for ID: " + sessionId));
        //s.setTransactionStatus(status.equalsIgnoreCase("0") ? UserSession.TransactionStatus.COMPLETED : UserSession.TransactionStatus.FAILED);
        //sessionManagementService.updateSession(sessionId, s);
        sessionManagementService.updateTransactionStatusAndError(
                sessionId,
                status.equalsIgnoreCase("0") ? UserSession.TransactionStatus.COMPLETED : UserSession.TransactionStatus.FAILED,
                status.equalsIgnoreCase("0") ? "Deposit completed successfully" : "Deposit failed"
        );
    }


    /**
     * Updates deposit balance asynchronously with proper error handling
     *
     * @param cardPayment The Bank data
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateDepositBalance(CardPayment cardPayment) {
        if (cardPayment == null) {
            logger.error("CardPayment cannot be null");
            return CompletableFuture.completedFuture(false);
        }

        logger.info("Starting deposit balance update for vendor: {}, amount: {}",
                cardPayment.getVendorDetails().getId(), cardPayment.getAmount());

        BalanceLedger balanceLedger = new BalanceLedger(null, null, cardPayment, null, null, 0, 0, 0);

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return this.updateMainAccountBalance(
                                cardPayment.getVendorDetails(),
                                (float) cardPayment.getAmount(),
                                0,
                                balanceLedger,
                                cardPayment
                        );
                    } catch (Exception e) {
                        logger.error("Failed to update main account balance", e);
                        throw new CompletionException("Main account balance update failed", e);
                    }
                }, balanceUpdateVirtualThread)
                .thenComposeAsync(updatedLedger -> {
                    if (updatedLedger == null || updatedLedger.getMainAccount() == null) {
                        logger.error("Invalid balance ledger returned from main account update");
                        return CompletableFuture.completedFuture(false);
                    }

                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            this.updateLedger(updatedLedger);
                            return true;
                        } catch (Exception e) {
                            logger.error("Failed to update ledger", e);
                            throw new CompletionException("Ledger update failed", e);
                        }
                    }, balanceUpdateVirtualThread);
                }, balanceUpdateVirtualThread)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Deposit balance update failed for vendor: {}",
                                cardPayment.getVendorDetails().getId(), throwable);
                        return false;
                    }

                    logger.info("Successfully updated deposit balance for vendor: {}",
                            cardPayment.getVendorDetails().getId());
                    return result;
                })
                .orTimeout(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        logger.error("Deposit balance update timed out for vendor: {}",
                                cardPayment.getVendorDetails().getId());
                    } else {
                        logger.error("Unexpected error during deposit balance update", throwable);
                    }
                    return false;
                });
    }

    /**
     * Synchronous version for cases where you need to wait for completion
     */
    public boolean updateDepositBalanceSync(CardPayment cardPayment) {
        try {
            return updateDepositBalance(cardPayment).get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Synchronous deposit balance update failed", e);
            return false;
        }
    }

    /**
     * Updates main account balance with proper validation and error handling
     */
    @Transactional
    public BalanceLedger updateMainAccountBalance(VendorDetails vendorDetailsData,
                                                  float totalPayment,
                                                  float desiredAmount,
                                                  BalanceLedger balanceLedger, CardPayment cardPayment) {

        if (vendorDetailsData == null) {
            throw new IllegalArgumentException("Vendor details cannot be null");
        }

        if (totalPayment < 0) {
            throw new IllegalArgumentException("Total payment cannot be negative");
        }

        if (balanceLedger == null) {
            throw new IllegalArgumentException("Balance ledger cannot be null");
        }

        try {
            logger.debug("Updating main account balance for vendor: {}, payment: {}",
                    vendorDetailsData.getId(), totalPayment);

            MainAccount balanceData = this.mainAccountService.findByVendorDetails(vendorDetailsData);

            if (balanceData == null) {
                throw new IllegalStateException("Main account not found for vendor: " + vendorDetailsData.getId());
            }

            // Create a copy to avoid modifying the original in case of rollback
            ModelMapper modelMapper = new ModelMapper();
            MainAccount updatedBalance = modelMapper.map(balanceData, MainAccount.class);  // Assuming you have a copy method

            // Calculate net amount after commission and VAT
            // float commissionRate = this.calculateCommission(totalPayment,vendorDetailsData,pushUssd);//vendorDetailsData.getCommissionRate(); // Always exists
            // float vatRate = Boolean.parseBoolean(vendorDetailsData.getHasVat()) ? this.getTaxAmount(pushUssd) : 0f;

            float commissionAmount = this.calculateCommission(totalPayment, vendorDetailsData, cardPayment);
            /*float vatAmount = Boolean.parseBoolean(vendorDetailsData.getHasVat()) ?
                    this.getTransactionTax(pushUssd).getTaxAmount() : 0f;*/

            float vatAmount = 0f;
            if (Boolean.parseBoolean(vendorDetailsData.getHasVat())) {
                TransactionTax tax = this.getTransactionTax(cardPayment);
                if (tax != null) {
                    vatAmount = tax.getTaxAmount();
                } else {
                    logger.warn("No tax record found for cardPayment: {}", cardPayment.getId());
                    // You may want to handle this case differently - perhaps throw an exception
                    // or set a default VAT amount based on vendorDetailsData
                    vatAmount = 0f;
                }
            }
            float netAmount = totalPayment - commissionAmount - vatAmount;

            // Update net amount
            updatedBalance.setCurrentAmount(updatedBalance.getCurrentAmount() + totalPayment);
            updatedBalance.setActualAmount(updatedBalance.getActualAmount() + totalPayment);
            updatedBalance.setDesiredAmount(updatedBalance.getDesiredAmount() + desiredAmount);

            // Update commission and VAT earned
            updatedBalance.setCommissionEarned(updatedBalance.getCommissionEarned() + commissionAmount);
            if (Boolean.parseBoolean(vendorDetailsData.getHasVat())) {
                updatedBalance.setVatCollected(updatedBalance.getVatCollected() + vatAmount);
            }

            if (vendorDetailsData.getCharges() != 0f) {
                updatedBalance.setVendorCharges(updatedBalance.getVendorCharges() + vendorDetailsData.getCharges());
            }

            MainAccount savedAccount = this.mainAccountService.update(updatedBalance);

            logger.debug("Successfully updated main account for vendor: {} with breakdown - " +
                            "Gross: {}, Net: {}, Commission: {}, VAT: {}",
                    vendorDetailsData.getId(),
                    totalPayment,
                    netAmount,
                    commissionAmount,
                    vatAmount);

            return BalanceLedger.builder()
                    .mainAccount(savedAccount)
                    .cardPayment(balanceLedger.getCardPayment())
                    .payBillPayment(balanceLedger.getPayBillPayment())
                    .commission(commissionAmount)
                    .vat(vatAmount)
                    .netAmount(netAmount)
                    .build();

        } catch (Exception e) {
            logger.error("Failed to update main account balance for vendor: {}",
                    vendorDetailsData.getId(), e);
            throw new RuntimeException("Main account balance update failed", e);
        }
    }

    private TransactionTax getTransactionTax(CardPayment cardPayment) {
        AtomicReference<TransactionTax> taxAmount = new AtomicReference<>();
        this.transactionTaxService.findByPaymentReference(cardPayment.getPaymentReference()).ifPresent(taxAmount::set);
        return taxAmount.get();
    }

    /**
     * Calculates commission for a given vendor and total payment amount
     */
    private float calculateCommission(float totalPayment, VendorDetails vendorDetailsData, CardPayment cardPayment) {
        AtomicReference<BigDecimal> totalCommission = new AtomicReference<>();
        sessionManagementService.getSession(cardPayment.getSessionId()).ifPresentOrElse(session -> {

            // double-check if commission is active
            if (!session.getCommissionConfig().getCommissionStatus().equals(CommissionConfig.CommissionStatus.ACTIVE)) {
                logger.info("Commission is not active for session: {}", session.getUserId());
                //float v = 0f;
                return;
            }


            // CommissionTier tier = commissionTierService.findByCommissionTireId(Long.parseLong(session.getCommissionConfig().getCommissionTireId())).orElseThrow(() -> new RuntimeException("Commission tier not found"));

            // Calculate commission
            BigDecimal baseFee = session.getCommissionConfig().getBaseFee();
            BigDecimal percentageFee = BigDecimal.valueOf((double) totalPayment).multiply(session.getCommissionConfig().getPercentageRate().divide(new BigDecimal("100")));
            totalCommission.set(baseFee.add(percentageFee));

        }, () -> {
            // session expired
            // for expired session (Get details from database)
            // Find applicable commission tier
            BigDecimal amount = BigDecimal.valueOf((double) totalPayment);
            orderService.findByPaymentSessionId(cardPayment.getSessionId()).ifPresent(order -> {
                Optional<CommissionTier> applicableTier = commissionTierService
                        .findApplicableTier(vendorDetailsData.getId(), order.getPaymentMethod().getId(), amount, this.mobileMoneyChannelService.findByType(PaymentChannel.BANK_PAYMENT_GATEWAY).getId(), this.mnoService.findByName(cardPayment.getOperator()).getId());

                if (applicableTier.isEmpty()) {
                    throw new RuntimeException("No applicable commission tier found");
                }

                CommissionTier tier = applicableTier.get();

                // Calculate commission
                BigDecimal baseFee = tier.getBaseFee();
                BigDecimal percentageFee = amount.multiply(tier.getPercentageRate().divide(new BigDecimal("100")));
                totalCommission.set(baseFee.add(percentageFee));
            });
        });

        // return totalCommission.get().floatValue();
        return totalCommission.get() != null ? totalCommission.get().floatValue() : 0f;
    }


    /**
     * Updates ledger with proper validation and error handling
     */
    @Transactional
    public void updateLedger(BalanceLedger balanceLedger) {
        if (balanceLedger == null || balanceLedger.getMainAccount() == null ||
                balanceLedger.getCardPayment() == null) {
            throw new IllegalArgumentException("Invalid balance ledger data");
        }

        VendorDetails vendorDetails = balanceLedger.getMainAccount().getVendorDetails();
        CardPayment cardPayment = balanceLedger.getCardPayment();

        try {
            logger.debug("Updating ledger for vendor: {}, amount: {}",
                    vendorDetails.getId(), cardPayment.getAmount());

            float prevAmount = calculatePreviousAmount(vendorDetails, (float) cardPayment.getAmount());
            float currentAmount = prevAmount + (float) cardPayment.getAmount();

            Ledger ledger = Ledger.builder()
                    .vendorDetails(vendorDetails)
                    .prevAmount(prevAmount)
                    .amount((float) cardPayment.getAmount())
                    .curAmount(currentAmount)
                    .receipt(cardPayment.getPaymentReference())
                    .requestType(determineRequestType(cardPayment.getCollectionType()))
                    //.createdAt(System.currentTimeMillis())
                    // VAT-related fields
                    .hasTaxation(Boolean.parseBoolean(vendorDetails.getHasVat()))
                    .vatRate(Boolean.parseBoolean(vendorDetails.getHasVat()) ? this.getTransactionTax(cardPayment) != null ? this.getTransactionTax(cardPayment).getTaxRate() : 0f : 0f)
                    .vatAmount(balanceLedger.getVat()) // From BalanceLedger
                    // Commission fields
                    .commissionRate(this.getCommissionRate((float) cardPayment.getAmount(), vendorDetails, cardPayment))
                    .commissionAmount(balanceLedger.getCommission())
                    .netAmount(balanceLedger.getNetAmount())
                    .build();

            this.ledgerService.addLedger(ledger);

            logger.debug("Successfully updated ledger for vendor: {}", vendorDetails.getId());

        } catch (Exception e) {
            logger.error("Failed to update ledger for vendor: {}", vendorDetails.getId(), e);
            throw new RuntimeException("Ledger update failed", e);
        }
    }

    /**
     * Calculates the commission rate with proper error handling
     */
    private float getCommissionRate(float amount, VendorDetails vendorDetails, CardPayment cardPayment) {
        AtomicReference<Float> commissionRate = new AtomicReference<>();
        sessionManagementService.getSession(cardPayment.getSessionId()).ifPresentOrElse(session -> {
            if (session.getCommissionConfig().getCommissionStatus().equals(CommissionConfig.CommissionStatus.ACTIVE)) {
                BigDecimal baseFee = session.getCommissionConfig().getBaseFee();
                BigDecimal percentageFee = BigDecimal.valueOf((double) amount).multiply(session.getCommissionConfig().getPercentageRate().divide(new BigDecimal("100")));
                commissionRate.set(percentageFee.floatValue());
            } else {
                commissionRate.set(0f);
            }
        }, () -> {
            // session expired
            // for expired session (Get details from database)
            BigDecimal totalAmount = BigDecimal.valueOf((double) amount);
            orderService.findByPaymentSessionId(cardPayment.getSessionId()).ifPresent(order -> {
                Optional<CommissionTier> applicableTier = commissionTierService
                        .findApplicableTier(vendorDetails.getId(), order.getPaymentMethod().getId(), totalAmount, this.mobileMoneyChannelService.findByType(PaymentChannel.BANK_PAYMENT_GATEWAY).getId(), this.mnoService.findByName(cardPayment.getOperator()).getId());

                if (applicableTier.isEmpty()) {
                    throw new RuntimeException("No applicable commission tier found");
                }

                CommissionTier tier = applicableTier.get();

                // Calculate commission
                BigDecimal baseFee = tier.getBaseFee();
                BigDecimal percentageFee = totalAmount.multiply(tier.getPercentageRate().divide(new BigDecimal("100")));
                commissionRate.set(percentageFee.floatValue());
            });
        });
        return commissionRate.get();

    }

    /**
     * Calculates the previous amount with proper error handling
     */
    private float calculatePreviousAmount(VendorDetails vendorDetails, float currentTransactionAmount) {
        try {
            Ledger lastLedger = this.ledgerService.findByLastAdded(vendorDetails);

            if (lastLedger == null) {
                MainAccount balance = this.mainAccountService.findByVendorDetails(vendorDetails);
                if (balance == null) {
                    logger.warn("No main account found for vendor: {}, using 0 as previous amount",
                            vendorDetails.getId());
                    return 0;
                }
                return balance.getCurrentAmount() - currentTransactionAmount;
            } else {
                return lastLedger.getCurAmount();
            }
        } catch (Exception e) {
            logger.error("Failed to calculate previous amount for vendor: {}", vendorDetails.getId(), e);
            return 0; // Safe fallback
        }
    }

    /**
     * Determines request type with validation
     */
    private RequestType determineRequestType(String collectionType) {
        if (collectionType == null) {
            logger.warn("Collection type is null, defaulting to CASH_IN");
            return RequestType.CASH_IN;
        }

        return collectionType.equalsIgnoreCase("PUSH USSD") ?
                RequestType.CASH_IN : collectionType.equalsIgnoreCase("CARD") ? RequestType.CASH_IN : collectionType.equalsIgnoreCase("PAY BILL") ? RequestType.CASH_IN : RequestType.CASH_OUT;
    }

    /**
     * Asynchronous version for better performance
     *
     * @param cardPayment The Bank data to update
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateDepositTransactionStatusAsync(CardPayment cardPayment) {
        if (cardPayment == null || cardPayment.getId() == null) {
            logger.error("CardPayment or its ID cannot be null");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return updateDepositTransactionStatus(cardPayment);
                    } catch (Exception e) {
                        logger.error("Async Deposit transaction status update failed for ID: {}",
                                cardPayment.getId(), e);
                        throw new CompletionException("Deposit transaction status update failed", e);
                    }
                }, depositUpdateVirtualThread)
                .orTimeout(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof TimeoutException) {
                            logger.error("Deposit transaction status update timed out for ID: {}",
                                    cardPayment.getId());
                        } else {
                            logger.error("Unexpected error during async Deposit transaction status update for ID: {}",
                                    cardPayment.getId(), throwable);
                        }
                        return false;
                    }
                    return result;
                });
    }


    /**
     * Synchronous version - update Deposit Status
     *
     * @param cardPayment The Bank data to update
     * @return boolean indicating success/failure
     */
    @Transactional
    @Retryable(value = {OptimisticLockingFailureException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 100))
    public boolean updateDepositTransactionStatus(CardPayment cardPayment) {
        if (cardPayment == null || cardPayment.getId() == null) {
            logger.error("CardPayment or its ID cannot be null");
            return false;
        }

        try {
            logger.debug("Updating Deposit transaction status for ID: {}", cardPayment.getId());

            return this.cardPaymentService.findByChannelAndPaymentReference(PaymentChannel.BANK_PAYMENT_GATEWAY, cardPayment.getPaymentReference())
                    .map(existingDeposit -> {
                       /* // Validate current status before updating
                        if (!isValidStatusTransition(existingPushUssd.getCollectionStatus(), CollectionStatus.DEPOSITED)) {
                            logger.warn("Invalid status transition from {} to DEPOSITED for USSD ID: {}",
                                    existingPushUssd.getCollectionStatus(), pushUssd.getId());
                            return false;
                        }*/

                        existingDeposit.setCollectionStatus(CollectionStatus.DEPOSITED);

                        CardPayment updatedDeposit = this.cardPaymentService.update(existingDeposit);

                        logger.info("Successfully updated Deposit transaction status to COMPLETED for ID: {}",
                                cardPayment.getId());
                        return updatedDeposit != null;
                    })
                    .orElseGet(() -> {
                        logger.error("Deposit not found with ID: {}", cardPayment.getId());
                        return false;
                    });

        } catch (Exception e) {
            logger.error("Failed to update Deposit transaction status for ID: {}", cardPayment.getId(), e);
            return false;
        }
    }

    /**
     * Asynchronous version for better performance
     *
     * @param cardPayment The Bank data to update
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateCardPaymentStatusAsync(CardPayment cardPayment) {
        if (cardPayment == null || cardPayment.getId() == null) {
            logger.error("CardPayment or its ID cannot be null");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return updateCardPaymentStatus(cardPayment);
                    } catch (Exception e) {
                        logger.error("Async Bank status update failed for ID: {}",
                                cardPayment.getId(), e);
                        throw new CompletionException("USSD status update failed", e);
                    }
                }, completeDepositVirtualThread)
                .orTimeout(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof TimeoutException) {
                            logger.error("Bank status update timed out for ID: {}",
                                    cardPayment.getId());
                        } else {
                            logger.error("Unexpected error during async USSD status update for ID: {}",
                                    cardPayment.getId(), throwable);
                        }
                        return false;
                    }
                    return result;
                });
    }


    /**
     * Synchronous version - maintains backward compatibility
     *
     * @param cardPayment The Bank data to update
     * @return boolean indicating success/failure
     */
    @Transactional
    @Retryable(value = {OptimisticLockingFailureException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 100))
    public boolean updateCardPaymentStatus(CardPayment cardPayment) {
        if (cardPayment == null || cardPayment.getId() == null) {
            logger.error("cardPayment or its ID cannot be null");
            return false;
        }

        try {
            logger.debug("Updating Bank status for ID: {}", cardPayment.getId());

            return this.cardPaymentService.findCardPaymentById(cardPayment.getId())
                    .map(existingCardPayment -> {
                       /* // Validate current status before updating
                        if (!isValidStatusTransition(existingPushUssd.getCollectionStatus(), CollectionStatus.DEPOSITED)) {
                            logger.warn("Invalid status transition from {} to DEPOSITED for USSD ID: {}",
                                    existingPushUssd.getCollectionStatus(), pushUssd.getId());
                            return false;
                        }*/

                        if (existingCardPayment.getErrorMessage() == null) {
                            existingCardPayment.setCollectionStatus(CollectionStatus.DEPOSITED);
                        }

                        CardPayment updatedCardPayment = this.cardPaymentService.update(existingCardPayment);

                        logger.info("Successfully updated Bank status to DEPOSITED for ID: {}",
                                cardPayment.getId());
                        return updatedCardPayment != null;
                    })
                    .orElseGet(() -> {
                        logger.error("CardPayment not found with ID: {}", cardPayment.getId());
                        return false;
                    });

        } catch (Exception e) {
            logger.error("Failed to update Bank status for ID: {}", cardPayment.getId(), e);
            return false;
        }
    }

    /**
     * Records the payment response from CyberSource
     *
     * @param paymentResponse
     * @param cardPayment
     * @return
     */
    public CompletionStage<CardPayment> recordPaymentResponse(PaymentResponse paymentResponse, CardPayment cardPayment) {
        AtomicReference<CardPayment> cardPaymentReference = new AtomicReference<>();
        return CompletableFuture.supplyAsync(() -> {
            this.cardPaymentService.findCardPaymentById(cardPayment.getId()).ifPresent(cardPayment1 -> {
                //updateTransactionStatus(transaction, depositResponse,cardPayment.getSessionId(),cardPayment.getStatus());
                cardPayment1.setBankTransactionId(paymentResponse.getTransactionId());
                cardPayment1.setBankApprovalCode(paymentResponse.getApprovalCode());
                cardPayment1.setBankResponseCode(paymentResponse.getResponseCode());
                cardPayment1.setBankResponseStatus(paymentResponse.getStatus());
                cardPaymentReference.set(this.cardPaymentService.updateCardPayment(cardPayment1));

            });
            return cardPaymentReference.get();
        }, completeDepositVirtualThread);
    }
}
