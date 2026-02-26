package com.hybrid9.pg.Lipanasi.resources;

import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.entities.payments.Deposit;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssdRef;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import com.nimbusds.jose.shaded.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@AllArgsConstructor
public class PushUssdResource {
    private final PushUssdRefService pushUssdRefService;
    private final VendorService vendorService;
    private final MainAccountService mainAccountService;
    private final PaymentUtilities paymentUtilities;
    private final DepositService depositService;
    private final MnoServiceImpl mnoService;
    private final Gson gson;

    // Creating a dedicated executor for I/O-bound operations
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public CompletableFuture<Deposit> createPushUssdReq(JsonObject pushUssdJsonObject, PushUssdService pushUssdService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String vendorCode = pushUssdJsonObject.get("code").getAsString();
                String finalReference = (pushUssdJsonObject.get("operator").getAsString().toLowerCase().contains("yas") ||
                        pushUssdJsonObject.get("operator").getAsString().toLowerCase().contains("tigo")) ?
                        this.paymentUtilities.generateTigopesaRefNumber() :
                        this.paymentUtilities.generateRefNumber();
                double amount = pushUssdJsonObject.get("amount").getAsDouble();
                String msisdn = pushUssdJsonObject.get("msisdn").getAsString();

                // Generating reference is I/O-bound, using a separate CompletableFuture
                CompletableFuture<String> referenceFuture = CompletableFuture.supplyAsync(() ->
                                this.generateUssdRef(pushUssdJsonObject.get("reference").getAsString(), finalReference),
                        ioExecutor);

                // Finding vendorx is I/O-bound, using a separate CompletableFuture
                CompletableFuture<Optional<VendorDetails>> vendorFuture = CompletableFuture.supplyAsync(() ->
                                this.vendorService.findVendorDetailsByCode(vendorCode),
                        ioExecutor);

                // Determining the operator is I/O-bound
                String formattedPhone = pushUssdJsonObject.get("operator").getAsString().toLowerCase().contains("tanzania") ?
                        paymentUtilities.formatPhoneNumber("255", msisdn) :
                        paymentUtilities.formatPhoneNumber("243", msisdn);

                CompletableFuture<String> operatorFuture = CompletableFuture.supplyAsync(() ->
                                pushUssdJsonObject.get("operator").getAsString().toLowerCase().contains("tanzania") ?
                                        this.mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", msisdn)) :
                                        this.mnoService.searchMno(paymentUtilities.formatPhoneNumber("243", msisdn)),
                        ioExecutor);

                // Wait for all async operations to complete
                String reference = referenceFuture.join();
                VendorDetails vendorDetails = vendorFuture.join().orElse(null);
                String operator = operatorFuture.join();

                // Transaction ID generation
                String transactionId;
                if (pushUssdJsonObject.get("operator").getAsString().toLowerCase().contains("airtel")) {
                    transactionId = paymentUtilities.generateAirtelRefNumber();
                } else if (pushUssdJsonObject.get("operator").getAsString().toLowerCase().contains("orange")) {
                    transactionId = paymentUtilities.generateOrangeCongoTransactionId();
                } else {
                    transactionId = UUID.randomUUID().toString();
                }

                // Build the deposit object
                Deposit deposit = Deposit.builder()
                        .msisdn(formattedPhone)
                        .amount(amount)
                        .channel(PaymentChannel.PUSH_USSD)
                        .paymentReference(reference)
                        .originalReference(pushUssdJsonObject.get("reference").getAsString())
                        .vendorDetails(vendorDetails)
                        .requestStatus(RequestStatus.NEW)
                        .transactionId(transactionId)
                        .currency(pushUssdJsonObject.get("currency").getAsString())
                        .sessionId(pushUssdJsonObject.get("sessionId").getAsString())
                        .operator(operator)
                        .build();

                // Record the deposit asynchronously
                return CompletableFuture.supplyAsync(() -> {
                    return this.depositService.recordDeposit(deposit);
                }, ioExecutor).join();

            } catch (Exception e) {
                // Handle final failure after retries
                e.printStackTrace();
                log.error("Request failed permanently", e);
                return new Deposit();
            }
        }, ioExecutor);
    }

    public String getVendorCode(String reference) {
        //get first three digits from reference
        if(reference.length() < 3) {
            return reference;
        }
        return reference.substring(0, 3);
    }

    public CompletableFuture<VendorDetails> getvendorAsync(String reference) {
        String vendorCode = this.getVendorCode(reference);
        return CompletableFuture.supplyAsync(() ->
                        this.vendorService.findVendorDetailsByCode(vendorCode).orElse(null),
                ioExecutor);
    }

    public VendorDetails getVendorDetails(String reference) {
        String vendorCode = this.getVendorCode(reference);
        return this.vendorService.findVendorDetailsByCode(vendorCode).orElse(null);
    }

    private String generateUssdRef(String reference, String genReference) {
        PushUssdRef pushUssdRef = new PushUssdRef().builder()
                .mapReference(genReference)
                .reference(reference)
                .build();
        PushUssdRef pushUssdRefResult = pushUssdRefService.addRefMap(pushUssdRef);

        if(pushUssdRefResult != null) {
            return pushUssdRefResult.getMapReference();
        } else {
            PushUssdRef pushUssdRef2 = new PushUssdRef().builder()
                    .mapReference(genReference)
                    .reference(reference)
                    .build();
            return pushUssdRefService.addRefMap(pushUssdRef2).getMapReference();
        }
    }

    private CompletableFuture<String> generateUssdRefAsync(String reference, String genReference) {
        return CompletableFuture.supplyAsync(() -> generateUssdRef(reference, genReference), ioExecutor);
    }

    public CompletableFuture<Long> getVendorDetailsAccountAsync(String vendorCode, String accountNumber) {
        return CompletableFuture.supplyAsync(() -> {
            AtomicLong accountId = new AtomicLong();
            this.vendorService.findVendorDetailsByCode(vendorCode).ifPresent(vendor -> {
                accountId.set(this.mainAccountService.findMainAccountByVendorDetailsAndAccountNumber(vendor, accountNumber).getId());
            });
            return accountId.get();
        }, ioExecutor);
    }

    public Long getVendorDetailsAccount(String vendorCode, String accountNumber) {
        AtomicLong accountId = new AtomicLong();
        this.vendorService.findVendorDetailsByCode(vendorCode).ifPresent(vendor -> {
            accountId.set(this.mainAccountService.findMainAccountByVendorDetailsAndAccountNumber(vendor, accountNumber).getId());
        });
        return accountId.get();
    }

    // Method to properly shutdown the executor when the application is terminating
    public void shutdown() {
        ioExecutor.shutdown();
    }
}