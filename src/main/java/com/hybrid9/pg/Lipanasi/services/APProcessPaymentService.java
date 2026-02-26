package com.hybrid9.pg.Lipanasi.services;

import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2.*;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class APProcessPaymentService {
    private final PayBillPaymentService paymentService;
    private final PaymentUtilities paymentUtilities;
    private final MnoServiceImpl mnoService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;

    public APProcessPaymentService(PayBillPaymentService paymentService, PaymentUtilities paymentUtilities, MnoServiceImpl mnoService, MainAccountService mainAccountService, VendorService vendorService) {
        this.paymentUtilities = paymentUtilities;
        this.mnoService = mnoService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.paymentService = paymentService;
    }
    public EnquiryResponse enquiryTransaction(EnquiryRequest request) {
        EnquiryResponse response = new EnquiryResponse();

        try {
            // Validate mandatory fields
            if (!validateMandatoryFieldsForEnquiry(request)) {
                response.setStatus("400");
                response.setMessage("Mandatory fields missing");
                response.setRef("");
                return response;
            }
            Optional<PayBillPayment> payment = this.paymentService.findPayBillByValidationIdAndMsisdn(request.getTxnId(), paymentUtilities.formatPhoneNumber("255", request.getMsisdn()));
            if (payment.isPresent()) {

                if (payment.get().getCollectionStatus() != CollectionStatus.REJECTED) {
                    response.setStatus("200");
                    response.setMessage("Transaction was collected successfully");
                    response.setRef(payment.get().getPaymentReference());
                } else {
                    response.setStatus("400");
                    response.setMessage("Transaction was not collected, " + payment.get().getValidationErrorMessage());
                    response.setRef(payment.get().getPaymentReference());
                }
            } else {
                response.setStatus("404");
                response.setMessage("TXN not found");
                response.setRef("");
            }


        } catch (Exception e) {
            log.error("Error doing enquiry: ", e);
            response.setStatus("400");
            response.setRef("");
            response.setMessage("Validation failed: " + e.getMessage());
        }
        return response;
    }

    public APPaymentResponse validateAndProcessPayment(PayBillPaymentRequest request) {
        APPaymentResponse response = new APPaymentResponse();

        try {
            // Validate mandatory fields
            if (!validateMandatoryFields(request)) {
                response.setStatus("400");
                response.setTxnId(getTxnId(request));
                response.setMessage("Mandatory fields missing");
                return response;
            }

            // Validate type
            if (!"C2B".equals(request.getType())) {
                response.setStatus("400");
                response.setTxnId(getTxnId(request));
                response.setMessage("Invalid transaction type");
                return response;
            }

            // Validate amount
            if (request.getAmount() < 100) {
                response.setStatus("400");
                response.setTxnId(getTxnId(request));
                response.setMessage("Invalid amount");
                return response;
            }

            // Log successful validation
            log.info("Transaction validated successfully:");
            log.info("Customer MSISDN: {}", request.getCustomerMsisdn());
            log.info("Merchant MSISDN: {}", request.getMerchantMsisdn());
            log.info("Amount: {}", request.getAmount());
            log.info("Reference1: {}", request.getReference1());

            VendorDetails vendorDetails = vendorService.findVendorDetailsByCode("SC001")
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));

            Optional<PayBillPayment> payment = this.paymentService.findPayBillByValidationIdAndMsisdn(request.getReference1(), paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn()));
            if (payment.isPresent()) {
                if (payment.get().getCollectionStatus() == CollectionStatus.COLLECTED || payment.get().getCollectionStatus() == CollectionStatus.DEPOSITED) {
                    response.setStatus("400");
                    response.setTxnId(payment.get().getPaymentReference());
                    response.setMessage("Transaction is Duplicate");
                    payment.get().setValidationErrorMessage("Transaction is Duplicate");
                    payment.get().setValidationErrorCode("400");
                    payment.get().setCollectionStatus(CollectionStatus.REJECTED);
                    this.paymentService.update(payment.get());

                } else if (payment.get().getCollectionStatus() == CollectionStatus.PAYMENT_INIT) {
                    payment.get().setCollectionStatus(CollectionStatus.COLLECTED);
                    payment.get().setAmount(request.getAmount().floatValue());
                    payment.get().setMsisdn(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn()));
                    payment.get().setOperator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn())));
                    payment.get().setPaymentReference(paymentUtilities.generateRefNumber());
                    payment.get().setAccountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()));
                    payment.get().setVendorDetails(vendorDetails);
                    this.paymentService.update(payment.get());
                    response.setStatus("200");
                    response.setTxnId(payment.get().getPaymentReference());
                    response.setMessage("Payment is collected successfully");
                }
            }
            ;
            return response;
           /* response.setStatus("200");
            response.setMessage("Transaction validated successfully");*/

        } catch (Exception e) {
            log.error("Error during validation: ", e);
            response.setStatus("400");
            response.setTxnId(getTxnId(request));
            response.setMessage("Validation failed: " + e.getMessage());
        }

        return response;
    }

    //start uat purpose

    public APPaymentResponse validateAndProcessPaymentUAT(PayBillPaymentRequest request) {
        APPaymentResponse response = new APPaymentResponse();

        try {
            // Validate mandatory fields
            if (!validateMandatoryFields(request)) {
                response.setStatus("400");
                response.setTxnId(getTxnId(request));
                response.setMessage("Mandatory fields missing");
                return response;
            }

            // Validate type
            if (!"C2B".equals(request.getType())) {
                response.setStatus("400");
                response.setTxnId(getTxnId(request));
                response.setMessage("Invalid transaction type");
                return response;
            }

            // Validate amount
            if (request.getAmount() < 100) {
                response.setStatus("400");
                response.setTxnId(getTxnId(request));
                response.setMessage("Invalid amount");
                return response;
            }

            // Log successful validation
            log.info("Transaction validated successfully:");
            log.info("Customer MSISDN: {}", request.getCustomerMsisdn());
            log.info("Merchant MSISDN: {}", request.getMerchantMsisdn());
            log.info("Amount: {}", request.getAmount());
            log.info("Reference1: {}", request.getReference1());

            VendorDetails vendorDetails = vendorService.findVendorDetailsByCode("SC001")
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));

            Optional<PayBillPayment> payment = this.paymentService.findPayBillByValidationIdAndMsisdn(request.getReference1(), paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn()));
            if (payment.isPresent()) {
                if (payment.get().getCollectionStatus() == CollectionStatus.COLLECTED || payment.get().getCollectionStatus() == CollectionStatus.DEPOSITED) {
                    response.setStatus("400");
                    response.setTxnId(payment.get().getPaymentReference());
                    response.setMessage("Transaction is Duplicate");
                    payment.get().setValidationErrorMessage("Transaction is Duplicate FOR UAT");
                    payment.get().setValidationErrorCode("400");
                    payment.get().setCollectionStatus(CollectionStatus.REJECTED);
                    this.paymentService.update(payment.get());

                } else if (payment.get().getCollectionStatus() == CollectionStatus.PAYMENT_INIT) {
                    payment.get().setCollectionStatus(CollectionStatus.MARKED_FOR_DEPOSIT);
                    payment.get().setAmount(request.getAmount().floatValue());
                    payment.get().setMsisdn(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn()));
                    payment.get().setOperator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn())));
                    payment.get().setPaymentReference(paymentUtilities.generateRefNumber());
                    payment.get().setAccountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()));
                    payment.get().setVendorDetails(vendorDetails);
                    payment.get().setReceiptNumber(request.getReference2());
                    this.paymentService.update(payment.get());
                    response.setStatus("200");
                    response.setTxnId(payment.get().getPaymentReference());
                    response.setMessage("Payment is collected successfully");
                }
            }
            ;
            return response;
           /* response.setStatus("200");
            response.setMessage("Transaction validated successfully");*/

        } catch (Exception e) {
            log.error("Error during validation: ", e);
            response.setStatus("400");
            response.setTxnId(getTxnId(request));
            response.setMessage("Validation failed: " + e.getMessage());
        }

        return response;
    }

    // end uat purpose

    private String getTxnId(PayBillPaymentRequest request) {
        AtomicReference<String> txnId = new AtomicReference<>();
        this.paymentService.findPayBillByValidationId(request.getReference1())
                .ifPresent(payment -> {
                    txnId.set(payment.getPaymentReference());
                });
        return txnId.get();

    }

    private boolean validateMandatoryFields(PayBillPaymentRequest request) {
        return request.getType() != null &&
                request.getCustomerMsisdn() != null &&
                request.getMerchantMsisdn() != null &&
                request.getAmount() != null &&
                request.getReference1() != null;
    }

    private boolean validateMandatoryFieldsForEnquiry(EnquiryRequest request) {
        return request.getMsisdn() != null &&
                request.getTxnId() != null;
    }
}
