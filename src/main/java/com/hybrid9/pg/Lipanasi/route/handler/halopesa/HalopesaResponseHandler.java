package com.hybrid9.pg.Lipanasi.route.handler.halopesa;

import com.hybrid9.pg.Lipanasi.dto.halopesa.HalopesaResponse;
import org.apache.camel.Exchange;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.StringReader;

public class HalopesaResponseHandler {
    public static void process(Exchange exchange) throws Exception {
        String XML_RESPONSE = exchange.getMessage().getBody(String.class);
        Document document = parseXML(XML_RESPONSE);

        // using XPath to extract values
        extractUsingXPath(document,exchange);
    }

    // Parse XML string to Document
    private static Document parseXML(String xmlString) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // Important for SOAP XML with namespaces
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlString)));
    }


    private static void extractUsingXPath(Document document, Exchange exchange) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();

        // Extract values using XPath expressions
        String responseCode = (String) xpath.evaluate("//response_code/text()", document, XPathConstants.STRING);
        String message = (String) xpath.evaluate("//message/text()", document, XPathConstants.STRING);
        String referenceId = (String) xpath.evaluate("//referenceid/text()", document, XPathConstants.STRING);
        String responseTime = (String) xpath.evaluate("//response_time/text()", document, XPathConstants.STRING);
        String responseType = (String) xpath.evaluate("//response_type/text()", document, XPathConstants.STRING);
        String additionData = (String) xpath.evaluate("//addition_data/text()", document, XPathConstants.STRING);

        System.out.println("Response Code: " + responseCode);
        System.out.println("Message: " + message);
        System.out.println("Reference ID: " + referenceId);
        System.out.println("Response Time: " + responseTime);
        System.out.println("Response Type: " + responseType);
        System.out.println("Addition Data: '" + additionData + "'");

        HalopesaResponse halopesaResponse = HalopesaResponse.builder()
                .responseCode(responseCode)
                .message(message)
                .referenceId(referenceId)
                .responseTime(responseTime)
                .responseType(responseType)
                .additionData(additionData)
                .build();

        // Set response body
        exchange.getMessage().setBody(halopesaResponse);
    }
}
