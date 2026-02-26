package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MixxByYasConfig extends MobileNetworkConfig {
    private String tokenUrl;
    private String billerMsisdn;
    private String username;
    private String password;
    private String tqsApiUrl;
    private String tqsPassword;
}
