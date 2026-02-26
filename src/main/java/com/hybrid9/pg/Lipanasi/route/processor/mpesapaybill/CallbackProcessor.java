package com.hybrid9.pg.Lipanasi.route.processor.mpesapaybill;

import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.MpesaBroker;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.Result;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.ServiceProvider;
import com.hybrid9.pg.Lipanasi.dto.mpesapaybill.v2.Transaction;
import org.apache.camel.Exchange;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
@Component("mpesa-callback-processor")
public class CallbackProcessor {

    @Value("${payment-gateway.mpesa.paybill.sp-id}")
    private String spId;

    @Value("${payment-gateway.mpesa.paybill.sp-password}")
    private String spPassword;

    public  void process(Exchange exchange) throws Exception {
        String originalTransaction = exchange.getIn().getBody(String.class);
        JSONObject mpesaCallbackParams = new JSONObject(originalTransaction);
        String transactionStatus = mpesaCallbackParams.getString("TransactionStatus");
        boolean isSuccess = transactionStatus.equalsIgnoreCase("success");

        MpesaBroker callbackRequest = createCallbackRequest(mpesaCallbackParams,isSuccess);
        exchange.getMessage().setBody(callbackRequest);
    }

    private MpesaBroker createCallbackRequest(JSONObject mpesaCallbackParams,boolean isSuccess) {
        MpesaBroker callbackRequest = new MpesaBroker();
        Result result = new Result();

        // Set ServiceProvider details
        ServiceProvider serviceProvider = new ServiceProvider();
        String timestamp = getCurrentTimestamp();
        serviceProvider.setSpId(spId);
        serviceProvider.setSpPassword(encryptPassword(spId, spPassword, timestamp));
        serviceProvider.setTimestamp(timestamp);

        // Set Transaction details
        Transaction transaction = new Transaction();
        transaction.setResultType(isSuccess ? "Completed" : "Failed");
        transaction.setResultCode(isSuccess ? "0" : "999");
        transaction.setResultDesc(mpesaCallbackParams.getString("ErrorMessage"));
        transaction.setServiceReceipt(mpesaCallbackParams.getString("mpesaReceipt"));
        transaction.setServiceDate(getCurrentDateTime());
        transaction.setOriginatorConversationID(mpesaCallbackParams.getString("originatorConversationId"));
        transaction.setConversationID(mpesaCallbackParams.getString("conversationId"));
        transaction.setTransactionID(mpesaCallbackParams.getString("transactionId"));
        transaction.setInitiator(mpesaCallbackParams.getString("initiator"));
        transaction.setInitiatorPassword(encryptPassword(spId, spPassword, timestamp));

        result.setServiceProvider(serviceProvider);
        result.setTransaction(transaction);
        callbackRequest.setResult(result);

        return callbackRequest;
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String encryptPassword(String spId, String password, String timestamp) {
        try {
            String input = spId + password + timestamp;
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error generating SP password", e);
        }
    }
}