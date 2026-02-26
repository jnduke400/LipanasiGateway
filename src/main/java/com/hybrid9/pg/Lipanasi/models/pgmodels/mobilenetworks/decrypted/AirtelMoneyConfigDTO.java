package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks.decrypted;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirtelMoneyConfigDTO {
    private String auth_url;
    private String check_url;
    private String client_id;
    private String client_secret;
    private String push_url;
    private String refund_url;
    private String status;
}
