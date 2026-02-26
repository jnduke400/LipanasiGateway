package com.hybrid9.pg.Lipanasi.resources.halopesa;

import com.hybrid9.pg.Lipanasi.dto.halopesa.HalopesaCallbackResponse;
import com.hybrid9.pg.Lipanasi.dto.halopesa.HalopesaResponse;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
@Component
public class HalopesaCallback {

    public HalopesaCallbackResponse parseCallback(String xmlResponse) throws Exception {
        Document document = parseXML(xmlResponse);

        // using XPath to extract values
        return extractUsingXPath(document);
    }

    // Parse XML string to Document
    private static Document parseXML(String xmlString) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // Important for SOAP XML with namespaces
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlString)));
    }


    private static HalopesaCallbackResponse extractUsingXPath(Document document) throws XPathExpressionException {
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

        return HalopesaCallbackResponse.builder()
                .responseCode(responseCode)
                .message(message)
                .referenceId(referenceId)
                .responseTime(responseTime)
                .responseType(responseType)
                .additionData(additionData)
                .build();
    }
}
