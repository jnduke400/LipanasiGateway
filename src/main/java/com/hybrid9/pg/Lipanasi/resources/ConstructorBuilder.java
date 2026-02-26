package com.hybrid9.pg.Lipanasi.resources;


import com.hybrid9.pg.Lipanasi.route.processor.DepositProcessor;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelServiceImpl;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTierService;
import com.hybrid9.pg.Lipanasi.services.commission.CommissionTransactionService;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.SessionManagementService;
import com.hybrid9.pg.Lipanasi.services.tax.TransactionTaxService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.*;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import org.springframework.stereotype.Component;

@Component
public class ConstructorBuilder {

    private PushUssdService pushUssdService;
    private MnoServiceImpl mnoService;
    private TransactionLogService transactionLogService;
    private LedgerService ledgerService;
    private CashOutService cashOutService;
    private DepositService depositService;
    private CashInLogService cashInLogService;
    private CashOutLogService cashOutLogService;
    private MainAccountService mainAccountService;
    private PayBillPaymentService payBillPaymentService;
    private SubAccountService subAccountService;
    private SubLedgerService subLedgerService;
    private final SessionManagementService sessionManagementService;
    private final PaymentMethodService paymentMethodService;
    private final OrderService orderService;
    private CommissionTierService commissionTierService;
    private CommissionTransactionService commissionTransactionService;
    private final TransactionTaxService transactionTaxService;
    private final MobileMoneyChannelServiceImpl mobileMoneyChannelService;

    public ConstructorBuilder(PushUssdService pushUssdService, MnoServiceImpl mnoService, TransactionLogService transactionLogService, LedgerService ledgerService,
                              CashOutService cashOutService, DepositService depositService, CashInLogService cashInLogService, CashOutLogService cashOutLogService,
                              MainAccountService mainAccountService, PayBillPaymentService payBillPaymentService, SubAccountService subAccountService,
                              SubLedgerService subLedgerService, SessionManagementService sessionManagementService, PaymentMethodService paymentMethodService,
                              OrderService orderService, CommissionTierService commissionTierService, CommissionTransactionService commissionTransactionService,
                              TransactionTaxService transactionTaxService, MobileMoneyChannelServiceImpl mobileMoneyChannelService) {
        this.sessionManagementService = sessionManagementService;
        this.paymentMethodService = paymentMethodService;
        this.orderService = orderService;
        this.commissionTierService = commissionTierService;
        this.commissionTransactionService = commissionTransactionService;
        this.transactionTaxService = transactionTaxService;
        this.pushUssdService = pushUssdService;
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
        this.mobileMoneyChannelService = mobileMoneyChannelService;
    }

    public DepositProcessor getDepositProcessor() {
        return new DepositProcessor(pushUssdService, mnoService, transactionLogService, ledgerService, cashOutService, depositService, cashInLogService, cashOutLogService,mainAccountService,payBillPaymentService, subAccountService, subLedgerService, sessionManagementService, paymentMethodService, orderService, commissionTierService, commissionTransactionService, transactionTaxService,mobileMoneyChannelService);
    }

}
