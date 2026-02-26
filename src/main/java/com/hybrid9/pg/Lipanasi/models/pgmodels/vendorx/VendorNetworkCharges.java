package com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorNetworkCharges implements Serializable {
    @Serial
    private static final long serialVersionUID = 1987235750L;

    private Long id;
    private Long merchantId;
    private String mno;
    private String chargeType;
    private Double rate;
    private Boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;


    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
