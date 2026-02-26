package com.hybrid9.pg.Lipanasi.dto.order;

import lombok.Data;

@Data
public class SessionInitRequest {
    private String userId;
    private String merchantId;
}
