package com.hybrid9.pg.Lipanasi.route.handler;

import com.hybrid9.pg.Lipanasi.dto.mpesa.TransactionResponse;
import org.apache.camel.Exchange;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public class TransactionResponseHandler {

    public  void process(Exchange exchange) throws Exception {
        String response = exchange.getMessage().getBody(String.class);

        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        // Extract response details
        TransactionResponse transResponse = new TransactionResponse();

        // Parse eventInfo section
        transResponse.setCode(getXPathValue(xpath, response, "//eventInfo/code"));
        transResponse.setDescription(getXPathValue(xpath, response, "//eventInfo/description"));
        transResponse.setDetail(getXPathValue(xpath, response, "//eventInfo/detail"));
        transResponse.setTransactionID(getXPathValue(xpath, response, "//eventInfo/transactionID"));

        // Parse response section
        transResponse.setThirdPartyReference(getXPathValue(xpath, response, "//response/dataItem[name='ThirdPartyReference']/value"));
        transResponse.setInsightReference(getXPathValue(xpath, response, "//response/dataItem[name='InsightReference']/value"));
        transResponse.setResponseCode(getXPathValue(xpath, response, "//response/dataItem[name='ResponseCode']/value"));

        // Validate response
        boolean isSuccess = "0".equals(transResponse.getResponseCode()) && "3".equals(transResponse.getCode());
        transResponse.setSuccess(isSuccess);

        // Store response details in headers
        exchange.getMessage().setHeader("TransactionStatus", isSuccess ? "SUCCESS" : "FAILED");
        exchange.getMessage().setHeader("InsightReference", transResponse.getInsightReference());
        exchange.getMessage().setHeader("TransactionID", transResponse.getTransactionID());
        exchange.getMessage().setHeader("ThirdPartyReference", transResponse.getThirdPartyReference());

        // Set response body
        exchange.getMessage().setBody(transResponse);

        // Log appropriate message
        String logMessage = isSuccess ?
                String.format("Transaction initiated successfully. InsightReference: %s", transResponse.getInsightReference()) :
                String.format("Transaction initiation failed. ResponseCode: %s", transResponse.getResponseCode());
        exchange.getMessage().setHeader("CamelLogMessage", logMessage);
    }

    private String getXPathValue(XPath xpath, String xml, String expression) throws Exception {
        return xpath.evaluate(expression, new InputSource(new StringReader(xml)));
    }
}
