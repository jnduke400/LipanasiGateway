package com.hybrid9.pg.Lipanasi.component.mixxtqs;


import com.hybrid9.pg.Lipanasi.entities.payments.tqs.TigoTransactionResponse;
import com.hybrid9.pg.Lipanasi.services.pushussd.PushUssdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;

@Slf4j
@Component
public class TigoResponseParser {
    private final PushUssdService pushUssdService;

    public TigoResponseParser(PushUssdService pushUssdService) {
        this.pushUssdService = pushUssdService;
    }

    /**
     * Parse XML response from TigoPesa API and convert it to TigoTransactionResponse entity
     * @param xmlResponse Raw XML response from TigoPesa API
     * @param pushUssdId ID of the related PushUssd transaction
     * @return TigoTransactionResponse entity with parsed data
     * @throws TigoParserException if parsing fails
     */
    public TigoTransactionResponse parseResponse(String xmlResponse, Long pushUssdId) {
        try {
            // Create XML document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entity processing to prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));
            doc.getDocumentElement().normalize();

            // Get root element
            Element commandElement = doc.getDocumentElement();

            // Build response entity based on the actual XML structure
            return TigoTransactionResponse.builder()
                    .type(getElementValue(commandElement, "TYPE"))
                    .resultCode(getElementValue(commandElement, "RESULTCODE"))
                    .resultDesc(getElementValue(commandElement, "RESULTDESC"))
                    .transactionId(getElementValue(commandElement, "TXNID"))
                    .transactionStatus(getElementValue(commandElement, "TXNSTATUS"))
                    .externalRefId(getElementValue(commandElement, "EXTERNALREFID"))
                    .referenceId(getElementValue(commandElement, "MSISDN")) // This matches your XML structure
                    .responseDate(LocalDateTime.now())
                    .rawResponse(xmlResponse)
                    .pushUssd(this.pushUssdService.findPushUssdById(pushUssdId).orElseThrow())
                    .build();

        } catch (Exception e) {
            throw new TigoParserException("Failed to parse TigoPesa response: " + e.getMessage(), e);
        }
    }

    /**
     * Safely get element value from XML, returning null if element doesn't exist
     */
    private String getElementValue(Element parent, String tagName) {
        Node node = parent.getElementsByTagName(tagName).item(0);
        if (node == null) {
            return null;
        }
        return node.getTextContent().trim();
    }

    /**
     * Validate response for critical fields and expected format
     * @param response Parsed response entity
     * @throws TigoParserException if validation fails
     */
    public void validateResponse(TigoTransactionResponse response) {
        // Only check for RESULTCODE as it's the most critical field
        // and it's present in both success and error responses
        if (response.getResultCode() == null) {
            throw new TigoParserException("Missing required field: RESULTCODE");
        }

        // Validate result code format - updated to match your examples
        if (!response.getResultCode().matches("^[0-9]+$") &&
                !response.getResultCode().matches("^GetSOError\\d{3}$")) {
            throw new TigoParserException("Invalid RESULTCODE format: " + response.getResultCode());
        }

        // No need to validate MSISDN/referenceId as it might be empty in error responses
    }

    /**
     * Service method to handle the complete response processing
     */
    public TigoTransactionResponse processResponse(String xmlResponse, Long pushUssdId) {
        // Parse response
        TigoTransactionResponse response = parseResponse(xmlResponse, pushUssdId);

        // Validate parsed response
        validateResponse(response);

        // Log response for debugging if needed
        logResponse(response);

        return response;
    }

    /**
     * Helper method to log response details for debugging
     */
    private void logResponse(TigoTransactionResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("Processed TigoPesa response: " +
                            "ResultCode={}, " +
                            "ResultDesc={}, " +
                            "TransactionStatus={}, " +
                            "TransactionId={}, " +
                            "ExternalRefId={}",
                    response.getResultCode(),
                    response.getResultDesc(),
                    response.getTransactionStatus(),
                    response.getTransactionId(),
                    response.getExternalRefId());
        }
    }

    /**
     * Custom exception for parsing errors
     */
    public static class TigoParserException extends RuntimeException {
        public TigoParserException(String message) {
            super(message);
        }

        public TigoParserException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}