package com.hybrid9.pg.Lipanasi.entities.vendorx;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.hybrid9.pg.Lipanasi.enums.CommissionFrequency;
import com.hybrid9.pg.Lipanasi.enums.VendorStatus;
import com.hybrid9.pg.Lipanasi.entities.AppUser;
import com.hybrid9.pg.Lipanasi.models.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2b_vendors",
        indexes = {
                @Index(name = "idx_vendor_external_id", columnList = "vendor_external_id"),
                @Index(name = "idx_vendor_code", columnList = "vendors_code"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_has_charges", columnList = "has_charges")
                /*@Index(name = "idx_is_airtime_allowed", columnList = "is_airtime_allowed"),
                @Index(name = "idx_is_c2b_allowed", columnList = "is_c2b_allowed"),
                @Index(name = "idx_is_bundle_allowed", columnList = "is_bundle_allowed"),
                @Index(name = "idx_is_b2c_allowed", columnList = "is_b2c_allowed")*/
        }
)
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
public class VendorDetails extends Auditable<String> {
    @Column(name = "institute_name")
    private String vendorName;
    private String vendorEmail;
    @Column(name = "vendors_code")
    private String vendorCode;
    @Column(name = "vendor_external_id")
    private String vendorExternalId;
    @Column(name = "api_key", length = 500)
    private String apiKey;
    private String taxId;
    /*@ManyToOne
    @JoinColumn(name = "commision_id")
    private Commission commission;*/
    private String settlementFrequency;
    private String billNumber;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address address;
    @ManyToOne
    @JoinColumn(name = "business_id")
    private Business business;
    @Column(name = "has_commission")
    private String hasCommission;
    @Column(name = "has_vat")
    private String hasVat;
    @Column(name = "vat_type")
    private String vatType;

    // Service Related Configurations
    @Column(name = "is_airtime_allowed")
    private Boolean isAirtimeAllowed;
    @Column(name = "is_c2b_allowed")
    private Boolean isC2bAllowed;
    @Column(name = "is_bundle_allowed")
    private Boolean isBundleAllowed;
    @Column(name = "is_b2c_allowed")
    private Boolean isB2cAllowed;

    @Builder.Default
    @Column(name = "charges")
    private float charges = 0f;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private VendorStatus status = VendorStatus.ACTIVE;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private CommissionFrequency commissionFrequency = CommissionFrequency.PER_TRANSACTION;
    private String callbackUrl;
}
