package com.hybrid9.pg.Lipanasi.component.halotelcollection.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonHeaderDTO {
    private String spId;
    private String spPassword;
    private String timestamp;
    private String merchantCode;  // Optional for second payload
}
