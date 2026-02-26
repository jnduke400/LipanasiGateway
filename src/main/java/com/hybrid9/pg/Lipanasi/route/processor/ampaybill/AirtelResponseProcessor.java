package com.hybrid9.pg.Lipanasi.route.processor.ampaybill;


import com.hybrid9.pg.Lipanasi.component.amtqs.AirtelMoneyResponseParser;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.tqs.AirtelMoneyResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AirtelResponseProcessor {

    private AirtelMoneyResponseParser airtelMoneyResponseParser;

    public AirtelResponseProcessor(AirtelMoneyResponseParser airtelMoneyResponseParser) {
        this.airtelMoneyResponseParser = airtelMoneyResponseParser;
    }

    public void process(Exchange exchange) throws Exception {
        String responseBody = exchange.getIn().getBody(String.class);

        try {
            // Parse the response using our utility
            AirtelMoneyResponseDTO responseDTO = AirtelMoneyResponseParser.parseResponseToDTO(responseBody);

            // Log the response for debugging
            log.debug("Parsed Airtel Money response: {}", responseDTO);

            // Extract relevant information
            Map<String, String> extractedInfo = AirtelMoneyResponseParser.extractInfoFromDTO(responseDTO);

            // Set the extracted information as exchange headers for use in route
            for (Map.Entry<String, String> entry : extractedInfo.entrySet()) {
                exchange.getIn().setHeader(entry.getKey(), entry.getValue());
            }

            // Determine transaction status and set a routing header
            String transactionStatus = extractedInfo.get("transactionStatus");
            if (transactionStatus != null) {
                exchange.getIn().setHeader("airtelTransactionStatus", transactionStatus);

                // Set the collection status based on transaction status
                String collectionStatus;
                switch (transactionStatus) {
                    case "TS":
                        collectionStatus = "COLLECTED";
                        break;
                    case "TF":
                    case "TE":
                        collectionStatus = "FAILED";
                        break;
                    case "TIP":
                        collectionStatus = "NEW";
                        break;
                    default:
                        collectionStatus = "PENDING";
                        break;
                }
                exchange.getIn().setHeader("newCollectionStatus", collectionStatus);
            }

            // Set any error message if present
            if (extractedInfo.containsKey("error")) {
                exchange.getIn().setHeader("errorMessage", extractedInfo.get("error"));
                exchange.setProperty("processingError", true);
            } else {
                exchange.setProperty("processingError", false);
            }

        } catch (Exception e) {
            log.error("Error processing Airtel Money response: {}", e.getMessage());
            exchange.getIn().setHeader("errorMessage", "Error processing response: " + e.getMessage());
            exchange.setProperty("processingError", true);
        }
    }
}


