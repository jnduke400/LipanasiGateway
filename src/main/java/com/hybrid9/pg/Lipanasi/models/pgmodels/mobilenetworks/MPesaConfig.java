package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MPesaConfig extends MobileNetworkConfig {
    private String tokenApiUrl;
    private String country;
    private String username;
    private String password;
    private String tokenEventId;
    private String requestEventId;
    private String businessName;
    private String businessNumber;
    // Paybill specific configuration
    private String paybillApiBaseUrl;
    private String paybillCallbackUrl;
    private String paybillSpId;
    private String paybillSpPassword;
}
