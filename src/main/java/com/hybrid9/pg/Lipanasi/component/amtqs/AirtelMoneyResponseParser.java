package com.hybrid9.pg.Lipanasi.component.amtqs;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hybrid9.pg.Lipanasi.dto.airtelmoney.tqs.AirtelMoneyResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
@Component
public class AirtelMoneyResponseParser {

    private static final Logger logger = LoggerFactory.getLogger(AirtelMoneyResponseParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses the Airtel Money TQS API response string into a DTO
     *
     * @param jsonResponse The JSON response string from Airtel Money TQS API
     * @return AirtelMoneyResponseDTO object
     * @throws IOException if parsing fails
     */
    public static AirtelMoneyResponseDTO parseResponseToDTO(String jsonResponse) throws IOException {
        try {
            return objectMapper.readValue(jsonResponse, AirtelMoneyResponseDTO.class);
        } catch (IOException e) {
            logger.error("Error parsing Airtel Money response to DTO: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extracts key information from the Airtel Money Response DTO
     *
     * @param responseDTO The parsed DTO
     * @return Map containing extracted values from the response
     */
    public static Map<String, String> extractInfoFromDTO(AirtelMoneyResponseDTO responseDTO) {
        Map<String, String> result = new HashMap<>();

        if (responseDTO == null) {
            result.put("error", "Response DTO is null");
            return result;
        }

        try {
            // Extract status information
            if (responseDTO.getStatus() != null) {
                result.put("statusCode", responseDTO.getStatus().getCode());
                result.put("statusMessage", responseDTO.getStatus().getMessage());
                result.put("responseCode", responseDTO.getStatus().getResponseCode());
                result.put("resultCode", responseDTO.getStatus().getResultCode());
                result.put("success", String.valueOf(responseDTO.getStatus().getSuccess()));
            }

            // Extract transaction information
            if (responseDTO.getData() != null && responseDTO.getData().getTransaction() != null) {
                AirtelMoneyResponseDTO.TransactionDTO transaction = responseDTO.getData().getTransaction();

                result.put("transactionStatus", transaction.getStatus());
                result.put("message", transaction.getMessage());
                result.put("transactionId", transaction.getId());

                // For successful transactions, extract Airtel Money ID
                if ("TS".equals(transaction.getStatus()) && transaction.getAirtelMoneyId() != null) {
                    result.put("airtelMoneyId", transaction.getAirtelMoneyId());
                    result.put("receiptNumber", transaction.getAirtelMoneyId());
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting information from response DTO: {}", e.getMessage());
            result.put("error", "Failed to extract information: " + e.getMessage());
        }

        return result;
    }

    /**
     * Combined method to parse response string and extract information
     *
     * @param jsonResponse The JSON response string from Airtel Money TQS API
     * @return Map containing extracted values from the response
     */
    public static Map<String, String> parseResponse(String jsonResponse) {
        try {
            AirtelMoneyResponseDTO responseDTO = parseResponseToDTO(jsonResponse);
            return extractInfoFromDTO(responseDTO);
        } catch (IOException e) {
            Map<String, String> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to parse response: " + e.getMessage());
            return errorResult;
        }
    }
}
