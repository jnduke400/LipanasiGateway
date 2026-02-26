package com.hybrid9.pg.Lipanasi.services.paybill;

import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2.*;
import com.hybrid9.pg.Lipanasi.dto.mixxpaybill.v2.SyncBillPayRequest;
import com.hybrid9.pg.Lipanasi.dto.mixxpaybill.v2.SyncBillPayResponse;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.PaymentRequest;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.OperatorMapping;
import com.hybrid9.pg.Lipanasi.resources.PaybillResource;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.payments.gw.OperatorManagementService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class MixxPayBillService {
    private final PayBillPaymentService paymentService;
    private final PaymentUtilities paymentUtilities;
    private final MnoServiceImpl mnoService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PaybillResource paybillResource;
    private final OrderService orderService;
    private final OperatorManagementService operatorManagementService;

    public MixxPayBillService(PayBillPaymentService paymentService, PaymentUtilities paymentUtilities,
                              MnoServiceImpl mnoService, MainAccountService mainAccountService,
                              VendorService vendorService,PaybillResource paybillResource,
                              OrderService orderService, OperatorManagementService operatorManagementService) {
        this.paymentUtilities = paymentUtilities;
        this.mnoService = mnoService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.paymentService = paymentService;
        this.paybillResource = paybillResource;
        this.orderService = orderService;
        this.operatorManagementService = operatorManagementService;
    }

    public SyncBillPayResponse validateAndProcessPayment(SyncBillPayRequest request) {
        SyncBillPayResponse response = new SyncBillPayResponse();

        String uniqueId = UUID.randomUUID().toString();

        try {
            // Get order
            Order order = orderService.findByReceipt(request.getCustomerReferenceId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // Compose payment request
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .orderNumber(order.getOrderNumber())
                    .paymentChannel("PAY_BILL")
                    .paymentMethod("mobile")
                    .msisdn(request.getMsisdn())
                    .build();

            // start validating session
            this.paybillResource.manageSession(paymentRequest, order);

            // Validate mandatory fields
            if (!validateMandatoryFields(request)) {
                response.setType("SYNC_BILLPAY_RESPONSE");
                response.setTxnId(request.getTxnId());
                response.setRefId(uniqueId);
                response.setResult("TF");
                response.setErrorCode("error112");
                response.setErrorDesc("Mandatory fields missing");
                response.setMsisdn(request.getMsisdn());
                response.setFlag("N");
                response.setContent("Mandatory fields missing");
                // Create payment entity
                recordPayBillTxn(request, order.getCustomer().getVendorDetails(), response);
                return response;
            }

            // Validate type
            if (!"SYNC_BILLPAY_REQUEST".equals(request.getType())) {
                response.setType("SYNC_BILLPAY_RESPONSE");
                response.setTxnId(request.getTxnId());
                response.setRefId(uniqueId);
                response.setResult("TF");
                response.setErrorCode("error016");
                response.setErrorDesc("Invalid payment");
                response.setMsisdn(request.getMsisdn());
                response.setFlag("N");
                response.setContent("Invalid payment, invalid type");
                // Create payment entity
                recordPayBillTxn(request, order.getCustomer().getVendorDetails(),response);
                return response;
            }

            // Validate amount
            if (request.getAmount() < 100) {
                response.setType("SYNC_BILLPAY_RESPONSE");
                response.setTxnId(request.getTxnId());
                response.setRefId(uniqueId);
                response.setResult("TF");
                response.setErrorCode("error012");
                response.setErrorDesc("Invalid Amount");
                response.setMsisdn(request.getMsisdn());
                response.setFlag("N");
                response.setContent("The amount is invalid");
                // Create payment entity
                recordPayBillTxn(request, order.getCustomer().getVendorDetails(),response);
                return response;
            }

            // Log successful validation
            log.info("Transaction validated successfully:");
            log.info("Customer MSISDN: {}", request.getMsisdn());
            log.info("Amount: {}", request.getAmount());
            log.info("Customer Reference: {}", request.getCustomerReferenceId());

            //return response;
            response.setType("SYNC_BILLPAY_RESPONSE");
            response.setTxnId(request.getTxnId());
            response.setRefId(uniqueId);
            response.setResult("TS");
            response.setErrorCode("error000");
            response.setErrorDesc("Successful transaction");
            response.setMsisdn(request.getMsisdn());
            response.setFlag("Y");
            response.setContent("The transaction was successful");

            // Create payment entity
            recordPayBillTxn(request, order.getCustomer().getVendorDetails(),response);

        } catch (Exception e) {
            log.error("Error during validation: ", e);
            response.setType("SYNC_BILLPAY_RESPONSE");
            response.setTxnId(request.getTxnId());
            response.setRefId(uniqueId);
            response.setResult("TF");
            response.setErrorCode("error013");
            response.setErrorDesc("Validation failed");
            response.setMsisdn(request.getMsisdn());
            response.setFlag("N");
            response.setContent("Error during validation: -"+e.getMessage());
        }

        return response;
    }
    private String getTxnId(PayBillPaymentRequest request) {
        AtomicReference<String> txnId = new AtomicReference<>();
        this.paymentService.findPayBillByValidationId(request.getReference1())
                .ifPresent(payment -> {
                    txnId.set(payment.getPaymentReference());
                });
        return txnId.get();

    }

    private void recordPayBillTxn(SyncBillPayRequest request, VendorDetails vendorDetails, SyncBillPayResponse response) {
        if (response.getErrorCode().equals("error000")) {
            // Update payment status to PAYMENT_INIT
            PayBillPayment payment = PayBillPayment.builder()
                    .validationId(request.getTxnId())
                    .validationErrorMessage(response.getErrorDesc())
                    .validationErrorCode(response.getErrorCode())
                    .originalReference(request.getCustomerReferenceId())
                    .paymentReference(paymentUtilities.generateRefNumber())
                    .amount(request.getAmount().floatValue())
                    .currency("TZS") // Assuming Tanzania Shillings
                    .msisdn(paymentUtilities.formatPhoneNumber("255", request.getMsisdn()))
                    .operator(this.operatorManagementService.getOperator(this.paymentUtilities.getOperatorPrefix(request.getMsisdn())).orElseThrow(() -> new RuntimeException("Operator not found for prefix: " + this.paymentUtilities.getOperatorPrefix(request.getMsisdn()))).getOperatorName())
                    .transactionDate(LocalDateTime.now().toString())
                    .collectionStatus(CollectionStatus.COLLECTED)
                    .vendorDetails(vendorDetails)
                    //.accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                    .build();

            // Save transaction
            paymentService.createPayBill(payment);
        }else {
            // Update payment status to REJECTED
            PayBillPayment payment = PayBillPayment.builder()
                    .validationId(request.getTxnId())
                    .validationErrorMessage(response.getErrorDesc())
                    .validationErrorCode(response.getErrorCode())
                    .originalReference(request.getCustomerReferenceId())
                    .paymentReference(paymentUtilities.generateRefNumber())
                    .amount(request.getAmount().floatValue())
                    .currency("TZS") // Assuming Tanzania Shillings
                    .msisdn(paymentUtilities.formatPhoneNumber("255", request.getMsisdn()))
                    .operator(this.operatorManagementService.getOperator(this.paymentUtilities.getOperatorPrefix(request.getMsisdn())).orElseThrow(() -> new RuntimeException("Operator not found for prefix: " + this.paymentUtilities.getOperatorPrefix(request.getMsisdn()))).getOperatorName())
                    .transactionDate(LocalDateTime.now().toString())
                    .collectionStatus(CollectionStatus.REJECTED)
                    .vendorDetails(vendorDetails)
                    //.accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                    .build();

            // Save transaction
            paymentService.createPayBill(payment);

        }

    }

    private boolean validateMandatoryFields(SyncBillPayRequest request) {
        return request.getType() != null &&
                request.getTxnId() != null &&
                request.getMsisdn() != null &&
                request.getAmount() != null &&
                request.getCompanyName() != null &&
                request.getCustomerReferenceId() != null;
    }

    private boolean validateMandatoryFieldsForEnquiry(EnquiryRequest request) {
        return request.getMsisdn() != null &&
                request.getTxnId() != null;
    }
}
