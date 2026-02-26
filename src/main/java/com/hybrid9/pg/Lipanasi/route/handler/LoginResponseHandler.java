package com.hybrid9.pg.Lipanasi.route.handler;

import com.hybrid9.pg.Lipanasi.dto.mpesa.LoginResponse;
import org.apache.camel.Exchange;
import org.xml.sax.InputSource;

import javax.annotation.processing.Processor;
import javax.security.auth.login.LoginException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public class LoginResponseHandler {
    private static final String INVALID_CREDENTIALS_MSG = "Invalid Credentials";
    private static final String INVALID_PACKET_MSG = "Invalid Packet";


    public static void process(Exchange exchange) throws Exception {
        String response = exchange.getMessage().getBody(String.class);

        // Create XPath evaluator
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        InputSource source = new InputSource(new StringReader(response));

        // Extract response code and session ID
        String code = xpath.evaluate("//eventInfo/code", source);

        // Reset the source for next evaluation
        source = new InputSource(new StringReader(response));
        String sessionId = xpath.evaluate("//response/dataItem[name='SessionId']/value", source);

        if (response.contains(INVALID_PACKET_MSG)) {
            handleInvalidPacket(exchange);
        } else if (sessionId != null && sessionId.contains(INVALID_CREDENTIALS_MSG)) {
            handleInvalidCredentials(exchange);
        } else if ("3".equals(code) && sessionId != null && !sessionId.isEmpty()) {
            handleSuccessfulLogin(exchange, sessionId);
        } else {
            handleUnexpectedResponse(exchange, response);
        }
    }

    private static void handleInvalidPacket(Exchange exchange) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(false);
        response.setResponseCode("-1");
        response.setResponseDesc("Invalid Packet");
        response.setErrorDetails("The login request packet format is invalid");

        exchange.getMessage().setHeader("LoginStatus", "FAILED");
        exchange.getMessage().setBody(response);
        try {
            throw new LoginException("Invalid login packet format");
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleInvalidCredentials(Exchange exchange) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(false);
        response.setResponseCode("-1");
        response.setResponseDesc("Invalid Credentials");
        response.setErrorDetails("The provided username or password is incorrect");

        exchange.getMessage().setHeader("LoginStatus", "FAILED");
        exchange.getMessage().setBody(response);
        try {
            throw new LoginException("Invalid credentials provided");
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleSuccessfulLogin(Exchange exchange, String sessionId) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(true);
        response.setResponseCode("0");
        response.setResponseDesc("Success");
        response.setSessionId(sessionId);

        exchange.getMessage().setHeader("LoginStatus", "SUCCESS");
        exchange.getMessage().setHeader("SessionId", sessionId);
        exchange.getMessage().setBody(response);
    }

    private static void handleUnexpectedResponse(Exchange exchange, String responseBody) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(false);
        response.setResponseCode("-1");
        response.setResponseDesc("Unexpected Response");
        response.setErrorDetails("Received unexpected response: " + responseBody);

        exchange.getMessage().setHeader("LoginStatus", "FAILED");
        exchange.getMessage().setBody(response);
        try {
            throw new LoginException("Unexpected login response received");
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }
}
