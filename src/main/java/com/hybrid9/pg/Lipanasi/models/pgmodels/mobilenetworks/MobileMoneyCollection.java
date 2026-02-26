package com.hybrid9.pg.Lipanasi.models.pgmodels.mobilenetworks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileMoneyCollection {
    private String credential;
    private String mno;
    private String status;

}
