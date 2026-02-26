package com.hybrid9.pg.Lipanasi.services.paybill;

import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.MpesaBroker;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.Response;
import com.hybrid9.pg.Lipanasi.dto.orderpayment.PaymentRequest;
import com.hybrid9.pg.Lipanasi.entities.order.Order;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.events.MpesaCallbackEvent;
import com.hybrid9.pg.Lipanasi.events.PaymentEventPublisher;
import com.hybrid9.pg.Lipanasi.entities.payments.paybill.PayBillPayment;
import com.hybrid9.pg.Lipanasi.resources.PaybillResource;
import com.hybrid9.pg.Lipanasi.serviceImpl.operators.MnoServiceImpl;
import com.hybrid9.pg.Lipanasi.services.order.OrderService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.payments.PayBillPaymentService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
@Slf4j
@Service
public class MpesaService {

    @Autowired
    private ProducerTemplate payBillTemplate;

    private final PayBillPaymentService paymentService;
    private final PaymentUtilities paymentUtilities;
    private final MnoServiceImpl mnoService;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final PaymentEventPublisher publisher;
    private final OrderService orderService;
    private final PaybillResource paybillResource;

    public MpesaService(PayBillPaymentService paymentService, PaymentUtilities paymentUtilities, MnoServiceImpl mnoService, MainAccountService mainAccountService, VendorService vendorService, PaymentEventPublisher publisher,OrderService orderService, PaybillResource paybillResource) {
        this.paymentUtilities = paymentUtilities;
        this.mnoService = mnoService;
        this.mainAccountService = mainAccountService;
        this.vendorService = vendorService;
        this.paymentService = paymentService;
        this.publisher = publisher;
        this.orderService = orderService;
        this.paybillResource = paybillResource;
    }

    private final Set<String> processedReceipts = new HashSet<>();

    /**
     * This method is used to process the MpesaBroker event and return the response.
     *
     * @param request
     * @return
     */
    public MpesaBroker processC2BTransaction(MpesaBroker request) {
        // Create immediate response
        MpesaBroker response = new MpesaBroker();
        Response resp = new Response();
        resp.setConversationID(request.getRequest().getTransaction().getConversationID());
        resp.setOriginatorConversationID(request.getRequest().getTransaction().getOriginatorConversationID());
        resp.setTransactionID(request.getRequest().getTransaction().getTransactionID());
        resp.setResponseCode("0");
        resp.setResponseDesc("Received");
        resp.setServiceStatus("Success");
        response.setResponse(resp);

        // Process transaction asynchronously
        MpesaCallbackEvent mpesaCallbackEvent = new MpesaCallbackEvent(this, request);
        publisher.publishMpesaCallbackEvent(mpesaCallbackEvent);

        return response;
    }

    /**
     * This method is used to publish the MpesaBroker event to the event bus, so that the
     * MpesaCallbackEventListener can process the event.
     *
     * @param event
     */
    @Transactional
    @EventListener
    @Async
    public void publishMpesaCallbackEvent(MpesaCallbackEvent event) {
        log.info(">>>>> Mpesa Pay Bill Payment V2 Event received >>>>>>>>>>>>>"+event.getRequest().toString());
        //create a 1 second delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Validate request
        validateRequest(event.getRequest());

        /*// create Institution
        VendorDetails vendorDetails = vendorService.findVendorDetailsByCode("SC001")
                .orElseThrow(() -> new RuntimeException("Vendor not found"));*/

        // Get order
        Order order = orderService.findByReceipt(event.getRequest().getRequest().getTransaction().getAccountReference())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Compose payment request
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderNumber(order.getOrderNumber())
                .paymentChannel("PAY_BILL")
                .paymentMethod("mobile")
                .msisdn(event.getRequest().getRequest().getTransaction().getInitiator())
                .build();

        // start validating session
        this.paybillResource.manageSession(paymentRequest, order);

        // Check for duplicate transaction
        Optional<PayBillPayment> existingPayment = this.paymentService.findPayBillByReceiptNumber(event.getRequest().getRequest().getTransaction().getMpesaReceipt());
        if (existingPayment.isPresent()) {
            // Send callback with existing transaction status
            sendCallback(event.getRequest(), true);
            return; // Exit the method here
        }

        // Process new transaction
        recordTransaction(event.getRequest(), order.getCustomer().getVendorDetails());
        // Add your business logic here

        // Send callback
        sendCallback(event.getRequest(), false);
    }

    /**
     * This method is used to record the transaction details and create the payment.
     *
     * @param request
     * @param vendorDetails
     */
    public void recordTransaction(MpesaBroker request, VendorDetails vendorDetails) {
        // Create immediate response
        PayBillPayment payment = PayBillPayment.builder()
                .vendorDetails(vendorDetails)
                .validationId(request.getRequest().getTransaction().getOriginatorConversationID())
                .payBillId(request.getRequest().getTransaction().getTransactionID())
                .paymentReference(paymentUtilities.generateRefNumber())
                .originalReference(request.getRequest().getTransaction().getAccountReference())
                .amount(Float.parseFloat(request.getRequest().getTransaction().getAmount().toString()))
                .currency("TZS") // Assuming Tanzanian Shillings
                .msisdn(paymentUtilities.formatPhoneNumber("255", request.getRequest().getTransaction().getInitiator()))
                .accountId(String.valueOf(mainAccountService.findMainAccountByAccountNumber("100019802002").getId()))
                .operator(mnoService.searchMno(paymentUtilities.formatPhoneNumber("255", request.getRequest().getTransaction().getInitiator())))
                .receiptNumber(request.getRequest().getTransaction().getMpesaReceipt())
                .collectionStatus(CollectionStatus.PAYMENT_INIT)
                .transactionDate(request.getRequest().getTransaction().getTransactionDate())
                .build();

        this.paymentService.createPayBill(payment);
    }

    public MpesaBroker processCallback(MpesaBroker result) {
        MpesaBroker response = new MpesaBroker();
        Response resp = new Response();
        resp.setConversationID(result.getResult().getTransaction().getConversationID());
        resp.setOriginatorConversationID(result.getResult().getTransaction().getOriginatorConversationID());
        resp.setResponseCode("0");
        resp.setResponseDesc("Attempted successfully");
        resp.setServiceStatus("0".equals(result.getResult().getTransaction().getResultCode()) ? "Confirming" : "Cancelling");
        resp.setTransactionID(result.getResult().getTransaction().getTransactionID());
        response.setResponse(resp);

        return response;
    }

    private void sendCallback(MpesaBroker request, boolean isDuplicate) {
        JSONObject json = new JSONObject();
        json.put("isDuplicate", isDuplicate ? "true" : "false");
        String ErrorMessage = isDuplicate? "failure, Transaction Already Processed [Duplicate]" : "Successful";
        String transactionStatus = isDuplicate? "failed" : "success";
        json.put("TransactionStatus", transactionStatus);
        json.put("ErrorMessage", ErrorMessage);
        json.put("transactionId", request.getRequest().getTransaction().getTransactionID());
        json.put("amount", request.getRequest().getTransaction().getAmount());
        json.put("initiator", request.getRequest().getTransaction().getInitiator());
        json.put("accountReference", request.getRequest().getTransaction().getAccountReference());
        json.put("conversationId", request.getRequest().getTransaction().getConversationID());
        json.put("originatorConversationId", request.getRequest().getTransaction().getOriginatorConversationID());
        json.put("mpesaReceipt", request.getRequest().getTransaction().getMpesaReceipt());
        payBillTemplate.sendBody("direct:mpesa-paybill-callback", json.toString());
    }

    private void validateRequest(MpesaBroker request) {
        if (request == null || request.getRequest() == null
                || request.getRequest().getServiceProvider() == null
                || request.getRequest().getTransaction() == null) {
            throw new InvalidPaymentRequestException("Invalid request: Payload cannot be null");
        }

        String spId = request.getRequest().getServiceProvider().getSpId();
        if (!"300300".equals(spId)) {
            throw new InvalidPaymentRequestException("Invalid spId: Expected 300300 but got " + spId);
        }

        if (Double.parseDouble(request.getRequest().getTransaction().getAmount().toString()) < 100) {
            throw new InvalidPaymentRequestException("Invalid Transaction Amount: Amount should be greater than or equal to 100");
        }
    }

    // InvalidRequestException.java
    public class InvalidPaymentRequestException extends RuntimeException {
        public InvalidPaymentRequestException(String message) {
            super(message);
        }
    }

    public String generateSpPassword(String spId, String password, String timestamp) {
        try {
            String input = spId + password + timestamp;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating SP password", e);
        }
    }
}
