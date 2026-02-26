package com.hybrid9.pg.Lipanasi.dto.airtelmoney.tqs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Root DTO for Airtel Money TQS API Response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirtelMoneyResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("data")
    private DataDTO data;

    @JsonProperty("status")
    private StatusDTO status;

    // Getters and setters
    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public StatusDTO getStatus() {
        return status;
    }

    public void setStatus(StatusDTO status) {
        this.status = status;
    }

    /**
     * Data section of the Airtel Money response
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("transaction")
        private TransactionDTO transaction;

        // Getters and setters
        public TransactionDTO getTransaction() {
            return transaction;
        }

        public void setTransaction(TransactionDTO transaction) {
            this.transaction = transaction;
        }
    }

    /**
     * Transaction details in the Airtel Money response
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransactionDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("airtel_money_id")
        private String airtelMoneyId;

        @JsonProperty("id")
        private String id;

        @JsonProperty("message")
        private String message;

        @JsonProperty("status")
        private String status;

        // Getters and setters
        public String getAirtelMoneyId() {
            return airtelMoneyId;
        }

        public void setAirtelMoneyId(String airtelMoneyId) {
            this.airtelMoneyId = airtelMoneyId;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * Status section of the Airtel Money response
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("result_code")
        private String resultCode;

        @JsonProperty("response_code")
        private String responseCode;

        @JsonProperty("success")
        private Boolean success;

        // Getters and setters
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getResultCode() {
            return resultCode;
        }

        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }

        public String getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(String responseCode) {
            this.responseCode = responseCode;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AirtelMoneyResponseDTO{");

        if (status != null) {
            sb.append("status=").append(status.getMessage()).append(", ");
            sb.append("code=").append(status.getCode()).append(", ");
            sb.append("success=").append(status.getSuccess());
        }

        if (data != null && data.getTransaction() != null) {
            sb.append(", transaction status=").append(data.getTransaction().getStatus()).append(", ");
            sb.append("message=").append(data.getTransaction().getMessage());

            if (data.getTransaction().getAirtelMoneyId() != null) {
                sb.append(", airtelMoneyId=").append(data.getTransaction().getAirtelMoneyId());
            }
        }

        sb.append('}');
        return sb.toString();
    }
}
