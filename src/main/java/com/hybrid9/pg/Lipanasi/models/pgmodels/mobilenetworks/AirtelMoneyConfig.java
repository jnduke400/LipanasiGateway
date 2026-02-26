package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AirtelMoneyConfig extends MobileNetworkConfig {
    private String tokenUrl;
    private String baseUrl;
    private String country;
    private String currency;
    private String clientId;
    private String clientSecret;
}
