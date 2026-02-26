package com.hybrid9.pg.Lipanasi.route.processor;

import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.component.BalanceLedger;
import com.hybrid9.pg.Lipanasi.component.PushUssdRequest;
import com.hybrid9.pg.Lipanasi.dto.deposit.DepositResponse;
import com.hybrid9.pg.Lipanasi.dto.paybill.PaymentResponse;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.Ledger;
import com.hybrid9.pg.Lipanasi.entities.payments.SubLedger;
import com.hybrid9.pg.Lipanasi.entities.payments.activity.Transaction;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.entities.payments.tax.TransactionTax;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import com.hybrid9.pg.Lipanasi.entities.vendorx.SubAccount;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.*;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelServiceImpl;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTierService;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTransactionService;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.*;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class PayBillDepositProcessor {
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
    private final MobileMoneyChannelServiceImpl mobileMoneyChannelService;


    public PayBillDepositProcessor(MnoServiceImpl mnoService, TransactionLogService transactionLogService,
                            LedgerService ledgerService, CashOutService cashOutService, DepositService depositService, CashInLogService cashInLogService,
                            CashOutLogService cashOutLogService, MainAccountService mainAccountService, PayBillPaymentService payBillPaymentService,
                            SubAccountService subAccountService, SubLedgerService subLedgerService, SessionManagementService sessionManagementService,
                            PaymentMethodService paymentMethodService, OrderService orderService, CommissionTierService commissionTierService,
                            CommissionTransactionService commissionTransactionService, TransactionTaxService transactionTaxService,
                            MobileMoneyChannelServiceImpl mobileMoneyChannelService) {
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
        this.mobileMoneyChannelService = mobileMoneyChannelService;
    }

    public void processDeposit(String msisdn, String accountNumber, String amount, String reference, String clientName, String status, String invoiceNo, String pushUssdId) {

    }

    Logger logger = LoggerFactory.getLogger(DepositProcessor.class);

    // Dedicated executor for IO-bound tasks
    private final Executor ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Dedicated executor for CPU-bound tasks
    private final Executor cpuExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private PayBillPayment initiatePayBillDeposit(PayBillPayment payBillPayment) {
        AtomicReference<PayBillPayment> billPayment = new AtomicReference<>();
        this.payBillPaymentService.findById(payBillPayment.getId()).ifPresent(payBillPayment1 -> {
            payBillPayment1.setCollectionStatus(CollectionStatus.INITIATED);
            PayBillPayment updateResult = this.payBillPaymentService.update(payBillPayment1);
            billPayment.set(updateResult);
        });
        return billPayment.get();
    }


   /* private PushUssdRequest initiateDeposit(PushUssdRequest pushUssdRequest) {
        AtomicReference<PushUssdRequest> pushUssdRequest1 = new AtomicReference<>();
        this.pushUssdService.findPushUssdById(pushUssdRequest.getPushUssd().getId()).ifPresent(pushUssd1 -> {
            pushUssd1.setCollectionStatus(CollectionStatus.INITIATED);
            PushUssd updateResult = this.pushUssdService.update(pushUssd1);
            pushUssdRequest1.set(new PushUssdRequest(updateResult.getCollectionStatus(), updateResult.getMsisdn(), updateResult.getOperator(), updateResult.getMessage(), updateResult.getAccountId(), String.valueOf(updateResult.getId()), updateResult));
        });
        return pushUssdRequest1.get();
    }*/

    /*private PushUssdRequest recordDepositTransaction(PushUssdRequest pushUssdRequest) {
        AtomicReference<PushUssdRequest> pushUssdRequest1 = new AtomicReference<>();
        this.pushUssdService.findPushUssdById(pushUssdRequest.getPushUssd().getId()).ifPresent(pushUssd1 -> {
            Transaction transaction = Transaction.builder().
                    msisdn(pushUssd1.getMsisdn()).
                    amount(pushUssd1.getAmount()).
                    channel(PaymentChannel.PUSH_USSD).
                    paymentReference(pushUssd1.getReference()).
                    transactionType(TransactionType.DEPOSIT).
                    requestStatus(RequestStatus.INITIATED).
                    vendorDetails(pushUssd1.getVendorDetails()).
                    currency(pushUssd1.getCurrency()).
                    build();
            this.transactionLogService.recordTransaction(transaction);
            pushUssdRequest1.set(new PushUssdRequest(pushUssd1.getCollectionStatus(), pushUssd1.getMsisdn(), pushUssd1.getOperator(), pushUssd1.getMessage(), pushUssd1.getAccountId(), String.valueOf(pushUssd1.getId()), pushUssd1));
        });
        return pushUssdRequest1.get();
    }*/

    private PayBillPayment recordPayBillDepositTransaction(PayBillPayment payBillPayment) {
        AtomicReference<PayBillPayment> payBill1 = new AtomicReference<>();
        this.payBillPaymentService.findById(payBillPayment.getId()).ifPresent(payBillPayment1 -> {
            Transaction transaction = Transaction.builder().
                    msisdn(payBillPayment1.getMsisdn()).
                    amount(payBillPayment1.getAmount()).
                    channel(PaymentChannel.PAY_BILL).
                    paymentReference(payBillPayment1.getPaymentReference()).
                    transactionType(TransactionType.DEPOSIT).
                    requestStatus(RequestStatus.INITIATED).
                    vendorDetails(payBillPayment1.getVendorDetails()).
                    build();
            this.transactionLogService.recordTransaction(transaction);
            payBill1.set(payBillPayment1);
        });
        return payBill1.get();
    }

    /*public void initDeposit(String msisdn, String mobileMoneyName, String message, String mainAccountId, PushUssd pushUssd) {

        PushUssdRequest pushUssdRequest = new PushUssdRequest(pushUssd.getCollectionStatus(), msisdn, mobileMoneyName, message, mainAccountId, String.valueOf(pushUssd.getId()), pushUssd);
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> initiateDeposit(pushUssdRequest), ioExecutor)
                .thenApplyAsync(this::recordDepositTransaction, ioExecutor);
        future.join();
    }*/

    /**
     * Initiate a deposit transaction (START: -> PENDING, INITIAL RESPONSE: -> PROCESSING, COMPLETED: -> COMPLETED, FAILED: -> [FAILED, CANCELLED,NETWORK_ERROR])
     *
     * @param
*/    /*public void initDeposit(PushUssd pushUssd) {
        // Initiate the transaction for the deposit
        Transaction transaction = Transaction.builder().
                msisdn(pushUssd.getMsisdn()).
                amount(pushUssd.getAmount()).
                paymentReference(pushUssd.getReference()).
                transactionType(TransactionType.DEPOSIT).
                requestStatus(RequestStatus.NEW).
                status(TransactionStatus.PENDING).
                vendorDetails(pushUssd.getVendorDetails()).
                currency(pushUssd.getCurrency()).
                build();

        // Get details from Redis / Database
        this.sessionManagementService.getSession(pushUssd.getSessionId()).ifPresentOrElse(session -> {
            if (session.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                // session expired,
                // for expired session (Get details from database)
                this.orderService.findByPaymentSessionId(pushUssd.getSessionId()).ifPresent(order -> {
                    transaction.setPaymentMethod(order.getPaymentMethod());
                    transaction.setOriginalReference(order.getReceipt());
                    transaction.setOrderNumber(order.getOrderNumber());
                    if (order.getPaymentMethod().getType().name().equalsIgnoreCase("MOBILE_MONEY")) {
                        transaction.setChannel(pushUssd.getCollectionType().equalsIgnoreCase("PUSH USSD") ? PaymentChannel.PUSH_USSD : PaymentChannel.PAY_BILL);
                    } else if (order.getPaymentMethod().getType().name().equalsIgnoreCase("BANK_TRANSFER") ||
                            order.getPaymentMethod().getType().name().equalsIgnoreCase("CREDIT_CARD")) {
                        transaction.setChannel(PaymentChannel.BANK_PAYMENT_GATEWAY);
                    } else {
                        transaction.setChannel(PaymentChannel.CASH);
                    }
                });
            } else {
                // for active session (Get details from Redis)
                transaction.setPaymentMethod(session.getCommissionConfig().getPaymentMethod());
                transaction.setOriginalReference(session.getReceiptNumber());
                transaction.setOrderNumber(session.getOrderNumber());
                if (session.getCommissionConfig().getPaymentMethod().getType().name().equalsIgnoreCase("MOBILE_MONEY")) {
                    transaction.setChannel(pushUssd.getCollectionType().equalsIgnoreCase("PUSH USSD") ? PaymentChannel.PUSH_USSD : PaymentChannel.PAY_BILL);
                } else if (session.getCommissionConfig().getPaymentMethod().getType().name().equalsIgnoreCase("BANK_TRANSFER") ||
                        session.getCommissionConfig().getPaymentMethod().getType().name().equalsIgnoreCase("CREDIT_CARD")) {
                    transaction.setChannel(PaymentChannel.BANK_PAYMENT_GATEWAY);
                } else {
                    transaction.setChannel(PaymentChannel.CASH);
                }
            }
        }, () -> {
            // session expired
            // for expired session (Get details from database)
            this.orderService.findByPaymentSessionId(pushUssd.getSessionId()).ifPresent(order -> {
                transaction.setPaymentMethod(order.getPaymentMethod());
                transaction.setOriginalReference(order.getReceipt());
                transaction.setOrderNumber(order.getOrderNumber());
                if (order.getPaymentMethod().getType().name().equalsIgnoreCase("MOBILE_MONEY")) {
                    transaction.setChannel(pushUssd.getCollectionType().equalsIgnoreCase("PUSH USSD") ? PaymentChannel.PUSH_USSD : PaymentChannel.PAY_BILL);
                } else if (order.getPaymentMethod().getType().name().equalsIgnoreCase("BANK_TRANSFER") ||
                        order.getPaymentMethod().getType().name().equalsIgnoreCase("CREDIT_CARD")) {
                    transaction.setChannel(PaymentChannel.BANK_PAYMENT_GATEWAY);
                } else {
                    transaction.setChannel(PaymentChannel.CASH);
                }
            });
        });


        // Record the transaction
        this.transactionLogService.recordTransaction(transaction);

    }*/
    @Async("initDepositVirtualThread")
    public CompletableFuture<Void> initDeposit(PayBillPayment payBillPayment) {
        return CompletableFuture.runAsync(() -> {
            // Initiate the transaction for the deposit
            Transaction transaction = Transaction.builder()
                    .msisdn(payBillPayment.getMsisdn())
                    .amount(payBillPayment.getAmount())
                    .operator(payBillPayment.getOperator())
                    .paymentReference(payBillPayment.getPaymentReference())
                    .transactionType(TransactionType.DEPOSIT)
                    .requestStatus(RequestStatus.NEW)
                    .status(TransactionStatus.PENDING)
                    .vendorDetails(payBillPayment.getVendorDetails())
                    .currency(payBillPayment.getCurrency())
                    .payBillPayment(payBillPayment)
                    .build();

            // Get details from Redis / Database
            this.sessionManagementService.getSession(payBillPayment.getSessionId()).ifPresentOrElse(session -> {
                if (session.getLastAccessedAt().isBefore(LocalDateTime.now().minusMinutes(DEFAULT_SESSION_EXPIRY))) {
                    // session expired - get details from database
                    populateTransactionFromDatabase(payBillPayment, transaction);
                } else {
                    // for active session - get details from Redis
                    populateTransactionFromSession(payBillPayment, transaction, session);
                }
            }, () -> {
                // session not found - get details from database
                populateTransactionFromDatabase(payBillPayment, transaction);
            });

            // Record the transaction
            this.transactionLogService.recordTransaction(transaction);
        }, initDepositVirtualThread);
    }

    /**
     * Populate transaction details from database (expired/missing session)
     */
    private void populateTransactionFromDatabase(PayBillPayment payBillPayment, Transaction transaction) {
        this.orderService.findByPaymentSessionId(payBillPayment.getSessionId()).ifPresent(order -> {
            transaction.setPaymentMethod(order.getPaymentMethod());
            transaction.setOriginalReference(order.getReceipt());
            transaction.setOrderNumber(order.getOrderNumber());
            transaction.setChannel(determinePaymentChannel(order.getPaymentMethod().getType().name(), payBillPayment.getCollectionType()));
        });
    }

    /**
     * Populate transaction details from active session
     */
    private void populateTransactionFromSession(PayBillPayment payBillPayment, Transaction transaction, UserSession session) {
        transaction.setPaymentMethod(session.getCommissionConfig().getPaymentMethod());
        transaction.setOriginalReference(session.getReceiptNumber());
        transaction.setOrderNumber(session.getOrderNumber());
        transaction.setChannel(determinePaymentChannel(session.getCommissionConfig().getPaymentMethod().getType().name(), payBillPayment.getCollectionType()));
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
    public CompletableFuture<Void> completeDeposit(DepositResponse depositResponse, PayBillPayment payBillPayment) {
        return CompletableFuture.runAsync(() -> {
            this.transactionLogService.findByMsisdnAndChannelAndPaymentReference(
                    payBillPayment.getMsisdn(),
                    PaymentChannel.PUSH_USSD,
                    payBillPayment.getPaymentReference()
            ).ifPresent(transaction -> {
                updateTransactionStatus(transaction, depositResponse, payBillPayment.getSessionId(), payBillPayment.getStatus());

            });
        }, completeDepositVirtualThread);
    }

    /**
     * Update transaction status and response based on deposit response
     */
    private void updateTransactionStatus(Transaction transaction, DepositResponse depositResponse, String sessionId, String status) {
        if (depositResponse.getStatus().equalsIgnoreCase("success")) {
            transaction.setRequestStatus(RequestStatus.COMPLETED);
            transaction.setThirdPartyResponse("push_ussd deposit success");
            transaction.setStatus(TransactionStatus.DEPOSITED);
        } else {
            transaction.setRequestStatus(RequestStatus.FAILED);
            transaction.setThirdPartyResponse("push_ussd deposit failed");
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

    /*public BalanceLedger updateMainAccountBalance(VendorDetails vendorDetailsData, float totalPayment, float desiredAmount, BalanceLedger balanceLedger) {
        try {
            MainAccount balanceData = this.mainAccountService.findByVendorDetails(vendorDetailsData);
            balanceData.setCurrentAmount(balanceData.getCurrentAmount() + totalPayment);
            balanceData.setActualAmount(balanceData.getActualAmount() + totalPayment);
            balanceData.setDesiredAmount(balanceData.getDesiredAmount() + desiredAmount);
            balanceData.setVendorCharges(balanceData.getVendorCharges() + vendorDetailsData.getCharges());

            return BalanceLedger.builder().
                    mainAccount(this.mainAccountService.update(balanceData)).
                    pushUssd(balanceLedger.getPushUssd()).
                    payBillPayment(balanceLedger.getPayBillPayment()).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return balanceLedger;
    }*/

    public BalanceLedger updateSubAccountBalance(VendorDetails vendorDetailsData, float totalPayment, float desiredAmount, BalanceLedger balanceLedger) {
        try {
            SubAccount balanceData = this.subAccountService.findByVendorDetails(vendorDetailsData);
            balanceData.setCurrentAmount(balanceData.getCurrentAmount() + totalPayment);
            balanceData.setActualAmount(balanceData.getActualAmount() + totalPayment);
            balanceData.setDesiredAmount(balanceData.getDesiredAmount() + desiredAmount);
            balanceData.setVendorCharges(balanceData.getVendorCharges() + vendorDetailsData.getCharges());

            return BalanceLedger.builder().
                    subAccount(this.subAccountService.update(balanceData)).
                    pushUssd(balanceLedger.getPushUssd()).
                    payBillPayment(balanceLedger.getPayBillPayment()).
                    build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return balanceLedger;
    }

    /*public void updateLedger(BalanceLedger balanceLedger) {
        try {
            float curAmount = 0;
            Ledger lastLedger = this.ledgerService.findByLastAdded(balanceLedger.getMainAccount().getVendorDetails());
            if (lastLedger == null) {
                MainAccount balance = this.mainAccountService.findByVendorDetails(balanceLedger.getMainAccount().getVendorDetails());
                curAmount = balance.getCurrentAmount() - balanceLedger.getPushUssd().getAmount();
            } else {
                curAmount = lastLedger.getCurAmount();
            }
            Ledger ledger = new Ledger();
            ledger.setVendorDetails(balanceLedger.getMainAccount().getVendorDetails());
            ledger.setPrevAmount(curAmount);
            ledger.setAmount(balanceLedger.getPushUssd().getAmount());
            ledger.setCurAmount(curAmount + balanceLedger.getPushUssd().getAmount());
            ledger.setReceipt(balanceLedger.getPushUssd().getReference());
            ledger.setRequestType(balanceLedger.getPushUssd().getCollectionType().equalsIgnoreCase("PUSH USSD") ? RequestType.CASH_IN : RequestType.CASH_OUT);
            this.ledgerService.addLedger(ledger);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public void updateSubLedger(BalanceLedger balanceLedger) {
        try {
            float curAmount = 0;
            SubLedger lastLedger = this.subLedgerService.findByLastAdded(balanceLedger.getSubAccount().getVendorDetails());
            /*Ledger lastLedger = this.ledgerService.findByLastAdded(balanceLedger.getMainAccount() != null ? balanceLedger.getMainAccount().getInstitution() : balanceLedger.getSubAccount().getInstitution());*/
            if (lastLedger == null) {
                SubAccount balance = this.subAccountService.findByVendorDetails(balanceLedger.getSubAccount().getVendorDetails());
                curAmount = balance.getCurrentAmount() - balanceLedger.getPushUssd().getAmount();
            } else {
                curAmount = lastLedger.getCurAmount();
            }
            SubLedger ledger = new SubLedger();
            ledger.setVendorDetails(balanceLedger.getSubAccount().getVendorDetails());
            ledger.setPrevAmount(curAmount);
            ledger.setAmount(balanceLedger.getPushUssd().getAmount());
            ledger.setCurAmount(curAmount + balanceLedger.getPushUssd().getAmount());
            ledger.setReceipt(balanceLedger.getPushUssd().getReference());
            ledger.setRequestType(balanceLedger.getPushUssd().getCollectionType().equalsIgnoreCase("PUSH USSD") ? RequestType.CASH_IN : balanceLedger.getPushUssd().getCollectionType().equalsIgnoreCase("CARD") ? RequestType.CASH_IN : balanceLedger.getPushUssd().getCollectionType().equalsIgnoreCase("PAY BILL") ? RequestType.CASH_IN : RequestType.CASH_OUT);
            this.subLedgerService.addLedger(ledger);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatePayBillLedger(BalanceLedger balanceLedger) {
        try {
            float curAmount = 0;
            Ledger lastLedger = this.ledgerService.findByLastAdded(balanceLedger.getMainAccount().getVendorDetails());
            if (lastLedger == null) {
                MainAccount balance = this.mainAccountService.findByVendorDetails(balanceLedger.getMainAccount().getVendorDetails());
                curAmount = balance.getCurrentAmount() - balanceLedger.getPayBillPayment().getAmount();
            } else {
                curAmount = lastLedger.getCurAmount();
            }
            Ledger ledger = new Ledger();
            ledger.setVendorDetails(balanceLedger.getMainAccount().getVendorDetails());
            ledger.setPrevAmount(curAmount);
            ledger.setAmount(balanceLedger.getPayBillPayment().getAmount());
            ledger.setCurAmount(curAmount + balanceLedger.getPayBillPayment().getAmount());
            ledger.setReceipt(balanceLedger.getPayBillPayment().getPayBillId());
            ledger.setRequestType(balanceLedger.getPayBillPayment().getCollectionType().equalsIgnoreCase("PAY BILL") ? RequestType.CASH_IN : balanceLedger.getPayBillPayment().getCollectionType().equalsIgnoreCase("CARD") ? RequestType.CASH_IN : balanceLedger.getPayBillPayment().getCollectionType().equalsIgnoreCase("PUSH USSD") ? RequestType.CASH_IN : RequestType.CASH_OUT);
            this.ledgerService.addLedger(ledger);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*public void updateDepositBalance(PushUssd pushUssd) {
        BalanceLedger balanceLedger = new BalanceLedger(null, pushUssd, null, null);
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> this.updateMainAccountBalance(pushUssd.getVendorDetails(), pushUssd.getAmount(), 0, balanceLedger), ioExecutor)
                .thenAcceptAsync(this::updateLedger, ioExecutor);
        future.join();
    }*/

    /**
     * Updates deposit balance asynchronously with proper error handling
     *
     * @param payBillPayment The Pay Bill data
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateDepositBalance(PayBillPayment payBillPayment) {
        if (payBillPayment == null) {
            logger.error("PushUssd cannot be null");
            return CompletableFuture.completedFuture(false);
        }

        logger.info("Starting deposit balance update for vendor: {}, amount: {}",
                payBillPayment.getVendorDetails().getId(), payBillPayment.getAmount());

        BalanceLedger balanceLedger = new BalanceLedger(null,null, null, payBillPayment, null, 0, 0, 0);

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return this.updateMainAccountBalance(
                                payBillPayment.getVendorDetails(),
                                payBillPayment.getAmount(),
                                0,
                                balanceLedger,
                                payBillPayment
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
                                payBillPayment.getVendorDetails().getId(), throwable);
                        return false;
                    }

                    logger.info("Successfully updated deposit balance for vendor: {}",
                            payBillPayment.getVendorDetails().getId());
                    return result;
                })
                .orTimeout(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        logger.error("Deposit balance update timed out for vendor: {}",
                                payBillPayment.getVendorDetails().getId());
                    } else {
                        logger.error("Unexpected error during deposit balance update", throwable);
                    }
                    return false;
                });
    }

    /**
     * Synchronous version for cases where you need to wait for completion
     */
    public boolean updateDepositBalanceSync(PayBillPayment payBillPayment) {
        try {
            return updateDepositBalance(payBillPayment).get(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
                                                  BalanceLedger balanceLedger, PayBillPayment payBillPayment) {

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

            float commissionAmount = this.calculateCommission(totalPayment, vendorDetailsData, payBillPayment);
            /*float vatAmount = Boolean.parseBoolean(vendorDetailsData.getHasVat()) ?
                    this.getTransactionTax(pushUssd).getTaxAmount() : 0f;*/

            float vatAmount = 0f;
            if (Boolean.parseBoolean(vendorDetailsData.getHasVat())) {
                TransactionTax tax = this.getTransactionTax(payBillPayment);
                if (tax != null) {
                    vatAmount = tax.getTaxAmount();
                } else {
                    logger.warn("No tax record found for payBillPayment: {}", payBillPayment.getId());
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
                    .pushUssd(balanceLedger.getPushUssd())
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

    private TransactionTax getTransactionTax(PayBillPayment payBillPayment) {
        AtomicReference<TransactionTax> taxAmount = new AtomicReference<>();
        this.transactionTaxService.findByPaymentReference(payBillPayment.getPaymentReference()).ifPresent(taxAmount::set);
        return taxAmount.get();
    }

    /**
     * Calculates commission for a given vendor and total payment amount
     */
    private float calculateCommission(float totalPayment, VendorDetails vendorDetailsData, PayBillPayment payBillPayment) {
        AtomicReference<BigDecimal> totalCommission = new AtomicReference<>();
        sessionManagementService.getSession(payBillPayment.getSessionId()).ifPresentOrElse(session -> {

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
            orderService.findByPaymentSessionId(payBillPayment.getSessionId()).ifPresent(order -> {
                Optional<CommissionTier> applicableTier = commissionTierService
                        .findApplicableTier(vendorDetailsData.getId(), order.getPaymentMethod().getId(), amount, this.mobileMoneyChannelService.findByType(PaymentChannel.PUSH_USSD).getId(), this.mnoService.findByName(payBillPayment.getOperator()).getId());

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

        //return totalCommission.get().floatValue();
        return totalCommission.get() != null ? totalCommission.get().floatValue() : 0f;
    }

    /**
     * Updates ledger with proper validation and error handling
     */
    @Transactional
    public void updateLedger(BalanceLedger balanceLedger) {
        if (balanceLedger == null || balanceLedger.getMainAccount() == null ||
                balanceLedger.getPushUssd() == null) {
            throw new IllegalArgumentException("Invalid balance ledger data");
        }

        VendorDetails vendorDetails = balanceLedger.getMainAccount().getVendorDetails();
        PayBillPayment payBillPayment = balanceLedger.getPayBillPayment();

        try {
            logger.debug("Updating ledger for vendor: {}, amount: {}",
                    vendorDetails.getId(), payBillPayment.getAmount());

            float prevAmount = calculatePreviousAmount(vendorDetails, payBillPayment.getAmount());
            float currentAmount = prevAmount + payBillPayment.getAmount();

            Ledger ledger = Ledger.builder()
                    .vendorDetails(vendorDetails)
                    .prevAmount(prevAmount)
                    .amount(payBillPayment.getAmount())
                    .curAmount(currentAmount)
                    .receipt(payBillPayment.getPaymentReference())
                    .requestType(determineRequestType(payBillPayment.getCollectionType()))
                    //.createdAt(System.currentTimeMillis())
                    // VAT-related fields
                    .hasTaxation(Boolean.parseBoolean(vendorDetails.getHasVat()))
                    .vatRate(Boolean.parseBoolean(vendorDetails.getHasVat()) ? this.getTransactionTax(payBillPayment) != null ? this.getTransactionTax(payBillPayment).getTaxRate() : 0f : 0f)
                    .vatAmount(balanceLedger.getVat()) // From BalanceLedger
                    // Commission fields
                    .commissionRate(this.getCommissionRate(payBillPayment.getAmount(), vendorDetails, payBillPayment))
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
    private float getCommissionRate(float amount, VendorDetails vendorDetails, PayBillPayment payBillPayment) {
        AtomicReference<Float> commissionRate = new AtomicReference<>();
        sessionManagementService.getSession(payBillPayment.getSessionId()).ifPresentOrElse(session -> {
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
            orderService.findByPaymentSessionId(payBillPayment.getSessionId()).ifPresent(order -> {
                Optional<CommissionTier> applicableTier = commissionTierService
                        .findApplicableTier(vendorDetails.getId(), order.getPaymentMethod().getId(), totalAmount, this.mobileMoneyChannelService.findByType(PaymentChannel.PUSH_USSD).getId(), this.mnoService.findByName(payBillPayment.getOperator()).getId());

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
     * Batch update method for multiple deposits
     */
    /*public CompletableFuture<Map<String, Boolean>> updateMultipleDeposits(List<PushUssd> pushUssdList) {
        if (pushUssdList == null || pushUssdList.isEmpty()) {
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        List<CompletableFuture<Map.Entry<String, Boolean>>> futures = pushUssdList.stream()
                .map(pushUssd ->
                        updateDepositBalance(pushUssd)
                                .thenApply(result ->
                                        Map.entry(pushUssd.getVendorDetails().getId(), result))
                )
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ))
                );
    }*/

    /*public void updatePayBillDepositBalance(PayBillPayment payBillPayment) {
        BalanceLedger balanceLedger = new BalanceLedger(null, null, payBillPayment,null);
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> this.updateMainAccountBalance(payBillPayment.getInstitution(), payBillPayment.getAmount(), 0, balanceLedger), ioExecutor)
                .thenAcceptAsync(this::updateLedger, ioExecutor);
        future.join();
    }*/

    /**
     * Synchronous version - maintains backward compatibility
     *
     * @param payBillPayment The Pay Bill data to update
     * @return boolean indicating success/failure
     */
    @Transactional
    @Retryable(value = {OptimisticLockingFailureException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 100))
    public boolean updateUssdPushStatus(PayBillPayment payBillPayment) {
        if (payBillPayment == null || payBillPayment.getId() == null) {
            logger.error("Pay Bill or its ID cannot be null");
            return false;
        }

        try {
            logger.debug("Updating Pay Bill status for ID: {}", payBillPayment.getId());

            return this.payBillPaymentService.findById(payBillPayment.getId())
                    .map(existingPushUssd -> {
                       /* // Validate current status before updating
                        if (!isValidStatusTransition(existingPushUssd.getCollectionStatus(), CollectionStatus.DEPOSITED)) {
                            logger.warn("Invalid status transition from {} to DEPOSITED for USSD ID: {}",
                                    existingPushUssd.getCollectionStatus(), pushUssd.getId());
                            return false;
                        }*/

                        if (existingPushUssd.getErrorMessage() == null) {
                            existingPushUssd.setCollectionStatus(CollectionStatus.DEPOSITED);
                        }

                        PayBillPayment payBillPaymentResult = this.payBillPaymentService.update(existingPushUssd);

                        logger.info("Successfully updated Pay Bill status to DEPOSITED for ID: {}",
                                payBillPayment.getId());
                        return payBillPaymentResult != null;
                    })
                    .orElseGet(() -> {
                        logger.error("Pay Bill Payment not found with ID: {}", payBillPayment.getId());
                        return false;
                    });

        } catch (Exception e) {
            logger.error("Failed to update Pay Bill status for ID: {}", payBillPayment.getId(), e);
            return false;
        }
    }

    /**
     * Synchronous version - update Deposit Status
     *
     * @param payBillPayment The USSD push data to update
     * @return boolean indicating success/failure
     */
    @Transactional
    @Retryable(value = {OptimisticLockingFailureException.class},
            maxAttempts = MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(delay = 100))
    public boolean updateDepositTransactionStatus(PayBillPayment payBillPayment) {
        if (payBillPayment == null || payBillPayment.getId() == null) {
            logger.error("PushUssd or its ID cannot be null");
            return false;
        }

        try {
            logger.debug("Updating Deposit transaction status for ID: {}", payBillPayment.getId());

            return this.depositService.findByMsisdnAndChannelAndPaymentReference(payBillPayment.getMsisdn(), PaymentChannel.PUSH_USSD, payBillPayment.getPaymentReference())
                    .map(existingDeposit -> {
                       /* // Validate current status before updating
                        if (!isValidStatusTransition(existingPushUssd.getCollectionStatus(), CollectionStatus.DEPOSITED)) {
                            logger.warn("Invalid status transition from {} to DEPOSITED for USSD ID: {}",
                                    existingPushUssd.getCollectionStatus(), pushUssd.getId());
                            return false;
                        }*/

                        existingDeposit.setRequestStatus(RequestStatus.COMPLETED);

                        Deposit updatedDeposit = this.depositService.update(existingDeposit);

                        logger.info("Successfully updated Deposit transaction status to COMPLETED for ID: {}",
                                payBillPayment.getId());
                        return updatedDeposit != null;
                    })
                    .orElseGet(() -> {
                        logger.error("Deposit not found with ID: {}", payBillPayment.getId());
                        return false;
                    });

        } catch (Exception e) {
            logger.error("Failed to update Deposit transaction status for ID: {}", payBillPayment.getId(), e);
            return false;
        }
    }


    /**
     * Asynchronous version for better performance
     *
     * @param payBillPayment The Pay Bill data to update
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateDepositTransactionStatusAsync(PayBillPayment payBillPayment) {
        if (payBillPayment == null || payBillPayment.getId() == null) {
            logger.error("PushUssd or its ID cannot be null");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return updateDepositTransactionStatus(payBillPayment);
                    } catch (Exception e) {
                        logger.error("Async Deposit transaction status update failed for ID: {}",
                                payBillPayment.getId(), e);
                        throw new CompletionException("Deposit transaction status update failed", e);
                    }
                }, depositUpdateVirtualThread)
                .orTimeout(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof TimeoutException) {
                            logger.error("Deposit transaction status update timed out for ID: {}",
                                    payBillPayment.getId());
                        } else {
                            logger.error("Unexpected error during async Deposit transaction status update for ID: {}",
                                    payBillPayment.getId(), throwable);
                        }
                        return false;
                    }
                    return result;
                });
    }

    /**
     * Asynchronous version for better performance
     *
     * @param payBillPayment The Pay Bill data to update
     * @return CompletableFuture<Boolean> indicating success/failure
     */
    public CompletableFuture<Boolean> updateUssdPushStatusAsync(PayBillPayment payBillPayment) {
        if (payBillPayment == null || payBillPayment.getId() == null) {
            logger.error("PushUssd or its ID cannot be null");
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return updateUssdPushStatus(payBillPayment);
                    } catch (Exception e) {
                        logger.error("Async pay bill status update failed for ID: {}",
                                payBillPayment.getId(), e);
                        throw new CompletionException("USSD status update failed", e);
                    }
                }, completeDepositVirtualThread)
                .orTimeout(OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .handle((result, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof TimeoutException) {
                            logger.error("pay bill status update timed out for ID: {}",
                                    payBillPayment.getId());
                        } else {
                            logger.error("Unexpected error during async pay bill status update for ID: {}",
                                    payBillPayment.getId(), throwable);
                        }
                        return false;
                    }
                    return result;
                });
    }


    public void initPayBillDeposit(PayBillPayment payBillPayment) {

        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> initiatePayBillDeposit(payBillPayment), ioExecutor)
                .thenApplyAsync(this::recordPayBillDepositTransaction, ioExecutor);
        future.join();
    }

    public void completePayBillDeposit(PaymentResponse responseBody, PayBillPayment payBillPayment) {
        if (responseBody.getStatus() == 200) {
            this.transactionLogService.findByMsisdnAndChannelAndPaymentReference(payBillPayment.getMsisdn(), PaymentChannel.PUSH_USSD, payBillPayment.getPaymentReference()).ifPresent(transaction -> {
                transaction.setRequestStatus(RequestStatus.INITIATED);
                transaction.setThirdPartyResponse("PAY BILL" + ":" + responseBody.getData().getStatus() + ":" + responseBody.getMessage());
                transactionLogService.updateTransaction(transaction);
            });
        } else {
            this.transactionLogService.findByMsisdnAndChannelAndPaymentReference(payBillPayment.getMsisdn(), PaymentChannel.PUSH_USSD, payBillPayment.getPaymentReference()).ifPresent(transaction -> {
                transaction.setRequestStatus(RequestStatus.FAILED);
                transaction.setThirdPartyResponse("PAY BILL" + ":" + responseBody.getData().getStatus() + ":" + responseBody.getMessage());
                transactionLogService.updateTransaction(transaction);
            });
        }
    }


    public void initiatePayBillDeposit(PaymentResponse responseBody, PayBillPayment payBillPayment) {
        if (responseBody.getStatus() == 200) {
            this.transactionLogService.findByMsisdnAndChannelAndPaymentReference(payBillPayment.getMsisdn(), PaymentChannel.PAY_BILL, payBillPayment.getPaymentReference()).ifPresent(transaction -> {
                transaction.setRequestStatus(RequestStatus.INITIATED);
                transaction.setPayBillTransactionId(responseBody.getData().getTransactionId());
                transaction.setThirdPartyResponse("PAY BILL" + ":" + responseBody.getData().getStatus() + ":" + responseBody.getMessage());
                transactionLogService.updateTransaction(transaction);
            });
        } else {
            this.transactionLogService.findByMsisdnAndChannelAndPaymentReference(payBillPayment.getMsisdn(), PaymentChannel.PAY_BILL, payBillPayment.getPaymentReference()).ifPresent(transaction -> {
                transaction.setRequestStatus(RequestStatus.FAILED);
                //transaction.setPayBillTransactionId(responseBody.getData().getTransactionId());
                transaction.setThirdPartyResponse("PAY BILL" + ":" + responseBody.getData().getStatus() + ":" + responseBody.getMessage());
                transactionLogService.updateTransaction(transaction);
            });
        }
    }

    public void updatePayBillDepositBalance(PayBillPayment payBillPayment) {
        BalanceLedger balanceLedger = new BalanceLedger(null, null, null, payBillPayment, null, 0, 0, 0);
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> this.updateMainAccountBalance(payBillPayment.getVendorDetails(), payBillPayment.getAmount(), 0, balanceLedger, null), ioExecutor)
                .thenAcceptAsync(this::updatePayBillLedger, ioExecutor);
        future.join();
    }

    /*public void updateUssdPushStatus(PushUssd pushUssd) {
        this.pushUssdService.findPushUssdById(pushUssd.getId()).ifPresent(pushUssd1 -> {
            pushUssd1.setCollectionStatus(CollectionStatus.DEPOSITED);
            this.pushUssdService.update(pushUssd1);
        });
    }*/


    public void updatePayBillStatus(PayBillPayment payBillPayment) {
        this.payBillPaymentService.findById(payBillPayment.getId()).ifPresent(payBillPayment1 -> {
            payBillPayment1.setCollectionStatus(CollectionStatus.DEPOSITED);
            this.payBillPaymentService.update(payBillPayment1);
        });
    }

    /*public void updateDepositTransactionStatus(PushUssd pushUssd) {
        this.depositService.findByMsisdnAndChannelAndPaymentReference(pushUssd.getMsisdn(), PaymentChannel.PUSH_USSD, pushUssd.getReference()).ifPresent(deposit -> {
            deposit.setRequestStatus(RequestStatus.COMPLETED);
            depositService.update(deposit);
        });
    }*/

    public void updateCongoDepositBalance(PushUssd pushUssd) {
        BalanceLedger balanceLedger = new BalanceLedger(null, pushUssd, null, null, null, 0, 0, 0);
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> this.updateSubAccountBalance(pushUssd.getVendorDetails(), pushUssd.getAmount(), 0, balanceLedger), ioExecutor)
                .thenAcceptAsync(this::updateSubLedger, ioExecutor);
        future.join();
    }

    public void confirmPayBillDeposit(Map<String, Object> responseBody, PayBillPayment payBillPayment) {
        if (responseBody.get("status").toString().equalsIgnoreCase("200")) {
            this.transactionLogService.findByMsisdnAndChannelAndPaymentReference(payBillPayment.getMsisdn(), PaymentChannel.PAY_BILL, payBillPayment.getPaymentReference()).ifPresent(transaction -> {
                transaction.setRequestStatus(RequestStatus.COMPLETED);
                transaction.setThirdPartyResponse("PAY BILL" + ": deposit confirmation success :" + responseBody.get("message"));
                transactionLogService.updateTransaction(transaction);
            });
        } else {
            this.transactionLogService.findByMsisdnAndChannelAndPaymentReference(payBillPayment.getMsisdn(), PaymentChannel.PAY_BILL, payBillPayment.getPaymentReference()).ifPresent(transaction -> {
                transaction.setRequestStatus(RequestStatus.FAILED);
                transaction.setThirdPartyResponse("PAY BILL" + ": deposit confirmation failed :" + responseBody.get("message"));
                transactionLogService.updateTransaction(transaction);
            });
        }
    }
}
