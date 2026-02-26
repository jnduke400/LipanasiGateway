package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HaloPesaConfig extends MobileNetworkConfig {
    private String spId;
    private String username;
    private String password;
    private String beneficiaryAccountId;
    private String businessNumber;
    private String secretKey;
    private String merchantCode;
    // for tqs service
    private String tqsUrl;
}
