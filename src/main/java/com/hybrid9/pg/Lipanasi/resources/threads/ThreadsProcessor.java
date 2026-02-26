package com.hybrid9.pg.Lipanasi.resources.threads;

import com.hybrid9.pg.Lipanasi.component.GeneralCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.AirtelMoneyCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.tz.AirtelMoneyTzCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.halopesa.HalopesaCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.mixxbyyas.CallbackRequest;
import com.hybrid9.pg.Lipanasi.dto.mpesa.USSDCallback;
import com.hybrid9.pg.Lipanasi.dto.mpesa.congo.MpesaCongoCallback;
import com.hybrid9.pg.Lipanasi.dto.orange.callback.OrangeCallbackRequest;
import com.hybrid9.pg.Lipanasi.enums.CollectionStatus;
import com.hybrid9.pg.Lipanasi.events.PaymentEventPublisher;
import com.hybrid9.pg.Lipanasi.events.PushUssdEvent;
import com.hybrid9.pg.Lipanasi.events.PushUssdSuccessEvent;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssd;
import com.hybrid9.pg.Lipanasi.entities.payments.pushussd.PushUssdCallback;
import com.hybrid9.pg.Lipanasi.resources.PushUssdResource;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdCallbackService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import com.nimbusds.jose.shaded.gson.JsonObject;
//import net.minidev.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
@Slf4j
@Component
public class ThreadsProcessor {



   /* // Dedicated executor for IO-bound tasks
    private final Executor callbackProcessorVirtualThread = Executors.newVirtualThreadPerTaskExecutor();

    // Dedicated executor for CPU-bound tasks
    private final Executor cpuExecutor = Executors.newVirtualThreadPerTaskExecutor();*/

    private final PushUssdResource pushUssdResource;
    private final PushUssdService pushUssdService;
    private final PushUssdRefService pushUssdRefService;
    private final PushUssdCallbackService ussdCallbackService;
    private final PaymentEventPublisher publisher;
    private final PaymentUtilities paymentUtilities;
    private final DepositService depositService;
    @Autowired
    @Qualifier("callbackProcessorVirtualThread")
    private ExecutorService callbackProcessorVirtualThread;
    @Autowired
    @Qualifier("cpuExecutor")
    private Executor cpuExecutor;


    public ThreadsProcessor(PushUssdResource pushUssdResource, PushUssdService pushUssdService,
                            PushUssdRefService pushUssdRefService, PushUssdCallbackService ussdCallbackService,
                            PaymentEventPublisher publisher,PaymentUtilities paymentUtilities,
                            DepositService depositService) {
        this.pushUssdResource = pushUssdResource;
        this.pushUssdService = pushUssdService;
        this.pushUssdRefService = pushUssdRefService;
        this.ussdCallbackService = ussdCallbackService;
        this.publisher = publisher;
        this.paymentUtilities = paymentUtilities;
        this.depositService = depositService;

    }

    private GeneralCallbackResponse getReference(GeneralCallbackResponse generalCallbackResponse) {
        log.debug("Reference number for Halopesa >>>>>> : {}", this.pushUssdRefService.getRefByMappingRef(generalCallbackResponse.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("referenceNumber").getAsString()));
        return GeneralCallbackResponse.builder()
                .reference(this.pushUssdRefService.getRefByMappingRef(generalCallbackResponse.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("referenceNumber").getAsString()).getReference())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .build();
    }
    private GeneralCallbackResponse getMpesaReference(GeneralCallbackResponse generalCallbackResponse) {
        log.debug("Reference number for Mpesa >>>>>> : {}", this.pushUssdRefService.getRefByMappingRef(generalCallbackResponse.getUssdCallback().getThirdPartyReference()).getReference());
        return GeneralCallbackResponse.builder()
                .reference(this.pushUssdRefService.getRefByMappingRef(generalCallbackResponse.getUssdCallback().getThirdPartyReference()).getReference())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .ussdCallback(generalCallbackResponse.getUssdCallback())
                .build();
    }

    private GeneralCallbackResponse getHalopesaReference(GeneralCallbackResponse generalCallbackResponse) {
        log.debug("Reference number for Halopesa >>>>>> : {}", this.pushUssdRefService.getRefByMappingRef(generalCallbackResponse.getHalopesaCallbackResponse().getReferenceId()).getReference());
        return GeneralCallbackResponse.builder()
                .reference(generalCallbackResponse.getHalopesaCallbackResponse().getReferenceId())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .pushUssd(this.pushUssdService.findByReference(generalCallbackResponse.getHalopesaCallbackResponse().getReferenceId()))
                .halopesaCallbackResponse(generalCallbackResponse.getHalopesaCallbackResponse())
                .build();
    }


    private GeneralCallbackResponse getTigopesaReference(GeneralCallbackResponse generalCallbackResponse) {
        return GeneralCallbackResponse.builder()
                .reference(this.pushUssdRefService.getRefByMappingRef(generalCallbackResponse.getCallbackRequest().getReferenceId()).getReference())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .build();
    }

    private GeneralCallbackResponse getAirtelMoneyReference(GeneralCallbackResponse generalCallbackResponse) {
        AtomicReference<String> referenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(generalCallbackResponse.getAirtelMoneyCallbackResponse().getTransaction().getId()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
        });
        return GeneralCallbackResponse.builder()
                .reference(this.pushUssdRefService.getRefByMappingRef(referenceNumber.get()).getReference())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .airtelMoneyCallbackResponse(generalCallbackResponse.getAirtelMoneyCallbackResponse())
                .build();
    }

    private GeneralCallbackResponse getAirtelMoneyTzReference(GeneralCallbackResponse generalCallbackResponse) {
        AtomicReference<String> referenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(generalCallbackResponse.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getId()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
        });
        return GeneralCallbackResponse.builder()
                .reference(this.pushUssdRefService.getRefByMappingRef(referenceNumber.get()).getReference())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .airtelMoneyCallbackResponse(generalCallbackResponse.getAirtelMoneyCallbackResponse())
                .airtelMoneyTzCallbackResponse(generalCallbackResponse.getAirtelMoneyTzCallbackResponse())
                .build();
    }

    private GeneralCallbackResponse getOrangeMoneyReference(GeneralCallbackResponse generalCallbackResponse) {
        AtomicReference<String> referenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(generalCallbackResponse.getOrangeCallback().getTransid()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
        });
        return GeneralCallbackResponse.builder()
                .reference(this.pushUssdRefService.getRefByMappingRef(referenceNumber.get()).getReference())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .airtelMoneyCallbackResponse(generalCallbackResponse.getAirtelMoneyCallbackResponse())
                .orangeCallback(generalCallbackResponse.getOrangeCallback())
                .build();
    }


    private GeneralCallbackResponse getMpesaCongoReference(GeneralCallbackResponse generalCallbackResponse) {
        return GeneralCallbackResponse.builder()
                .reference(this.pushUssdRefService.getRefByMappingRef(generalCallbackResponse.getMpesaCongoCallback().getThirdPartyReference()).getReference())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .ussdCallback(generalCallbackResponse.getUssdCallback())
                .mpesaCongoCallback(generalCallbackResponse.getMpesaCongoCallback())
                .build();
    }

    private GeneralCallbackResponse getRefByMsisdnAndNonce(GeneralCallbackResponse generalCallbackResponse) {
        return GeneralCallbackResponse.builder()
                .pushUssd(this.pushUssdService.findByReference(generalCallbackResponse.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("referenceNumber").getAsString()))
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .build();
    }

    private GeneralCallbackResponse getMpesaRefByMsisdnAndNonce(GeneralCallbackResponse generalCallbackResponse) {
        return GeneralCallbackResponse.builder()
                .pushUssd(this.pushUssdService.findByReference(generalCallbackResponse.getUssdCallback().getThirdPartyReference()))
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .ussdCallback(generalCallbackResponse.getUssdCallback())
                .build();
    }

    /**
     * Get Halopesa reference by MSISDN and nonce
     * @param generalCallbackResponse
     * @return
     */
    private GeneralCallbackResponse getHalopesaRefByMsisdnAndNonce(GeneralCallbackResponse generalCallbackResponse) {
        return GeneralCallbackResponse.builder()
                .pushUssd(generalCallbackResponse.getPushUssd())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .halopesaCallbackResponse(generalCallbackResponse.getHalopesaCallbackResponse())
                .build();
    }

    private GeneralCallbackResponse getMpesaCongoRefByMsisdnAndNonce(GeneralCallbackResponse generalCallbackResponse) {
        return GeneralCallbackResponse.builder()
                .pushUssd(this.pushUssdService.findByReference(generalCallbackResponse.getMpesaCongoCallback().getThirdPartyReference()))
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .ussdCallback(generalCallbackResponse.getUssdCallback())
                .mpesaCongoCallback(generalCallbackResponse.getMpesaCongoCallback())
                .build();
    }

    private GeneralCallbackResponse getTigopesRefByMsisdnAndNonce(GeneralCallbackResponse generalCallbackResponse) {
        return GeneralCallbackResponse.builder()
                .pushUssd(this.pushUssdService.findByReference(generalCallbackResponse.getCallbackRequest().getReferenceId()))
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .build();
    }

    private GeneralCallbackResponse getAirtelMoneyRefByMsisdnAndNonce(GeneralCallbackResponse generalCallbackResponse) {
        AtomicReference<String> referenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(generalCallbackResponse.getAirtelMoneyCallbackResponse().getTransaction().getId()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
        });
        return GeneralCallbackResponse.builder()
                .pushUssd(this.pushUssdService.findByReference(referenceNumber.get()))
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .airtelMoneyCallbackResponse(generalCallbackResponse.getAirtelMoneyCallbackResponse())
                .build();
    }

    private GeneralCallbackResponse getAirtelMoneyTzRefByMsisdnAndNonce(GeneralCallbackResponse generalCallbackResponse) {
        AtomicReference<String> referenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(generalCallbackResponse.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getId()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
        });
        return GeneralCallbackResponse.builder()
                .pushUssd(this.pushUssdService.findByReference(referenceNumber.get()))
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .airtelMoneyCallbackResponse(generalCallbackResponse.getAirtelMoneyCallbackResponse())
                .airtelMoneyTzCallbackResponse(generalCallbackResponse.getAirtelMoneyTzCallbackResponse())
                .build();
    }

    private GeneralCallbackResponse getOrangeMoneyRefByMsisdnAndNonce(GeneralCallbackResponse generalCallbackResponse) {
        AtomicReference<String> referenceNumber = new AtomicReference<>();
        this.depositService.findByTransactionId(generalCallbackResponse.getOrangeCallback().getTransid()).ifPresent(deposit -> {
            referenceNumber.set(deposit.getPaymentReference());
        });
        return GeneralCallbackResponse.builder()
                .pushUssd(this.pushUssdService.findByReference(referenceNumber.get()))
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .airtelMoneyCallbackResponse(generalCallbackResponse.getAirtelMoneyCallbackResponse())
                .orangeCallback(generalCallbackResponse.getOrangeCallback())
                .build();
    }

    private GeneralCallbackResponse getVendorDetails(GeneralCallbackResponse generalCallbackResponse) {
        return GeneralCallbackResponse.builder()
                .pushUssd(generalCallbackResponse.getPushUssd())
                .jsonObject(generalCallbackResponse.getJsonObject())
                .reference(generalCallbackResponse.getReference())
                .vendorDetails(this.pushUssdResource.getVendorDetails(generalCallbackResponse.getReference()))
                .callbackRequest(generalCallbackResponse.getCallbackRequest())
                .ussdCallback(generalCallbackResponse.getUssdCallback())
                .airtelMoneyCallbackResponse(generalCallbackResponse.getAirtelMoneyCallbackResponse())        //.build();
                .orangeCallback(generalCallbackResponse.getOrangeCallback())
                .mpesaCongoCallback(generalCallbackResponse.getMpesaCongoCallback())
                .airtelMoneyTzCallbackResponse(generalCallbackResponse.getAirtelMoneyTzCallbackResponse())
                .halopesaCallbackResponse(generalCallbackResponse.getHalopesaCallbackResponse())
                .build();
    }

    private PushUssd updatePushUssd(GeneralCallbackResponse generalCallbackResponse) {
        PushUssd pushUssd = generalCallbackResponse.getPushUssd();
        JsonObject jsonObject = generalCallbackResponse.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject();
        pushUssd.setStatus(jsonObject.get("resultCode").getAsString());        //
        pushUssd.setMessage(jsonObject.get("resultStatus").getAsString());
        pushUssd.setEvent(jsonObject.get("message").getAsString());
        pushUssd.setReceiptNumber(jsonObject.get("receiptNumber").getAsString());
        if (jsonObject.get("message").getAsString().equalsIgnoreCase("success")) {
            pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
        }
        else {
            pushUssd.setCollectionStatus(CollectionStatus.FAILED);
        }
        return this.pushUssdService.update(pushUssd);
    }

    private PushUssd updateMpesaPushUssd(GeneralCallbackResponse generalCallbackResponse) {
        PushUssd pushUssd = generalCallbackResponse.getPushUssd();
        pushUssd.setStatus(generalCallbackResponse.getUssdCallback().getResultCode().equalsIgnoreCase("0") ? "0": "-1");        //
        pushUssd.setMessage(generalCallbackResponse.getUssdCallback().getResultDesc());
        pushUssd.setEvent(generalCallbackResponse.getUssdCallback().getTransactionStatus());
        pushUssd.setReceiptNumber(generalCallbackResponse.getUssdCallback().getTransID());
        if (generalCallbackResponse.getUssdCallback().getTransactionStatus().equalsIgnoreCase("success")) {
            pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
        }
        else {
            pushUssd.setCollectionStatus(CollectionStatus.FAILED);
        }
        return this.pushUssdService.update(pushUssd);
    }

    private PushUssd updateMpesaCongoPushUssd(GeneralCallbackResponse generalCallbackResponse) {
        PushUssd pushUssd = generalCallbackResponse.getPushUssd();
        pushUssd.setStatus(generalCallbackResponse.getMpesaCongoCallback().getResultCode());        //
        pushUssd.setMessage(generalCallbackResponse.getMpesaCongoCallback().getResultDesc());
        pushUssd.setEvent(generalCallbackResponse.getMpesaCongoCallback().getResultCode().equalsIgnoreCase("0") ? "success" : "failed");
        pushUssd.setReceiptNumber(generalCallbackResponse.getMpesaCongoCallback().getOriginatorConversationId());
        if (generalCallbackResponse.getUssdCallback().getResultCode().equalsIgnoreCase("0")) {
            pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
        }
        else {
            pushUssd.setCollectionStatus(CollectionStatus.FAILED);
        }
        return this.pushUssdService.update(pushUssd);
    }


    private PushUssd updateTigopesPushUssd(GeneralCallbackResponse generalCallbackResponse) {
        PushUssd pushUssd = generalCallbackResponse.getPushUssd();
        pushUssd.setStatus(generalCallbackResponse.getCallbackRequest().isStatus() ? "0": "-1");        //
        pushUssd.setMessage(generalCallbackResponse.getCallbackRequest().isStatus() ? "success" : "failed");
        pushUssd.setEvent(generalCallbackResponse.getCallbackRequest().isStatus() ? "success" : "failed");
        pushUssd.setReceiptNumber(generalCallbackResponse.getCallbackRequest().getMfsTransactionId());
        if (generalCallbackResponse.getCallbackRequest().isStatus()) {
            pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
        }
        else {
            pushUssd.setCollectionStatus(CollectionStatus.FAILED);
        }
        return this.pushUssdService.update(pushUssd);
    }

    private PushUssd updateAirtelMoneyPushUssd(GeneralCallbackResponse generalCallbackResponse) {
        PushUssd pushUssd = generalCallbackResponse.getPushUssd();
        pushUssd.setStatus(generalCallbackResponse.getAirtelMoneyCallbackResponse().getTransaction().getStatus_code().equalsIgnoreCase("TS") ? "0": "-1");        //
        pushUssd.setMessage(generalCallbackResponse.getAirtelMoneyCallbackResponse().getTransaction().getMessage());
        pushUssd.setEvent(generalCallbackResponse.getAirtelMoneyCallbackResponse().getTransaction().getStatus_code().equalsIgnoreCase("TS") ? "success" : "failed");
        pushUssd.setReceiptNumber(generalCallbackResponse.getAirtelMoneyCallbackResponse().getTransaction().getAirtel_money_id());
        if (generalCallbackResponse.getAirtelMoneyCallbackResponse().getTransaction().getStatus_code().equalsIgnoreCase("TS")) {
            pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
        }
        else {
            pushUssd.setCollectionStatus(CollectionStatus.FAILED);
        }
        return this.pushUssdService.update(pushUssd);
    }

    private PushUssd updateAirtelMoneyTzPushUssd(GeneralCallbackResponse generalCallbackResponse) {
        PushUssd pushUssd = generalCallbackResponse.getPushUssd();
        pushUssd.setStatus(generalCallbackResponse.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getStatus_code().equalsIgnoreCase("TS") ? "0": "-1");        //
        pushUssd.setMessage(generalCallbackResponse.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getMessage());
        pushUssd.setEvent(generalCallbackResponse.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getStatus_code().equalsIgnoreCase("TS") ? "success" : "failed");
        pushUssd.setReceiptNumber(generalCallbackResponse.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getAirtel_money_id());
        if (generalCallbackResponse.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getStatus_code().equalsIgnoreCase("TS")) {
            pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
        }
        else {
            pushUssd.setCollectionStatus(CollectionStatus.FAILED);
        }
        return this.pushUssdService.update(pushUssd);
    }

    private PushUssd updateOrangeMoneyPushUssd(GeneralCallbackResponse generalCallbackResponse) {
        PushUssd pushUssd = generalCallbackResponse.getPushUssd();
        pushUssd.setStatus(generalCallbackResponse.getOrangeCallback().getResultCode());        //
        pushUssd.setMessage(generalCallbackResponse.getOrangeCallback().getResultDesc());
        pushUssd.setEvent(generalCallbackResponse.getOrangeCallback().getResultCode().equalsIgnoreCase("0") ? "success" : "failed");
        pushUssd.setReceiptNumber(generalCallbackResponse.getOrangeCallback().getSystemid());
        if (generalCallbackResponse.getOrangeCallback().getResultCode().equalsIgnoreCase("0")) {
            pushUssd.setCollectionStatus(CollectionStatus.COLLECTED);
        }
        else {
            pushUssd.setCollectionStatus(CollectionStatus.FAILED);
        }
        return this.pushUssdService.update(pushUssd);
    }

    private void publishEvent(PushUssd pushUssdResult) {
        /*initiate cash collection process*/


        if(pushUssdResult.getStatus() != null && pushUssdResult.getMessage().equalsIgnoreCase("failed")) {
            PushUssdEvent pushUssdEvent = new PushUssdEvent(this, pushUssdResult);
            publisher.publishPushUssdEvent(pushUssdEvent);
        }

        if(pushUssdResult.getReference() == null && pushUssdResult.getMessage() != null && pushUssdResult.getMessage().equalsIgnoreCase("success")) {
            PushUssdSuccessEvent pushUssdEvent = new PushUssdSuccessEvent(this, pushUssdResult);
            publisher.publishPushUssdSuccessEvent(pushUssdEvent);
        }
    }

    public JSONObject processCallbackData(JsonObject jsonObject, String nonce, JSONObject response) {
        log.info("Received Json Object for Halopesa: {}", jsonObject);
        GeneralCallbackResponse call = GeneralCallbackResponse.builder()
                .reference(null)
                .pushUssd(null)
                .vendorDetails(null)
                .jsonObject(jsonObject)
                .build();


        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
                    return this.getReference(call);
                }, callbackProcessorVirtualThread)
                .thenApplyAsync(this::recordCallback, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getRefByMsisdnAndNonce, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getVendorDetails, callbackProcessorVirtualThread)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        response.put("status", "failed");
                        response.put("message", "Operation failed");
                        throw new CompletionException(new RuntimeException("Result is null before updatePushUssd"));
                    }
                    return this.updatePushUssd(result);
                }, callbackProcessorVirtualThread);
                //.thenAcceptAsync(this::publishEvent, callbackProcessorVirtualThread);
        try {
            future.join();
        } catch (CompletionException e) {
            // Handle the error message here
            System.out.println(e.getCause().getMessage());
        }
        return response;

    }

    public JSONObject processMpesaCallbackData(USSDCallback ussdCallback, String nonce, JSONObject response) {
        GeneralCallbackResponse call = GeneralCallbackResponse.builder()
                .reference(null)
                .pushUssd(null)
                .vendorDetails(null)
                //.jsonObject(jsonObject)
                .ussdCallback(ussdCallback)
                .build();


        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
                    return this.getMpesaReference(call);
                }, callbackProcessorVirtualThread)
                .thenApplyAsync(this::recordMpesaCallback, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getMpesaRefByMsisdnAndNonce, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getVendorDetails, callbackProcessorVirtualThread)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        response.put("status", "failed");
                        response.put("message", "Operation failed");
                        throw new CompletionException(new RuntimeException("Result is null before updatePushUssd"));
                    }
                    return this.updateMpesaPushUssd(result);
                }, callbackProcessorVirtualThread);
                //.thenAcceptAsync(this::publishEvent, callbackProcessorVirtualThread);
        try {
            future.join();
        } catch (CompletionException e) {
            // Handle the error message here
            System.out.println(e.getCause().getMessage());
        }
        return response;

    }





    private GeneralCallbackResponse recordCallback(GeneralCallbackResponse response) {
        log.info("Start Recording Callback for Halopes >>>>>: {}", response.getJsonObject().toString());
        PushUssdCallback ussdCallback = PushUssdCallback.builder()
                .amount(response.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("amount").getAsFloat())
                .responseMessage(response.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("resultStatus").toString())
                .dateTime(response.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("date").toString())
                .receipt(response.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("receiptNumber").getAsString())
                .reference(response.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("referenceNumber").getAsString())
                .transactionNumber(response.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("transactionNumber").getAsString())
                .status(response.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("resultCode").getAsString())
                .message(response.getJsonObject().get("body").getAsJsonObject().get("result").getAsJsonObject().get("message").getAsString())
                .build();
        this.ussdCallbackService.newCallback(ussdCallback);
        return response;

    }

    /**
     * Record MPESA Callback
     * @param response
     * @return
     */
    private GeneralCallbackResponse recordMpesaCallback(GeneralCallbackResponse response) {
        PushUssdCallback ussdCallback = PushUssdCallback.builder()
                .amount(Float.parseFloat(response.getUssdCallback().getAmount()))
                .responseMessage(response.getUssdCallback().getResultDesc())
                .dateTime(response.getUssdCallback().getDate())
                .receipt(response.getUssdCallback().getConversationID())
                .reference(response.getUssdCallback().getThirdPartyReference())
                .transactionNumber(response.getUssdCallback().getTransID())
                .status(response.getUssdCallback().getResultCode())
                .message(response.getUssdCallback().getTransactionStatus())
                .build();
        this.ussdCallbackService.newCallback(ussdCallback);
        return response;

    }

    /**
     * Record Halopesa Callback
     *
     * @param response The Halopesa callback response object
     * @return The response object
     */
    private GeneralCallbackResponse recordHalopesaCallback(GeneralCallbackResponse response) {
        PushUssdCallback ussdCallback = PushUssdCallback.builder()
                .amount(Float.parseFloat(String.valueOf(response.getPushUssd().getAmount())))
                .responseMessage(response.getHalopesaCallbackResponse().getMessage())
                .dateTime(response.getHalopesaCallbackResponse().getResponseTime())
                .receipt(response.getHalopesaCallbackResponse().getAdditionData())
                .reference(response.getHalopesaCallbackResponse().getReferenceId())
                //.transactionNumber(response.getUssdCallback().getTransID())
                .status(response.getHalopesaCallbackResponse().getResponseCode())
                .message(response.getHalopesaCallbackResponse().getResponseCode().equals("0") ? "Ussd Push Initiated Successfully" : "Ussd Push Initiated Failed")
                .build();
        this.ussdCallbackService.newCallback(ussdCallback);
        return response;

    }


    private GeneralCallbackResponse recordMpesaCongoCallback(GeneralCallbackResponse response) {
        PushUssdCallback ussdCallback = PushUssdCallback.builder()
                .amount(Float.parseFloat(response.getMpesaCongoCallback().getAmount()))
                .responseMessage(response.getMpesaCongoCallback().getResultDesc())
                //.dateTime(response.getMpesaCongoCallback().getTransactionTime())
                .receipt(response.getMpesaCongoCallback().getConversationId())
                .reference(response.getMpesaCongoCallback().getThirdPartyReference())
                .transactionNumber(response.getMpesaCongoCallback().getInsightReference())
                .status(response.getMpesaCongoCallback().getResultCode())
                //.message(response.getMpesaCongoCallback().get)
                .build();
        this.ussdCallbackService.newCallback(ussdCallback);
        return response;

    }
    private GeneralCallbackResponse recordTigoPesaCallback(GeneralCallbackResponse response) {
        PushUssdCallback ussdCallback = PushUssdCallback.builder()
                .amount(Float.parseFloat(response.getCallbackRequest().getAmount()))
                .responseMessage(response.getCallbackRequest().getDescription())
                .receipt(response.getCallbackRequest().getMfsTransactionId())
                .reference(response.getCallbackRequest().getReferenceId())
                .transactionNumber(response.getCallbackRequest().getMfsTransactionId())
                .status(response.getCallbackRequest().isStatus() ? "0" : "-1")
                .message(response.getCallbackRequest().isStatus() ? "success" : "failed")
                .build();
        this.ussdCallbackService.newCallback(ussdCallback);
        return response;

    }

    private GeneralCallbackResponse recordAirtelMoneyCallback(GeneralCallbackResponse response) {
        this.depositService.findByTransactionId(response.getAirtelMoneyCallbackResponse().getTransaction().getId()).ifPresent(deposit -> {
            PushUssdCallback ussdCallback = PushUssdCallback.builder()
                    .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                    .responseMessage(response.getAirtelMoneyCallbackResponse().getTransaction().getMessage())
                    .receipt(response.getAirtelMoneyCallbackResponse().getTransaction().getAirtel_money_id())
                    .reference(deposit.getPaymentReference())
                    .transactionNumber(response.getAirtelMoneyCallbackResponse().getTransaction().getId())
                    .status(response.getAirtelMoneyCallbackResponse().getTransaction().getStatus_code().equalsIgnoreCase("TS") ? "0" : "-1")
                    .message(response.getAirtelMoneyCallbackResponse().getTransaction().getStatus_code().equalsIgnoreCase("TS") ? "success" : "failed")
                    .build();
            this.ussdCallbackService.newCallback(ussdCallback);
        });

        return response;

    }

    private GeneralCallbackResponse recordAirtelMoneyTzCallback(GeneralCallbackResponse response) {
        this.depositService.findByTransactionId(response.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getId()).ifPresent(deposit -> {
            PushUssdCallback ussdCallback = PushUssdCallback.builder()
                    .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                    .responseMessage(response.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getMessage())
                    .receipt(response.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getAirtel_money_id())
                    .reference(deposit.getPaymentReference())
                    .transactionNumber(response.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getId())
                    .status(response.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getStatus_code().equalsIgnoreCase("TS") ? "0" : "-1")
                    .message(response.getAirtelMoneyTzCallbackResponse().getPayload().getTransaction().getStatus_code().equalsIgnoreCase("TS") ? "success" : "failed")
                    .build();
            this.ussdCallbackService.newCallback(ussdCallback);
        });

        return response;

    }


    private GeneralCallbackResponse recordOrangeMoneyCallback(GeneralCallbackResponse response) {
        this.depositService.findByTransactionId(response.getOrangeCallback().getTransid()).ifPresent(deposit -> {
            PushUssdCallback ussdCallback = PushUssdCallback.builder()
                    .amount(Float.parseFloat(String.valueOf(deposit.getAmount())))
                    .responseMessage(response.getOrangeCallback().getResultDesc())
                    //.receipt(response.getAirtelMoneyCallbackResponse().getTransaction().getAirtel_money_id())
                    .reference(deposit.getPaymentReference())
                    .transactionNumber(response.getOrangeCallback().getTransid())
                    .status(response.getOrangeCallback().getResultCode())
                    .message(response.getOrangeCallback().getResultDesc())
                    .build();
            this.ussdCallbackService.newCallback(ussdCallback);
        });

        return response;

    }


    public JSONObject processTigopesaCallbackData(CallbackRequest callbackRequest, Object o, JSONObject response) {
        GeneralCallbackResponse call = GeneralCallbackResponse.builder()
                .reference(null)
                .pushUssd(null)
                .vendorDetails(null)
                .callbackRequest(callbackRequest)
                .build();


        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
                    return this.getTigopesaReference(call);
                }, callbackProcessorVirtualThread)
                .thenApplyAsync(this::recordTigoPesaCallback, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getTigopesRefByMsisdnAndNonce, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getVendorDetails, callbackProcessorVirtualThread)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        response.put("status", "failed");
                        response.put("message", "Operation failed");
                        throw new CompletionException(new RuntimeException("Result is null before updatePushUssd"));
                    }
                    return this.updateTigopesPushUssd(result);
                }, callbackProcessorVirtualThread);
        //.thenAcceptAsync(this::publishEvent, callbackProcessorVirtualThread);
        try {
            future.join();
        } catch (CompletionException e) {
            // Handle the error message here
            System.out.println(e.getCause().getMessage());
        }
        return response;
    }

    public JSONObject processAirtelMoneyCallbackData(AirtelMoneyCallbackResponse airtelMoneyCallbackResponse, Object o, JSONObject response) {
        GeneralCallbackResponse call = GeneralCallbackResponse.builder()
                .reference(null)
                .pushUssd(null)
                .vendorDetails(null)
                .callbackRequest(null)
                .airtelMoneyCallbackResponse(airtelMoneyCallbackResponse)
                .build();


        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
                    return this.getAirtelMoneyReference(call);
                }, callbackProcessorVirtualThread)
                .thenApplyAsync(this::recordAirtelMoneyCallback, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getAirtelMoneyRefByMsisdnAndNonce, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getVendorDetails, callbackProcessorVirtualThread)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        response.put("status", "failed");
                        response.put("message", "Operation failed");
                        throw new CompletionException(new RuntimeException("Result is null before updatePushUssd"));
                    }
                    return this.updateAirtelMoneyPushUssd(result);
                }, callbackProcessorVirtualThread);
        try {
            future.join();
        } catch (CompletionException e) {
            // Handle the error message here
            System.out.println(e.getCause().getMessage());
        }
        return response;
    }

    public JSONObject processOrangeMoneyCallbackData(OrangeCallbackRequest.DoCallback ussdCallback, Object o, JSONObject response) {
        GeneralCallbackResponse call = GeneralCallbackResponse.builder()
                .reference(null)
                .pushUssd(null)
                .vendorDetails(null)
                .callbackRequest(null)
                .airtelMoneyCallbackResponse(null)
                .orangeCallback(ussdCallback)
                .build();

        CompletableFuture future = CompletableFuture.supplyAsync(() -> this.getOrangeMoneyReference(call), callbackProcessorVirtualThread)
                .thenApplyAsync(this::recordOrangeMoneyCallback, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getOrangeMoneyRefByMsisdnAndNonce, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getVendorDetails, callbackProcessorVirtualThread)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        response.put("status", "failed");
                        response.put("message", "Operation failed");
                        throw new CompletionException(new RuntimeException("Result is null before updatePushUssd"));
                    }
                    return this.updateOrangeMoneyPushUssd(result);
                }, callbackProcessorVirtualThread);
        try {
            future.join();
        } catch (CompletionException e) {
            // Handle the error message here
            System.out.println(e.getCause().getMessage());
        }
        return response;
    }

    public JSONObject processMpesaCongoCallbackData(MpesaCongoCallback ussdCallback, Object o, JSONObject response) {
        GeneralCallbackResponse call = GeneralCallbackResponse.builder()
                .reference(null)
                .pushUssd(null)
                .vendorDetails(null)
                .callbackRequest(null)
                .airtelMoneyCallbackResponse(null)
                .orangeCallback(null)
                .mpesaCongoCallback(ussdCallback)
                .build();

        CompletableFuture future = CompletableFuture.supplyAsync(() -> this.getMpesaCongoReference(call), callbackProcessorVirtualThread)
                .thenApplyAsync(this::recordMpesaCongoCallback, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getMpesaCongoRefByMsisdnAndNonce, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getVendorDetails, callbackProcessorVirtualThread)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        response.put("status", "failed");
                        response.put("message", "Operation failed");
                        throw new CompletionException(new RuntimeException("Result is null before updatePushUssd"));
                    }
                    return this.updateMpesaCongoPushUssd(result);
                }, callbackProcessorVirtualThread);
        try {
            future.join();
        } catch (CompletionException e) {
            // Handle the error message here
            System.out.println(e.getCause().getMessage());
        }
        return response;
    }

    public JSONObject processAirtelMoneyTzCallbackData(AirtelMoneyTzCallbackResponse ussdCallback, Object o, JSONObject response) {
        GeneralCallbackResponse call = GeneralCallbackResponse.builder()
                .reference(null)
                .pushUssd(null)
                .vendorDetails(null)
                .callbackRequest(null)
                .airtelMoneyCallbackResponse(null)
                .airtelMoneyTzCallbackResponse(ussdCallback)
                .build();


        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
                    return this.getAirtelMoneyTzReference(call);
                }, callbackProcessorVirtualThread)
                .thenApplyAsync(this::recordAirtelMoneyTzCallback, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getAirtelMoneyTzRefByMsisdnAndNonce, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getVendorDetails, callbackProcessorVirtualThread)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        response.put("status", "failed");
                        response.put("message", "Operation failed");
                        throw new CompletionException(new RuntimeException("Result is null before updatePushUssd"));
                    }
                    return this.updateAirtelMoneyTzPushUssd(result);
                }, callbackProcessorVirtualThread);
        try {
            future.join();
        } catch (CompletionException e) {
            // Handle the error message here
            System.out.println(e.getCause().getMessage());
        }
        return response;
    }

    /**
     * Start processing Halopesa callback
     * @param callbackResponse
     * @param o
     * @param response
     * @return
     */
    public JSONObject processHalopesaCallbackData(HalopesaCallbackResponse callbackResponse, Object o, JSONObject response) {
        GeneralCallbackResponse call = GeneralCallbackResponse.builder()
                .reference(null)
                .pushUssd(null)
                .vendorDetails(null)
                //.jsonObject(jsonObject)
                .halopesaCallbackResponse(callbackResponse)
                .build();


        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
                    return this.getHalopesaReference(call);
                }, callbackProcessorVirtualThread)
                .thenApplyAsync(this::recordHalopesaCallback, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getHalopesaRefByMsisdnAndNonce, callbackProcessorVirtualThread)
                .thenApplyAsync(this::getVendorDetails, callbackProcessorVirtualThread)
                .thenApplyAsync(result -> {
                    if (result == null) {
                        response.put("status", "failed");
                        response.put("message", "Operation failed");
                        throw new CompletionException(new RuntimeException("Result is null before updatePushUssd"));
                    }
                    return this.updateMpesaPushUssd(result);
                }, callbackProcessorVirtualThread);
        //.thenAcceptAsync(this::publishEvent, callbackProcessorVirtualThread);
        try {
            future.join();
        } catch (CompletionException e) {
            // Handle the error message here
            System.out.println(e.getCause().getMessage());
        }
        return response;
    }
}
