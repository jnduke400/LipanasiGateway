package com.hybrid9.pg.Lipanasi.services.commission;

import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTier;
import com.hybrid9.pg.Lipanasi.entities.payments.commission.CommissionTransaction;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.enums.PaymentMethodType;
import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
import com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx.VendorManager;
import com.hybrid9.pg.Lipanasi.models.session.UserSession;
import com.hybrid9.pg.Lipanasi.resources.ExternalResources;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelConfigServiceImpl;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MobileMoneyChannelServiceImpl;
import com.hybrid9.pg.Lipanasi.services.payments.gw.OperatorManagementService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class CommissionService {

    @Autowired
    @Qualifier("ioExecutor")
    private Executor ioExecutor;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private MnoServiceImpl mnoService;

    @Autowired
    private MobileMoneyChannelConfigServiceImpl mobileMoneyChannelConfigService;

    @Autowired
    private MobileMoneyChannelServiceImpl mobileMoneyChannelService;

    @Autowired
    private CommissionTierService commissionTierService;

    @Autowired
    private CommissionTransactionService commissionTransactionService;

    @Autowired
    private OperatorManagementService operatorManagementService;

    @Autowired
    private ExternalResources externalResources;

    @Transactional
    public CompletableFuture<CommissionTransaction> calculateCommission(
            String vendorExternalId,
            BigDecimal amount,
            PaymentMethodType paymentMethodType,
            Long paymentMethodId,
            Long mobileOperatorId,
            Long mobileMoneyChannelId, String paymentReference, PushUssd pushUssd, PayBillPayment payBillPayment, CardPayment cardPayment) {
        return CompletableFuture.supplyAsync(() -> {
            // Fetch the vendor
            VendorDetails vendor = Optional.of(vendorService.findVendorDetailsByVendorExternalId(vendorExternalId))
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));

            // Find applicable commission tier
            Optional<CommissionTier> applicableTier = commissionTierService
                    .findApplicableTier(vendor.getId(), paymentMethodId, amount,mobileMoneyChannelId,mobileOperatorId);

            if (applicableTier.isEmpty()) {
                throw new RuntimeException("No applicable commission tier found");
            }

            CommissionTier tier = applicableTier.get();

            // Calculate commission
            BigDecimal baseFee = tier.getBaseFee();
            BigDecimal percentageFee = amount.multiply(tier.getPercentageRate().divide(new BigDecimal("100")));
            BigDecimal totalCommission = baseFee.add(percentageFee);

            // Create transaction record
            CommissionTransaction transaction = new CommissionTransaction();
            transaction.setTransactionReference(paymentReference);
            transaction.setVendor(vendor);
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setTransactionAmount(amount);
            transaction.setCommissionAmount(totalCommission);
            transaction.setBaseFee(baseFee);
            transaction.setPercentageFee(tier.getPercentageRate());
            transaction.setPaymentMethod(tier.getPaymentMethod());
            transaction.setCardPayment(cardPayment);
            transaction.setPushUssd(pushUssd);
            transaction.setPayBillPayment(payBillPayment);

            // Set mobile money specific details if applicable
            if (paymentMethodType == PaymentMethodType.MOBILE_MONEY) {
                if (mobileOperatorId != null) {
                /*MnoMapping operator = new MnoMapping();
                operator.setId(mobileOperatorId);*/
                    Optional.of(mnoService.findById(mobileOperatorId)).ifPresent(transaction::setMobileOperator);
                    /*transaction.setMobileOperator(operator);*/
                }

                if (mobileMoneyChannelId != null) {
                /*MobileMoneyChannel channel = new MobileMoneyChannel();
                channel.setId(mobileMoneyChannelId);*/
                    Optional.of(this.mobileMoneyChannelService.findById(mobileMoneyChannelId)).ifPresent(transaction::setMobileMoneyChannel);
                    /*transaction.setMobileMoneyChannel(channel);*/
                }
            }

            return commissionTransactionService.recordCommission(transaction);
        }, ioExecutor);
    }


    @Transactional
    public CompletableFuture<CommissionTransaction> calculateCommissionWhenSessionIsActive(
            BigDecimal amount,
            PaymentMethodType paymentMethodType,
            String mobileNumber,
            String paymentChannelName, UserSession session, VendorManager vendor, String paymentReference, PushUssd pushUssd, PayBillPayment payBillPayment, CardPayment cardPayment) {
        return CompletableFuture.supplyAsync(() -> {

            // double-check if commission is active
            if (!session.getCommissionConfig().getCommissionStatus().equals(CommissionConfig.CommissionStatus.ACTIVE)) {
                log.info("Commission is not active for session: {}", session.getUserId());
                return CommissionTransaction.builder().build();
            }


            // CommissionTier tier = commissionTierService.findByCommissionTireId(Long.parseLong(session.getCommissionConfig().getCommissionTireId())).orElseThrow(() -> new RuntimeException("Commission tier not found"));

            // Calculate commission
            BigDecimal baseFee = session.getCommissionConfig().getBaseFee();
            BigDecimal percentageFee = amount.multiply(session.getCommissionConfig().getPercentageRate().divide(new BigDecimal("100")));
            BigDecimal totalCommission = baseFee.add(percentageFee);

            // Create transaction record
            CommissionTransaction transaction = new CommissionTransaction();
            transaction.setTransactionReference(paymentReference);
            transaction.setVendor(this.vendorService.findVendorDetailsByVendorExternalId(session.getMerchantId()));
            transaction.setTransactionDate(LocalDateTime.now());
            transaction.setTransactionAmount(amount);
            transaction.setCommissionAmount(totalCommission);
            transaction.setBaseFee(baseFee);
            transaction.setPercentageFee(session.getCommissionConfig().getPercentageRate());
            transaction.setPaymentMethod(session.getCommissionConfig().getPaymentMethod());
            transaction.setCardPayment(cardPayment);
            transaction.setPushUssd(pushUssd);
            transaction.setPayBillPayment(payBillPayment);

            // Set mobile money specific details if applicable
            if (paymentMethodType == PaymentMethodType.MOBILE_MONEY) {
                // Fetch operator mapping from Redis session
                operatorManagementService.getOperator(mobileNumber.substring(0, 5)).ifPresent(operatorMapping -> {
                    transaction.setMobileOperator(operatorMapping.getMnoMapping());
                });

                // Set mobile money channel from Redis session
                transaction.setMobileMoneyChannel(this.mobileMoneyChannelService.findByType(PaymentChannel.valueOf(paymentChannelName)));
            }

            return commissionTransactionService.recordCommission(transaction);
        }, ioExecutor);

    }
}
