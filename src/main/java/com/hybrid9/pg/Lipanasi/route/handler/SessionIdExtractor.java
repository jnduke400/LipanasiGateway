package com.hybrid9.pg.Lipanasi.route.handler;

import com.hybrid9.pg.Lipanasi.dto.mpesa.LoginResponse;
import org.apache.camel.Exchange;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public class SessionIdExtractor {
    private static final String SESSION_ID_XPATH = "//response/dataItem[name='SessionId']/value/text()";

    public static void process(Exchange exchange,LoginResponse loginResponse) throws Exception {
        String response = exchange.getMessage().getBody(String.class);

        // Create XPath evaluator
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();

        // Compile XPath expression
        XPathExpression expression = xpath.compile(SESSION_ID_XPATH);

        // Extract SessionId
        String sessionId = loginResponse.getSessionId();//expression.evaluate(new InputSource(new StringReader(response)));

        if (sessionId != null && !sessionId.isEmpty() && !"Invalid Credentials".equals(sessionId)) {
            // Store SessionId in exchange header and body
            exchange.getMessage().setHeader("SessionId", sessionId);
            loginResponse.setSessionId(sessionId);
            exchange.getMessage().setBody(loginResponse);

            // Log successful extraction
            exchange.getMessage().setHeader("CamelLogMessage",
                    String.format("Successfully extracted SessionId: %s", sessionId));
        } else {
            // Handle failure
            exchange.getMessage().setBody(loginResponse);

            // Log failure
            exchange.getMessage().setHeader("CamelLogMessage",
                    "Failed to extract valid SessionId from response");
        }
    }
}
