package com.hybrid9.pg.Lipanasi.dto.operator;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;

@Builder
@Value
public class OperatorResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1846550000L;
    String operatorName;
    String operatorPrefix;
    String operatorCountryCode;
    String operatorPhoneNumber;
    String message;
    String status;
}
