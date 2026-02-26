package com.hybrid9.pg.Lipanasi.services;

import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2.PayBillValidationRequest;
import com.hybrid9.pg.Lipanasi.dto.airtelmoneypaybill.v2.APResponse;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class APValidationService {
    private final PayBillPaymentService paymentService;
    private final PaymentUtilities paymentUtilities;
    private final MnoServiceImpl mnoService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;

    public APValidationService(PayBillPaymentService paymentService, PaymentUtilities paymentUtilities, MnoServiceImpl mnoService, MainAccountService mainAccountService, VendorService vendorService) {
        this.paymentUtilities = paymentUtilities;
        this.mnoService = mnoService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.paymentService = paymentService;
    }
    // UAT SECTION

    public APResponse validateTransactionUAT(PayBillValidationRequest request) {
        APResponse response = new APResponse();


        VendorDetails vendorDetails = vendorService.findVendorDetailsByCode("SC001")
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        try {
            // Validate mandatory fields
            if (!validateMandatoryFields(request)) {
                response.setStatus("400");
                response.setMessage("Mandatory fields missing");
                // Create payment entity
                recordPayBillTxnUAT(request, vendorDetails,response);
                return response;
            }

            // Validate type
            if (!"C2B".equals(request.getType())) {
                response.setStatus("400");
                response.setMessage("Invalid transaction type");
                // Create payment entity
                recordPayBillTxnUAT(request, vendorDetails,response);
                return response;
            }

            // Validate amount
            if (request.getAmount() < 100) {
                response.setStatus("400");
                response.setMessage("Invalid amount");
                // Create payment entity
                recordPayBillTxnUAT(request, vendorDetails,response);
                return response;
            }

            // Log successful validation
            log.info("Transaction validated successfully:");
            log.info("Customer MSISDN: {}", request.getCustomerMsisdn());
            log.info("Merchant MSISDN: {}", request.getMerchantMsisdn());
            log.info("Amount: {}", request.getAmount());
            log.info("Reference1: {}", request.getReference1());

            response.setStatus("200");
            response.setMessage("Transaction validated successfully");

            // Create payment entity
            recordPayBillTxnUAT(request, vendorDetails,response);

        } catch (Exception e) {
            log.error("Error during validation: ", e);
            response.setStatus("400");
            response.setMessage("Validation failed: " + e.getMessage());
        }

        return response;
    }

    private void recordPayBillTxnUAT(PayBillValidationRequest request, VendorDetails vendorDetails, APResponse response) {
        if (response.getStatus().equals("200")) {
            // Update payment status to PAYMENT_INIT
            PayBillPayment payment = PayBillPayment.builder()
                    .validationId(request.getReference1())
                    .validationErrorMessage(response.getMessage()+"For UAT")
                    .validationErrorCode(response.getStatus())
                    .paymentReference(paymentUtilities.generateRefNumber())
                    .amount(request.getAmount().floatValue())
                    .currency("TZS") // Assuming Tanzania Shillings
                    .msisdn(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn()))
                    .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn())))
                    .transactionDate(LocalDateTime.now().toString())
                    .collectionStatus(CollectionStatus.PAYMENT_INIT)
                    .vendorDetails(vendorDetails)
                    .accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                    .build();

            // Save transaction
            paymentService.createPayBill(payment);
        }else {
            // Update payment status to REJECTED
            PayBillPayment payment = PayBillPayment.builder()
                    .validationId(request.getReference1())
                    .validationErrorMessage(response.getMessage()+"For UAT")
                    .validationErrorCode(response.getStatus())
                    .paymentReference(paymentUtilities.generateRefNumber())
                    .amount(request.getAmount().floatValue())
                    .currency("TZS") // Assuming Tanzania Shillings
                    .msisdn(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn()))
                    .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn())))
                    .transactionDate(LocalDateTime.now().toString())
                    .collectionStatus(CollectionStatus.REJECTED)
                    .vendorDetails(vendorDetails)
                    .accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                    .build();

            // Save transaction
            paymentService.createPayBill(payment);

        }

    }


    // PROD SECTION
    public APResponse validateTransaction(PayBillValidationRequest request) {
        APResponse response = new APResponse();


        VendorDetails vendorDetails = vendorService.findVendorDetailsByCode("SC001")
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        try {
            // Validate mandatory fields
            if (!validateMandatoryFields(request)) {
                response.setStatus("400");
                response.setMessage("Mandatory fields missing");
                // Create payment entity
                recordPayBillTxn(request, vendorDetails,response);
                return response;
            }

            // Validate type
            if (!"C2B".equals(request.getType())) {
                response.setStatus("400");
                response.setMessage("Invalid transaction type");
                // Create payment entity
                recordPayBillTxn(request, vendorDetails,response);
                return response;
            }

            // Validate amount
            if (request.getAmount() < 100) {
                response.setStatus("400");
                response.setMessage("Invalid amount");
                // Create payment entity
                recordPayBillTxn(request, vendorDetails,response);
                return response;
            }

            // Log successful validation
            log.info("Transaction validated successfully:");
            log.info("Customer MSISDN: {}", request.getCustomerMsisdn());
            log.info("Merchant MSISDN: {}", request.getMerchantMsisdn());
            log.info("Amount: {}", request.getAmount());
            log.info("Reference1: {}", request.getReference1());

            response.setStatus("200");
            response.setMessage("Transaction validated successfully");

            // Create payment entity
            recordPayBillTxn(request, vendorDetails,response);

        } catch (Exception e) {
            log.error("Error during validation: ", e);
            response.setStatus("400");
            response.setMessage("Validation failed: " + e.getMessage());
        }

        return response;
    }

    private void recordPayBillTxn(PayBillValidationRequest request, VendorDetails vendorDetails, APResponse response) {
        if (response.getStatus().equals("200")) {
            // Update payment status to PAYMENT_INIT
            PayBillPayment payment = PayBillPayment.builder()
                    .validationId(request.getReference1())
                    .validationErrorMessage(response.getMessage())
                    .validationErrorCode(response.getStatus())
                    .paymentReference(paymentUtilities.generateRefNumber())
                    .amount(request.getAmount().floatValue())
                    .currency("TZS") // Assuming Tanzania Shillings
                    .msisdn(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn()))
                    .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn())))
                    .transactionDate(LocalDateTime.now().toString())
                    .collectionStatus(CollectionStatus.PAYMENT_INIT)
                    .vendorDetails(vendorDetails)
                    .accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                    .build();

            // Save transaction
            paymentService.createPayBill(payment);
        }else {
            // Update payment status to REJECTED
            PayBillPayment payment = PayBillPayment.builder()
                    .validationId(request.getReference1())
                    .validationErrorMessage(response.getMessage())
                    .validationErrorCode(response.getStatus())
                    .paymentReference(paymentUtilities.generateRefNumber())
                    .amount(request.getAmount().floatValue())
                    .currency("TZS") // Assuming Tanzania Shillings
                    .msisdn(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn()))
                    .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getCustomerMsisdn())))
                    .transactionDate(LocalDateTime.now().toString())
                    .collectionStatus(CollectionStatus.REJECTED)
                    .vendorDetails(vendorDetails)
                    .accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                    .build();

            // Save transaction
            paymentService.createPayBill(payment);

        }

    }

    private boolean validateMandatoryFields(PayBillValidationRequest request) {
        return request.getType() != null &&
                request.getCustomerMsisdn() != null &&
                request.getMerchantMsisdn() != null &&
                request.getAmount() != null &&
                request.getReference1() != null;
    }
}
