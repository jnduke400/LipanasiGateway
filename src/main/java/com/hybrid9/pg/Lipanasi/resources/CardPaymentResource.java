package com.hybrid9.pg.Lipanasi.resources;

import com.google.gson.Gson;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.BillingInformation;
import com.hybrid9.pg.Lipanasi.entities.payments.bank.CardPayment;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssdRef;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.enums.PaymentChannel;
import com.hybrid9.pg.Lipanasi.enums.RequestStatus;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.bank.BillingInfoService;
import com.hybrid9.pg.Lipanasi.services.bank.CardPaymentService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import com.nimbusds.jose.shaded.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@AllArgsConstructor
public class CardPaymentResource {
    private final PushUssdRefService pushUssdRefService;
    private final VendorService vendorService;
    private final MainAccountService mainAccountService;
    private final PaymentUtilities paymentUtilities;
    private final CardPaymentService cardPaymentService;
    private final BillingInfoService billingInfoService;
    private final MnoServiceImpl mnoService;
    private final Gson gson;

    // Creating a dedicated executor for I/O-bound operations
    private final ExecutorService ioExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public CompletableFuture<CardPayment> createCardPaymentReq(JsonObject cardPaymentJsonObject, PushUssdService pushUssdService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String vendorCode = cardPaymentJsonObject.get("code").getAsString();

                if (cardPaymentJsonObject.get("operator") == null) {
                    String cardReference = this.paymentUtilities.generateRefNumber();
                    cardPaymentJsonObject.addProperty("cardReference", cardReference);
                } else {
                    String finalReference = (cardPaymentJsonObject.get("operator").getAsString().toLowerCase().contains("yas") ||
                            cardPaymentJsonObject.get("operator").getAsString().toLowerCase().contains("tigo")) ?
                            this.paymentUtilities.generateTigopesaRefNumber() :
                            this.paymentUtilities.generateRefNumber();
                    cardPaymentJsonObject.addProperty("cardReference", finalReference);
                }


                double amount = cardPaymentJsonObject.get("amount").getAsDouble();
                String msisdn = cardPaymentJsonObject.get("msisdn").getAsString();

                // Generating reference is I/O-bound, using a separate CompletableFuture
                CompletableFuture<String> referenceFuture = CompletableFuture.supplyAsync(() ->
                                this.generateCardPaymentRef(cardPaymentJsonObject.get("reference").getAsString(), cardPaymentJsonObject.get("cardReference").getAsString()),
                        ioExecutor);

                // Finding vendorx is I/O-bound, using a separate CompletableFuture
                CompletableFuture<Optional<VendorDetails>> vendorFuture = CompletableFuture.supplyAsync(() ->
                                this.vendorService.findVendorDetailsByCode(vendorCode),
                        ioExecutor);

                // Determining the operator is I/O-bound
                String formattedPhone = cardPaymentJsonObject.get("operator") == null? "Not-Mobile-Operator" : cardPaymentJsonObject.get("operator").getAsString().toLowerCase().contains("tanzania") ?
                        paymentUtilities.formatPhoneNumber("255", msisdn) :
                        paymentUtilities.formatPhoneNumber("243", msisdn);

                CompletableFuture<String> operatorFuture = CompletableFuture.supplyAsync(() ->
                                cardPaymentJsonObject.get("operator") == null? "Not-Mobile-Operator" : cardPaymentJsonObject.get("operator").getAsString().toLowerCase().contains("tanzania") ?
                                        this.mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", msisdn)) :
                                        this.mnoService.searchMno(paymentUtilities.formatPhoneNumber("243", msisdn)),
                        ioExecutor);

                // Wait for all async operations to complete
                String reference = referenceFuture.join();
                VendorDetails vendorDetails = vendorFuture.join().orElse(null);
                String operator = operatorFuture.join();

                // Transaction ID generation
                String transactionId;
                if (cardPaymentJsonObject.get("operator") != null && cardPaymentJsonObject.get("operator").getAsString().toLowerCase().contains("airtel")) {
                    transactionId = paymentUtilities.generateAirtelRefNumber();
                } else if (cardPaymentJsonObject.get("operator") != null && cardPaymentJsonObject.get("operator").getAsString().toLowerCase().contains("orange")) {
                    transactionId = paymentUtilities.generateOrangeCongoTransactionId();
                } else {
                    transactionId = UUID.randomUUID().toString();
                }
                JsonObject billingInfoObject = cardPaymentJsonObject.get("billingInfo").getAsJsonObject();
                BillingInformation billingInformation = BillingInformation.builder()
                        .firstName(billingInfoObject.get("firstName").getAsString())
                        .lastName(billingInfoObject.get("lastName").getAsString())
                        .address1(billingInfoObject.get("address1").getAsString().equalsIgnoreCase("null") ? "123 Main Street" : billingInfoObject.get("address1").getAsString())
                        .city(billingInfoObject.get("city").getAsString().equalsIgnoreCase("null") ? "Dar Es Salaam" : billingInfoObject.get("city").getAsString())
                        .state(billingInfoObject.get("state").getAsString().equalsIgnoreCase("null") ? "Kinondoni" : billingInfoObject.get("state").getAsString())
                        .postalCode(billingInfoObject.get("postalCode").getAsString().equalsIgnoreCase("null") ? "14100" : billingInfoObject.get("postalCode").getAsString())
                        .country(billingInfoObject.get("country").getAsString().equalsIgnoreCase("null") ? "TZ" : billingInfoObject.get("country").getAsString())
                        .email(billingInfoObject.get("email").getAsString().equalsIgnoreCase("null") ? "john.doe@example.com" : billingInfoObject.get("email").getAsString())
                        .phone(billingInfoObject.get("phone").getAsString().equalsIgnoreCase("null") ? "255688044555" : billingInfoObject.get("phone").getAsString())
                        .build();


                // Build the Card Payment object
                CardPayment cardPayment = CardPayment.builder()
                        /*.msisdn(formattedPhone)*/
                        .amount(amount)
                        .channel(PaymentChannel.BANK_PAYMENT_GATEWAY)
                        .paymentReference(reference)
                        .originalReference(cardPaymentJsonObject.get("reference").getAsString())
                        .vendorDetails(vendorDetails)
                        .bankName("CRDB")
                        .bankId("1234")
                        .status("0")
                        .collectionStatus(CollectionStatus.NEW)
                        .transactionId(transactionId)
                        .currency(cardPaymentJsonObject.get("currency").getAsString())
                        .sessionId(cardPaymentJsonObject.get("sessionId").getAsString())
                        .cardToken(cardPaymentJsonObject.get("transientToken").getAsString())
                        .billingInformation( this.billingInfoService.createBillingInfo(billingInformation))
                        .build();

                // Record the card payment asynchronously
                return CompletableFuture.supplyAsync(() -> {
                    return this.cardPaymentService.recordCardPayment(cardPayment);
                }, ioExecutor).join();

            } catch (Exception e) {
                // Handle final failure after retries
                e.printStackTrace();
                log.error("Request failed permanently", e);
                return new CardPayment();
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

    private String generateCardPaymentRef(String reference, String genReference) {
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

    private CompletableFuture<String> generateCardPaymentRefAsync(String reference, String genReference) {
        return CompletableFuture.supplyAsync(() -> generateCardPaymentRef(reference, genReference), ioExecutor);
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