package com.hybrid9.pg.Lipanasi.models.pgmodels.vendorx;

import com.hybrid9.pg.Lipanasi.models.pgmodels.commissions.CommissionConfig;
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
public class VendorManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1000166151L;
    private String vendorId;
    private String vendorName;
    private String vendorCode;
    private String vendorExternalId;
    private String vendorAccountNumber;
    private String vendorHasCommission;
    private String vendorKey;
    private String vendorStatus;
    private String vendorHasVat;
    private String vendorVatType;
    private String vendorCharges;

    // Service Related Configurations
    private boolean vendorBundle;
    private boolean vendorC2b;
    private boolean vendorAirtime;
    private boolean vendorB2c;

    private String vendorCallbackUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;


    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

}
