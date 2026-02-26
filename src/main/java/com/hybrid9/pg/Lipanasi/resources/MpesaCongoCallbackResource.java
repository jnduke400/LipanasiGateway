package com.hybrid9.pg.Lipanasi.resources;

import com.hybrid9.pg.Lipanasi.dto.mpesa.congo.MpesaCongoCallback;
import com.hybrid9.pg.Lipanasi.events.PaymentEventPublisher;
import com.hybrid9.pg.Lipanasi.resources.threads.ThreadsProcessor;
import com.hybrid9.pg.Lipanasi.services.payments.DepositService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdCallbackService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdRefService;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import com.nimbusds.jose.shaded.gson.Gson;
import lombok.extern.slf4j.Slf4j;
//import net.minidev.json.JSONObject;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class MpesaCongoCallbackResource {

    public JSONObject processCallback(String soapPayload, PushUssdRefService ussdRefService, PushUssdResource pushUssdResource, PushUssdService pushUssdService, PushUssdRefService pushUssdRefService, PushUssdCallbackService ussdCallbackService, PaymentEventPublisher publisher, PaymentUtilities paymentUtilities, DepositService depositService) throws Exception {
        // Parse SOAP payload using DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(soapPayload)));

        // Extract data items and create entity
        MpesaCongoCallback entity = extractDataItems(doc);

        //Start logging
        /*String originalReference = ussdRefService.getRefByMappingRef(entity.getThirdPartyReference()).getReference();*/
        AtomicReference<String> originalReference = new AtomicReference<>();
        Optional.ofNullable(ussdRefService.getRefByMappingRef(entity.getThirdPartyReference())).ifPresent(pushUssdRef -> originalReference.set(pushUssdRef.getReference()));

        log.info("Transaction Id: {}, Reference: {}, OriginalReference: {}, Received Callback for Mpesa congo: {}", entity.getTransactionId(), entity.getThirdPartyReference(), originalReference.get(), soapPayload);
        //End logging

        // Save to database
        //CallbackEntity savedEntity = callbackRepository.save(entity);

        return mpesaCongoUssdCallback(entity, pushUssdResource, pushUssdService, pushUssdRefService, ussdCallbackService, publisher, paymentUtilities, depositService);
    }

    private MpesaCongoCallback extractDataItems(Document doc) {
        MpesaCongoCallback entity = new MpesaCongoCallback();
        NodeList dataItems = doc.getElementsByTagName("dataItem");

        for (int i = 0; i < dataItems.getLength(); i++) {
            Element dataItem = (Element) dataItems.item(i);
            String name = getElementTextContent(dataItem, "name");
            String value = getElementTextContent(dataItem, "value");

            switch (name) {
                case "ResultType":
                    entity.setResultType(value);
                    break;
                case "ResultCode":
                    entity.setResultCode(value);
                    break;
                case "ResultDesc":
                    entity.setResultDesc(value);
                    break;
                case "OriginatorConversationID":
                    entity.setOriginatorConversationId(value);
                    break;
                case "ConversationID":
                    entity.setConversationId(value);
                    break;
                case "ThirdPartyReference":
                    entity.setThirdPartyReference(value);
                    break;
                case "Amount":
                    entity.setAmount(value);
                    break;
                case "TransactionTime":
                    entity.setTransactionTime(value);
                    break;
                case "InsightReference":
                    entity.setInsightReference(value);
                    break;
                case "TransactionID":
                    entity.setTransactionId(value);
                    break;
            }
        }
        return entity;
    }

    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private JSONObject mpesaCongoUssdCallback(MpesaCongoCallback ussdCallback, PushUssdResource pushUssdResource, PushUssdService pushUssdService, PushUssdRefService pushUssdRefService, PushUssdCallbackService ussdCallbackService, PaymentEventPublisher publisher, PaymentUtilities paymentUtilities, DepositService depositService) {
        JSONObject response = new JSONObject();
        try {
            Gson g = new Gson();

            /*JsonObject jsonObject = new Gson().toJsonTree(ussdCallback).getAsJsonObject();*/
            //start processing
            ThreadsProcessor threadsProcessor = new ThreadsProcessor(pushUssdResource, pushUssdService, pushUssdRefService, ussdCallbackService, publisher, paymentUtilities, depositService);
            response = threadsProcessor.processMpesaCongoCallbackData(ussdCallback, null, response);
            if (!response.has("status")) {
                response.put("status", "success");
                response.put("message", "Push Ussd updated successfully");
            }


        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "failed");
            response.put("message", "Something went wrong, Operation failed");
        }
        return response;

    }



}
