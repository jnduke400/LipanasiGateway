package com.hybrid9.pg.Lipanasi.enums;

public enum HaloPesaResponseCode {

    SUCCESS("0", "Transaction successful"),
    PARSE_ERROR("9001", "Cannot parse the request"),
    IP_NOT_WHITELISTED("9002", "The remote IP was not whitelisted"),
    WRONG_CREDENTIAL("9003", "Wrong credential"),
    DUPLICATE_REQUEST("9004", "Duplicated Request"),
    INVALID_REFERENCE("9005", "The reference number is not correct"),
    TIME_DIFFERENCE_TOO_LARGE("9006", "The difference between the request time and current time is too large"),
    INVALID_CHECKSUM("9007", "Invalid checksum"),
    FUNCTION_CODE_NOT_REGISTERED("9008", "The function code was not registered"),
    INSUFFICIENT_BALANCE("9009", "The balance of customer is not enough for the payment"),
    INVALID_BENEFICIARY_ACCOUNT("9010", "The beneficiary account is invalid"),
    TRANSACTION_ID_NOT_FOUND("9011", "Cannot find the provided transaction ID"),
    GENERAL_SYSTEM_ERROR("9012", "General System Error"),
    UNKNOWN("UNKNOWN", "Unknown response code");

    private final String code;
    private final String message;

    HaloPesaResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static HaloPesaResponseCode fromCode(String code) {
        for (HaloPesaResponseCode rc : values()) {
            if (rc.code.equals(code)) {
                return rc;
            }
        }
        return UNKNOWN;
    }
}

