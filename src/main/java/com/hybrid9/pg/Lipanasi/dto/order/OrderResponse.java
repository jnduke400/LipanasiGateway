package com.hybrid9.pg.Lipanasi.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String status;
    private String message;
    private String errorCode;
    private boolean successful;
}
